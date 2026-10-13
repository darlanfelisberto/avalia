package br.edu.iffar.avalia.core.dto;

import java.util.UUID;

public class ComponenteDTO {
    private UUID id;
    private String codigo;
    private String nome;
    private String docenteNome;
    private String turma;

    public ComponenteDTO() {}

    public ComponenteDTO(UUID id, String codigo, String nome, String docenteNome, String turma) {
        this.id = id;
        this.codigo = codigo;
        this.nome = nome;
        this.docenteNome = docenteNome;
        this.turma = turma;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDocenteNome() { return docenteNome; }
    public void setDocenteNome(String docenteNome) { this.docenteNome = docenteNome; }

    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }
}
