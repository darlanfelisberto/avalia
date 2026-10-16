package br.edu.iffar.avalia.core.dao;

import br.edu.iffar.avalia.academico.model.PessoaAcademica;
import br.edu.iffar.avalia.core.model.AuthSessao;
import br.edu.iffar.avalia.core.model.UsuarioPermissao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class AuthDao {
    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    public UsuarioPermissao buscarUsuarioAtivo(String cpf) {
        return em.createQuery("select u from UsuarioPermissao u where u.cpf = :cpf and u.ativo = true", UsuarioPermissao.class)
                .setParameter("cpf", cpf).getResultStream().findFirst().orElse(null);
    }

    public void persistirUsuario(UsuarioPermissao usuario) {
        em.persist(usuario);
    }

    public void registrarSessao(String tokenHash, String cpf, String papeis) {
        em.flush();
        AuthSessao sessao = new AuthSessao();
        sessao.setTokenHash(tokenHash);
        sessao.setCpf(cpf);
        sessao.setPapeis(papeis);
        sessao.setExpiraEm(OffsetDateTime.now().plusHours(12));
        em.persist(sessao);
    }

    public AuthSessao buscarSessaoValida(String tokenHash) {
        AuthSessao sessao = em.find(AuthSessao.class, tokenHash);
        return sessao != null && sessao.getExpiraEm().isAfter(OffsetDateTime.now()) ? sessao : null;
    }

    public void encerrarSessao(String tokenHash) {
        AuthSessao sessao = em.find(AuthSessao.class, tokenHash);
        if (sessao != null) em.remove(sessao);
    }

    public PessoaAcademica buscarPessoaPorCpf(String cpf) {
        @SuppressWarnings("unchecked")
        List<PessoaAcademica> pessoas = em.createNativeQuery(
                        """
                            select p.* from academico.pessoa p
                            where regexp_replace(p.cpf_cnpj, '[^0-9]', '', 'g') = ?1 limit 1
                            """, PessoaAcademica.class)
                .setParameter(1, cpf).getResultList();
        return pessoas.isEmpty() ? null : pessoas.get(0);
    }

    public boolean existeAluno(int pessoaId) {
        return contar("select count(*) from academico.aluno where id_pessoa = ?1", pessoaId);
    }

    public boolean existeDocenteTurma(int pessoaId) {
        return contar("""
            select count(*) from academico.docente_turma dt
            join academico.servidor s on s.id = dt.id_servidor where s.id_pessoa = ?1
            """, pessoaId);
    }

    public boolean existeServidor(int pessoaId) {
        return contar("select count(*) from academico.servidor where id_pessoa = ?1", pessoaId);
    }

    public String campusServidor(int pessoaId) {
        return primeiraString("""
            select u.nome from academico.servidor s
            join academico.unidade u on u.id = s.id_unidade_lotacao where s.id_pessoa = ?1 limit 1
            """, pessoaId);
    }

    public String campusAluno(int pessoaId) {
        return primeiraString("""
            select u.nome from academico.aluno a
            join academico.curso c on c.id = a.id_curso
            join academico.unidade u on u.id = c.id_unidade where a.id_pessoa = ?1 limit 1
            """, pessoaId);
    }

    private boolean contar(String sql, int pessoaId) {
        return ((Number) em.createNativeQuery(sql).setParameter(1, pessoaId).getSingleResult()).longValue() > 0;
    }

    private String primeiraString(String sql, int pessoaId) {
        List<?> valores = em.createNativeQuery(sql).setParameter(1, pessoaId).getResultList();
        return valores.isEmpty() || valores.get(0) == null ? null : valores.get(0).toString();
    }
}
