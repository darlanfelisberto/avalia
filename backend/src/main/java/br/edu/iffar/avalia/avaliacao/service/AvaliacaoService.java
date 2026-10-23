package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dao.InstrumentoAvaliativoDAO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.*;
import br.edu.iffar.avalia.avaliacao.dao.EntidadeAcademicaDao;
import br.edu.iffar.avalia.avaliacao.dao.TipoEntidadeDao;
import br.edu.iffar.avalia.avaliacao.model.TipoEntidade;
import br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoRespondente;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoAlvo;
import br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.Questao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.StatusInstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.TipoSessao;
import br.edu.iffar.avalia.core.exception.ValidacaoException;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/** Regras de criação, publicação e composição dos instrumentos de avaliação. */
@ApplicationScoped
@Transactional
public class AvaliacaoService {

    @Inject
    private InstrumentoAvaliativoDAO instrumentoAvaliativoDAO;

    @Inject
    private EntidadeAcademicaDao entidadeAcademicaDao;

    @Inject
    private TipoEntidadeDao tiposEntidadeDao;

    @Inject
    private AuthService auth;

    @Inject
    private Validator validator;

    // ------------------------------------------------------------------ catálogos

    /** Atualiza os espelhos avaliativos a partir dos dados acadêmicos já sincronizados. */
    public void sincronizarEntidadesAcademicas() {
        entidadeAcademicaDao.sincronizar();
    }

    public List<ConjuntoOpcaoDTO> listarConjuntos() {
        List<ConjuntoOpcaoDTO> resultado = new ArrayList<>();
//        for (br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao entidade : instrumentoAvaliativoDAO.listarConjuntos()) {
//            resultado.add(ConjuntoOpcaoDTO.create(entidade));
//        }
        return resultado;
    }

    public ConjuntoOpcaoDTO salvarConjunto(ConjuntoOpcaoDTO dto) {
        if (dto == null) throw new ValidacaoException("Dados do conjunto não informados.");
//        if (instrumentoAvaliativoDAO.existeConjunto(dto.nome.trim())) {
//            throw new ValidacaoException("Já existe um conjunto de opções com este nome.");
//        }
        br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao conjunto = new br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao();
        conjunto.setNome(dto.nome == null ? null : dto.nome.trim());
        int ordem = 1;
        for (AvaliacaoDTO.OpcaoQuestaoDTO o : dto.opcoes == null ? List.<AvaliacaoDTO.OpcaoQuestaoDTO>of() : dto.opcoes) {
            if (o == null) throw new ValidacaoException("Opção inválida.");
            OpcaoQuestao opcao = new OpcaoQuestao();
            opcao.setConjunto(conjunto);
            opcao.setTexto(o.texto == null ? null : o.texto.trim());
            opcao.setValor(o.valor == null ? null : BigDecimal.valueOf(o.valor));
            opcao.setCorreta(o.correta);
            opcao.setOrdem(ordem++);
            conjunto.getOpcoes().add(opcao);
        }
        instrumentoAvaliativoDAO.persistir(conjunto);
        instrumentoAvaliativoDAO.flush();
        return ConjuntoOpcaoDTO.create(conjunto);
    }

    public List<InstrumentoAvaliativoDTO> listarInstrumentos(int paginaSolicitada, int tamanhoSolicitado) {
        if (tamanhoSolicitado < 1 || tamanhoSolicitado > 100)
            throw new ValidacaoException("O tamanho da página deve estar entre 1 e 100.");

        List<InstrumentoAvaliativoDTO> resultado = new ArrayList<>();
        for (var instrimento : instrumentoAvaliativoDAO.listInstrumentos(paginaSolicitada, tamanhoSolicitado)) {
            resultado.add(new InstrumentoAvaliativoDTO()
                    .createResumo(instrimento));
        }
        return resultado;
    }

