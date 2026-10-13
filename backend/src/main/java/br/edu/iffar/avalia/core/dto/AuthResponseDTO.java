package br.edu.iffar.avalia.core.dto;

public class AuthResponseDTO {
    private boolean autenticado;
    private String token;
    private String mensagem;
    private UsuarioDTO usuario;

    public AuthResponseDTO() {}

    public AuthResponseDTO(boolean autenticado, String token, String mensagem, UsuarioDTO usuario) {
        this.autenticado = autenticado;
        this.token = token;
        this.mensagem = mensagem;
        this.usuario = usuario;
    }

    public boolean isAutenticado() { return autenticado; }
    public void setAutenticado(boolean autenticado) { this.autenticado = autenticado; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public UsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(UsuarioDTO usuario) { this.usuario = usuario; }
}
