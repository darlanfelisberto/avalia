package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Persistência do instrumento avaliativo no schema "avaliacao" (SQL nativo, pois o schema é
 * independente do modelo JPA legado do schema public).
 */
@ApplicationScoped
@Transactional
public class AvaliacaoService {

    private static final Set<String> MODULOS = Set.of("AUTOAVALIACAO", "ADPD", "APRENDIZAGEM");
    private static final Set<String> TIPOS_QUESTAO = Set.of("ESCOLHA_UNICA", "MULTIPLA_ESCOLHA", "ESCALA", "DESCRITIVA");
    private static final String TS = "to_char(%s at time zone 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')";

    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    // ------------------------------------------------------------------ catálogos

    public List<TipoEntidade> listarTiposEntidade() {
        List<TipoEntidade> out = new ArrayList<>();
        for (Object[] r : rows("select cast(tipo_entidade_id as text), codigo, nome, pode_ser_avaliada, pode_responder " +
                "from avaliacao.tipo_entidade order by nome")) {
            TipoEntidade t = new TipoEntidade();
            t.id = (String) r[0];
            t.codigo = (String) r[1];
            t.nome = (String) r[2];
            t.podeSerAvaliada = (Boolean) r[3];
            t.podeResponder = (Boolean) r[4];
            out.add(t);
        }
        return out;
    }

