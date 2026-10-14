package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "encaminhamento", schema = "avaliacao")
public class Encaminhamento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "encaminhamento_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    private InstrumentoAvaliativo instrumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidade_avaliada_id")
    private Entidade avaliada;

    @Column(nullable = false, length = 22)
    private String tipo;

    @Column(nullable = false, columnDefinition = "text")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Entidade responsavel;

    private LocalDate prazo;

    @Column(name = "registrado_em", nullable = false)
    private OffsetDateTime registradoEm = OffsetDateTime.now();

    public UUID getId() { return id; }
    public InstrumentoAvaliativo getInstrumento() { return instrumento; }
    public void setInstrumento(InstrumentoAvaliativo instrumento) { this.instrumento = instrumento; }
    public Entidade getAvaliada() { return avaliada; }
    public void setAvaliada(Entidade avaliada) { this.avaliada = avaliada; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Entidade getResponsavel() { return responsavel; }
    public void setResponsavel(Entidade responsavel) { this.responsavel = responsavel; }
    public LocalDate getPrazo() { return prazo; }
    public void setPrazo(LocalDate prazo) { this.prazo = prazo; }
}
