package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.academico.dao.PessoaDao;
import br.edu.iffar.avalia.avaliacao.dao.InstrumentoAvaliativoDAO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.InstrumentoAvaliativoDTO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.ConjuntoOpcaoDTO;
import br.edu.iffar.avalia.avaliacao.dto.RespostaAvaliacaoDTO.*;
import br.edu.iffar.avalia.avaliacao.dao.ResponderAvaliacaoDao;
import br.edu.iffar.avalia.avaliacao.model.Entidade;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.Participacao;
import br.edu.iffar.avalia.avaliacao.model.RespostaInstrumento;
import br.edu.iffar.avalia.avaliacao.model.RespostaQuestao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import br.edu.iffar.avalia.core.exception.ValidacaoException;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.*;
import java.time.LocalDate;

@ApplicationScoped
@Transactional
public class ResponderAvaliacaoService {
    @Inject
    private InstrumentoAvaliativoDAO instrumentoAvaliativoDAO;

    @Inject
    private AvaliacaoService instrumentos;

    @Inject
    private ResponderAvaliacaoDao respostaDao;

    @Inject
    private AuthService authService;
    @Inject
    private PessoaDao pessoaDao;

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

    public List<InstrumentoAvaliativoDTO> listar() {
        UsuarioDTO usuario = new UsuarioDTO();
        List<InstrumentoAvaliativo> listIA = this.instrumentoAvaliativoDAO.listAllForUser(pessoaDao.buscarPorCpf(this.authService.getCpf()));

        List<InstrumentoAvaliativoDTO> avaliativoDTOList = new ArrayList<>();
        for (InstrumentoAvaliativo ia :listIA) {
            avaliativoDTOList.add(new InstrumentoAvaliativoDTO().createResumo(ia));
        }
        return avaliativoDTOList;
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

    public InstrumentoAvaliativoDTO buscar(UsuarioDTO usuario, String instrumentoId) {
            InstrumentoAvaliativo inst = this.instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(instrumentoId));
            return new InstrumentoAvaliativoDTO().createDetalhe(inst);
    }

    public List<ConjuntoOpcaoDTO> listarConjuntos(UsuarioDTO usuario, String instrumentoId) {
        InstrumentoAvaliativoDTO instrumento = buscar(usuario, instrumentoId);
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
        if (envio == null || envio.alvoId == null) throw new ValidacaoException("Selecione a avaliação.");
//        Disponivel disponivel = elegivel(usuario, instrumentoId, envio.alvoId, envio.contextoId);
//        if (disponivel.respondido) throw new ValidacaoException("Você já respondeu esta avaliação.");
        if (envio.respostas == null) throw new ValidacaoException("Respostas não informadas.");

        Map<String, Item> respostas = new HashMap<>();
        for (Item item : envio.respostas) {
            if (item == null || item.sessaoQuestaoId == null || respostas.putIfAbsent(item.sessaoQuestaoId, item) != null)
                throw new ValidacaoException("Questão repetida ou inválida.");
        }
//        List<Object[]> questoes = instrumentoAvaliativoDAO.questoesParaResposta(UUID.fromString(instrumentoId));
//        if (questoes.isEmpty()) throw new ValidacaoException("Instrumento sem questões.");
//        Set<String> idsValidos = new HashSet<>();
//        for (Object[] questao : questoes) {
//            String id = (String) questao[0];
//            String tipo = (String) questao[1];
//            idsValidos.add(id);
//            Item item = respostas.get(id);
//            List<String> opcoes = item == null || item.opcaoIds == null ? List.of() : item.opcaoIds;
//            String texto = item == null ? null : item.texto;
//            boolean preenchida = !opcoes.isEmpty() || (texto != null && !texto.isBlank());
//            if ((Boolean) questao[3] && !preenchida)
//                throw new ValidacaoException("Responda todas as questões obrigatórias.");
//            if (!preenchida) continue;
//            if ("DESCRITIVA".equals(tipo)) {
//                if (!opcoes.isEmpty() || texto == null || texto.isBlank())
//                    throw new ValidacaoException("Resposta descritiva inválida.");
//            } else {
//                if (texto != null && !texto.isBlank() || opcoes.isEmpty()
//                        || (!"MULTIPLA_ESCOLHA".equals(tipo) && opcoes.size() != 1)
//                        || new HashSet<>(opcoes).size() != opcoes.size())
//                    throw new ValidacaoException("Opções de resposta inválidas.");
//                for (String opcao : opcoes) {
////                    OpcaoQuestao entidadeOpcao = instrumentoAvaliativoDAO.buscar(OpcaoQuestao.class, UUID.fromString(opcao));
////                    if (entidadeOpcao == null || !entidadeOpcao.getConjunto().getId().toString().equals(questao[2]))
//                        throw new ValidacaoException("Opção fora do instrumento.");
//                }
//            }
//    }
//        if (!idsValidos.containsAll(respostas.keySet()))
//            throw new ValidacaoException("Questão fora do instrumento.");
//
////        String tipoCodigo = disponivel.tipoRespondenteCodigo != null
////                ? disponivel.tipoRespondenteCodigo : codigosRespondente(usuario).stream().findFirst()
////                    .orElseThrow(() -> new SecurityException("Perfil sem tipo de respondente autorizado."));
////        UUID respondenteId = avaliacaoDao.buscarOuCriarRespondente(usuario.getCpf(), usuario.getNome(), tipoCodigo);
//        InstrumentoAvaliativo instrumento = instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(instrumentoId));
//        Entidade alvo = instrumentoAvaliativoDAO.referencia(Entidade.class, UUID.fromString(envio.alvoId));
////        Entidade respondente = avaliacaoDao.referencia(Entidade.class, respondenteId);
//        Entidade contexto = envio.contextoId == null ? null : instrumentoAvaliativoDAO.referencia(Entidade.class, UUID.fromString(envio.contextoId));
//
//        RespostaInstrumento resposta = new RespostaInstrumento();
//        resposta.setInstrumento(instrumento);
//        resposta.setAvaliada(alvo);
////        resposta.setRespondente(respondente);
//        resposta.setContexto(contexto);
//        instrumentoAvaliativoDAO.persistir(resposta);
//        instrumentoAvaliativoDAO.flush();
//        for (Object[] questao : questoes) {
//            Item item = respostas.get((String) questao[0]);
//            if (item == null) continue;
//            if ("DESCRITIVA".equals(questao[1])) {
//                if (item.texto == null || item.texto.isBlank()) continue;
//                inserirResposta(resposta, (String) questao[0], null, item.texto.trim());
//            } else if (item.opcaoIds != null) {
//                for (String opcao : item.opcaoIds)
//                    inserirResposta(resposta, (String) questao[0], opcao, null);
//            }
//        }
//        Participacao participacao = new Participacao();
//        participacao.setInstrumento(instrumento);
//        participacao.setAvaliada(alvo);
////        participacao.setRespondente(respondente);
//        participacao.setContexto(contexto);
//        instrumentoAvaliativoDAO.persistir(participacao);
//        instrumentoAvaliativoDAO.flush();
//
////        resposta.setRespondente(instrumento.isAnonimo() ? null : respondente);
//        resposta.setDataEnvio(LocalDate.now());
//        resposta.setStatus("ENVIADA");
//        instrumentoAvaliativoDAO.flush();
    }

}