    public InstrumentoAvaliativoDTO buscarInstrumento(String id) {
        InstrumentoAvaliativo inst = instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(id));
        return new InstrumentoAvaliativoDTO().createDetalhe(inst);
    }

    public InstrumentoAvaliativoDTO salvarInstrumento(InstrumentoAvaliativoDTO dto) {
        if (dto == null)
            throw new ValidacaoException("Dados do instrumento não informados.");

        InstrumentoAvaliativo entidade = blank(dto.id) ? null : instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(dto.id));

        if (!blank(dto.id) && entidade == null)
            throw new ValidacaoException("Instrumento não encontrado.");

        boolean rascunho = entidade == null || entidade.getStatus() == StatusInstrumentoAvaliativo.RASCUNHO;
        validar(dto, rascunho);
//        if (instrumentoAvaliativoDAO.existeInstrumento(dto.titulo.trim(), dto.cicloReferencia.trim(),
//                UUID.fromString(dto.tipoEntidadeAvaliadaId), entidade == null ? null : entidade.getId())) {
//            throw new ValidacaoException("Já existe um instrumento para este tipo avaliado, ciclo e título.");
//        }
        boolean novo = entidade == null;
        if (novo) {
            entidade = new InstrumentoAvaliativo();
            entidade.setCriadoPor(auth.getCpf());
        }
        if (rascunho) entidade.setTipoEntidadeAvaliada(tiposEntidadeDao.referencia(UUID.fromString(dto.tipoEntidadeAvaliadaId)));
        entidade.setTitulo(trimOrNull(dto.titulo));
        entidade.setDescricao(dto.descricao);
        entidade.setCicloReferencia(trimOrNull(dto.cicloReferencia));
        entidade.setInicioAplicacao(parseData(dto.inicioAplicacao, "início da aplicação"));
        entidade.setFimAplicacao(parseData(dto.fimAplicacao, "término da aplicação"));
        entidade.setAnonimo(dto.anonimo);
        entidade.setPermiteEdicao(dto.permiteEdicao);
        entidade.setAtualizadoEm(OffsetDateTime.now());
        if (novo) instrumentoAvaliativoDAO.persistir(entidade);
        instrumentoAvaliativoDAO.flush();
        String id = entidade.getId().toString();
        if (rascunho) substituirTiposAlvo(id, dto);

        // Respondentes e estrutura só podem mudar enquanto o instrumento é rascunho
        if (rascunho) {
            substituirRespondentes(entidade, dto.respondentes);
//            substituirEstrutura(entidade, dto.paginas, dto.sessoes, usuario);
        }
        return buscarInstrumento(id);
    }

    @Transactional
    public InstrumentoAvaliativoDTO publicar(String id) {
        InstrumentoAvaliativo entidade = instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(id));
        if (entidade == null) throw new ValidacaoException("Instrumento não encontrado.");
        if (entidade.getStatus() != StatusInstrumentoAvaliativo.RASCUNHO) throw new ValidacaoException("Apenas rascunhos podem ser publicados.");
        if (entidade.contarQuestoes() == 0) throw new ValidacaoException("Inclua ao menos uma questão antes de publicar.");
        if (entidade.getInstrumentoTipoRespondenteList().isEmpty()) throw new ValidacaoException("Defina ao menos um público respondente antes de publicar.");

        entidade.setStatus(StatusInstrumentoAvaliativo.PUBLICADO);
        entidade.setAtualizadoEm(OffsetDateTime.now());
        instrumentoAvaliativoDAO.flush();
