package br.edu.iffar.avalia.core.dto;

import java.util.ArrayList;
import java.util.List;

public class GrupoDTO {
    private String id;
    private String titulo;
    private String descricao;
    private String secao;
    private List<QuestaoDTO> questoes = new ArrayList<>();

    public GrupoDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getSecao() { return secao; }
    public void setSecao(String secao) { this.secao = secao; }

    public List<QuestaoDTO> getQuestoes() { return questoes; }
    public void setQuestoes(List<QuestaoDTO> questoes) { this.questoes = questoes; }
}
