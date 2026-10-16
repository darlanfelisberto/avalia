package br.edu.iffar.avalia.avaliacao.resource;

import br.edu.iffar.avalia.avaliacao.dto.PermissaoTipoEntidadeDTO;
import br.edu.iffar.avalia.avaliacao.service.TipoEntidadeService;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.function.Supplier;

/** Endpoints do catálogo de tipos de entidade e das permissões entre tipos. */
@Path("/avaliacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class TipoEntidadeResource {
    @Inject
    private TipoEntidadeService service;

    @Inject
    private AuthService auth;

    @GET
    @Path("/tipos-entidade")
    public Response listar() {
        return executar(() -> Response.ok(service.listar()).build());
    }

    @GET
    @Path("/permissoes-tipos-entidade")
    public Response listarPermissoes(@HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> {
            exigirGestor(papel, "INSTRUMENTO_VISUALIZAR");
            return Response.ok(service.listarPermissoes()).build();
        });
    }

    @PUT
    @Path("/permissoes-tipos-entidade")
    public Response salvarPermissoes(List<PermissaoTipoEntidadeDTO> permissoes,
                                     @HeaderParam("X-Papel-Ativo") String papel) {
        return executar(() -> {
            exigirGestor(papel, "INSTRUMENTO_EDITAR");
            service.salvarPermissoes(permissoes);
            return Response.noContent().build();
        });
    }

    private void exigirGestor(String papel, String permissao) {
        UsuarioDTO usuario = auth.validarJwt(papel);
        if (!("CPA".equals(usuario.getPapelAtivo()) || "ADMIN".equals(usuario.getPapelAtivo()))
                || !usuario.getPermissoes().contains(permissao))
            throw new SecurityException("Acesso restrito à administração de instrumentos.");
    }

    private Response executar(Supplier<Response> acao) {
        try {
            return acao.get();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        } catch (RuntimeException e) {
            Throwable causa = e;
            while (causa.getCause() != null) causa = causa.getCause();
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(causa.getMessage()).build();
        }
    }
}
