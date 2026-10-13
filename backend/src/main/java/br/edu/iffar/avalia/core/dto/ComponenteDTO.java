package br.edu.iffar.avalia.core.dto;

import java.util.UUID;

public class ComponenteDTO {
    private UUID componenteCurricularId;
    private String codigo;
    private String nome;
    private String docenteNome;
    private String turma;

    public ComponenteDTO() {}

    public ComponenteDTO(UUID componenteCurricularId, String codigo, String nome, String docenteNome, String turma) {
        this.componenteCurricularId = componenteCurricularId;
        this.codigo = codigo;
        this.nome = nome;
        this.docenteNome = docenteNome;
        this.turma = turma;
    }

    public UUID getComponenteCurricularId() { return componenteCurricularId; }
    public void setComponenteCurricularId(UUID componenteCurricularId) { this.componenteCurricularId = componenteCurricularId; }

    public UUID getId() { return componenteCurricularId; }
    public void setId(UUID id) { this.componenteCurricularId = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDocenteNome() { return docenteNome; }
    public void setDocenteNome(String docenteNome) { this.docenteNome = docenteNome; }

    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }
}