    /** Atualiza os espelhos avaliativos a partir dos dados acadêmicos já sincronizados. */
    public void sincronizarEntidadesAcademicas() {
        em.createNativeQuery("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                "select te.tipo_entidade_id, 'academico:aluno:' || a.id, coalesce(nullif(p.nome_social, ''), p.nome) || ' · ' || coalesce(a.matricula, a.id::text) " +
                "from academico.aluno a join academico.pessoa p on p.id = a.id_pessoa " +
                "cross join avaliacao.tipo_entidade te where te.codigo = 'ALUNO' " +
                "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                "select te.tipo_entidade_id, 'academico:docente:' || s.id, coalesce(nullif(p.nome_social, ''), p.nome) " +
                "from (select distinct id_servidor from academico.docente_turma where id_servidor is not null) dt " +
                "join academico.servidor s on s.id = dt.id_servidor join academico.pessoa p on p.id = s.id_pessoa " +
                "cross join avaliacao.tipo_entidade te where te.codigo = 'DOCENTE' " +
                "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                "select te.tipo_entidade_id, 'academico:turma:' || t.id, left(concat_ws(' · ', t.codigo_turma, cc.nome, t.ano || '/' || t.periodo), 300) " +
                "from academico.turma t join academico.componente_curricular cc on cc.id = t.id_componente_curricular " +
                "cross join avaliacao.tipo_entidade te where te.codigo = 'TURMA' " +
                "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                "select te.tipo_entidade_id, 'academico:unidade:' || u.id, coalesce(nullif(u.sigla, ''), u.nome) " +
                "from academico.unidade u cross join avaliacao.tipo_entidade te " +
                "where (te.codigo = 'CAMPUS' and lower(concat_ws(' ', u.nome, u.sigla, u.tipo, u.categoria)) like '%campus%') " +
                "or (te.codigo = 'SETOR' and lower(concat_ws(' ', u.nome, u.sigla, u.tipo, u.categoria)) not like '%campus%') " +
                "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                "select te.tipo_entidade_id, p.cpf_cnpj, coalesce(nullif(p.nome_social, ''), p.nome) " +
                "from academico.aluno a join academico.pessoa p on p.id = a.id_pessoa " +
                "cross join avaliacao.tipo_entidade te where te.codigo = 'ALUNO' and p.cpf_cnpj is not null " +
                "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                "select te.tipo_entidade_id, p.cpf_cnpj, coalesce(nullif(p.nome_social, ''), p.nome) " +
                "from academico.servidor s join academico.pessoa p on p.id = s.id_pessoa " +
                "cross join avaliacao.tipo_entidade te where te.codigo = 'SERVIDOR' and p.cpf_cnpj is not null " +
                "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                "select te.tipo_entidade_id, p.cpf_cnpj, coalesce(nullif(p.nome_social, ''), p.nome) " +
                "from academico.docente_turma dt join academico.servidor s on s.id = dt.id_servidor " +
                "join academico.pessoa p on p.id = s.id_pessoa cross join avaliacao.tipo_entidade te " +
                "where te.codigo = 'DOCENTE' and p.cpf_cnpj is not null " +
                "on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true").executeUpdate();
        em.createNativeQuery("delete from avaliacao.entidade_vinculo v using avaliacao.entidade er, avaliacao.tipo_entidade tr " +
                "where v.entidade_origem_id = er.entidade_id and er.tipo_entidade_id = tr.tipo_entidade_id " +
                "and tr.codigo in ('ALUNO','DOCENTE','SERVIDOR') and v.tipo_vinculo in ('MATRICULADO_EM','LECIONA','PERTENCE_A') " +
                "and er.referencia_externa not like 'academico:%'").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select er.entidade_id, turma.entidade_id, 'MATRICULADO_EM' from academico.matricula m " +
                "join academico.aluno a on a.id = m.id_aluno join academico.pessoa p on p.id = a.id_pessoa " +
                "join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'ALUNO' " +
                "join avaliacao.entidade turma on turma.referencia_externa = 'academico:turma:' || m.id_turma " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select er.entidade_id, turma.entidade_id, 'LECIONA' from academico.docente_turma dt " +
                "join academico.servidor s on s.id = dt.id_servidor join academico.pessoa p on p.id = s.id_pessoa " +
                "join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'DOCENTE' " +
                "join avaliacao.entidade turma on turma.referencia_externa = 'academico:turma:' || dt.id_turma " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select distinct er.entidade_id, unit.entidade_id, 'PERTENCE_A' from academico.servidor s " +
                "join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'SERVIDOR' " +
                "join avaliacao.entidade unit on unit.referencia_externa in ('academico:unidade:' || s.id_unidade_lotacao, 'academico:unidade:' || s.id_unidade_exercicio) " +
                "join avaliacao.tipo_entidade tu on tu.tipo_entidade_id = unit.tipo_entidade_id and tu.codigo in ('CAMPUS','SETOR') " +
                "where s.id_unidade_lotacao is not null or s.id_unidade_exercicio is not null " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select distinct er.entidade_id, campus.entidade_id, 'PERTENCE_A' from academico.servidor s " +
                "join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'SERVIDOR' " +
                "join academico.unidade_closure uc on uc.id_descendente in (s.id_unidade_lotacao, s.id_unidade_exercicio) " +
                "join avaliacao.entidade campus on campus.referencia_externa = 'academico:unidade:' || uc.id_ancestral " +
                "join avaliacao.tipo_entidade tc on tc.tipo_entidade_id = campus.tipo_entidade_id and tc.codigo = 'CAMPUS' " +
                "where p.cpf_cnpj is not null " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select distinct er.entidade_id, unit.entidade_id, 'PERTENCE_A' from academico.servidor s " +
                "join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'DOCENTE' " +
                "join avaliacao.entidade unit on unit.referencia_externa in ('academico:unidade:' || s.id_unidade_lotacao, 'academico:unidade:' || s.id_unidade_exercicio) " +
                "join avaliacao.tipo_entidade tu on tu.tipo_entidade_id = unit.tipo_entidade_id and tu.codigo in ('CAMPUS','SETOR') " +
                "where s.id_unidade_lotacao is not null or s.id_unidade_exercicio is not null " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select distinct er.entidade_id, campus.entidade_id, 'PERTENCE_A' from academico.servidor s " +
                "join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'DOCENTE' " +
                "join academico.unidade_closure uc on uc.id_descendente in (s.id_unidade_lotacao, s.id_unidade_exercicio) " +
                "join avaliacao.entidade campus on campus.referencia_externa = 'academico:unidade:' || uc.id_ancestral " +
                "join avaliacao.tipo_entidade tc on tc.tipo_entidade_id = campus.tipo_entidade_id and tc.codigo = 'CAMPUS' " +
                "where p.cpf_cnpj is not null " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select distinct er.entidade_id, campus.entidade_id, 'PERTENCE_A' from academico.aluno a " +
                "join academico.pessoa p on p.id = a.id_pessoa join academico.curso c on c.id = a.id_curso " +
                "join academico.unidade_closure uc on uc.id_descendente = c.id_unidade " +
                "join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'ALUNO' " +
                "join avaliacao.entidade campus on campus.referencia_externa = 'academico:unidade:' || uc.id_ancestral " +
                "join avaliacao.tipo_entidade tc on tc.tipo_entidade_id = campus.tipo_entidade_id and tc.codigo = 'CAMPUS' " +
                "where p.cpf_cnpj is not null " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo) " +
                "select distinct er.entidade_id, setor.entidade_id, 'PERTENCE_A' from academico.aluno a " +
                "join academico.pessoa p on p.id = a.id_pessoa join academico.curso c on c.id = a.id_curso " +
                "join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'ALUNO' " +
                "join avaliacao.entidade setor on setor.referencia_externa = 'academico:unidade:' || c.id_unidade " +
                "join avaliacao.tipo_entidade ts on ts.tipo_entidade_id = setor.tipo_entidade_id and ts.codigo = 'SETOR' " +
                "where p.cpf_cnpj is not null " +
                "on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing").executeUpdate();
        em.createNativeQuery("insert into avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id) " +
                "select i.instrumento_avaliativo_id, ita.tipo_entidade_alvo_id, e.entidade_id " +
                "from avaliacao.instrumento_avaliativo i join avaliacao.instrumento_tipo_alvo ita " +
                "on ita.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                "join avaliacao.tipo_entidade te on te.tipo_entidade_id = ita.tipo_entidade_alvo_id " +
                "join avaliacao.entidade e on e.tipo_entidade_id = te.tipo_entidade_id " +
                "where i.status = 'PUBLICADO' and ((te.codigo = 'ALUNO' and e.referencia_externa like 'academico:aluno:%') " +
                "or (te.codigo = 'DOCENTE' and e.referencia_externa like 'academico:docente:%') " +
                "or (te.codigo in ('CAMPUS','SETOR') and e.referencia_externa like 'academico:unidade:%')) " +
                "on conflict (instrumento_avaliativo_id, entidade_id) do nothing").executeUpdate();
    }

    public List<ConjuntoOpcao> listarConjuntos() {
        Map<String, ConjuntoOpcao> mapa = new LinkedHashMap<>();
        for (Object[] r : rows("select cast(conjunto_opcao_id as text), nome from avaliacao.conjunto_opcao order by nome")) {
            ConjuntoOpcao c = new ConjuntoOpcao();
            c.id = (String) r[0];
            c.nome = (String) r[1];
            mapa.put(c.id, c);
        }
        for (Object[] r : rows("select cast(opcao_questao_id as text), cast(conjunto_opcao_id as text), texto, valor, correta, ordem " +
                "from avaliacao.opcao_questao order by ordem")) {
            ConjuntoOpcao c = mapa.get((String) r[1]);
            if (c == null) continue;
            Opcao o = new Opcao();
            o.id = (String) r[0];
            o.texto = (String) r[2];
            o.valor = r[3] == null ? null : ((Number) r[3]).doubleValue();
            o.correta = (Boolean) r[4];
            o.ordem = ((Number) r[5]).intValue();
            c.opcoes.add(o);
        }
        return new ArrayList<>(mapa.values());
    }

    public ConjuntoOpcao salvarConjunto(ConjuntoOpcao dto) {
        if (blank(dto.nome)) throw new IllegalArgumentException("Informe o nome do conjunto de opções.");
        if (dto.opcoes == null || dto.opcoes.size() < 2) throw new IllegalArgumentException("Um conjunto precisa de ao menos 2 opções.");
        if (count("select count(*) from avaliacao.conjunto_opcao where lower(nome) = lower(?1)", dto.nome.trim()) > 0) {
            throw new IllegalArgumentException("Já existe um conjunto de opções com este nome.");
        }
        UUID id = UUID.randomUUID();
        exec("insert into avaliacao.conjunto_opcao (conjunto_opcao_id, nome) values (cast(?1 as uuid), ?2)", id.toString(), dto.nome.trim());
        int ordem = 1;
        for (Opcao o : dto.opcoes) {
            if (blank(o.texto)) throw new IllegalArgumentException("Toda opção precisa de um texto.");
            exec("insert into avaliacao.opcao_questao (conjunto_opcao_id, texto, valor, correta, ordem) " +
                            "values (cast(?1 as uuid), ?2, cast(?3 as numeric), ?4, ?5)",
                    id.toString(), o.texto.trim(), o.valor, o.correta, ordem++);
        }
        return listarConjuntos().stream().filter(c -> c.id.equals(id.toString())).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ instrumento

    public List<Instrumento> listarInstrumentos() {
        List<Instrumento> out = new ArrayList<>();
        for (Object[] r : rows("select cast(i.instrumento_avaliativo_id as text), i.titulo, i.modulo, i.ciclo_referencia, i.status, " +
                ts("i.inicio_aplicacao") + ", " + ts("i.fim_aplicacao") + ", t.nome, " +
                "(select count(*) from avaliacao.sessao_questao sq join avaliacao.instrumento_sessao s " +
                "   on s.instrumento_sessao_id = sq.instrumento_sessao_id " +
                "  where s.instrumento_avaliativo_id = i.instrumento_avaliativo_id) " +
                "from avaliacao.instrumento_avaliativo i join avaliacao.tipo_entidade t " +
                "  on t.tipo_entidade_id = i.tipo_entidade_avaliada_id order by i.atualizado_em desc")) {
            Instrumento i = new Instrumento();
            i.id = (String) r[0];
            i.titulo = (String) r[1];
            i.modulo = (String) r[2];
            i.cicloReferencia = (String) r[3];
            i.status = (String) r[4];
            i.inicioAplicacao = (String) r[5];
            i.fimAplicacao = (String) r[6];
            i.tipoEntidadeAvaliadaNome = (String) r[7];
            i.totalQuestoes = ((Number) r[8]).intValue();
            for (Object[] x : rows("select cast(ita.tipo_entidade_alvo_id as text), te.nome " +
                    "from avaliacao.instrumento_tipo_alvo ita join avaliacao.tipo_entidade te " +
                    "on te.tipo_entidade_id = ita.tipo_entidade_alvo_id " +
                    "where ita.instrumento_avaliativo_id = cast(?1 as uuid) order by te.nome", i.id)) {
                i.tiposEntidadeAlvoIds.add((String) x[0]);
                i.tiposEntidadeAlvoNomes.add((String) x[1]);
            }
            out.add(i);
        }
        return out;
    }

    public Instrumento buscarInstrumento(String id) {
        List<Object[]> base = rows("select cast(i.instrumento_avaliativo_id as text), cast(i.tipo_entidade_avaliada_id as text), t.nome, " +
                "i.modulo, i.titulo, i.descricao, i.ciclo_referencia, " + ts("i.inicio_aplicacao") + ", " + ts("i.fim_aplicacao") + ", " +
                "i.anonimo, i.permite_edicao, i.status, i.criado_por, " + ts("i.atualizado_em") + " " +
                "from avaliacao.instrumento_avaliativo i join avaliacao.tipo_entidade t " +
                "  on t.tipo_entidade_id = i.tipo_entidade_avaliada_id " +
                "where i.instrumento_avaliativo_id = cast(?1 as uuid)", id);
        if (base.isEmpty()) return null;

        Object[] r = base.get(0);
        Instrumento i = new Instrumento();
        i.id = (String) r[0];
        i.tipoEntidadeAvaliadaId = (String) r[1];
        i.tipoEntidadeAvaliadaNome = (String) r[2];
        i.modulo = (String) r[3];
        i.titulo = (String) r[4];
        i.descricao = (String) r[5];
        i.cicloReferencia = (String) r[6];
        i.inicioAplicacao = (String) r[7];
        i.fimAplicacao = (String) r[8];
        i.anonimo = (Boolean) r[9];
        i.permiteEdicao = (Boolean) r[10];
        i.status = (String) r[11];
        i.criadoPor = (String) r[12];
        i.atualizadoEm = (String) r[13];

        for (Object[] x : rows("select cast(ita.tipo_entidade_alvo_id as text), te.nome " +
                "from avaliacao.instrumento_tipo_alvo ita join avaliacao.tipo_entidade te " +
                "on te.tipo_entidade_id = ita.tipo_entidade_alvo_id " +
                "where ita.instrumento_avaliativo_id = cast(?1 as uuid) order by te.nome", id)) {
            i.tiposEntidadeAlvoIds.add((String) x[0]);
            i.tiposEntidadeAlvoNomes.add((String) x[1]);
        }
        if (i.tiposEntidadeAlvoIds.isEmpty()) {
            i.tiposEntidadeAlvoIds.add(i.tipoEntidadeAvaliadaId);
            i.tiposEntidadeAlvoNomes.add(i.tipoEntidadeAvaliadaNome);
        }

        for (Object[] x : rows("select cast(tipo_entidade_respondente_id as text), tipo_vinculo " +
                "from avaliacao.instrumento_respondente where instrumento_avaliativo_id = cast(?1 as uuid)", id)) {
            Respondente resp = new Respondente();
            resp.tipoEntidadeId = (String) x[0];
            resp.tipoVinculo = (String) x[1];
            i.respondentes.add(resp);
        }

        Map<String, Sessao> sessoes = new LinkedHashMap<>();
        for (Object[] x : rows("select cast(instrumento_sessao_id as text), cast(instrumento_sessao_pai_id as text), tipo, titulo, descricao, ordem " +
                "from avaliacao.instrumento_sessao where instrumento_avaliativo_id = cast(?1 as uuid) order by ordem", id)) {
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
        for (Object[] x : rows("select cast(sq.instrumento_sessao_id as text), cast(q.questao_id as text), q.enunciado, q.tipo, q.assunto, " +
                "cast(q.conjunto_opcao_id as text), sq.obrigatoria, sq.peso, sq.ordem, cast(sq.sessao_questao_id as text) " +
                "from avaliacao.sessao_questao sq " +
                "join avaliacao.questao q on q.questao_id = sq.questao_id " +
                "join avaliacao.instrumento_sessao s on s.instrumento_sessao_id = sq.instrumento_sessao_id " +
                "where s.instrumento_avaliativo_id = cast(?1 as uuid) order by sq.ordem", id)) {
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
        String id = blank(dto.id) ? null : dto.id;
        String status = "RASCUNHO";

        if (id != null) {
            List<Object[]> atual = rows("select status from avaliacao.instrumento_avaliativo where instrumento_avaliativo_id = cast(?1 as uuid)", id);
            if (atual.isEmpty()) throw new IllegalArgumentException("Instrumento não encontrado.");
            status = (String) atual.get(0)[0];
        }
        boolean rascunho = "RASCUNHO".equals(status);
        validar(dto, rascunho);
        if (count("select count(*) from avaliacao.instrumento_avaliativo where modulo = ?1 and ciclo_referencia = ?2 " +
                "and titulo = ?3 and (cast(?4 as text) is null or instrumento_avaliativo_id <> cast(?4 as uuid))",
                dto.modulo, dto.cicloReferencia.trim(), dto.titulo.trim(), id) > 0) {
            throw new IllegalArgumentException("Já existe um instrumento com este módulo, ciclo e título.");
        }

        if (id == null) {
            id = UUID.randomUUID().toString();
            exec("insert into avaliacao.instrumento_avaliativo (instrumento_avaliativo_id, tipo_entidade_avaliada_id, modulo, titulo, descricao, " +
                            "ciclo_referencia, inicio_aplicacao, fim_aplicacao, anonimo, permite_edicao, criado_por) " +
                            "values (cast(?1 as uuid), cast(?2 as uuid), ?3, ?4, cast(?5 as text), ?6, cast(?7 as timestamptz), cast(?8 as timestamptz), ?9, ?10, ?11)",
                    id, dto.tipoEntidadeAvaliadaId, dto.modulo, dto.titulo.trim(), dto.descricao, dto.cicloReferencia.trim(),
                    dto.inicioAplicacao, dto.fimAplicacao, dto.anonimo, dto.permiteEdicao, usuario);
            substituirTiposAlvo(id, dto);
        } else {
            exec("update avaliacao.instrumento_avaliativo set titulo = ?2, descricao = cast(?3 as text), ciclo_referencia = ?4, " +
                            "inicio_aplicacao = cast(?5 as timestamptz), fim_aplicacao = cast(?6 as timestamptz), anonimo = ?7, " +
                            "permite_edicao = ?8, atualizado_em = now() where instrumento_avaliativo_id = cast(?1 as uuid)",
                    id, dto.titulo.trim(), dto.descricao, dto.cicloReferencia.trim(), dto.inicioAplicacao, dto.fimAplicacao,
                    dto.anonimo, dto.permiteEdicao);
            if (rascunho) {
                exec("update avaliacao.instrumento_avaliativo set modulo = ?2, tipo_entidade_avaliada_id = cast(?3 as uuid) " +
                        "where instrumento_avaliativo_id = cast(?1 as uuid)", id, dto.modulo, dto.tipoEntidadeAvaliadaId);
                substituirTiposAlvo(id, dto);
            }
        }

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
        exec("update avaliacao.instrumento_avaliativo set status = 'PUBLICADO', atualizado_em = now() " +
                "where instrumento_avaliativo_id = cast(?1 as uuid)", id);
        String tipoContextoCodigo = (String) em.createNativeQuery("select codigo from avaliacao.tipo_entidade where tipo_entidade_id = cast(?1 as uuid)")
                .setParameter(1, i.tipoEntidadeAvaliadaId).getSingleResult();
        if ("INSTITUICAO".equals(tipoContextoCodigo) && i.tiposEntidadeAlvoIds.contains(i.tipoEntidadeAvaliadaId)) {
            exec("insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo) " +
                    "values (cast(?1 as uuid), ?2, ?3) on conflict (tipo_entidade_id, referencia_externa) do nothing",
                    i.tipoEntidadeAvaliadaId, "instrumento:" + id, i.titulo);
            exec("insert into avaliacao.instrumento_entidade_avaliada " +
                            "(instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id) " +
                            "select cast(?1 as uuid), cast(?2 as uuid), e.entidade_id from avaliacao.entidade e " +
                    "where e.tipo_entidade_id = cast(?2 as uuid) and e.referencia_externa = ?3 " +
                    "on conflict (instrumento_avaliativo_id, entidade_id) do nothing",
                    id, i.tipoEntidadeAvaliadaId, "instrumento:" + id);
        }
        exec("insert into avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id) " +
                        "select i.instrumento_avaliativo_id, ita.tipo_entidade_alvo_id, e.entidade_id " +
                        "from avaliacao.instrumento_avaliativo i join avaliacao.instrumento_tipo_alvo ita " +
                        "on ita.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                        "join avaliacao.tipo_entidade te on te.tipo_entidade_id = ita.tipo_entidade_alvo_id " +
                        "join avaliacao.entidade e on e.tipo_entidade_id = ita.tipo_entidade_alvo_id " +
                        "where i.instrumento_avaliativo_id = cast(?1 as uuid) " +
                        "and (e.referencia_externa like 'academico:%' " +
                        "or (te.codigo = 'INSTITUICAO' and e.referencia_externa = 'instrumento:' || i.instrumento_avaliativo_id)) " +
                        "on conflict (instrumento_avaliativo_id, entidade_id) do nothing", id);
        return buscarInstrumento(id);
    }

    public boolean excluir(String id) {
        Instrumento i = buscarInstrumento(id);
        if (i == null) return false;
        if (!"RASCUNHO".equals(i.status)) throw new IllegalArgumentException("Só é possível excluir instrumentos em rascunho.");
        List<String> questoes = idsQuestoes(id);
        exec("delete from avaliacao.instrumento_avaliativo where instrumento_avaliativo_id = cast(?1 as uuid)", id);
        removerQuestoesOrfas(questoes);
        return true;
    }

    // ------------------------------------------------------------------ internos

    private void substituirRespondentes(String id, List<Respondente> respondentes) {
        exec("delete from avaliacao.instrumento_respondente where instrumento_avaliativo_id = cast(?1 as uuid)", id);
        for (Respondente r : respondentes) {
            exec("insert into avaliacao.instrumento_respondente (instrumento_avaliativo_id, tipo_entidade_respondente_id, tipo_vinculo) " +
                            "values (cast(?1 as uuid), cast(?2 as uuid), cast(?3 as text))",
                    id, r.tipoEntidadeId, blank(r.tipoVinculo) ? null : r.tipoVinculo.trim());
        }
    }

    private void substituirTiposAlvo(String id, Instrumento dto) {
        List<String> tiposAlvo = dto.tiposEntidadeAlvoIds == null || dto.tiposEntidadeAlvoIds.isEmpty()
                ? List.of(dto.tipoEntidadeAvaliadaId) : dto.tiposEntidadeAlvoIds;
        exec("delete from avaliacao.instrumento_tipo_alvo where instrumento_avaliativo_id = cast(?1 as uuid)", id);
        for (String tipoId : new LinkedHashSet<>(tiposAlvo)) {
            exec("insert into avaliacao.instrumento_tipo_alvo (instrumento_avaliativo_id, tipo_entidade_alvo_id) " +
                    "values (cast(?1 as uuid), cast(?2 as uuid))", id, tipoId);
        }
    }

    private void substituirEstrutura(String id, List<Pagina> paginas, List<Sessao> sessoes, String usuario) {
        List<String> anteriores = idsQuestoes(id);
        // on delete cascade remove as associações com questões
        exec("delete from avaliacao.instrumento_sessao where instrumento_avaliativo_id = cast(?1 as uuid)", id);

        int ordemPagina = 1;
        for (Pagina p : paginas) {
            String paginaId = UUID.randomUUID().toString();
            exec("insert into avaliacao.instrumento_sessao (instrumento_sessao_id, instrumento_avaliativo_id, tipo, titulo, descricao, ordem) " +
                    "values (cast(?1 as uuid), cast(?2 as uuid), 'PAGINA', ?3, cast(?4 as text), ?5)",
                    paginaId, id, p.titulo.trim(), p.descricao, ordemPagina++);
        }
        int ordemSessao = 1;
        for (Sessao s : sessoes) {
            String sessaoId = UUID.randomUUID().toString();
            exec("insert into avaliacao.instrumento_sessao (instrumento_sessao_id, instrumento_avaliativo_id, tipo, titulo, descricao, ordem) " +
                            "values (cast(?1 as uuid), cast(?2 as uuid), 'SESSAO', ?3, cast(?4 as text), ?5)",
                    sessaoId, id, s.titulo.trim(), s.descricao, ordemSessao++);
            int ordemQuestao = 1;
            for (Questao q : s.questoes) {
                String questaoId = upsertQuestao(q, usuario);
                exec("insert into avaliacao.sessao_questao (instrumento_sessao_id, questao_id, ordem, obrigatoria, peso) " +
                                "values (cast(?1 as uuid), cast(?2 as uuid), ?3, ?4, cast(?5 as numeric))",
                        sessaoId, questaoId, ordemQuestao++, q.obrigatoria, q.peso);
            }
        }
        removerQuestoesOrfas(anteriores);
    }

    private String upsertQuestao(Questao q, String usuario) {
        String conjunto = "DESCRITIVA".equals(q.tipo) ? null : q.conjuntoOpcaoId;
        if (!blank(q.questaoId) && count("select count(*) from avaliacao.questao where questao_id = cast(?1 as uuid)", q.questaoId) > 0) {
            exec("update avaliacao.questao set enunciado = ?2, tipo = ?3, assunto = cast(?4 as text), conjunto_opcao_id = cast(?5 as uuid) " +
                    "where questao_id = cast(?1 as uuid)", q.questaoId, q.enunciado.trim(), q.tipo, q.assunto, conjunto);
            return q.questaoId;
        }
        String novo = UUID.randomUUID().toString();
        exec("insert into avaliacao.questao (questao_id, conjunto_opcao_id, enunciado, tipo, assunto, criado_por) " +
                "values (cast(?1 as uuid), cast(?2 as uuid), ?3, ?4, cast(?5 as text), ?6)",
                novo, conjunto, q.enunciado.trim(), q.tipo, q.assunto, usuario);
        return novo;
    }

    private List<String> idsQuestoes(String instrumentoId) {
        List<String> ids = new ArrayList<>();
        for (Object[] r : rows("select cast(sq.questao_id as text) from avaliacao.sessao_questao sq " +
                "join avaliacao.instrumento_sessao s on s.instrumento_sessao_id = sq.instrumento_sessao_id " +
                "where s.instrumento_avaliativo_id = cast(?1 as uuid)", instrumentoId)) {
            ids.add((String) r[0]);
        }
        return ids;
    }

    /** Remove questões que deixaram de ser usadas por qualquer instrumento. */
    private void removerQuestoesOrfas(List<String> candidatas) {
        for (String qid : candidatas) {
            exec("delete from avaliacao.questao where questao_id = cast(?1 as uuid) " +
                    "and not exists (select 1 from avaliacao.sessao_questao where questao_id = cast(?1 as uuid))", qid);
        }
    }

    private void validar(Instrumento d, boolean validarRespondentes) {
        if (d == null) throw new IllegalArgumentException("Dados do instrumento não informados.");
        if (blank(d.titulo)) throw new IllegalArgumentException("Informe o título do instrumento.");
        if (blank(d.cicloReferencia)) throw new IllegalArgumentException("Informe o ciclo de referência (ex.: 2026/1).");
        if (d.modulo == null || !MODULOS.contains(d.modulo)) throw new IllegalArgumentException("Módulo inválido.");
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
            if (count("select count(*) from avaliacao.tipo_entidade where tipo_entidade_id = cast(?1 as uuid) and pode_ser_avaliada", tipoAlvo) != 1)
                throw new IllegalArgumentException("Tipo de alvo inválido.");
        }
        String contextoCodigo = (String) em.createNativeQuery("select codigo from avaliacao.tipo_entidade where tipo_entidade_id = cast(?1 as uuid)")
                .setParameter(1, d.tipoEntidadeAvaliadaId).getSingleResult();
        if ("TURMA".equals(contextoCodigo)) {
            Set<String> codigos = new HashSet<>();
            for (String tipoId : d.tiposEntidadeAlvoIds) {
                codigos.add((String) em.createNativeQuery("select codigo from avaliacao.tipo_entidade where tipo_entidade_id = cast(?1 as uuid)")
                        .setParameter(1, tipoId).getSingleResult());
            }
            if (codigos.isEmpty() || !Set.of("DOCENTE", "ALUNO").containsAll(codigos))
                throw new IllegalArgumentException("Uma avaliação de turma pode incluir docentes e alunos como alvos.");
        } else if (d.tiposEntidadeAlvoIds.size() != 1 || !d.tiposEntidadeAlvoIds.contains(d.tipoEntidadeAvaliadaId)) {
            throw new IllegalArgumentException("Selecione o próprio tipo avaliado como alvo.");
        }
        if (validarRespondentes) for (Respondente r : d.respondentes) {
            if (blank(r.tipoEntidadeId)) throw new IllegalArgumentException("Respondente sem tipo definido.");
            if (!vistos.add(r.tipoEntidadeId)) throw new IllegalArgumentException("Público respondente repetido.");
            List<?> codigoRespondente = em.createNativeQuery("select codigo from avaliacao.tipo_entidade where tipo_entidade_id = cast(?1 as uuid) and pode_responder")
                    .setParameter(1, r.tipoEntidadeId).getResultList();
            if (codigoRespondente.isEmpty()) throw new IllegalArgumentException("Público respondente inválido.");
            String vinculo = blank(r.tipoVinculo) ? null : r.tipoVinculo.trim();
            String codigo = (String) codigoRespondente.get(0);
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

    private static String ts(String coluna) {
        return String.format(TS, coluna);
    }

    private Query query(String sql, Object... params) {
        Query q = em.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) q.setParameter(i + 1, params[i]);
        return q;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Object... params) {
        List<?> res = query(sql, params).getResultList();
        List<Object[]> out = new ArrayList<>();
        for (Object o : res) out.add(o instanceof Object[] arr ? arr : new Object[]{o});
        return out;
    }

    private long count(String sql, Object... params) {
        return ((Number) query(sql, params).getSingleResult()).longValue();
    }

    private void exec(String sql, Object... params) {
        query(sql, params).executeUpdate();
    }
}
