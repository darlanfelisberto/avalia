package br.edu.iffar.avalia.academico.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pessoa importada do SIGAA, consultada sem alterar o espelho acadêmico. */
@Entity
@Table(name = "pessoa", schema = "academico")
public class PessoaAcademica {
    @Id
    private Integer id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(length = 200)
    private String email;

    @Column(name = "cpf_cnpj", length = 14)
    private String cpfCnpj;

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCpfCnpj() { return cpfCnpj; }
}
