package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.*;
import br.edu.iffar.avalia.avaliacao.dao.AvaliacaoDao;
import br.edu.iffar.avalia.avaliacao.dao.EntidadeAcademicaDao;
import br.edu.iffar.avalia.avaliacao.dao.InstrumentoAvaliativoDAO;
import br.edu.iffar.avalia.avaliacao.dao.TipoEntidadeDao;
import br.edu.iffar.avalia.avaliacao.model.TipoEntidade;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoRespondente;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoAlvo;
import br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.Questao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/** Regras de criação, publicação e composição dos instrumentos de avaliação. */
@ApplicationScoped
@Transactional
public class AvaliacaoService {

    private static final Set<String> TIPOS_QUESTAO = Set.of("ESCOLHA_UNICA", "MULTIPLA_ESCOLHA", "ESCALA", "DESCRITIVA");

    @Inject
    private AvaliacaoDao dao;

    @Inject
    private EntidadeAcademicaDao entidadeAcademicaDao;

    @Inject
    private InstrumentoAvaliativoDAO instrumentoDao;

    @Inject
    private TipoEntidadeDao tiposEntidadeDao;

    // ------------------------------------------------------------------ catálogos

    /** Atualiza os espelhos avaliativos a partir dos dados acadêmicos já sincronizados. */
    public void sincronizarEntidadesAcademicas() {
        entidadeAcademicaDao.sincronizar();
    }

    public List<ConjuntoOpcaoDTO> listarConjuntos() {
        List<ConjuntoOpcaoDTO> resultado = new ArrayList<>();
        for (br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao entidade : dao.listarConjuntos()) {
            resultado.add(ConjuntoOpcaoDTO.create(entidade));
        }
        return resultado;
    }

    public ConjuntoOpcaoDTO salvarConjunto(ConjuntoOpcaoDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Dados do conjunto não informados.");
        if (blank(dto.nome)) throw new IllegalArgumentException("Informe o nome do conjunto de opções.");
        if (dto.opcoes == null || dto.opcoes.size() < 2) throw new IllegalArgumentException("Um conjunto precisa de ao menos 2 opções.");
        if (dao.existeConjunto(dto.nome.trim())) {
            throw new IllegalArgumentException("Já existe um conjunto de opções com este nome.");
        }
        br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao conjunto = new br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao();
        conjunto.setNome(dto.nome.trim());
        dao.persistir(conjunto);
        int ordem = 1;
        for (AvaliacaoDTO.OpcaoQuestaoDTO o : dto.opcoes) {
            if (blank(o.texto)) throw new IllegalArgumentException("Toda opção precisa de um texto.");
            OpcaoQuestao opcao = new OpcaoQuestao();
            opcao.setConjunto(conjunto);
            opcao.setTexto(o.texto.trim());
            opcao.setValor(o.valor == null ? null : BigDecimal.valueOf(o.valor));
            opcao.setCorreta(o.correta);
            opcao.setOrdem(ordem++);
            conjunto.getOpcoes().add(opcao);
            dao.persistir(opcao);
        }
        dao.flush();
        return ConjuntoOpcaoDTO.create(conjunto);
    }

    public List<InstrumentoAvaliativoDTO> listarInstrumentos(int paginaSolicitada, int tamanhoSolicitado) {
        if (tamanhoSolicitado < 1 || tamanhoSolicitado > 100)
            throw new IllegalArgumentException("O tamanho da página deve estar entre 1 e 100.");

        List<InstrumentoAvaliativoDTO> resultado = new ArrayList<>();
        for (var instrimento : instrumentoDao.listInstrumentos(paginaSolicitada, tamanhoSolicitado)) {
            resultado.add(new InstrumentoAvaliativoDTO()
                    .createResumo(instrimento));
        }
        return resultado;
    }

    public InstrumentoAvaliativoDTO buscarInstrumento(String id) {
        InstrumentoAvaliativo inst = instrumentoDao.findInstrumentosById(UUID.fromString(id));
        if (inst == null) return null;
        return new InstrumentoAvaliativoDTO().createDetalhe(inst);
    }

