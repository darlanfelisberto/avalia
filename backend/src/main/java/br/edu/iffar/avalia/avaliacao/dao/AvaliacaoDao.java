package br.edu.iffar.avalia.avaliacao.dao;

import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Acesso ao schema avaliacao. SQL nativo fica reservado a consultas de projeção e cargas em lote. */
@ApplicationScoped
public class AvaliacaoDao {
    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    public <T> T buscar(Class<T> tipo, UUID id) {
        return em.find(tipo, id);
    }

    public <T> T referencia(Class<T> tipo, UUID id) {
        return em.getReference(tipo, id);
    }

    public void persistir(Object entidade) {
        em.persist(entidade);
    }

    public void remover(Object entidade) {
        em.remove(entidade);
    }

    public void flush() {
        em.flush();
    }

    public InstrumentoAvaliativo buscarInstrumento(UUID id) {
        return buscar(InstrumentoAvaliativo.class, id);
    }

    public List<InstrumentoAvaliativo> listarInstrumentos() {
        return em.createQuery("select i from InstrumentoAvaliativo i order by i.atualizadoEm desc", InstrumentoAvaliativo.class)
                .getResultList();
    }

    public boolean existeInstrumento(String titulo, String ciclo, UUID tipoId, UUID ignorarId) {
        String jpql = """
            select count(i) from InstrumentoAvaliativo i
            where i.titulo = :titulo and i.cicloReferencia = :ciclo and i.tipoEntidadeAvaliada.id = :tipo
            """;
        if (ignorarId != null) jpql += " and i.id <> :ignorarId";
        var query = em.createQuery(jpql, Long.class)
                .setParameter("titulo", titulo)
                .setParameter("ciclo", ciclo)
                .setParameter("tipo", tipoId);
        if (ignorarId != null) query.setParameter("ignorarId", ignorarId);
        return query.getSingleResult() > 0;
    }

    public List<ConjuntoOpcao> listarConjuntos() {
        return em.createQuery("select distinct c from ConjuntoOpcao c left join fetch c.opcoes where c.exclusivo is null or c.exclusivo = false order by c.nome", ConjuntoOpcao.class)
                .getResultList();
    }

    public boolean existeConjunto(String nome) {
        return em.createQuery("select count(c) from ConjuntoOpcao c where lower(c.nome) = lower(:nome)", Long.class)
                .setParameter("nome", nome).getSingleResult() > 0;
    }

    /** O UPSERT mantém a identidade do respondente estável sob envios concorrentes. */
    public UUID buscarOuCriarRespondente(String cpf, String nome, String codigoTipo) {
        String id = (String) nativeQuery("""
            insert into avaliacao.entidade
            (tipo_entidade_id, referencia_externa, rotulo)
            select tipo_entidade_id, ?1, ?2 from avaliacao.tipo_entidade where codigo = ?3
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo
            returning cast(entidade_id as text)
            """)
                .setParameter(1, cpf).setParameter(2, nome).setParameter(3, codigoTipo).getSingleResult();
        return UUID.fromString(id);
    }

    public List<Object[]> questoesParaResposta(UUID instrumentoId) {
        return rows("""
            select cast(sq.sessao_questao_id as text), q.tipo,
            cast(q.conjunto_opcao_id as text), sq.obrigatoria
            from avaliacao.sessao_questao sq
            join avaliacao.instrumento_sessao s on s.instrumento_sessao_id = sq.instrumento_sessao_id
            join avaliacao.questao q on q.questao_id = sq.questao_id
            where s.instrumento_avaliativo_id = cast(?1 as uuid) and s.tipo = 'SESSAO'
            """,
                instrumentoId.toString());
    }

    public void removerRespondentes(UUID instrumentoId) {
        exec("delete from avaliacao.instrumento_tipo_respondente where instrumento_avaliativo_id = cast(?1 as uuid)",
                instrumentoId.toString());
    }

    public void removerTiposAlvo(UUID instrumentoId) {
        exec("delete from avaliacao.instrumento_tipo_alvo where instrumento_avaliativo_id = cast(?1 as uuid)",
                instrumentoId.toString());
    }

    public void removerSessoes(UUID instrumentoId) {
        exec("delete from avaliacao.instrumento_sessao where instrumento_avaliativo_id = cast(?1 as uuid)",
                instrumentoId.toString());
    }

    public Query nativeQuery(String sql) {
        return em.createNativeQuery(sql);
    }

    public Query query(String sql, Object... params) {
        Query query = nativeQuery(sql);
        for (int i = 0; i < params.length; i++) query.setParameter(i + 1, params[i]);
        return query;
    }

    public List<Object[]> rows(String sql, Object... params) {
        List<?> resultado = query(sql, params).getResultList();
        List<Object[]> linhas = new ArrayList<>(resultado.size());
        for (Object linha : resultado) linhas.add(linha instanceof Object[] colunas ? colunas : new Object[]{linha});
        return linhas;
    }

    public long count(String sql, Object... params) {
        return ((Number) query(sql, params).getSingleResult()).longValue();
    }

    public int exec(String sql, Object... params) {
        return query(sql, params).executeUpdate();
    }
}
