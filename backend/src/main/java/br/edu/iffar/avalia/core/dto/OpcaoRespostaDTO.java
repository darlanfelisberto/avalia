package br.edu.iffar.avalia.core.dto;

import java.util.UUID;

public class OpcaoRespostaDTO {
    private UUID id;
    private String texto;
    private Integer valor;

    public OpcaoRespostaDTO() {}

    public OpcaoRespostaDTO(UUID id, String texto) {
        this.id = id;
        this.texto = texto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public Integer getValor() { return valor; }
    public void setValor(Integer valor) { this.valor = valor; }
}
