package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "entidade_vinculo", schema = "avaliacao")
public class EntidadeVinculo extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entidade_vinculo_id")
    private UUID entidadeVinculoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entidade_origem_id", nullable = false)
    private Entidade origem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entidade_destino_id", nullable = false)
    private Entidade destino;

    @Column(name = "tipo_vinculo", nullable = false, length = 30)
    private String tipoVinculo;

    public UUID getId() { return entidadeVinculoId; }
    public UUID getEntidadeVinculoId() { return entidadeVinculoId; }
    public void setEntidadeVinculoId(UUID entidadeVinculoId) { this.entidadeVinculoId = entidadeVinculoId; }
    public Entidade getOrigem() { return origem; }
    public void setOrigem(Entidade origem) { this.origem = origem; }
    public Entidade getDestino() { return destino; }
    public void setDestino(Entidade destino) { this.destino = destino; }
    public String getTipoVinculo() { return tipoVinculo; }
    public void setTipoVinculo(String tipoVinculo) { this.tipoVinculo = tipoVinculo; }

    @Override
    public UUID getMMId() {
        return this.entidadeVinculoId;
    }
}
