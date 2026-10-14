package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.Instrumento;
import br.edu.iffar.avalia.avaliacao.dto.RespostaAvaliacaoDTO.*;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.*;

@ApplicationScoped
@Transactional
public class ResponderAvaliacaoService {
    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    @Inject
    private AvaliacaoService instrumentos;

    private String codigoRespondente(UsuarioDTO usuario) {
        if (!usuario.getPermissoes().contains("AVALIACAO_RESPONDER"))
            throw new SecurityException("Seu perfil não tem permissão para responder avaliações.");
        return switch (usuario.getPapelAtivo()) {
            case "DISCENTE" -> "ALUNO";
            case "DOCENTE" -> "DOCENTE";
            case "TAE", "GESTAO", "CPA", "ADMIN" -> "SERVIDOR";
            default -> throw new SecurityException("Perfil sem tipo de respondente definido.");
        };
    }

    @SuppressWarnings("unchecked")
    public List<Disponivel> listar(UsuarioDTO usuario) {
        String codigo = codigoRespondente(usuario);
        List<Disponivel> disponiveis = new ArrayList<>();
        List<Object[]> linhas = em.createNativeQuery("select cast(i.instrumento_avaliativo_id as text), i.titulo, i.descricao, i.modulo, " +
                        "cast(alvo.entidade_id as text), alvo.rotulo, exists (" +
                        "select 1 from avaliacao.participacao p join avaliacao.entidade er " +
                        "on er.entidade_id = p.entidade_respondente_id " +
                        "where p.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                        "and p.entidade_avaliada_id = alvo.entidade_id " +
                        "and er.tipo_entidade_id = ir.tipo_entidade_respondente_id " +
                        "and er.referencia_externa = ?2) " +
                        "from avaliacao.instrumento_avaliativo i " +
                        "join avaliacao.instrumento_respondente ir on ir.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                        "join avaliacao.tipo_entidade t on t.tipo_entidade_id = ir.tipo_entidade_respondente_id " +
                        "join avaliacao.instrumento_entidade_avaliada ia on ia.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                        "join avaliacao.entidade alvo on alvo.entidade_id = ia.entidade_id " +
                        "where i.status = 'PUBLICADO' and now() between i.inicio_aplicacao and i.fim_aplicacao " +
                        "and t.codigo = ?1 and (ir.tipo_vinculo is null or exists (" +
                        "select 1 from avaliacao.entidade er join avaliacao.entidade_vinculo v " +
                        "on v.entidade_origem_id = er.entidade_id " +
                        "where er.tipo_entidade_id = t.tipo_entidade_id and er.referencia_externa = ?2 " +
                        "and v.entidade_destino_id = alvo.entidade_id and v.tipo_vinculo = ir.tipo_vinculo)) " +
                        "order by i.titulo, alvo.rotulo")
                .setParameter(1, codigo).setParameter(2, usuario.getCpf()).getResultList();
        for (Object[] linha : linhas) {
            Disponivel d = new Disponivel();
            d.instrumentoId = (String) linha[0];
            d.titulo = (String) linha[1];
            d.descricao = (String) linha[2];
            d.modulo = (String) linha[3];
            d.alvoId = (String) linha[4];
            d.alvoNome = (String) linha[5];
            d.respondido = (Boolean) linha[6];
            disponiveis.add(d);
        }
        return disponiveis;
    }

    private Disponivel elegivel(UsuarioDTO usuario, String instrumentoId, String alvoId) {
        UUID.fromString(instrumentoId);
        UUID.fromString(alvoId);
        return listar(usuario).stream()
                .filter(d -> d.instrumentoId.equals(instrumentoId) && d.alvoId.equals(alvoId))
                .findFirst().orElseThrow(() -> new SecurityException("Avaliação indisponível para este perfil."));
    }

    public Instrumento buscar(UsuarioDTO usuario, String instrumentoId, String alvoId) {
        elegivel(usuario, instrumentoId, alvoId);
        return instrumentos.buscarInstrumento(instrumentoId);
    }

