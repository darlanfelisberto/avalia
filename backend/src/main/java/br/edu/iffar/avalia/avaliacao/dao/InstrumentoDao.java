package br.edu.iffar.avalia.avaliacao.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** Projeções e operações em lote dos instrumentos que exigem SQL PostgreSQL. */
@ApplicationScoped
public class InstrumentoDao {
    private static final String TS = "to_char(%s at time zone 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')";

    @Inject
    private AvaliacaoDao dao;

    private static String ts(String coluna) {
        return String.format(TS, coluna);
    }

    public List<Object[]> listarInstrumentosBase() {
        return dao.rows("""
                select cast(i.instrumento_avaliativo_id as text), i.titulo, i.ciclo_referencia, i.status,
                    %s, %s, t.nome,
                    (select count(*) from avaliacao.sessao_questao sq
                     join avaliacao.instrumento_sessao s
                       on s.instrumento_sessao_id = sq.instrumento_sessao_id
                     where s.instrumento_avaliativo_id = i.instrumento_avaliativo_id)
                from avaliacao.instrumento_avaliativo i
                join avaliacao.tipo_entidade t on t.tipo_entidade_id = i.tipo_entidade_avaliada_id
                order by i.atualizado_em desc
                """.formatted(ts("i.inicio_aplicacao"), ts("i.fim_aplicacao")));
    }

    public List<Object[]> listarTiposAlvo(String id) {
        return dao.rows("""
            select cast(ita.tipo_entidade_alvo_id as text), te.nome
            from avaliacao.instrumento_tipo_alvo ita join avaliacao.tipo_entidade te
            on te.tipo_entidade_id = ita.tipo_entidade_alvo_id
            where ita.instrumento_avaliativo_id = cast(?1 as uuid) order by te.nome
            """, id);
    }

    public List<Object[]> listarTodosTiposAlvo() {
        return dao.rows("""
            select cast(ita.instrumento_avaliativo_id as text),
            cast(ita.tipo_entidade_alvo_id as text), te.nome
            from avaliacao.instrumento_tipo_alvo ita join avaliacao.tipo_entidade te
            on te.tipo_entidade_id = ita.tipo_entidade_alvo_id order by te.nome
            """);
    }

    public List<Object[]> buscarInstrumentoBase(String id) {
        return dao.rows("""
                select cast(i.instrumento_avaliativo_id as text), cast(i.tipo_entidade_avaliada_id as text), t.nome,
                    i.titulo, i.descricao, i.ciclo_referencia,
                    %s, %s, i.anonimo, i.permite_edicao, i.status, i.criado_por, %s
                from avaliacao.instrumento_avaliativo i
                join avaliacao.tipo_entidade t on t.tipo_entidade_id = i.tipo_entidade_avaliada_id
                where i.instrumento_avaliativo_id = cast(?1 as uuid)
                """.formatted(ts("i.inicio_aplicacao"), ts("i.fim_aplicacao"), ts("i.atualizado_em")), id);
    }

    public List<Object[]> listarRespondentes(String id) {
        return dao.rows("""
            select cast(tipo_entidade_respondente_id as text), tipo_vinculo
            from avaliacao.instrumento_respondente where instrumento_avaliativo_id = cast(?1 as uuid)
            """, id);
    }

    public List<Object[]> listarSessoes(String id) {
        return dao.rows("""
            select cast(instrumento_sessao_id as text), cast(instrumento_sessao_pai_id as text), tipo, titulo, descricao, ordem
            from avaliacao.instrumento_sessao where instrumento_avaliativo_id = cast(?1 as uuid) order by ordem
            """, id);
    }

    public List<Object[]> listarQuestoes(String id) {
        return dao.rows("""
            select cast(sq.instrumento_sessao_id as text), cast(q.questao_id as text), q.enunciado, q.tipo, q.assunto,
            cast(q.conjunto_opcao_id as text), sq.obrigatoria, sq.peso, sq.ordem, cast(sq.sessao_questao_id as text)
            from avaliacao.sessao_questao sq
            join avaliacao.questao q on q.questao_id = sq.questao_id
            join avaliacao.instrumento_sessao s on s.instrumento_sessao_id = sq.instrumento_sessao_id
            where s.instrumento_avaliativo_id = cast(?1 as uuid) order by sq.ordem
            """, id);
    }

    public List<Object[]> listarIdsQuestoes(String id) {
        return dao.rows("""
            select cast(sq.questao_id as text) from avaliacao.sessao_questao sq
            join avaliacao.instrumento_sessao s on s.instrumento_sessao_id = sq.instrumento_sessao_id
            where s.instrumento_avaliativo_id = cast(?1 as uuid)
            """, id);
    }

    public void criarEntidadeInstitucional(String tipoId, String referencia, String titulo) {
        dao.exec("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            values (cast(?1 as uuid), ?2, ?3) on conflict (tipo_entidade_id, referencia_externa) do nothing
            """,
                    tipoId, referencia, titulo);
    }

    public void vincularEntidadeInstitucional(String id, String tipoId, String referencia) {
        dao.exec("""
            insert into avaliacao.instrumento_entidade_avaliada
            (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id)
            select cast(?1 as uuid), cast(?2 as uuid), e.entidade_id from avaliacao.entidade e
            where e.tipo_entidade_id = cast(?2 as uuid) and e.referencia_externa = ?3
            on conflict (instrumento_avaliativo_id, entidade_id) do nothing
            """,
                    id, tipoId, referencia);
    }

    public void popularAlvos(String id) {
        dao.exec("""
            insert into avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id)
            select i.instrumento_avaliativo_id, ita.tipo_entidade_alvo_id, e.entidade_id
            from avaliacao.instrumento_avaliativo i join avaliacao.instrumento_tipo_alvo ita
            on ita.instrumento_avaliativo_id = i.instrumento_avaliativo_id
            join avaliacao.tipo_entidade te on te.tipo_entidade_id = ita.tipo_entidade_alvo_id
            join avaliacao.entidade e on e.tipo_entidade_id = ita.tipo_entidade_alvo_id
            where i.instrumento_avaliativo_id = cast(?1 as uuid)
            and (e.referencia_externa like 'academico:%'
            or (te.codigo = 'INSTITUICAO' and e.referencia_externa = 'instrumento:' || i.instrumento_avaliativo_id))
            on conflict (instrumento_avaliativo_id, entidade_id) do nothing
            """, id);
    }

    public void removerQuestaoOrfa(String id) {
        dao.exec("""
            delete from avaliacao.questao where questao_id = cast(?1 as uuid)
            and not exists (select 1 from avaliacao.sessao_questao where questao_id = cast(?1 as uuid))
            """, id);
    }
}
