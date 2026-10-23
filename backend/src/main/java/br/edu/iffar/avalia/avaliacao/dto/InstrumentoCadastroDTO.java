package br.edu.iffar.avalia.avaliacao.dto;

/** Campos do cadastro de instrumento usados pela interface JSF. */
public class InstrumentoCadastroDTO {
    private String id;
    private String titulo;
    private String descricao;
    private String cicloReferencia;
    private String inicioAplicacao;
    private String fimAplicacao;
    private String tipoAvaliadoId;
    private String[] respondentesIds = new String[0];
    private boolean anonimo = true;
    private boolean permiteEdicao = true;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCicloReferencia() { return cicloReferencia; }
    public void setCicloReferencia(String cicloReferencia) { this.cicloReferencia = cicloReferencia; }
    public String getInicioAplicacao() { return inicioAplicacao; }
    public void setInicioAplicacao(String inicioAplicacao) { this.inicioAplicacao = inicioAplicacao; }
    public String getFimAplicacao() { return fimAplicacao; }
    public void setFimAplicacao(String fimAplicacao) { this.fimAplicacao = fimAplicacao; }
    public String getTipoAvaliadoId() { return tipoAvaliadoId; }
    public void setTipoAvaliadoId(String tipoAvaliadoId) { this.tipoAvaliadoId = tipoAvaliadoId; }
    public String[] getRespondentesIds() { return respondentesIds; }
    public void setRespondentesIds(String[] respondentesIds) {
        this.respondentesIds = respondentesIds == null ? new String[0] : respondentesIds;
    }
    public boolean isAnonimo() { return anonimo; }
    public void setAnonimo(boolean anonimo) { this.anonimo = anonimo; }
    public boolean isPermiteEdicao() { return permiteEdicao; }
    public void setPermiteEdicao(boolean permiteEdicao) { this.permiteEdicao = permiteEdicao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
