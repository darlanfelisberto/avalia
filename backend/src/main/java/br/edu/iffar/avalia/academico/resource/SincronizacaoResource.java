package br.edu.iffar.avalia.academico.resource;

import br.edu.iffar.avalia.academico.dto.SincronizacaoRequestDTO;
import br.edu.iffar.avalia.academico.dto.SincronizacaoStatusDTO;
import br.edu.iffar.avalia.academico.service.SincronizacaoSigaaService;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/sincronizacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SincronizacaoResource {

    @Inject
    private SincronizacaoSigaaService sincronizacaoService;

    @Inject
    private AuthService authService;

    private boolean autorizado(String token, String papel) {
        try {
            return authService.validarSessao(token, papel).getPermissoes().contains("SIGAA_SINCRONIZAR");
        } catch (SecurityException e) {
            return false;
        }
    }

    @GET
    @Path("/status")
    public Response getStatus(@HeaderParam("Authorization") String token,
                              @HeaderParam("X-Papel-Ativo") String papel) {
        if (!autorizado(token, papel)) return Response.status(Response.Status.UNAUTHORIZED).build();
        SincronizacaoStatusDTO status = sincronizacaoService.getStatus();
        return Response.ok(status).build();
    }

    @POST
    @Path("/executar")
    public Response executar(SincronizacaoRequestDTO req,
                            @HeaderParam("Authorization") String token,
                            @HeaderParam("X-Papel-Ativo") String papel) {
        if (!autorizado(token, papel)) return Response.status(Response.Status.UNAUTHORIZED).build();
        Map<String, Object> resp = sincronizacaoService.executarSincronizacao(req);
        return Response.ok(resp).build();
    }
}