    public InstrumentoAvaliativoDTO salvarInstrumento(InstrumentoAvaliativoDTO dto, String usuario) {
        if (dto == null) throw new IllegalArgumentException("Dados do instrumento não informados.");
        InstrumentoAvaliativo entidade = blank(dto.id) ? null : dao.buscarInstrumento(UUID.fromString(dto.id));
        if (!blank(dto.id) && entidade == null) throw new IllegalArgumentException("Instrumento não encontrado.");
        boolean rascunho = entidade == null || "RASCUNHO".equals(entidade.getStatus());
        validar(dto, rascunho);
        if (dao.existeInstrumento(dto.titulo.trim(), dto.cicloReferencia.trim(),
                UUID.fromString(dto.tipoEntidadeAvaliadaId), entidade == null ? null : entidade.getId())) {
            throw new IllegalArgumentException("Já existe um instrumento para este tipo avaliado, ciclo e título.");
        }
        boolean novo = entidade == null;
        if (novo) {
            entidade = new InstrumentoAvaliativo();
            entidade.setCriadoPor(usuario);
        }
        if (rascunho) entidade.setTipoEntidadeAvaliada(tiposEntidadeDao.referencia(UUID.fromString(dto.tipoEntidadeAvaliadaId)));
        entidade.setTitulo(dto.titulo.trim());
        entidade.setDescricao(dto.descricao);
        entidade.setCicloReferencia(dto.cicloReferencia.trim());
        entidade.setInicioAplicacao(OffsetDateTime.parse(dto.inicioAplicacao));
        entidade.setFimAplicacao(OffsetDateTime.parse(dto.fimAplicacao));
        entidade.setAnonimo(dto.anonimo);
        entidade.setPermiteEdicao(dto.permiteEdicao);
        entidade.setAtualizadoEm(OffsetDateTime.now());
        if (novo) dao.persistir(entidade);
        dao.flush();
        String id = entidade.getId().toString();
        if (rascunho) substituirTiposAlvo(id, dto);

        // Respondentes e estrutura só podem mudar enquanto o instrumento é rascunho
        if (rascunho) {
            substituirRespondentes(id, dto.respondentes);
            substituirEstrutura(id, dto.paginas, dto.sessoes, usuario);
        }
        return buscarInstrumento(id);
    }

    public InstrumentoAvaliativoDTO publicar(String id) {
        InstrumentoAvaliativoDTO i = buscarInstrumento(id);
        if (i == null) throw new IllegalArgumentException("Instrumento não encontrado.");
        if (!"RASCUNHO".equals(i.status)) throw new IllegalArgumentException("Apenas rascunhos podem ser publicados.");
        if (i.totalQuestoes == 0) throw new IllegalArgumentException("Inclua ao menos uma questão antes de publicar.");
        if (i.respondentes.isEmpty()) throw new IllegalArgumentException("Defina ao menos um público respondente antes de publicar.");
        InstrumentoAvaliativo entidade = dao.buscarInstrumento(UUID.fromString(id));
        entidade.setStatus("PUBLICADO");
        entidade.setAtualizadoEm(OffsetDateTime.now());
        dao.flush();
        String tipoContextoCodigo = tiposEntidadeDao.codigo(UUID.fromString(i.tipoEntidadeAvaliadaId));
        if ("INSTITUICAO".equals(tipoContextoCodigo)) {
            instrumentoDao.criarEntidadeInstitucional(i.tipoEntidadeAvaliadaId, "instrumento:" + id, i.titulo);
            instrumentoDao.vincularEntidadeInstitucional(id, i.tipoEntidadeAvaliadaId, "instrumento:" + id);
        }
        instrumentoDao.popularAlvos(id);
        return buscarInstrumento(id);
    }

    public boolean excluir(String id) {
        InstrumentoAvaliativoDTO i = buscarInstrumento(id);
        if (i == null) return false;
        if (!"RASCUNHO".equals(i.status)) throw new IllegalArgumentException("Só é possível excluir instrumentos em rascunho.");
        List<String> questoes = idsQuestoes(id);
        dao.remover(dao.buscarInstrumento(UUID.fromString(id)));
        dao.flush();
        removerQuestoesOrfas(questoes);
        return true;
    }

    // ------------------------------------------------------------------ internos