//        String tipoContextoCodigo = tiposEntidadeDao.codigo(UUID.fromString(i.tipoEntidadeAvaliadaId));
//        if ("INSTITUICAO".equals(tipoContextoCodigo)) {
//            instrumentoDao.criarEntidadeInstitucional(i.tipoEntidadeAvaliadaId, "instrumento:" + id, i.titulo);
//            instrumentoDao.vincularEntidadeInstitucional(id, i.tipoEntidadeAvaliadaId, "instrumento:" + id);
//        }
////        instrumentoDao.popularAlvos(id);
        return new InstrumentoAvaliativoDTO().createDetalhe(entidade);
    }

    public boolean excluir(String id) {
        InstrumentoAvaliativoDTO i = buscarInstrumento(id);
        if (i == null) return false;
        if (!StatusInstrumentoAvaliativo.RASCUNHO.name().equals(i.status)) throw new ValidacaoException("Só é possível excluir instrumentos em rascunho.");
//        List<String> questoes = idsQuestoes(id);
        instrumentoAvaliativoDAO.remover(instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(id)));
        instrumentoAvaliativoDAO.flush();
//        removerQuestoesOrfas(questoes);
        return true;
    }

    // ------------------------------------------------------------------ internos

    private void substituirRespondentes(InstrumentoAvaliativo instrumento,
                                        List<AvaliacaoDTO.InstrumentoTipoRespondenteDTO> respondentes) {
        UUID instrumentoId = instrumento.getId();
//        instrumentoAvaliativoDAO.removerRespondentes(instrumentoId);
        for (AvaliacaoDTO.InstrumentoTipoRespondenteDTO r : respondentes) {
            InstrumentoTipoRespondente entidade = new InstrumentoTipoRespondente();
            entidade.setInstrumentoAvaliativo(instrumento);
            entidade.setTipoEntidade(tiposEntidadeDao.referencia(UUID.fromString(r.tipoEntidadeId)));
            instrumentoAvaliativoDAO.persistir(entidade);
        }
        instrumentoAvaliativoDAO.flush();
    }

    private void substituirTiposAlvo(String id, InstrumentoAvaliativoDTO dto) {
        UUID instrumentoId = UUID.fromString(id);
//        instrumentoAvaliativoDAO.removerTiposAlvo(instrumentoId);
        instrumentoAvaliativoDAO.persistir(new InstrumentoTipoAlvo(instrumentoId, UUID.fromString(dto.tipoEntidadeAvaliadaId)));
        instrumentoAvaliativoDAO.flush();
    }

//    private void substituirEstrutura(InstrumentoAvaliativo instrumento,
//                                     List<AvaliacaoDTO.InstrumentoSessaoPaginaDTO> paginas,
//                                     List<AvaliacaoDTO.InstrumentoSessaoDTO> sessoes, String usuario) {
//        String id = instrumento.getId().toString();
//        List<String> anteriores = idsQuestoes(id);
//        instrumentoAvaliativoDAO.removerSessoes(instrumento.getId());
//
//        int ordemPagina = 1;
//        for (AvaliacaoDTO.InstrumentoSessaoPaginaDTO p : paginas) {
//            InstrumentoSessao pagina = new InstrumentoSessao();
//            pagina.setInstrumentoAvaliativo(instrumento);
//            pagina.setTipoSessao("PAGINA");
//            pagina.setTitulo(p.titulo.trim());
//            pagina.setDescricao(p.descricao);
//            pagina.setOrdem(ordemPagina++);
//            instrumentoAvaliativoDAO.persistir(pagina);
//        }
//        int ordemSessao = 1;
//        for (AvaliacaoDTO.InstrumentoSessaoDTO s : sessoes) {
//            InstrumentoSessao sessao = new InstrumentoSessao();
//            sessao.setInstrumentoAvaliativo(instrumento);
//            sessao.setTipoSessao("SESSAO");
//            sessao.setTitulo(s.titulo.trim());
//            sessao.setDescricao(s.descricao);
//            sessao.setOrdem(ordemSessao++);
//            instrumentoAvaliativoDAO.persistir(sessao);
//            int ordemQuestao = 1;
//            for (AvaliacaoDTO.SessaoQuestaoDTO q : s.questoes) {
//                String questaoId = upsertQuestao(q, usuario);
//                SessaoQuestao posicao = new SessaoQuestao();
//                posicao.setInstrumentoSessao(sessao);
//                posicao.setQuestao(instrumentoAvaliativoDAO.referencia(Questao.class,
//                        UUID.fromString(questaoId)));
//                posicao.setOrdem(ordemQuestao++);
//                posicao.setObrigatoria(q.obrigatoria);
//                posicao.setPeso(q.peso == null ? null : BigDecimal.valueOf(q.peso));
//                instrumentoAvaliativoDAO.persistir(posicao);
//            }
//        }
//        instrumentoAvaliativoDAO.flush();
//        removerQuestoesOrfas(anteriores);
//    }

