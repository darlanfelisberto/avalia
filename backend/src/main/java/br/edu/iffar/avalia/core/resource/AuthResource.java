package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.dto.AuthResponseDTO;
import br.edu.iffar.avalia.core.dto.OidcCallbackDTO;
import br.edu.iffar.avalia.core.service.AuthService;
import br.edu.iffar.avalia.core.service.OidcClientService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
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
            return Response.ok(oidcClient.configuracaoPublica()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new AuthResponseDTO(false, null, "Provedor OIDC indisponível.", null)).build();
        }
    }

    @POST
    @Path("/oidc/callback")
    public Response callbackOidc(OidcCallbackDTO callback) {
        try {
            String cpf = oidcClient.trocarCodigoPorCpf(callback);
            AuthResponseDTO resposta = authService.autenticarUsuarioOidc(cpf);
            return resposta.isAutenticado() ? Response.ok(resposta).build()
                    : Response.status(Response.Status.FORBIDDEN).entity(resposta).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AuthResponseDTO(false, null, e.getMessage(), null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new AuthResponseDTO(false, null, "Falha na comunicação com o provedor OIDC.", null)).build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String authorization) {
        authService.encerrarSessao(authorization);
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    public Response sessaoAtual(@HeaderParam("Authorization") String authorization,
                               @HeaderParam("X-Papel-Ativo") String papel) {
        try {
            return Response.ok(authService.validarSessao(authorization, papel)).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
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
