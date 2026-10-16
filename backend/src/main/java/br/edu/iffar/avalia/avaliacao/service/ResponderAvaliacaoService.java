package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.Instrumento;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.dto.RespostaAvaliacaoDTO.*;
import br.edu.iffar.avalia.avaliacao.dao.AvaliacaoDao;
import br.edu.iffar.avalia.avaliacao.dao.ResponderAvaliacaoDao;
import br.edu.iffar.avalia.avaliacao.model.Entidade;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.Participacao;
import br.edu.iffar.avalia.avaliacao.model.RespostaInstrumento;
import br.edu.iffar.avalia.avaliacao.model.RespostaQuestao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.*;
import java.time.LocalDate;

@ApplicationScoped
@Transactional
public class ResponderAvaliacaoService {
    @Inject
    private AvaliacaoDao dao;

    @Inject
    private AvaliacaoService instrumentos;

    @Inject
    private ResponderAvaliacaoDao respostaDao;

    private List<String> codigosRespondente(UsuarioDTO usuario) {
        if (!usuario.getPermissoes().contains("AVALIACAO_RESPONDER"))
            throw new SecurityException("Seu perfil não tem permissão para responder avaliações.");
        Set<String> codigos = new LinkedHashSet<>();
        List<String> papeis = usuario.getPapeis();
        if (papeis.contains("DISCENTE")) codigos.add("ALUNO");
        if (papeis.contains("DOCENTE")) codigos.add("DOCENTE");
        if (papeis.stream().anyMatch(p -> List.of("TAE", "CPA", "ADMIN").contains(p))) codigos.add("SERVIDOR");
        return List.copyOf(codigos);
    }

    public List<Disponivel> listar(UsuarioDTO usuario) {
        List<String> codigos = codigosRespondente(usuario);
        Map<String, Disponivel> disponiveis = new LinkedHashMap<>();
        for (String codigo : codigos) {
            for (Object[] linha : respostaDao.listarGerais(codigo, usuario.getCpf())) {
                Disponivel d = new Disponivel();
                d.instrumentoId = (String) linha[0];
                d.titulo = (String) linha[1];
                d.descricao = (String) linha[2];
                d.alvoId = (String) linha[3];
                d.alvoNome = (String) linha[4];
                d.alvoTipoCodigo = (String) linha[5];
                d.respondido = (Boolean) linha[8];
                d.tipoRespondenteCodigo = codigo;
                adicionarDisponivel(disponiveis, d);
            }
            for (Disponivel d : listarAlvosDeTurma(usuario, codigo)) adicionarDisponivel(disponiveis, d);
        }
        for (Object[] linha : respostaDao.listarInstitucionais(usuario.getCpf())) {
            Disponivel d = new Disponivel();
            d.instrumentoId = (String) linha[0];
            d.titulo = (String) linha[1];
            d.descricao = (String) linha[2];
            d.alvoId = (String) linha[3];
            d.alvoNome = (String) linha[4];
            d.alvoTipoCodigo = (String) linha[5];
            d.respondido = (Boolean) linha[8];
            d.tipoRespondenteCodigo = (String) linha[9];
            adicionarDisponivel(disponiveis, d);
        }
        if (codigos.contains("ALUNO"))
            for (Disponivel d : listarDocentesDaTurma(usuario)) adicionarDisponivel(disponiveis, d);
        return new ArrayList<>(disponiveis.values());
    }

    private void adicionarDisponivel(Map<String, Disponivel> disponiveis, Disponivel d) {
        String chave = String.join("|", Objects.toString(d.instrumentoId, ""),
                Objects.toString(d.alvoId, ""), Objects.toString(d.contextoId, ""),
                Objects.toString(d.tipoRespondenteCodigo, ""));
        disponiveis.putIfAbsent(chave, d);
    }

