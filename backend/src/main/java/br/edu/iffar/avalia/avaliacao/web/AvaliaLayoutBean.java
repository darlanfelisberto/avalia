package br.edu.iffar.avalia.avaliacao.web;

import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import br.edu.iffar.avalia.core.service.AuthService;
import br.edu.iffar.avalia.core.service.OidcClientService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("avaliaLayout")
@RequestScoped
public class AvaliaLayoutBean {
    @Inject private AuthService auth;
    @Inject private OidcClientService oidc;

    private UsuarioDTO usuario;
    private boolean usuarioConsultado;

    private UsuarioDTO usuario() {
        if (!usuarioConsultado) {
            usuarioConsultado = true;
            if (!FacesContext.getCurrentInstance().getExternalContext()
                    .getRequestCookieMap().containsKey("avalia_access")) return null;
            try {
                usuario = auth.validarJwt();
            } catch (RuntimeException e) {
                usuario = null;
            }
        }
        return usuario;
    }

    public String getNomeUsuario() {
        return usuario() == null ? "Visitante" : usuario().getNome();
    }

    public String getInicialUsuario() {
        String nome = getNomeUsuario();
        return nome == null || nome.isBlank() ? "U" : nome.substring(0, 1).toUpperCase();
    }

    public String getIdentificacaoUsuario() {
        return usuario() == null ? "Entre no AVALIA para salvar" : usuario().getCpfFormatado();
    }

    public String getPapelUsuario() {
        return usuario() == null ? "Prévia JSF" : usuario().getPapelAtivo();
    }

    public String getFrontendOrigin() {
        return oidc.frontendOrigin();
    }
}