    private void substituirRespondentes(String id, List<AvaliacaoDTO.InstrumentoTipoRespondenteDTO> respondentes) {
        UUID instrumentoId = UUID.fromString(id);
        dao.removerRespondentes(instrumentoId);
        InstrumentoAvaliativo instrumento = dao.referencia(InstrumentoAvaliativo.class, instrumentoId);
        for (AvaliacaoDTO.InstrumentoTipoRespondenteDTO r : respondentes) {
            InstrumentoTipoRespondente entidade = new InstrumentoTipoRespondente();
            entidade.setInstrumentoAvaliativo(instrumento);
            entidade.setTipoEntidade(tiposEntidadeDao.referencia(UUID.fromString(r.tipoEntidadeId)));
            dao.persistir(entidade);
        }
        dao.flush();
    }

    private void substituirTiposAlvo(String id, InstrumentoAvaliativoDTO dto) {
        UUID instrumentoId = UUID.fromString(id);
        dao.removerTiposAlvo(instrumentoId);
        dao.persistir(new InstrumentoTipoAlvo(instrumentoId, UUID.fromString(dto.tipoEntidadeAvaliadaId)));
        dao.flush();
    }

    private void substituirEstrutura(String id, List<AvaliacaoDTO.InstrumentoSessaoPaginaDTO> paginas,
                                     List<AvaliacaoDTO.InstrumentoSessaoDTO> sessoes, String usuario) {
        List<String> anteriores = idsQuestoes(id);
        dao.removerSessoes(UUID.fromString(id));
        InstrumentoAvaliativo instrumento = dao.referencia(InstrumentoAvaliativo.class, UUID.fromString(id));

        int ordemPagina = 1;
        for (AvaliacaoDTO.InstrumentoSessaoPaginaDTO p : paginas) {
            InstrumentoSessao pagina = new InstrumentoSessao();
            pagina.setInstrumentoAvaliativo(instrumento);
            pagina.setTipoSessao("PAGINA");
            pagina.setTitulo(p.titulo.trim());
            pagina.setDescricao(p.descricao);
            pagina.setOrdem(ordemPagina++);
            dao.persistir(pagina);
        }
        int ordemSessao = 1;
        for (AvaliacaoDTO.InstrumentoSessaoDTO s : sessoes) {
            InstrumentoSessao sessao = new InstrumentoSessao();
            sessao.setInstrumentoAvaliativo(instrumento);
            sessao.setTipoSessao("SESSAO");
            sessao.setTitulo(s.titulo.trim());
            sessao.setDescricao(s.descricao);
            sessao.setOrdem(ordemSessao++);
            dao.persistir(sessao);
            int ordemQuestao = 1;
            for (AvaliacaoDTO.SessaoQuestaoDTO q : s.questoes) {
                String questaoId = upsertQuestao(q, usuario);
                SessaoQuestao posicao = new SessaoQuestao();
                posicao.setInstrumentoSessao(sessao);
                posicao.setQuestao(dao.referencia(Questao.class,
                        UUID.fromString(questaoId)));
                posicao.setOrdem(ordemQuestao++);
                posicao.setObrigatoria(q.obrigatoria);
                posicao.setPeso(q.peso == null ? null : BigDecimal.valueOf(q.peso));
                dao.persistir(posicao);
            }
        }
        dao.flush();
        removerQuestoesOrfas(anteriores);
    }

    private String upsertQuestao(AvaliacaoDTO.SessaoQuestaoDTO q, String usuario) {
        String conjunto = "DESCRITIVA".equals(q.tipo) ? null : q.conjuntoOpcaoId;
        Questao entidade = blank(q.questaoId) ? null :
                dao.buscar(Questao.class, UUID.fromString(q.questaoId));
        boolean nova = entidade == null;
        if (nova) {
            entidade = new Questao();
            entidade.setCriadoPor(usuario);
        }
        entidade.setConjuntoOpcao(conjunto == null ? null : dao.referencia(
                br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao.class, UUID.fromString(conjunto)));
        entidade.setEnunciado(q.enunciado.trim());
        entidade.setTipo(q.tipo);
        entidade.setAssunto(q.assunto);
        if (nova) dao.persistir(entidade);
        dao.flush();
        return entidade.getId().toString();
    }

    private List<String> idsQuestoes(String instrumentoId) {
        List<String> ids = new ArrayList<>();
        for (Object[] r : instrumentoDao.listarIdsQuestoes(instrumentoId)) {
            ids.add((String) r[0]);
        }
        return ids;
    }

