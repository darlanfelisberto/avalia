package br.edu.iffar.avalia.avaliacao.dao;

import br.edu.iffar.avalia.avaliacao.model.TipoEntidade;
import br.edu.iffar.avalia.avaliacao.model.TipoEntidadePermissao;
import br.edu.iffar.avalia.core.exception.ValidacaoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.UUID;

/** Consultas e persistência do catálogo de tipos e suas permissões de avaliação. */
@ApplicationScoped
public class TipoEntidadeDao {
    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    public List<TipoEntidade> listar() {
        return em.createQuery("select t from TipoEntidade t order by t.nome", TipoEntidade.class)
                .getResultList();
    }

    public TipoEntidade buscar(UUID id) {
        return em.find(TipoEntidade.class, id);
    }

    public String codigo(UUID id) {
        TipoEntidade tipo = buscar(id);
        if (tipo == null) throw new ValidacaoException("Tipo de entidade não encontrado.");
        return tipo.getCodigo();
    }

    public List<TipoEntidadePermissao> listarPermissoes() {
        return em.createQuery("""
                        select p from TipoEntidadePermissao p
                        join fetch p.respondente join fetch p.avaliado
                        order by p.respondente.nome, p.avaliado.nome
                        """, TipoEntidadePermissao.class)
                .getResultList();
    }

    public boolean podeAvaliar(UUID respondenteId, UUID avaliadoId) {
        return em.createQuery("""
                        select count(p) from TipoEntidadePermissao p
                        where p.respondente.id = :respondente and p.avaliado.id = :avaliado
                        """, Long.class)
                .setParameter("respondente", respondenteId)
                .setParameter("avaliado", avaliadoId)
                .getSingleResult() > 0;
    }

    public void removerPermissoes() {
        em.flush();
        em.createQuery("delete from TipoEntidadePermissao").executeUpdate();
        em.clear();
    }

    public void salvarPermissoes(List<TipoEntidadePermissao> permissoes) {
        for (TipoEntidadePermissao permissao : permissoes) em.persist(permissao);
        em.flush();
    }

    public TipoEntidade referencia(UUID id) {
        return em.getReference(TipoEntidade.class, id);
    }
}
