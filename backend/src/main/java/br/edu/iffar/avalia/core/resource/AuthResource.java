package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.dto.AuthResponseDTO;
import br.edu.iffar.avalia.core.dto.LoginRequestDTO;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AuthResource {

    @Inject
    private AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequestDTO req) {
        if (req == null || req.getCpf() == null || req.getCpf().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new AuthResponseDTO(false, null, "CPF é obrigatório para identificação.", null))
                    .build();
        }

        AuthResponseDTO resp = authService.autenticarPorCpf(req.getCpf());
        if (!resp.isAutenticado()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(resp)
                    .build();
        }

        return Response.ok(resp).build();
    }

    @GET
    @Path("/demonstracao")
    public Response listarUsuariosDemonstracao() {
        List<UsuarioDTO> lista = authService.listarUsuariosDemonstracao();
        return Response.ok(lista).build();
    }

    @GET
    @Path("/permissoes")
    public Response listarPermissoesDisponiveis() {
        List<String> todas = List.of(
                "INSTRUMENTO_CRIAR",
                "INSTRUMENTO_EDITAR",
                "INSTRUMENTO_EXCLUIR",
                "INSTRUMENTO_VISUALIZAR",
                "AVALIACAO_RESPONDER",
                "DOCENTE_MANIFESTAR",
                "GESTAO_ENCAMINHAR",
                "SIGAA_SINCRONIZAR",
                "AUDITORIA_ACESSAR"
        );
        return Response.ok(todas).build();
    }
}