    /** Remove questões que deixaram de ser usadas por qualquer instrumento. */
    private void removerQuestoesOrfas(List<String> candidatas) {
        for (String qid : candidatas) {
            instrumentoDao.removerQuestaoOrfa(qid);
        }
    }

    private void validar(InstrumentoAvaliativoDTO d, boolean validarRespondentes) {
        if (d == null) throw new IllegalArgumentException("Dados do instrumento não informados.");
        if (blank(d.titulo)) throw new IllegalArgumentException("Informe o título do instrumento.");
        if (blank(d.cicloReferencia)) throw new IllegalArgumentException("Informe o ciclo de referência (ex.: 2026/1).");
        if (blank(d.tipoEntidadeAvaliadaId)) throw new IllegalArgumentException("Informe o que será avaliado.");
        TipoEntidade tipoAvaliado = tiposEntidadeDao.buscar(UUID.fromString(d.tipoEntidadeAvaliadaId));
        if (tipoAvaliado == null || !tipoAvaliado.isPodeSerAvaliada())
            throw new IllegalArgumentException("Tipo avaliado inválido.");
        if (blank(d.inicioAplicacao) || blank(d.fimAplicacao)) throw new IllegalArgumentException("Informe o período de aplicação.");
        if (!OffsetDateTime.parse(d.fimAplicacao).isAfter(OffsetDateTime.parse(d.inicioAplicacao))) {
            throw new IllegalArgumentException("O término da aplicação deve ser posterior ao início.");
        }
        if (d.respondentes == null) d.respondentes = new ArrayList<>();
        if (d.paginas == null) d.paginas = new ArrayList<>();
        if (d.sessoes == null) d.sessoes = new ArrayList<>();

        Set<String> vistos = new HashSet<>();
        String contextoCodigo = tiposEntidadeDao.codigo(UUID.fromString(d.tipoEntidadeAvaliadaId));
        if (validarRespondentes) for (AvaliacaoDTO.InstrumentoTipoRespondenteDTO r : d.respondentes) {
            if (blank(r.tipoEntidadeId)) throw new IllegalArgumentException("Respondente sem tipo definido.");
            if (!vistos.add(r.tipoEntidadeId)) throw new IllegalArgumentException("Público respondente repetido.");
            TipoEntidade tipoRespondente = tiposEntidadeDao.buscar(UUID.fromString(r.tipoEntidadeId));
            if (tipoRespondente == null || !tipoRespondente.isPodeResponder())
                throw new IllegalArgumentException("Público respondente inválido.");
            String vinculo = blank(r.tipoVinculo) ? null : r.tipoVinculo.trim();
            String codigo = tipoRespondente.getCodigo();
            if (!tiposEntidadeDao.podeAvaliar(UUID.fromString(r.tipoEntidadeId), UUID.fromString(d.tipoEntidadeAvaliadaId))) {
                throw new IllegalArgumentException("O tipo " + tipoRespondente.getNome()
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
            if (!vinculoValido) throw new IllegalArgumentException("Vínculo incompatível com o público e o contexto avaliados.");
        }
        for (AvaliacaoDTO.InstrumentoSessaoPaginaDTO p : d.paginas) {
            if (blank(p.titulo)) throw new IllegalArgumentException("Toda página precisa de um título.");
        }
        for (AvaliacaoDTO.InstrumentoSessaoDTO s : d.sessoes) {
            if (blank(s.titulo)) throw new IllegalArgumentException("Toda sessão precisa de um título.");
            if (s.questoes == null) s.questoes = new ArrayList<>();
            for (AvaliacaoDTO.SessaoQuestaoDTO q : s.questoes) {
                if (blank(q.enunciado)) throw new IllegalArgumentException("Toda questão precisa de um enunciado (sessão \"" + s.titulo + "\").");
                if (q.tipo == null || !TIPOS_QUESTAO.contains(q.tipo)) throw new IllegalArgumentException("Tipo de questão inválido.");
                if (!"DESCRITIVA".equals(q.tipo) && blank(q.conjuntoOpcaoId)) {
                    throw new IllegalArgumentException("A questão \"" + q.enunciado + "\" precisa de um conjunto de opções.");
                }
            }
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

}
