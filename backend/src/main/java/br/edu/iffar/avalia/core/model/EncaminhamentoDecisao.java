package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "encaminhamento_decisao")
public class EncaminhamentoDecisao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "encaminhamento_decisao_id", nullable = false, updatable = false)
    private java.util.UUID encaminhamentoDecisaoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private Instrumento instrumento;

    @Column(name = "responsavel_nome", nullable = false)
    private String responsavelNome;

    @Column(name = "papel", nullable = false, length = 64)
    private String papel;

    @Column(name = "tipo_decisao", nullable = false, length = 64)
    private String tipoDecisao;

    @Column(name = "detalhamento", nullable = false)
    private String detalhamento;

    @Column(name = "prazo_revisao")
    private LocalDate prazoRevisao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public EncaminhamentoDecisao() {}

    public java.util.UUID getEncaminhamentoDecisaoId() { return encaminhamentoDecisaoId; }
    public void setEncaminhamentoDecisaoId(java.util.UUID encaminhamentoDecisaoId) { this.encaminhamentoDecisaoId = encaminhamentoDecisaoId; }

    public java.util.UUID getId() { return encaminhamentoDecisaoId; }
    public void setId(java.util.UUID id) { this.encaminhamentoDecisaoId = id; }

    public Instrumento getInstrumento() { return instrumento; }
    public void setInstrumento(Instrumento instrumento) { this.instrumento = instrumento; }

    public String getResponsavelNome() { return responsavelNome; }
    public void setResponsavelNome(String responsavelNome) { this.responsavelNome = responsavelNome; }

    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }

    public String getTipoDecisao() { return tipoDecisao; }
    public void setTipoDecisao(String tipoDecisao) { this.tipoDecisao = tipoDecisao; }

    public String getDetalhamento() { return detalhamento; }
    public void setDetalhamento(String detalhamento) { this.detalhamento = detalhamento; }

    public LocalDate getPrazoRevisao() { return prazoRevisao; }
    public void setPrazoRevisao(LocalDate prazoRevisao) { this.prazoRevisao = prazoRevisao; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
