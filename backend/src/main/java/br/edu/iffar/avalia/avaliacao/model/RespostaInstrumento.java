package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "resposta_instrumento", schema = "avaliacao")
public class RespostaInstrumento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "resposta_instrumento_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    private InstrumentoAvaliativo instrumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entidade_avaliada_id", nullable = false)
    private Entidade avaliada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidade_respondente_id")
    private Entidade respondente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidade_contexto_id")
    private Entidade contexto;

    @Column(nullable = false, length = 12)
    private String status = "EM_ANDAMENTO";

    @Column(name = "data_envio")
    private LocalDate dataEnvio;

    public UUID getId() { return id; }
    public InstrumentoAvaliativo getInstrumento() { return instrumento; }
    public void setInstrumento(InstrumentoAvaliativo instrumento) { this.instrumento = instrumento; }
    public Entidade getAvaliada() { return avaliada; }
    public void setAvaliada(Entidade avaliada) { this.avaliada = avaliada; }
    public Entidade getRespondente() { return respondente; }
    public void setRespondente(Entidade respondente) { this.respondente = respondente; }
    public Entidade getContexto() { return contexto; }
    public void setContexto(Entidade contexto) { this.contexto = contexto; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDate dataEnvio) { this.dataEnvio = dataEnvio; }
}
