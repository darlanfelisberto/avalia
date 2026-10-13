package br.edu.iffar.avalia.core.dto;

import br.edu.iffar.avalia.core.model.TipoQuestao;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestaoDTO {
    private UUID id;
    private String enunciado;
    private TipoQuestao tipo;
    private boolean obrigatoria;
    private Integer escalaMin;
    private Integer escalaMax;
    private String rotuloMin;
    private String rotuloMax;
    private List<OpcaoRespostaDTO> opcoes = new ArrayList<>();
    private String ajuda;

    public QuestaoDTO() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public TipoQuestao getTipo() { return tipo; }
    public void setTipo(TipoQuestao tipo) { this.tipo = tipo; }

    public boolean isObrigatoria() { return obrigatoria; }
    public void setObrigatoria(boolean obrigatoria) { this.obrigatoria = obrigatoria; }

    public Integer getEscalaMin() { return escalaMin; }
    public void setEscalaMin(Integer escalaMin) { this.escalaMin = escalaMin; }

    public Integer getEscalaMax() { return escalaMax; }
    public void setEscalaMax(Integer escalaMax) { this.escalaMax = escalaMax; }

    public String getRotuloMin() { return rotuloMin; }
    public void setRotuloMin(String rotuloMin) { this.rotuloMin = rotuloMin; }

    public String getRotuloMax() { return rotuloMax; }
    public void setRotuloMax(String rotuloMax) { this.rotuloMax = rotuloMax; }

    public List<OpcaoRespostaDTO> getOpcoes() { return opcoes; }
    public void setOpcoes(List<OpcaoRespostaDTO> opcoes) { this.opcoes = opcoes; }

    public String getAjuda() { return ajuda; }
    public void setAjuda(String ajuda) { this.ajuda = ajuda; }
}
