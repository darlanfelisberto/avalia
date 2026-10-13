package br.edu.iffar.avalia.core.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GrupoDTO {
    private UUID grupoQuestoesId;
    private String titulo;
    private String descricao;
    private String secao;
    private List<QuestaoDTO> questoes = new ArrayList<>();

    public GrupoDTO() {}

    public UUID getGrupoQuestoesId() { return grupoQuestoesId; }
    public void setGrupoQuestoesId(UUID grupoQuestoesId) { this.grupoQuestoesId = grupoQuestoesId; }

    public UUID getId() { return grupoQuestoesId; }
    public void setId(UUID id) { this.grupoQuestoesId = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getSecao() { return secao; }
    public void setSecao(String secao) { this.secao = secao; }

    public List<QuestaoDTO> getQuestoes() { return questoes; }
    public void setQuestoes(List<QuestaoDTO> questoes) { this.questoes = questoes; }
}
