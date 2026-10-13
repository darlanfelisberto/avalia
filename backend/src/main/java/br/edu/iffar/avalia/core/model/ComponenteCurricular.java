package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "componente_curricular")
public class ComponenteCurricular {

    @Id
    @Column(name = "id", length = 64)
    private String id;

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