//    private String upsertQuestao(AvaliacaoDTO.SessaoQuestaoDTO q, String usuario) {
//        Questao entidade = blank(q.questaoId) ? null :
//                instrumentoAvaliativoDAO.buscar(Questao.class, UUID.fromString(q.questaoId));
//        boolean nova = entidade == null;
//        if (nova) {
//            entidade = new Questao();
//            entidade.setCriadoPor(usuario);
//        }
//        ConjuntoOpcao anterior = entidade.getConjuntoOpcao();
//        entidade.setEnunciado(q.enunciado.trim());
//        entidade.setTipo(q.tipo);
//        entidade.setAssunto(q.assunto);
//        if (nova) instrumentoAvaliativoDAO.persistir(entidade);
//        instrumentoAvaliativoDAO.flush();
//
//        ConjuntoOpcao conjunto = null;
//        if (!"DESCRITIVA".equals(q.tipo)) {
//            if (q.opcoes != null && !q.opcoes.isEmpty()) {
//                conjunto = anterior != null && anterior.isExclusivo() ? anterior : new ConjuntoOpcao();
//                if (conjunto.getId() == null) {
//                    conjunto.setNome("Questão " + entidade.getId());
//                    conjunto.setExclusivo(true);
//                    instrumentoAvaliativoDAO.persistir(conjunto);
//                } else {
//                    conjunto.getOpcoes().clear();
//                    instrumentoAvaliativoDAO.flush();
//                }
//                int ordem = 1;
//                for (AvaliacaoDTO.OpcaoQuestaoDTO o : q.opcoes) {
//                    OpcaoQuestao opcao = new OpcaoQuestao();
//                    opcao.setConjunto(conjunto);
//                    opcao.setTexto(o.texto.trim());
//                    opcao.setValor(o.valor == null ? null : BigDecimal.valueOf(o.valor));
//                    opcao.setCorreta(o.correta);
//                    opcao.setOrdem(ordem++);
//                    conjunto.getOpcoes().add(opcao);
//                    instrumentoAvaliativoDAO.persistir(opcao);
//                }
//            } else {
//                conjunto = instrumentoAvaliativoDAO.buscar(ConjuntoOpcao.class, UUID.fromString(q.conjuntoOpcaoId));
//                if (conjunto == null || conjunto.isExclusivo())
//                    throw new ValidacaoException("Conjunto de opções inválido para esta questão.");
//            }
//        }
//        entidade.setConjuntoOpcao(conjunto);
//        instrumentoAvaliativoDAO.flush();
//        if (anterior != null && anterior.isExclusivo() && anterior != conjunto) instrumentoAvaliativoDAO.remover(anterior);
//        return entidade.getId().toString();
//    }

