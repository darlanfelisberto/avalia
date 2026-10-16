package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "instrumento_tipo_alvo", schema = "avaliacao")
@IdClass(InstrumentoTipoAlvo.Chave.class)
public class InstrumentoTipoAlvo {
    @Id
    @Column(name = "instrumento_avaliativo_id")
    private UUID instrumentoId;

    @Id
    @Column(name = "tipo_entidade_alvo_id")
    private UUID tipoAlvoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_avaliativo_id", insertable = false, updatable = false)
    private InstrumentoAvaliativo instrumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_entidade_alvo_id", insertable = false, updatable = false)
    private TipoEntidade tipoAlvo;

    public InstrumentoTipoAlvo() {}

    public InstrumentoTipoAlvo(UUID instrumentoId, UUID tipoAlvoId) {
        this.instrumentoId = instrumentoId;
        this.tipoAlvoId = tipoAlvoId;
    }

    public UUID getInstrumentoId() { return instrumentoId; }
    public UUID getTipoAlvoId() { return tipoAlvoId; }
    public TipoEntidade getTipoAlvo() { return tipoAlvo; }

    public static class Chave implements Serializable {
        private UUID instrumentoId;
        private UUID tipoAlvoId;

        public Chave() {}

        public Chave(UUID instrumentoId, UUID tipoAlvoId) {
            this.instrumentoId = instrumentoId;
            this.tipoAlvoId = tipoAlvoId;
        }

        @Override
        public boolean equals(Object outro) {
            if (this == outro) return true;
            if (!(outro instanceof Chave chave)) return false;
            return Objects.equals(instrumentoId, chave.instrumentoId) && Objects.equals(tipoAlvoId, chave.tipoAlvoId);
        }

        @Override
        public int hashCode() { return Objects.hash(instrumentoId, tipoAlvoId); }
    }
}
