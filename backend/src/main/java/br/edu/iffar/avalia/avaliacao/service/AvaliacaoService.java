package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.*;
import br.edu.iffar.avalia.avaliacao.dao.AvaliacaoDao;
import br.edu.iffar.avalia.avaliacao.dao.EntidadeAcademicaDao;
import br.edu.iffar.avalia.avaliacao.dao.InstrumentoDao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoRespondente;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoAlvo;
import br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.QuestaoAvaliativa;
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
    private InstrumentoDao consultas;

    // ------------------------------------------------------------------ catálogos

    public List<TipoEntidade> listarTiposEntidade() {
        List<TipoEntidade> out = new ArrayList<>();
        for (br.edu.iffar.avalia.avaliacao.model.TipoEntidade entidade : dao.listarTiposEntidade()) {
            TipoEntidade t = new TipoEntidade();
            t.id = entidade.getId().toString();
            t.codigo = entidade.getCodigo();
            t.nome = entidade.getNome();
            t.podeSerAvaliada = entidade.isPodeSerAvaliada();
            t.podeResponder = entidade.isPodeResponder();
            out.add(t);
        }
        return out;
    }

    /** Atualiza os espelhos avaliativos a partir dos dados acadêmicos já sincronizados. */
    public void sincronizarEntidadesAcademicas() {
        entidadeAcademicaDao.sincronizar();
    }

    public List<ConjuntoOpcao> listarConjuntos() {
        List<ConjuntoOpcao> resultado = new ArrayList<>();
        for (br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao entidade : dao.listarConjuntos()) {
            ConjuntoOpcao c = new ConjuntoOpcao();
            c.id = entidade.getId().toString();
            c.nome = entidade.getNome();
            for (br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao opcao : entidade.getOpcoes()) {
                Opcao o = new Opcao();
                o.id = opcao.getId().toString();
                o.texto = opcao.getTexto();
                o.valor = opcao.getValor() == null ? null : opcao.getValor().doubleValue();
                o.correta = opcao.isCorreta();
                o.ordem = opcao.getOrdem();
                c.opcoes.add(o);
            }
            resultado.add(c);
        }
        return resultado;
    }

    public ConjuntoOpcao salvarConjunto(ConjuntoOpcao dto) {
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
        for (Opcao o : dto.opcoes) {
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
        return listarConjuntos().stream().filter(c -> c.id.equals(conjunto.getId().toString())).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ instrumento

    public List<Instrumento> listarInstrumentos() {
        List<Instrumento> out = new ArrayList<>();
        Map<String, Instrumento> porId = new LinkedHashMap<>();
        for (Object[] r : consultas.listarInstrumentosBase()) {
            Instrumento i = new Instrumento();
            i.id = (String) r[0];
            i.titulo = (String) r[1];
            i.cicloReferencia = (String) r[2];
            i.status = (String) r[3];
            i.inicioAplicacao = (String) r[4];
            i.fimAplicacao = (String) r[5];
            i.tipoEntidadeAvaliadaNome = (String) r[6];
            i.totalQuestoes = ((Number) r[7]).intValue();
            out.add(i);
            porId.put(i.id, i);
        }
        for (Object[] x : consultas.listarTodosTiposAlvo()) {
            Instrumento instrumento = porId.get(x[0]);
            if (instrumento != null) {
                instrumento.tiposEntidadeAlvoIds.add((String) x[1]);
                instrumento.tiposEntidadeAlvoNomes.add((String) x[2]);
            }
        }
        return out;
    }

    public Instrumento buscarInstrumento(String id) {
        List<Object[]> base = consultas.buscarInstrumentoBase(id);
        if (base.isEmpty()) return null;

        Object[] r = base.get(0);
        Instrumento i = new Instrumento();
        i.id = (String) r[0];
        i.tipoEntidadeAvaliadaId = (String) r[1];
        i.tipoEntidadeAvaliadaNome = (String) r[2];
        i.titulo = (String) r[3];
        i.descricao = (String) r[4];
        i.cicloReferencia = (String) r[5];
        i.inicioAplicacao = (String) r[6];
        i.fimAplicacao = (String) r[7];
        i.anonimo = (Boolean) r[8];
        i.permiteEdicao = (Boolean) r[9];
        i.status = (String) r[10];
        i.criadoPor = (String) r[11];
        i.atualizadoEm = (String) r[12];

        for (Object[] x : consultas.listarTiposAlvo(id)) {
            i.tiposEntidadeAlvoIds.add((String) x[0]);
            i.tiposEntidadeAlvoNomes.add((String) x[1]);
        }
        if (i.tiposEntidadeAlvoIds.isEmpty()) {
            i.tiposEntidadeAlvoIds.add(i.tipoEntidadeAvaliadaId);
            i.tiposEntidadeAlvoNomes.add(i.tipoEntidadeAvaliadaNome);
        }

        for (Object[] x : consultas.listarRespondentes(id)) {
            Respondente resp = new Respondente();
            resp.tipoEntidadeId = (String) x[0];
            resp.tipoVinculo = (String) x[1];
            i.respondentes.add(resp);
        }

        Map<String, Sessao> sessoes = new LinkedHashMap<>();
        for (Object[] x : consultas.listarSessoes(id)) {
            if ("PAGINA".equals(x[2])) {
                Pagina p = new Pagina();
                p.id = (String) x[0];
                p.titulo = (String) x[3];
                p.descricao = (String) x[4];
                p.ordem = ((Number) x[5]).intValue();
                i.paginas.add(p);
            } else {
                Sessao s = new Sessao();
                s.id = (String) x[0];
                s.titulo = (String) x[3];
                s.descricao = (String) x[4];
                s.ordem = ((Number) x[5]).intValue();
                sessoes.put(s.id, s);
                i.sessoes.add(s);
            }
        }
        for (Object[] x : consultas.listarQuestoes(id)) {
            Sessao s = sessoes.get((String) x[0]);
            if (s == null) continue;
            Questao q = new Questao();
            q.questaoId = (String) x[1];
            q.enunciado = (String) x[2];
            q.tipo = (String) x[3];
            q.assunto = (String) x[4];
            q.conjuntoOpcaoId = (String) x[5];
            q.obrigatoria = (Boolean) x[6];
            q.peso = x[7] == null ? null : ((Number) x[7]).doubleValue();
            q.ordem = ((Number) x[8]).intValue();
            q.sessaoQuestaoId = (String) x[9];
            s.questoes.add(q);
            i.totalQuestoes++;
        }
        return i;
    }

    public Instrumento salvarInstrumento(Instrumento dto, String usuario) {
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
        if (rascunho) entidade.setTipoEntidadeAvaliada(dao.referencia(
                br.edu.iffar.avalia.avaliacao.model.TipoEntidade.class, UUID.fromString(dto.tipoEntidadeAvaliadaId)));
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

    public Instrumento publicar(String id) {
        Instrumento i = buscarInstrumento(id);
        if (i == null) throw new IllegalArgumentException("Instrumento não encontrado.");
        if (!"RASCUNHO".equals(i.status)) throw new IllegalArgumentException("Apenas rascunhos podem ser publicados.");
        if (i.totalQuestoes == 0) throw new IllegalArgumentException("Inclua ao menos uma questão antes de publicar.");
        if (i.respondentes.isEmpty()) throw new IllegalArgumentException("Defina ao menos um público respondente antes de publicar.");
        InstrumentoAvaliativo entidade = dao.buscarInstrumento(UUID.fromString(id));
        entidade.setStatus("PUBLICADO");
        entidade.setAtualizadoEm(OffsetDateTime.now());
        dao.flush();
        String tipoContextoCodigo = dao.codigoTipo(UUID.fromString(i.tipoEntidadeAvaliadaId));
        if ("INSTITUICAO".equals(tipoContextoCodigo) && i.tiposEntidadeAlvoIds.contains(i.tipoEntidadeAvaliadaId)) {
            consultas.criarEntidadeInstitucional(i.tipoEntidadeAvaliadaId, "instrumento:" + id, i.titulo);
            consultas.vincularEntidadeInstitucional(id, i.tipoEntidadeAvaliadaId, "instrumento:" + id);
        }
        consultas.popularAlvos(id);
        return buscarInstrumento(id);
    }

    public boolean excluir(String id) {
        Instrumento i = buscarInstrumento(id);
        if (i == null) return false;
        if (!"RASCUNHO".equals(i.status)) throw new IllegalArgumentException("Só é possível excluir instrumentos em rascunho.");
        List<String> questoes = idsQuestoes(id);
        dao.remover(dao.buscarInstrumento(UUID.fromString(id)));
        dao.flush();
        removerQuestoesOrfas(questoes);
        return true;
    }

    // ------------------------------------------------------------------ internos

    private void substituirRespondentes(String id, List<Respondente> respondentes) {
        UUID instrumentoId = UUID.fromString(id);
        dao.removerRespondentes(instrumentoId);
        InstrumentoAvaliativo instrumento = dao.referencia(InstrumentoAvaliativo.class, instrumentoId);
        for (Respondente r : respondentes) {
            InstrumentoRespondente entidade = new InstrumentoRespondente();
            entidade.setInstrumento(instrumento);
            entidade.setTipoEntidadeRespondente(dao.referencia(
                    br.edu.iffar.avalia.avaliacao.model.TipoEntidade.class, UUID.fromString(r.tipoEntidadeId)));
            entidade.setTipoVinculo(blank(r.tipoVinculo) ? null : r.tipoVinculo.trim());
            dao.persistir(entidade);
        }
        dao.flush();
    }

    private void substituirTiposAlvo(String id, Instrumento dto) {
        List<String> tiposAlvo = dto.tiposEntidadeAlvoIds == null || dto.tiposEntidadeAlvoIds.isEmpty()
                ? List.of(dto.tipoEntidadeAvaliadaId) : dto.tiposEntidadeAlvoIds;
        UUID instrumentoId = UUID.fromString(id);
        dao.removerTiposAlvo(instrumentoId);
        for (String tipoId : new LinkedHashSet<>(tiposAlvo)) {
            dao.persistir(new InstrumentoTipoAlvo(
                    instrumentoId, UUID.fromString(tipoId)));
        }
        dao.flush();
    }

    private void substituirEstrutura(String id, List<Pagina> paginas, List<Sessao> sessoes, String usuario) {
        List<String> anteriores = idsQuestoes(id);
        dao.removerSessoes(UUID.fromString(id));
        InstrumentoAvaliativo instrumento = dao.referencia(InstrumentoAvaliativo.class, UUID.fromString(id));

        int ordemPagina = 1;
        for (Pagina p : paginas) {
            InstrumentoSessao pagina = new InstrumentoSessao();
            pagina.setInstrumento(instrumento);
            pagina.setTipo("PAGINA");
            pagina.setTitulo(p.titulo.trim());
            pagina.setDescricao(p.descricao);
            pagina.setOrdem(ordemPagina++);
            dao.persistir(pagina);
        }
        int ordemSessao = 1;
        for (Sessao s : sessoes) {
            InstrumentoSessao sessao = new InstrumentoSessao();
            sessao.setInstrumento(instrumento);
            sessao.setTipo("SESSAO");
            sessao.setTitulo(s.titulo.trim());
            sessao.setDescricao(s.descricao);
            sessao.setOrdem(ordemSessao++);
            dao.persistir(sessao);
            int ordemQuestao = 1;
            for (Questao q : s.questoes) {
                String questaoId = upsertQuestao(q, usuario);
                SessaoQuestao posicao = new SessaoQuestao();
                posicao.setSessao(sessao);
                posicao.setQuestao(dao.referencia(QuestaoAvaliativa.class,
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

    private String upsertQuestao(Questao q, String usuario) {
        String conjunto = "DESCRITIVA".equals(q.tipo) ? null : q.conjuntoOpcaoId;
        QuestaoAvaliativa entidade = blank(q.questaoId) ? null :
                dao.buscar(QuestaoAvaliativa.class, UUID.fromString(q.questaoId));
        boolean nova = entidade == null;
        if (nova) {
            entidade = new QuestaoAvaliativa();
            entidade.setCriadoPor(usuario);
        }
        entidade.setConjunto(conjunto == null ? null : dao.referencia(
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
        for (Object[] r : consultas.listarIdsQuestoes(instrumentoId)) {
            ids.add((String) r[0]);
        }
        return ids;
    }

    /** Remove questões que deixaram de ser usadas por qualquer instrumento. */
    private void removerQuestoesOrfas(List<String> candidatas) {
        for (String qid : candidatas) {
            consultas.removerQuestaoOrfa(qid);
        }
    }

    private void validar(Instrumento d, boolean validarRespondentes) {
        if (d == null) throw new IllegalArgumentException("Dados do instrumento não informados.");
        if (blank(d.titulo)) throw new IllegalArgumentException("Informe o título do instrumento.");
        if (blank(d.cicloReferencia)) throw new IllegalArgumentException("Informe o ciclo de referência (ex.: 2026/1).");
        if (blank(d.tipoEntidadeAvaliadaId)) throw new IllegalArgumentException("Informe o que será avaliado.");
        if (d.tiposEntidadeAlvoIds == null || d.tiposEntidadeAlvoIds.isEmpty()) {
            d.tiposEntidadeAlvoIds = new ArrayList<>(List.of(d.tipoEntidadeAvaliadaId));
        }
        if (blank(d.inicioAplicacao) || blank(d.fimAplicacao)) throw new IllegalArgumentException("Informe o período de aplicação.");
        if (!OffsetDateTime.parse(d.fimAplicacao).isAfter(OffsetDateTime.parse(d.inicioAplicacao))) {
            throw new IllegalArgumentException("O término da aplicação deve ser posterior ao início.");
        }
        if (d.respondentes == null) d.respondentes = new ArrayList<>();
        if (d.paginas == null) d.paginas = new ArrayList<>();
        if (d.sessoes == null) d.sessoes = new ArrayList<>();

        Set<String> vistos = new HashSet<>();
        Set<String> tiposAlvoVistos = new HashSet<>();
        for (String tipoAlvo : d.tiposEntidadeAlvoIds) {
            if (blank(tipoAlvo)) throw new IllegalArgumentException("Tipo de alvo sem identificação.");
            if (!tiposAlvoVistos.add(tipoAlvo)) throw new IllegalArgumentException("Tipo de alvo repetido.");
            br.edu.iffar.avalia.avaliacao.model.TipoEntidade tipo = dao.buscar(
                    br.edu.iffar.avalia.avaliacao.model.TipoEntidade.class, UUID.fromString(tipoAlvo));
            if (tipo == null || !tipo.isPodeSerAvaliada())
                throw new IllegalArgumentException("Tipo de alvo inválido.");
        }
        String contextoCodigo = dao.codigoTipo(UUID.fromString(d.tipoEntidadeAvaliadaId));
        if ("TURMA".equals(contextoCodigo)) {
            Set<String> codigos = new HashSet<>();
            for (String tipoId : d.tiposEntidadeAlvoIds) {
                codigos.add(dao.codigoTipo(UUID.fromString(tipoId)));
            }
            if (codigos.isEmpty() || !Set.of("DOCENTE", "ALUNO").containsAll(codigos))
                throw new IllegalArgumentException("Uma avaliação de turma pode incluir docentes e alunos como alvos.");
        } else if (d.tiposEntidadeAlvoIds.size() != 1 || !d.tiposEntidadeAlvoIds.contains(d.tipoEntidadeAvaliadaId)) {
            throw new IllegalArgumentException("Selecione o próprio tipo avaliado como alvo.");
        }
        if (validarRespondentes) for (Respondente r : d.respondentes) {
            if (blank(r.tipoEntidadeId)) throw new IllegalArgumentException("Respondente sem tipo definido.");
            if (!vistos.add(r.tipoEntidadeId)) throw new IllegalArgumentException("Público respondente repetido.");
            br.edu.iffar.avalia.avaliacao.model.TipoEntidade tipoRespondente = dao.buscar(
                    br.edu.iffar.avalia.avaliacao.model.TipoEntidade.class, UUID.fromString(r.tipoEntidadeId));
            if (tipoRespondente == null || !tipoRespondente.isPodeResponder())
                throw new IllegalArgumentException("Público respondente inválido.");
            String vinculo = blank(r.tipoVinculo) ? null : r.tipoVinculo.trim();
            String codigo = tipoRespondente.getCodigo();
            if (!Set.of("ALUNO", "DOCENTE", "SERVIDOR").contains(codigo))
                throw new IllegalArgumentException("Este público ainda não possui perfil de acesso para responder.");
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
        for (Pagina p : d.paginas) {
            if (blank(p.titulo)) throw new IllegalArgumentException("Toda página precisa de um título.");
        }
        for (Sessao s : d.sessoes) {
            if (blank(s.titulo)) throw new IllegalArgumentException("Toda sessão precisa de um título.");
            if (s.questoes == null) s.questoes = new ArrayList<>();
            for (Questao q : s.questoes) {
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
