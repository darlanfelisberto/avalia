package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * esta entidade existe para facilitar para a api a remoção e adicao dos tipos de entidades
 * respondentes do intrumento
 */
@Entity
@Table(name = "instrumento_tipo_respondente", schema = "avaliacao")
public class InstrumentoTipoRespondente extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrumento_tipo_respondente_id")
    private UUID instrumentoTipoRespondenteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    @NotNull(message = "O público respondente precisa estar vinculado a um instrumento.")
    private InstrumentoAvaliativo instrumentoAvaliativo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_entidade_id", nullable = false)
    @NotNull(message = "Informe o tipo de entidade respondente.")
    private TipoEntidade tipoEntidade;

    public UUID getId() { return instrumentoTipoRespondenteId; }
    public UUID getInstrumentoTipoRespondenteId() { return instrumentoTipoRespondenteId; }
    public void setInstrumentoTipoRespondenteId(UUID instrumentoTipoRespondenteId) { this.instrumentoTipoRespondenteId = instrumentoTipoRespondenteId; }
    public InstrumentoAvaliativo getInstrumentoAvaliativo() { return instrumentoAvaliativo; }
    public void setInstrumentoAvaliativo(InstrumentoAvaliativo instrumento) { this.instrumentoAvaliativo = instrumento; }
    public TipoEntidade getTipoEntidade() { return tipoEntidade; }
    public void setTipoEntidade(TipoEntidade tipoEntidadeRespondente) { this.tipoEntidade = tipoEntidadeRespondente; }

    @Override
    public UUID getMMId() {
        return this.instrumentoTipoRespondenteId;
    }
}
