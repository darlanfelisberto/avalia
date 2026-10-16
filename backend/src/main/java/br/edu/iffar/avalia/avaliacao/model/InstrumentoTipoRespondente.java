package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "instrumento_tipo_respondente", schema = "avaliacao")
public class InstrumentoTipoRespondente extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrumento_tipo_respondente_id")
    private UUID instrumentoTipoRespondenteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    private InstrumentoAvaliativo instrumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_entidade_respondente_id", nullable = false)
    private TipoEntidade tipoEntidadeRespondente;

    @Column(name = "tipo_vinculo", length = 30)
    private String tipoVinculo;

    public UUID getId() { return instrumentoTipoRespondenteId; }
    public UUID getInstrumentoTipoRespondenteId() { return instrumentoTipoRespondenteId; }
    public void setInstrumentoTipoRespondenteId(UUID instrumentoTipoRespondenteId) { this.instrumentoTipoRespondenteId = instrumentoTipoRespondenteId; }
    public InstrumentoAvaliativo getInstrumento() { return instrumento; }
    public void setInstrumento(InstrumentoAvaliativo instrumento) { this.instrumento = instrumento; }
    public TipoEntidade getTipoEntidadeRespondente() { return tipoEntidadeRespondente; }
    public void setTipoEntidadeRespondente(TipoEntidade tipoEntidadeRespondente) { this.tipoEntidadeRespondente = tipoEntidadeRespondente; }
    public String getTipoVinculo() { return tipoVinculo; }
    public void setTipoVinculo(String tipoVinculo) { this.tipoVinculo = tipoVinculo; }

    @Override
    public UUID getMMId() {
        return this.instrumentoTipoRespondenteId;
    }
}