//    private List<String> idsQuestoes(String instrumentoId) {
//        List<String> ids = new ArrayList<>();
////        for (Object[] r : instrumentoAvaliativoDAO.listarIdsQuestoes(instrumentoId)) {
////            ids.add((String) r[0]);
////        }
//        return ids;
//    }
//
//    /** Remove questões que deixaram de ser usadas por qualquer instrumento. */
//    private void removerQuestoesOrfas(List<String> candidatas) {
//        for (String qid : candidatas) {
////            instrumentoAvaliativoDAO.removerQuestaoOrfa(qid);
//        }
//    }

    private void validar(InstrumentoAvaliativoDTO d, boolean validarRespondentes) {

        if (blank(d.tipoEntidadeAvaliadaId)) throw new ValidacaoException("Informe o que será avaliado.");
        TipoEntidade tipoAvaliado = tiposEntidadeDao.buscar(UUID.fromString(d.tipoEntidadeAvaliadaId));
        if (tipoAvaliado == null || !tipoAvaliado.isPodeSerAvaliada())
            throw new ValidacaoException("Tipo avaliado inválido.");
        if (d.respondentes == null) d.respondentes = new ArrayList<>();
        if (d.paginas == null) d.paginas = new ArrayList<>();
        if (d.sessoes == null) d.sessoes = new ArrayList<>();

        Set<String> vistos = new HashSet<>();
        String contextoCodigo = tiposEntidadeDao.codigo(UUID.fromString(d.tipoEntidadeAvaliadaId));
        if (validarRespondentes) for (AvaliacaoDTO.InstrumentoTipoRespondenteDTO r : d.respondentes) {
            if (r == null) throw new ValidacaoException("Público respondente inválido.");
            if (blank(r.tipoEntidadeId)) throw new ValidacaoException("Respondente sem tipo definido.");
            if (!vistos.add(r.tipoEntidadeId)) throw new ValidacaoException("Público respondente repetido.");
            TipoEntidade tipoRespondente = tiposEntidadeDao.buscar(UUID.fromString(r.tipoEntidadeId));
            if (tipoRespondente == null || !tipoRespondente.isPodeResponder())
                throw new ValidacaoException("Público respondente inválido.");
            String vinculo = blank(r.tipoVinculo) ? null : r.tipoVinculo.trim();
            String codigo = tipoRespondente.getCodigo();
            if (!tiposEntidadeDao.podeAvaliar(UUID.fromString(r.tipoEntidadeId), UUID.fromString(d.tipoEntidadeAvaliadaId))) {
                throw new ValidacaoException("O tipo " + tipoRespondente.getNome()
                        + " não está autorizado a avaliar o tipo selecionado.");
            }
            boolean vinculoValido = switch (contextoCodigo) {
                case "INSTITUICAO" -> vinculo == null;
                case "CAMPUS", "SETOR" -> vinculo == null || "PERTENCE_A".equals(vinculo);
                case "TURMA" -> ("ALUNO".equals(codigo) && (vinculo == null || "MATRICULADO_EM".equals(vinculo)))
                        || ("DOCENTE".equals(codigo) && (vinculo == null || "LECIONA".equals(vinculo)));
                case "DOCENTE" -> "ALUNO".equals(codigo) && (vinculo == null || "MATRICULADO_EM".equals(vinculo));
                default -> vinculo == null || Set.of("MATRICULADO_EM", "LECIONA", "PERTENCE_A").contains(vinculo);
            };
            if (!vinculoValido) throw new ValidacaoException("Vínculo incompatível com o público e o contexto avaliados.");
        }
        InstrumentoAvaliativo instrumentoValidacao = new InstrumentoAvaliativo();
        int ordemSessao = 0;
        for (AvaliacaoDTO.InstrumentoSessaoPaginaDTO p : d.paginas) {
            if (p == null) throw new ValidacaoException("Página inválida.");
            validarModelo(criarSessaoValidacao(p.titulo, TipoSessao.PAGINA, instrumentoValidacao, ordemSessao++));
        }
        for (AvaliacaoDTO.InstrumentoSessaoDTO s : d.sessoes) {
            if (s == null) throw new ValidacaoException("Sessão inválida.");
            InstrumentoSessao sessaoValidacao = criarSessaoValidacao(
                    s.titulo, TipoSessao.SESSAO, instrumentoValidacao, ordemSessao++);
            validarModelo(sessaoValidacao);
            if (s.questoes == null) s.questoes = new ArrayList<>();
            int ordemQuestao = 0;
            for (AvaliacaoDTO.SessaoQuestaoDTO q : s.questoes) {
                if (q == null) throw new ValidacaoException("Questão inválida.");
                int ordemAtual = ordemQuestao++;
                Questao questao = new Questao();
                questao.setEnunciado(q.enunciado);
                questao.setTipo(q.tipo);
                questao.setAssunto(q.assunto);
                questao.setCriadoPor(auth.getCpf());

                SessaoQuestao posicaoValidacao = new SessaoQuestao();
                posicaoValidacao.setInstrumentoSessao(sessaoValidacao);
                posicaoValidacao.setQuestao(questao);
                posicaoValidacao.setOrdem(ordemAtual);
                posicaoValidacao.setObrigatoria(q.obrigatoria);
                posicaoValidacao.setPeso(q.peso == null ? null : BigDecimal.valueOf(q.peso));
                validarModelo(posicaoValidacao);

                boolean temConjunto = !blank(q.conjuntoOpcaoId);
                boolean temOpcoesExclusivas = q.opcoes != null && !q.opcoes.isEmpty();
                if (temConjunto && temOpcoesExclusivas)
                    throw new ValidacaoException("Informe um conjunto reutilizável ou opções exclusivas, não ambos.");
                if (temOpcoesExclusivas) {
                    ConjuntoOpcao conjuntoValidacao = new ConjuntoOpcao();
                    conjuntoValidacao.setNome("Opções da questão");
                    conjuntoValidacao.setExclusivo(true);
                    int ordemOpcao = 0;
                    for (AvaliacaoDTO.OpcaoQuestaoDTO o : q.opcoes) {
                        if (o == null) throw new ValidacaoException("Opção inválida.");
                        OpcaoQuestao opcaoValidacao = new OpcaoQuestao();
                        opcaoValidacao.setConjunto(conjuntoValidacao);
                        opcaoValidacao.setTexto(o.texto);
                        opcaoValidacao.setValor(o.valor == null ? null : BigDecimal.valueOf(o.valor));
                        opcaoValidacao.setOrdem(ordemOpcao++);
                        conjuntoValidacao.getOpcoes().add(opcaoValidacao);
                    }
                    questao.setConjuntoOpcao(conjuntoValidacao);
                    validarModelo(conjuntoValidacao);
                } else if (temConjunto) {
                    ConjuntoOpcao conjunto = instrumentoAvaliativoDAO.buscar(
                            ConjuntoOpcao.class, UUID.fromString(q.conjuntoOpcaoId));
                    if (conjunto == null || conjunto.isExclusivo())
                        throw new ValidacaoException("Conjunto de opções inválido para esta questão.");
                    questao.setConjuntoOpcao(conjunto);
                }
                validarModelo(questao);
            }
        }
    }

    private InstrumentoSessao criarSessaoValidacao(String titulo, TipoSessao tipo,
                                                    InstrumentoAvaliativo instrumento, int ordem) {
        InstrumentoSessao sessao = new InstrumentoSessao();
        sessao.setInstrumentoAvaliativo(instrumento);
        sessao.setTipoSessao(tipo);
        sessao.setTitulo(titulo);
        sessao.setOrdem(ordem);
        return sessao;
    }

    private <T> void validarModelo(T entidade) {
        var violacoes = validator.validate(entidade);
        if (!violacoes.isEmpty()) {
            String mensagens = violacoes.stream()
                    .map(violacao -> violacao.getMessage())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(" "));
            throw new ValidacaoException(mensagens);
        }
    }

    private OffsetDateTime parseData(String valor, String campo) {
        if (blank(valor)) return null;
        try {
            return OffsetDateTime.parse(valor);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ValidacaoException("Formato inválido para " + campo + ". Use uma data ISO-8601.", e);
        }
    }

    private static String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

}
