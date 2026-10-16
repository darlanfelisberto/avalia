package br.edu.iffar.avalia.avaliacao.resource;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.Instrumento;
import br.edu.iffar.avalia.avaliacao.service.AvaliacaoService;
import br.edu.iffar.avalia.core.service.AuthService;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
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
    AvaliacaoService service;

    @Inject
    AuthService auth;

    private UsuarioDTO exigirPermissao(String permissao) {
        UsuarioDTO usuario = auth.validarJwt();
        if (!usuario.getPermissoes().contains(permissao))
            throw new SecurityException("Permissão insuficiente para esta operação.");
        return usuario;
    }

    @GET
    @Path("/conjuntos-opcao")
    public Response conjuntos() {
        return executar(() -> { exigirPermissao("INSTRUMENTO_VISUALIZAR"); return Response.ok(service.listarConjuntos()).build(); });
    }

    @POST
    @Path("/conjuntos-opcao")
    public Response salvarConjunto(ConjuntoOpcao dto) {
        return executar(() -> { exigirPermissao("INSTRUMENTO_CRIAR"); return Response.ok(service.salvarConjunto(dto)).build(); });
    }

    @GET
    @Path("/instrumentos")
    public Response listar() {
        return executar(() -> { exigirPermissao("INSTRUMENTO_VISUALIZAR"); return Response.ok(service.listarInstrumentos()).build(); });
    }

    @GET
    @Path("/instrumentos/{id}")
    public Response buscar(@PathParam("id") String id) {
        return executar(() -> {
            exigirPermissao("INSTRUMENTO_VISUALIZAR");
            Instrumento i = service.buscarInstrumento(id);
            return i == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(i).build();
        });
    }

    @POST
    @Path("/instrumentos")
    public Response salvar(Instrumento dto) {
        return executar(() -> {
            UsuarioDTO usuario = exigirPermissao(dto == null || dto.id == null || dto.id.isBlank()
                    ? "INSTRUMENTO_CRIAR" : "INSTRUMENTO_EDITAR");
            return Response.ok(service.salvarInstrumento(dto, usuario.getCpf())).build();
        });
    }

    @POST
    @Path("/instrumentos/{id}/publicar")
    public Response publicar(@PathParam("id") String id) {
        return executar(() -> { exigirPermissao("INSTRUMENTO_EDITAR"); return Response.ok(service.publicar(id)).build(); });
    }

    @DELETE
    @Path("/instrumentos/{id}")
    public Response excluir(@PathParam("id") String id) {
        return executar(() -> { exigirPermissao("INSTRUMENTO_EXCLUIR"); return service.excluir(id)
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
