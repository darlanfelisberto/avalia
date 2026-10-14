package br.edu.iffar.avalia.avaliacao.resource;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.Instrumento;
import br.edu.iffar.avalia.avaliacao.service.AvaliacaoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.function.Supplier;

/** API do novo modelo de instrumento avaliativo (schema avaliacao). */
@Path("/avaliacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvaliacaoResource {

    @Inject
    AvaliacaoService service;

    @GET
    @Path("/tipos-entidade")
    public Response tiposEntidade() {
        return executar(() -> Response.ok(service.listarTiposEntidade()).build());
    }

    @GET
    @Path("/conjuntos-opcao")
    public Response conjuntos() {
        return executar(() -> Response.ok(service.listarConjuntos()).build());
    }

    @POST
    @Path("/conjuntos-opcao")
    public Response salvarConjunto(ConjuntoOpcao dto) {
        return executar(() -> Response.ok(service.salvarConjunto(dto)).build());
    }

    @GET
    @Path("/instrumentos")
    public Response listar() {
        return executar(() -> Response.ok(service.listarInstrumentos()).build());
    }

    @GET
    @Path("/instrumentos/{id}")
    public Response buscar(@PathParam("id") String id) {
        return executar(() -> {
            Instrumento i = service.buscarInstrumento(id);
            return i == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(i).build();
        });
    }

    @POST
    @Path("/instrumentos")
    public Response salvar(Instrumento dto, @HeaderParam("X-Usuario") String usuario) {
        return executar(() -> Response.ok(service.salvarInstrumento(dto, usuario == null || usuario.isBlank() ? "cpa" : usuario)).build());
    }

    @POST
    @Path("/instrumentos/{id}/publicar")
    public Response publicar(@PathParam("id") String id) {
        return executar(() -> Response.ok(service.publicar(id)).build());
    }

    @DELETE
    @Path("/instrumentos/{id}")
    public Response excluir(@PathParam("id") String id) {
        return executar(() -> service.excluir(id)
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build());
    }

    /** Erros de validação viram 400 com a mensagem em texto puro, para exibição direta no formulário. */
    private Response executar(Supplier<Response> acao) {
        try {
            return acao.get();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        } catch (RuntimeException e) {
            Throwable causa = e;
            while (causa.getCause() != null) causa = causa.getCause();
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(causa.getMessage()).build();
        }
    }
}
