package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "tipo_entidade", schema = "avaliacao")
@Cacheable
public class TipoEntidade extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tipo_entidade_id")
    private UUID tipoEntidadeId;

    @Column(nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "pode_ser_avaliada", nullable = false)
    private boolean podeSerAvaliada = true;

    @Column(name = "pode_responder", nullable = false)
    private boolean podeResponder = true;

    public UUID getId() { return tipoEntidadeId; }
    public UUID getTipoEntidadeId() { return tipoEntidadeId; }
    public void setTipoEntidadeId(UUID tipoEntidadeId) { this.tipoEntidadeId = tipoEntidadeId; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public boolean isPodeSerAvaliada() { return podeSerAvaliada; }
    public boolean isPodeResponder() { return podeResponder; }

    @Override
    public UUID getMMId() {
        return this.tipoEntidadeId;
    }
}
