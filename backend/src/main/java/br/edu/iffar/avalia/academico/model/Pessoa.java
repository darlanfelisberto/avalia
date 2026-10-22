package br.edu.iffar.avalia.academico.model;

import br.com.feliva.sharedClass.db.Model;
import br.edu.iffar.avalia.avaliacao.model.TipoEntidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.List;

/** Pessoa importada do SIGAA, consultada sem alterar o espelho acadêmico. */
@Entity
@Table(name = "pessoa", schema = "academico")
public class Pessoa extends Model<Integer> {
    @Id
    @Column(name = "id_pessoa")
    private Integer idPessoa;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(length = 200)
    private String email;

    @Column(name = "cpf_cnpj", length = 14)
    private String cpfCnpj;

    @ManyToMany
    @JoinTable(
            name = "mv_pessoa_tipos_entidade",
            schema = "avaliacao",
            joinColumns = @JoinColumn(name = "id_pessoa", referencedColumnName = "id_pessoa"),
            inverseJoinColumns = @JoinColumn(name = "tipo_entidade_id", referencedColumnName = "tipo_entidade_id")
    )
    @Immutable
    private List<TipoEntidade> tipoEntidades;

    public Integer getIdPessoa() { return idPessoa; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCpfCnpj() { return cpfCnpj; }
    public List<TipoEntidade> getTipoEntidades() { return tipoEntidades; }

    @Override
    public Integer getMMId() {
        return idPessoa;
    }
}
