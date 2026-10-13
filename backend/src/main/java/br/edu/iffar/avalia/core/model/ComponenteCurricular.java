package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "componente_curricular")
public class ComponenteCurricular {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "componente_curricular_id", nullable = false, updatable = false)
    private java.util.UUID componenteCurricularId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private Instrumento instrumento;

    @Column(name = "codigo", nullable = false, length = 64)
    private String codigo;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "docente_nome", nullable = false)
    private String docenteNome;

    @Column(name = "turma", length = 64)
    private String turma;

    public ComponenteCurricular() {}

    public java.util.UUID getComponenteCurricularId() { return componenteCurricularId; }
    public void setComponenteCurricularId(java.util.UUID componenteCurricularId) { this.componenteCurricularId = componenteCurricularId; }

    public java.util.UUID getId() { return componenteCurricularId; }
    public void setId(java.util.UUID id) { this.componenteCurricularId = id; }

    public Instrumento getInstrumento() { return instrumento; }
    public void setInstrumento(Instrumento instrumento) { this.instrumento = instrumento; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDocenteNome() { return docenteNome; }
    public void setDocenteNome(String docenteNome) { this.docenteNome = docenteNome; }

    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }
}
