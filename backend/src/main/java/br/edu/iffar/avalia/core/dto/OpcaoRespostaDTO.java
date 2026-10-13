package br.edu.iffar.avalia.core.dto;

public class OpcaoRespostaDTO {
    private String id;
    private String texto;
    private Integer valor;

    public OpcaoRespostaDTO() {}

    public OpcaoRespostaDTO(String id, String texto) {
        this.id = id;
        this.texto = texto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public Integer getValor() { return valor; }
    public void setValor(Integer valor) { this.valor = valor; }
}
