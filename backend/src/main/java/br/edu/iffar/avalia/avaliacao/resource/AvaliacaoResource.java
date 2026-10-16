package br.edu.iffar.avalia.avaliacao.resource;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.Instrumento;
import br.edu.iffar.avalia.avaliacao.service.AvaliacaoService;
import br.edu.iffar.avalia.core.service.AuthService;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
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

    @Inject
    AuthService auth;

    private void exigirGestor(String token, String papel) {
        UsuarioDTO usuario = auth.validarSessao(token, papel);
        if (!("CPA".equals(usuario.getPapelAtivo()) || "ADMIN".equals(usuario.getPapelAtivo()))
                || !usuario.getPermissoes().contains("INSTRUMENTO_VISUALIZAR"))
            throw new SecurityException("Acesso restrito à administração de instrumentos.");
    }

    @GET
    @Path("/conjuntos-opcao")
    public Response conjuntos(@HeaderParam("Authorization") String token,
                             @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> { exigirGestor(token, papel); return Response.ok(service.listarConjuntos()).build(); });
    }

    @POST
    @Path("/conjuntos-opcao")
    public Response salvarConjunto(ConjuntoOpcao dto, @HeaderParam("Authorization") String token,
                                  @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> { exigirGestor(token, papel); return Response.ok(service.salvarConjunto(dto)).build(); });
    }

    @GET
    @Path("/instrumentos")
    public Response listar(@HeaderParam("Authorization") String token,
                           @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> { exigirGestor(token, papel); return Response.ok(service.listarInstrumentos()).build(); });
    }

    @GET
    @Path("/instrumentos/{id}")
    public Response buscar(@PathParam("id") String id, @HeaderParam("Authorization") String token,
                           @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> {
            exigirGestor(token, papel);
            Instrumento i = service.buscarInstrumento(id);
            return i == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(i).build();
        });
    }

    @POST
    @Path("/instrumentos")
    public Response salvar(Instrumento dto, @HeaderParam("Authorization") String token,
                           @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> {
            exigirGestor(token, papel);
            UsuarioDTO usuario = auth.validarSessao(token, papel);
            return Response.ok(service.salvarInstrumento(dto, usuario.getCpf())).build();
        });
    }

    @POST
    @Path("/instrumentos/{id}/publicar")
    public Response publicar(@PathParam("id") String id, @HeaderParam("Authorization") String token,
                             @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> { exigirGestor(token, papel); return Response.ok(service.publicar(id)).build(); });
    }

    @DELETE
    @Path("/instrumentos/{id}")
    public Response excluir(@PathParam("id") String id, @HeaderParam("Authorization") String token,
                            @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> { exigirGestor(token, papel); return service.excluir(id)
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build(); });
    }

    /** Erros de validação viram 400 com a mensagem em texto puro, para exibição direta no formulário. */
    private Response executar(Supplier<Response> acao) {
        try {
            return acao.get();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        } catch (RuntimeException e) {
            Throwable causa = e;
            while (causa.getCause() != null) causa = causa.getCause();
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(causa.getMessage()).build();
        }
    }
}