    @SuppressWarnings("unchecked")
    public void enviar(UsuarioDTO usuario, String instrumentoId, Envio envio) {
        if (envio == null || envio.alvoId == null) throw new IllegalArgumentException("Selecione a avaliação.");
        Disponivel disponivel = elegivel(usuario, instrumentoId, envio.alvoId);
        if (disponivel.respondido) throw new IllegalArgumentException("Você já respondeu esta avaliação.");
        if (envio.respostas == null) throw new IllegalArgumentException("Respostas não informadas.");

        Map<String, Item> respostas = new HashMap<>();
        for (Item item : envio.respostas) {
            if (item == null || item.sessaoQuestaoId == null || respostas.putIfAbsent(item.sessaoQuestaoId, item) != null)
                throw new IllegalArgumentException("Questão repetida ou inválida.");
        }
        List<Object[]> questoes = em.createNativeQuery("select cast(sq.sessao_questao_id as text), q.tipo, " +
                        "cast(q.conjunto_opcao_id as text), sq.obrigatoria " +
                        "from avaliacao.sessao_questao sq " +
                        "join avaliacao.instrumento_sessao s on s.instrumento_sessao_id = sq.instrumento_sessao_id " +
                        "join avaliacao.questao q on q.questao_id = sq.questao_id " +
                        "where s.instrumento_avaliativo_id = cast(?1 as uuid) and s.tipo = 'SESSAO'")
                .setParameter(1, instrumentoId).getResultList();
        if (questoes.isEmpty()) throw new IllegalArgumentException("Instrumento sem questões.");
        Set<String> idsValidos = new HashSet<>();
        for (Object[] questao : questoes) {
            String id = (String) questao[0];
            String tipo = (String) questao[1];
            idsValidos.add(id);
            Item item = respostas.get(id);
            List<String> opcoes = item == null || item.opcaoIds == null ? List.of() : item.opcaoIds;
            String texto = item == null ? null : item.texto;
            boolean preenchida = !opcoes.isEmpty() || (texto != null && !texto.isBlank());
            if ((Boolean) questao[3] && !preenchida)
                throw new IllegalArgumentException("Responda todas as questões obrigatórias.");
            if (!preenchida) continue;
            if ("DESCRITIVA".equals(tipo)) {
                if (!opcoes.isEmpty() || texto == null || texto.isBlank())
                    throw new IllegalArgumentException("Resposta descritiva inválida.");
            } else {
                if (texto != null && !texto.isBlank() || opcoes.isEmpty()
                        || (!"MULTIPLA_ESCOLHA".equals(tipo) && opcoes.size() != 1)
                        || new HashSet<>(opcoes).size() != opcoes.size())
                    throw new IllegalArgumentException("Opções de resposta inválidas.");
                for (String opcao : opcoes) {
                    UUID.fromString(opcao);
                    Number existe = (Number) em.createNativeQuery("select count(*) from avaliacao.opcao_questao " +
                                    "where opcao_questao_id = cast(?1 as uuid) and conjunto_opcao_id = cast(?2 as uuid)")
                            .setParameter(1, opcao).setParameter(2, questao[2]).getSingleResult();
                    if (existe.intValue() != 1) throw new IllegalArgumentException("Opção fora do instrumento.");
                }
            }
        }
        if (!idsValidos.containsAll(respostas.keySet()))
            throw new IllegalArgumentException("Questão fora do instrumento.");

        String tipoCodigo = codigoRespondente(usuario);
        String respondenteId = (String) em.createNativeQuery("insert into avaliacao.entidade " +
                        "(tipo_entidade_id, referencia_externa, rotulo) " +
                        "select tipo_entidade_id, ?1, ?2 from avaliacao.tipo_entidade where codigo = ?3 " +
                        "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo " +
                        "returning cast(entidade_id as text)")
                .setParameter(1, usuario.getCpf()).setParameter(2, usuario.getNome())
                .setParameter(3, tipoCodigo).getSingleResult();
        String respostaId = UUID.randomUUID().toString();
        em.createNativeQuery("insert into avaliacao.resposta_instrumento " +
                        "(resposta_instrumento_id, instrumento_avaliativo_id, entidade_avaliada_id, entidade_respondente_id) " +
                        "values (cast(?1 as uuid), cast(?2 as uuid), cast(?3 as uuid), cast(?4 as uuid))")
                .setParameter(1, respostaId).setParameter(2, instrumentoId)
                .setParameter(3, envio.alvoId).setParameter(4, respondenteId).executeUpdate();
        for (Object[] questao : questoes) {
            Item item = respostas.get((String) questao[0]);
            if (item == null) continue;
            if ("DESCRITIVA".equals(questao[1])) {
                if (item.texto == null || item.texto.isBlank()) continue;
                inserirResposta(respostaId, (String) questao[0], null, item.texto.trim());
            } else if (item.opcaoIds != null) {
                for (String opcao : item.opcaoIds)
                    inserirResposta(respostaId, (String) questao[0], opcao, null);
            }
        }
        em.createNativeQuery("insert into avaliacao.participacao " +
                        "(instrumento_avaliativo_id, entidade_avaliada_id, entidade_respondente_id) " +
                        "values (cast(?1 as uuid), cast(?2 as uuid), cast(?3 as uuid))")
                .setParameter(1, instrumentoId).setParameter(2, envio.alvoId)
                .setParameter(3, respondenteId).executeUpdate();
        em.createNativeQuery("update avaliacao.resposta_instrumento set status = 'ENVIADA', " +
                        "data_envio = current_date, entidade_respondente_id = " +
                        "case when (select anonimo from avaliacao.instrumento_avaliativo " +
                        "where instrumento_avaliativo_id = cast(?2 as uuid)) then null else cast(?3 as uuid) end " +
                        "where resposta_instrumento_id = cast(?1 as uuid)")
                .setParameter(1, respostaId).setParameter(2, instrumentoId)
                .setParameter(3, respondenteId).executeUpdate();
    }

    private void inserirResposta(String respostaId, String questaoId, String opcaoId, String texto) {
        em.createNativeQuery("insert into avaliacao.resposta_questao " +
                        "(resposta_instrumento_id, sessao_questao_id, opcao_questao_id, texto) " +
                        "values (cast(?1 as uuid), cast(?2 as uuid), cast(?3 as uuid), cast(?4 as text))")
                .setParameter(1, respostaId).setParameter(2, questaoId)
                .setParameter(3, opcaoId).setParameter(4, texto).executeUpdate();
    }
}
