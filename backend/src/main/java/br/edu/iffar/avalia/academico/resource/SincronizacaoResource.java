package br.edu.iffar.avalia.academico.resource;

import br.edu.iffar.avalia.academico.dto.SincronizacaoRequestDTO;
import br.edu.iffar.avalia.academico.dto.SincronizacaoStatusDTO;
import br.edu.iffar.avalia.academico.service.SincronizacaoSigaaService;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/sincronizacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@RolesAllowed({"CPA", "ADMIN"})
public class SincronizacaoResource {

    @Inject
    private SincronizacaoSigaaService sincronizacaoService;

    @Inject
    private AuthService authService;

    private boolean autorizado() {
        try {
            return authService.validarJwt().getPermissoes().contains("SIGAA_SINCRONIZAR");
        } catch (SecurityException e) {
            return false;
        }
    }

    @GET
    @Path("/status")
    public Response getStatus() {
        if (!autorizado()) return Response.status(Response.Status.FORBIDDEN).build();
        SincronizacaoStatusDTO status = sincronizacaoService.getStatus();
        return Response.ok(status).build();
    }

    @POST
    @Path("/executar")
    public Response executar(SincronizacaoRequestDTO req) {
        if (!autorizado()) return Response.status(Response.Status.FORBIDDEN).build();
        Map<String, Object> resp = sincronizacaoService.executarSincronizacao(req);
        return Response.ok(resp).build();
    }
}
