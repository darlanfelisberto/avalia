package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "instrumento_entidade_avaliada", schema = "avaliacao")
public class InstrumentoEntidadeAvaliada extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrumento_entidade_avaliada_id")
    private UUID instrumentoEntidadeAvaliadaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    private InstrumentoAvaliativo instrumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_entidade_alvo_id", nullable = false)
    private TipoEntidade tipoAlvo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entidade_id", nullable = false)
    private Entidade entidade;

    public UUID getId() { return instrumentoEntidadeAvaliadaId; }
    public UUID getInstrumentoEntidadeAvaliadaId() { return instrumentoEntidadeAvaliadaId; }
    public void setInstrumentoEntidadeAvaliadaId(UUID instrumentoEntidadeAvaliadaId) { this.instrumentoEntidadeAvaliadaId = instrumentoEntidadeAvaliadaId; }
    public InstrumentoAvaliativo getInstrumento() { return instrumento; }
    public void setInstrumento(InstrumentoAvaliativo instrumento) { this.instrumento = instrumento; }
    public TipoEntidade getTipoAlvo() { return tipoAlvo; }
    public void setTipoAlvo(TipoEntidade tipoAlvo) { this.tipoAlvo = tipoAlvo; }
    public Entidade getEntidade() { return entidade; }
    public void setEntidade(Entidade entidade) { this.entidade = entidade; }

    @Override
    public UUID getMMId() {
        return this.instrumentoEntidadeAvaliadaId;
    }
}
