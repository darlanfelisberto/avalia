package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.dto.AuthResponseDTO;
import br.edu.iffar.avalia.core.dto.OidcCallbackDTO;
import br.edu.iffar.avalia.core.service.AuthService;
import br.edu.iffar.avalia.core.service.OidcClientService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.util.List;

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
                    .entity(new AuthResponseDTO(false, null, "Provedor OIDC indisponível.", null)).header("Cache-Control", "no-store").build();
        }
    }

    @POST
    @Path("/oidc/callback")
    public Response callbackOidc(OidcCallbackDTO callback) {
        try {
            String cpf = oidcClient.trocarCodigoPorCpf(callback);
            AuthResponseDTO resposta = authService.autenticarUsuarioOidc(cpf);
            if (!resposta.isAutenticado())
                return Response.status(Response.Status.FORBIDDEN).entity(resposta).header("Cache-Control", "no-store").build();
            String sessionToken = resposta.getToken();
            resposta.setToken(null);
            return Response.ok(resposta).cookie(sessionCookie(sessionToken, 12 * 60 * 60))
                    .header("Cache-Control", "no-store").build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponseDTO(false, null, e.getMessage(), null)).header("Cache-Control", "no-store").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new AuthResponseDTO(false, null, "Falha na comunicação com o provedor OIDC.", null)).header("Cache-Control", "no-store").build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam("avalia_session") String sessionToken) {
        authService.encerrarSessao(sessionToken);
        return Response.noContent().cookie(sessionCookie("", 0)).header("Cache-Control", "no-store").build();
    }

    @GET
    @Path("/me")
    public Response sessaoAtual(@CookieParam("avalia_session") String sessionToken,
                               @HeaderParam("X-Papel-Ativo") String papel) {
        try {
            return Response.ok(authService.validarSessao(sessionToken, papel)).header("Cache-Control", "no-store").build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED).header("Cache-Control", "no-store").build();
        }
    }

    private NewCookie sessionCookie(String value, int maxAge) {
        return new NewCookie.Builder("avalia_session")
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
