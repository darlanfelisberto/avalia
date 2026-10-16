package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "participacao", schema = "avaliacao")
public class Participacao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "participacao_id")
    private UUID participacaoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    private InstrumentoAvaliativo instrumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entidade_avaliada_id", nullable = false)
    private Entidade avaliada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entidade_respondente_id", nullable = false)
    private Entidade respondente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidade_contexto_id")
    private Entidade contexto;

    @Column(name = "registrada_em", nullable = false)
    private OffsetDateTime registradaEm = OffsetDateTime.now();

    public UUID getId() { return participacaoId; }
    public UUID getParticipacaoId() { return participacaoId; }
    public void setParticipacaoId(UUID participacaoId) { this.participacaoId = participacaoId; }
    public InstrumentoAvaliativo getInstrumento() { return instrumento; }
    public void setInstrumento(InstrumentoAvaliativo instrumento) { this.instrumento = instrumento; }
    public Entidade getAvaliada() { return avaliada; }
    public void setAvaliada(Entidade avaliada) { this.avaliada = avaliada; }
    public Entidade getRespondente() { return respondente; }
    public void setRespondente(Entidade respondente) { this.respondente = respondente; }
    public Entidade getContexto() { return contexto; }
    public void setContexto(Entidade contexto) { this.contexto = contexto; }

    @Override
    public UUID getMMId() {
        return this.participacaoId;
    }
}
