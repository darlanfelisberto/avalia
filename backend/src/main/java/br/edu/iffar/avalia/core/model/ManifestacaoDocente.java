package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "manifestacao_docente")
public class ManifestacaoDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "manifestacao_docente_id", nullable = false, updatable = false)
    private java.util.UUID manifestacaoDocenteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private Instrumento instrumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "componente_curricular_id", nullable = false)
    private ComponenteCurricular componenteCurricular;

    @Column(name = "docente_nome", nullable = false)
    private String docenteNome;

    @Column(name = "comentario")
    private String comentario;

    @Column(name = "plano_desenvolvimento")
    private String planoDesenvolvimento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ManifestacaoDocente() {}

    public java.util.UUID getManifestacaoDocenteId() { return manifestacaoDocenteId; }
    public void setManifestacaoDocenteId(java.util.UUID manifestacaoDocenteId) { this.manifestacaoDocenteId = manifestacaoDocenteId; }

    public java.util.UUID getId() { return manifestacaoDocenteId; }
    public void setId(java.util.UUID id) { this.manifestacaoDocenteId = id; }

    public Instrumento getInstrumento() { return instrumento; }
    public void setInstrumento(Instrumento instrumento) { this.instrumento = instrumento; }

    public ComponenteCurricular getComponenteCurricular() { return componenteCurricular; }
    public void setComponenteCurricular(ComponenteCurricular componenteCurricular) { this.componenteCurricular = componenteCurricular; }

    public ComponenteCurricular getComponente() { return componenteCurricular; }
    public void setComponente(ComponenteCurricular componente) { this.componenteCurricular = componente; }

    public String getDocenteNome() { return docenteNome; }
    public void setDocenteNome(String docenteNome) { this.docenteNome = docenteNome; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public String getPlanoDesenvolvimento() { return planoDesenvolvimento; }
    public void setPlanoDesenvolvimento(String planoDesenvolvimento) { this.planoDesenvolvimento = planoDesenvolvimento; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
