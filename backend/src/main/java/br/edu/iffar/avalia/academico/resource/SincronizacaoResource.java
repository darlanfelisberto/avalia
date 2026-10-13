package br.edu.iffar.avalia.academico.resource;

import br.edu.iffar.avalia.academico.dto.SincronizacaoRequestDTO;
import br.edu.iffar.avalia.academico.dto.SincronizacaoStatusDTO;
import br.edu.iffar.avalia.academico.service.SincronizacaoSigaaService;
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

    @GET
    @Path("/status")
    public Response getStatus() {
        SincronizacaoStatusDTO status = sincronizacaoService.getStatus();
        return Response.ok(status).build();
    }

    @POST
    @Path("/executar")
    public Response executar(SincronizacaoRequestDTO req) {
        Map<String, Object> resp = sincronizacaoService.executarSincronizacao(req);
        return Response.ok(resp).build();
    }
}
