package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.dto.AuthResponseDTO;
import br.edu.iffar.avalia.core.dto.OidcCallbackDTO;
import br.edu.iffar.avalia.core.service.AuthService;
import br.edu.iffar.avalia.core.service.OidcClientService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.time.Duration;
import java.time.Instant;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AuthResource {

    @Inject
    private AuthService authService;

    @Inject
    private OidcClientService oidcClient;

    @GET
    @Path("/oidc/config")
    public Response configuracaoOidc() {
        try {
            return Response.ok(oidcClient.configuracaoPublica()).header("Cache-Control", "no-store").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new AuthResponseDTO(false, "Provedor OIDC indisponível.", null)).header("Cache-Control", "no-store").build();
        }
    }

    @POST
    @Path("/oidc/callback")
    public Response callbackOidc(OidcCallbackDTO callback) {
        try {
            OidcClientService.TokensOidc tokens = oidcClient.trocarCodigoPorTokens(callback);
            AuthResponseDTO resposta = authService.autenticarUsuarioOidc(tokens.identidade());
            if (!resposta.isAutenticado())
                return Response.status(Response.Status.FORBIDDEN).entity(resposta).header("Cache-Control", "no-store").build();
            int maxAge = (int) Math.max(1, Math.min(12 * 60 * 60,
                    Duration.between(Instant.now(), tokens.identidade().expiraEm()).getSeconds()));
            return Response.ok(resposta).cookie(accessCookie(tokens.accessToken(), maxAge))
                    .header("Cache-Control", "no-store").build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponseDTO(false, e.getMessage(), null)).header("Cache-Control", "no-store").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new AuthResponseDTO(false, "Falha na comunicação com o provedor OIDC.", null)).header("Cache-Control", "no-store").build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout() {
        return Response.noContent().cookie(accessCookie("", 0), cookie("avalia_session", "", 0))
                .header("Cache-Control", "no-store").build();
    }

    @GET
    @Path("/me")
    @RolesAllowed("user")
    public Response sessaoAtual() {
        try {
            return Response.ok(authService.validarJwt()).header("Cache-Control", "no-store").build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED).header("Cache-Control", "no-store").build();
        }
    }

    private NewCookie accessCookie(String value, int maxAge) {
        return cookie("avalia_access", value, maxAge);
    }

    private NewCookie cookie(String name, String value, int maxAge) {
        return new NewCookie.Builder(name)
                .value(value).path("/api").maxAge(maxAge).httpOnly(true)
                .secure(oidcClient.cookieSecure()).sameSite(NewCookie.SameSite.STRICT).build();
    }

    @GET
    @Path("/permissoes")
    public Response listarPermissoesDisponiveis() {
        List<String> todas = List.of(
                "INSTRUMENTO_CRIAR",
                "INSTRUMENTO_EDITAR",
                "INSTRUMENTO_EXCLUIR",
                "INSTRUMENTO_VISUALIZAR",
                "AVALIACAO_RESPONDER",
                "SIGAA_SINCRONIZAR"
        );
        return Response.ok(todas).build();
    }
}
