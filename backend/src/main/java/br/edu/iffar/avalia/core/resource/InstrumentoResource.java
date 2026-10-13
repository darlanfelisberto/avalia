package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.dto.InstrumentoDTO;
import br.edu.iffar.avalia.core.service.InstrumentoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/instrumentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InstrumentoResource {

    @Inject
    private InstrumentoService instrumentoService;

    @GET
    public Response listarTodos() {
        List<InstrumentoDTO> lista = instrumentoService.listarTodos();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") String id) {
        InstrumentoDTO dto = instrumentoService.buscarPorId(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"mensagem\": \"Instrumento avaliativo não encontrado\"}")
                    .build();
        }
        return Response.ok(dto).build();
    }

    @POST
    public Response criar(InstrumentoDTO dto) {
        if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensagem\": \"Título do instrumento é obrigatório\"}")
                    .build();
        }

        InstrumentoDTO salvo = instrumentoService.salvarOuAtualizar(dto);
        return Response.status(Response.Status.CREATED).entity(salvo).build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizar(@PathParam("id") String id, InstrumentoDTO dto) {
        dto.setId(id);
        InstrumentoDTO atualizado = instrumentoService.salvarOuAtualizar(dto);
        return Response.ok(atualizado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response remover(@PathParam("id") String id) {
        boolean removido = instrumentoService.remover(id);
        if (removido) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
