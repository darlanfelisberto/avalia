package br.edu.iffar.avalia.avaliacao.resource;

import br.edu.iffar.avalia.avaliacao.dto.RespostaAvaliacaoDTO.Envio;
import br.edu.iffar.avalia.avaliacao.service.ResponderAvaliacaoService;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.function.Supplier;

@Path("/avaliacao/responder")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class ResponderAvaliacaoResource {
    @Inject AuthService auth;
    @Inject ResponderAvaliacaoService service;

    @GET
    public Response listar() {
        return executar(() -> Response.ok(service.listar()).build());
    }

    @GET
    @Path("/{instrumentoId}/{alvoId}")
    public Response buscar(@PathParam("instrumentoId") String instrumentoId,
                           @PathParam("alvoId") String alvoId,
                           @QueryParam("contextoId") String contextoId) {
        return executar(() -> Response.ok(service.buscar(usuario(), instrumentoId, alvoId, contextoId)).build());
    }

    @GET
    @Path("/{instrumentoId}/{alvoId}/conjuntos-opcao")
    public Response conjuntos(@PathParam("instrumentoId") String instrumentoId,
                             @PathParam("alvoId") String alvoId,
                             @QueryParam("contextoId") String contextoId) {
        return executar(() -> Response.ok(service.listarConjuntos(usuario(),
                instrumentoId, alvoId, contextoId)).build());
    }

    @POST
    @Path("/{instrumentoId}/envios")
    public Response enviar(@PathParam("instrumentoId") String instrumentoId, Envio envio) {
        return executar(() -> {
//            service.enviar(usuario(), instrumentoId, envio);
            return Response.noContent().build();
        });
    }

    private UsuarioDTO usuario() {
        return auth.validarJwt();
    }

    private Response executar(Supplier<Response> acao) {
        try {
            return acao.get();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().type(MediaType.TEXT_PLAIN)
                    .entity(e.getMessage()).build();
        }

    }
}