    @SuppressWarnings("unchecked")
    private List<Disponivel> listarAlvosDeTurma(UsuarioDTO usuario, String codigo) {
        List<Disponivel> out = new ArrayList<>();
        List<Object[]> rows = respostaDao.listarAlvosDeTurma(usuario.getCpf(), codigo);
        for (Object[] row : rows) {
            Disponivel d = new Disponivel();
            d.instrumentoId = (String) row[0];
            d.titulo = (String) row[1];
            d.descricao = (String) row[2];
            d.alvoId = (String) row[3];
            d.alvoNome = (String) row[4];
            d.alvoTipoCodigo = (String) row[5];
            d.contextoId = (String) row[6];
            d.contextoNome = (String) row[7];
            d.respondido = (Boolean) row[8];
            d.tipoRespondenteCodigo = codigo;
            out.add(d);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Disponivel> listarDocentesDaTurma(UsuarioDTO usuario) {
        List<Disponivel> out = new ArrayList<>();
        List<Object[]> rows = respostaDao.listarDocentesDaTurma(usuario.getCpf());
        for (Object[] row : rows) {
            Disponivel d = new Disponivel();
            d.instrumentoId = (String) row[0];
            d.titulo = (String) row[1];
            d.descricao = (String) row[2];
            d.alvoId = (String) row[3];
            d.alvoNome = (String) row[4];
            d.alvoTipoCodigo = (String) row[5];
            d.contextoId = (String) row[6];
            d.contextoNome = (String) row[7];
            d.respondido = (Boolean) row[8];
            d.tipoRespondenteCodigo = "ALUNO";
            out.add(d);
        }
        return out;
    }

    private Disponivel elegivel(UsuarioDTO usuario, String instrumentoId, String alvoId, String contextoId) {
        UUID.fromString(instrumentoId);
        UUID.fromString(alvoId);
        return listar(usuario).stream()
                .filter(d -> d.instrumentoId.equals(instrumentoId) && d.alvoId.equals(alvoId)
                        && Objects.equals(d.contextoId, contextoId))
                .findFirst().orElseThrow(() -> new SecurityException("Avaliação indisponível para este perfil."));
    }

    public Instrumento buscar(UsuarioDTO usuario, String instrumentoId, String alvoId, String contextoId) {
        elegivel(usuario, instrumentoId, alvoId, contextoId);
        return instrumentos.buscarInstrumento(instrumentoId);
    }

    public List<ConjuntoOpcao> listarConjuntos(UsuarioDTO usuario, String instrumentoId, String alvoId, String contextoId) {
        Instrumento instrumento = buscar(usuario, instrumentoId, alvoId, contextoId);
        Set<String> ids = new HashSet<>();
        instrumento.sessoes.forEach(s -> s.questoes.forEach(q -> {
            if (q.conjuntoOpcaoId != null) ids.add(q.conjuntoOpcaoId);
        }));
        return instrumentos.listarConjuntos().stream()
                .filter(c -> ids.contains(c.id))
                .peek(c -> c.opcoes.forEach(o -> o.correta = false))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public void enviar(UsuarioDTO usuario, String instrumentoId, Envio envio) {
        if (envio == null || envio.alvoId == null) throw new IllegalArgumentException("Selecione a avaliação.");
        Disponivel disponivel = elegivel(usuario, instrumentoId, envio.alvoId, envio.contextoId);
        if (disponivel.respondido) throw new IllegalArgumentException("Você já respondeu esta avaliação.");
        if (envio.respostas == null) throw new IllegalArgumentException("Respostas não informadas.");

        Map<String, Item> respostas = new HashMap<>();
        for (Item item : envio.respostas) {
            if (item == null || item.sessaoQuestaoId == null || respostas.putIfAbsent(item.sessaoQuestaoId, item) != null)
                throw new IllegalArgumentException("Questão repetida ou inválida.");
        }
        List<Object[]> questoes = dao.questoesParaResposta(UUID.fromString(instrumentoId));
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
                    OpcaoQuestao entidadeOpcao = dao.buscar(OpcaoQuestao.class, UUID.fromString(opcao));
                    if (entidadeOpcao == null || !entidadeOpcao.getConjunto().getId().toString().equals(questao[2]))
                        throw new IllegalArgumentException("Opção fora do instrumento.");
                }
            }
        }
        if (!idsValidos.containsAll(respostas.keySet()))
            throw new IllegalArgumentException("Questão fora do instrumento.");

        String tipoCodigo = disponivel.tipoRespondenteCodigo != null
                ? disponivel.tipoRespondenteCodigo : codigosRespondente(usuario).stream().findFirst()
                    .orElseThrow(() -> new SecurityException("Perfil sem tipo de respondente autorizado."));
        UUID respondenteId = dao.buscarOuCriarRespondente(usuario.getCpf(), usuario.getNome(), tipoCodigo);
        InstrumentoAvaliativo instrumento = dao.buscarInstrumento(UUID.fromString(instrumentoId));
        Entidade alvo = dao.referencia(Entidade.class, UUID.fromString(envio.alvoId));
        Entidade respondente = dao.referencia(Entidade.class, respondenteId);
        Entidade contexto = envio.contextoId == null ? null : dao.referencia(Entidade.class, UUID.fromString(envio.contextoId));

        RespostaInstrumento resposta = new RespostaInstrumento();
        resposta.setInstrumento(instrumento);
        resposta.setAvaliada(alvo);
        resposta.setRespondente(respondente);
        resposta.setContexto(contexto);
        dao.persistir(resposta);
        dao.flush();
        for (Object[] questao : questoes) {
            Item item = respostas.get((String) questao[0]);
            if (item == null) continue;
            if ("DESCRITIVA".equals(questao[1])) {
                if (item.texto == null || item.texto.isBlank()) continue;
                inserirResposta(resposta, (String) questao[0], null, item.texto.trim());
            } else if (item.opcaoIds != null) {
                for (String opcao : item.opcaoIds)
                    inserirResposta(resposta, (String) questao[0], opcao, null);
            }
        }
        Participacao participacao = new Participacao();
        participacao.setInstrumento(instrumento);
        participacao.setAvaliada(alvo);
        participacao.setRespondente(respondente);
        participacao.setContexto(contexto);
        dao.persistir(participacao);
        dao.flush();

        resposta.setRespondente(instrumento.isAnonimo() ? null : respondente);
        resposta.setDataEnvio(LocalDate.now());
        resposta.setStatus("ENVIADA");
        dao.flush();
    }

    private void inserirResposta(RespostaInstrumento resposta, String questaoId, String opcaoId, String texto) {
        RespostaQuestao item = new RespostaQuestao();
        item.setResposta(resposta);
        item.setQuestao(dao.referencia(SessaoQuestao.class, UUID.fromString(questaoId)));
        item.setOpcao(opcaoId == null ? null : dao.referencia(OpcaoQuestao.class, UUID.fromString(opcaoId)));
        item.setTexto(texto);
        dao.persistir(item);
    }
}
