package br.edu.iffar.avalia.core.dto;

import java.util.UUID;

public class OpcaoRespostaDTO {
    private UUID opcaoQuestaoId;
    private String texto;
    private Integer valor;

    public OpcaoRespostaDTO() {}

    public OpcaoRespostaDTO(UUID opcaoQuestaoId, String texto) {
        this.opcaoQuestaoId = opcaoQuestaoId;
        this.texto = texto;
    }

    public UUID getOpcaoQuestaoId() { return opcaoQuestaoId; }
    public void setOpcaoQuestaoId(UUID opcaoQuestaoId) { this.opcaoQuestaoId = opcaoQuestaoId; }

    public UUID getId() { return opcaoQuestaoId; }
    public void setId(UUID id) { this.opcaoQuestaoId = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public Integer getValor() { return valor; }
    public void setValor(Integer valor) { this.valor = valor; }
}
