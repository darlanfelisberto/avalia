package br.edu.iffar.avalia.academico.dao;

import br.edu.iffar.avalia.academico.model.Pessoa;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/** Consultas às pessoas importadas para o espelho acadêmico. */
@ApplicationScoped
public class PessoaDao {
    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    public Pessoa buscar(Integer id) {
        return em.find(Pessoa.class, id);
    }

    public Pessoa buscarPorCpf(String cpfCnpj) {
        return em.createQuery("""
                                select p from Pessoa p 
                                left join fetch p.tipoEntidades t
                                where p.cpfCnpj = :cpfCnpj order by p.idPessoa""",
                        Pessoa.class)
                .setParameter("cpfCnpj", cpfCnpj)
                .getSingleResult();
    }

    public List<Pessoa> buscarPorNome(String nome) {
        return em.createQuery(
                        "select p from Pessoa p where lower(p.nome) like lower(:nome) order by p.nome",
                        Pessoa.class)
                .setParameter("nome", "%" + nome + "%")
                .getResultList();
    }
}
