package br.edu.iffar.avalia.avaliacao.dao;

import br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.model.Entidade;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoEntidadeAvaliada;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoRespondente;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoAlvo;
import br.edu.iffar.avalia.avaliacao.model.QuestaoAvaliativa;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Projeções e operações em lote dos instrumentos que exigem SQL PostgreSQL. */
@ApplicationScoped
public class InstrumentoDao {
    public record PaginaEntidades(List<InstrumentoAvaliativo> itens, boolean temProxima) {}

    private static final String TS = "to_char(%s at time zone 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')";

    @Inject
    private AvaliacaoDao dao;

    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    private static String ts(String coluna) {
        return String.format(TS, coluna);
    }

    public PaginaEntidades listarInstrumentosBase(int pagina, int tamanho) {
        List<InstrumentoAvaliativo> instrumentos = em.createQuery("""
                select i from InstrumentoAvaliativo i
                left join fetch i.origem
                left join fetch i.tipoEntidadeAvaliada
                order by i.atualizadoEm desc, i.id desc
                """, InstrumentoAvaliativo.class)
                .setFirstResult((pagina - 1) * tamanho)
                .setMaxResults(tamanho)
                .getResultList();
        boolean temProxima = instrumentos.size() == tamanho;

        if (instrumentos.isEmpty()) return new PaginaEntidades(instrumentos, temProxima);
        List<UUID> ids = instrumentos.stream().map(InstrumentoAvaliativo::getId).toList();

        // Busca cada coleção em uma consulta separada para carregar o grafo sem gerar
        // multiplicação de linhas nem conflito entre várias coleções List no Hibernate.
        em.createQuery("""
                select distinct i from InstrumentoAvaliativo i
                left join fetch i.respondentes r
                left join fetch r.tipoEntidadeRespondente
                where i.id in :ids
                """, InstrumentoAvaliativo.class).setParameter("ids", ids).getResultList();
        em.createQuery("""
                select distinct i from InstrumentoAvaliativo i
                left join fetch i.tiposAlvo ta
                left join fetch ta.tipoAlvo
                where i.id in :ids
                """, InstrumentoAvaliativo.class).setParameter("ids", ids).getResultList();
        em.createQuery("""
                select distinct i from InstrumentoAvaliativo i
                left join fetch i.entidadesAvaliadas ea
                left join fetch ea.tipoAlvo
                left join fetch ea.entidade e
                left join fetch e.tipo
                where i.id in :ids
                """, InstrumentoAvaliativo.class).setParameter("ids", ids).getResultList();
        em.createQuery("""
                select distinct i from InstrumentoAvaliativo i
                left join fetch i.sessoes s
                left join fetch s.questoes sq
                left join fetch sq.questao q
                left join fetch q.conjunto c
                left join fetch c.opcoes
                where i.id in :ids
                """, InstrumentoAvaliativo.class).setParameter("ids", ids).getResultList();
        return new PaginaEntidades(instrumentos, temProxima);
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
