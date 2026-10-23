package br.edu.iffar.avalia.avaliacao.resource;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.ConjuntoOpcaoDTO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.InstrumentoAvaliativoDTO;
import br.edu.iffar.avalia.avaliacao.service.AvaliacaoService;
import br.edu.iffar.avalia.core.service.AuthService;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.NoResultException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.function.Supplier;

/** API do novo modelo de instrumento avaliativo (schema avaliacao). */
@Path("/avaliacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"CPA", "ADMIN"})
public class AvaliacaoResource {

    @Inject
    AvaliacaoService avaliacaoService;

    @GET
    @Path("/conjuntos-opcao")
    public Response conjuntos() {
        return executar(() -> {  return Response.ok(avaliacaoService.listarConjuntos()).build(); });
    }

    @POST
    @Path("/conjuntos-opcao")
    public Response salvarConjunto(ConjuntoOpcaoDTO dto) {
        return executar(() -> {  return Response.ok(avaliacaoService.salvarConjunto(dto)).build(); });
    }

    @GET
    @Path("/instrumentos")
    public Response listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                           @QueryParam("tamanho") @DefaultValue("100") int tamanho) {
        return executar(() -> { return Response.ok(avaliacaoService.listarInstrumentos(pagina, tamanho)).build(); });
    }

    @GET
    @Path("/instrumentos/{id}")
    public Response buscar(@PathParam("id") String id) {
        return executar(() -> {
            InstrumentoAvaliativoDTO i = avaliacaoService.buscarInstrumento(id);
            return i == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(i).build();
        });
    }

    @POST
    @Path("/instrumentos")
    public Response salvar(InstrumentoAvaliativoDTO dto) {
        return executar(() -> {
            return Response.ok(avaliacaoService.salvarInstrumento(dto)).build();
        });
    }

    @POST
    @Path("/instrumentos/{id}/publicar")
    public Response publicar(@PathParam("id") String id) {
        return executar(() -> { return Response.ok(avaliacaoService.publicar(id)).build(); });
    }

    @DELETE
    @Path("/instrumentos/{id}")
    public Response excluir(@PathParam("id") String id) {
        return executar(() -> { return avaliacaoService.excluir(id)
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build(); });
    }

    /** Erros de validação viram 400 com a mensagem em texto puro, para exibição direta no formulário. */
    private Response executar(Supplier<Response> acao) {
        try {
            return acao.get();
        } catch (NoResultException e) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity("Objeto não encontrado.").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        } catch (RuntimeException e) {
            Throwable causa = e;
            while (causa != null) {
                if (causa instanceof ConstraintViolationException violacao) {
                    String mensagens = violacao.getConstraintViolations().stream()
                            .map(item -> item.getMessage())
                            .distinct()
                            .sorted()
                            .collect(java.util.stream.Collectors.joining(" "));
                    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                            .entity(mensagens).build();
                }
                causa = causa.getCause();
            }
            causa = e;
            while (causa.getCause() != null) causa = causa.getCause();
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(causa.getMessage()).build();
        }
    }
}
