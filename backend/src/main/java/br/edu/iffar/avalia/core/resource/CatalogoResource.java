package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.model.AlvoAvaliado;
import br.edu.iffar.avalia.core.model.PublicoAvaliador;
import br.edu.iffar.avalia.core.model.TipoAlvoAvaliado;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/catalogo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CatalogoResource {

    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    // --- ALVOS AVALIADOS ---

    @GET
    @Path("/alvos")
    public List<AlvoAvaliado> listarAlvos(@QueryParam("tipo") String tipo) {
        if (tipo != null && !tipo.trim().isEmpty()) {
            return em.createQuery("SELECT a FROM AlvoAvaliado a WHERE a.tipo = :tipo ORDER BY a.nome ASC", AlvoAvaliado.class)
                    .setParameter("tipo", TipoAlvoAvaliado.valueOf(tipo))
                    .getResultList();
        }
        return em.createQuery("SELECT a FROM AlvoAvaliado a ORDER BY a.tipo ASC, a.nome ASC", AlvoAvaliado.class)
                .getResultList();
    }

    @POST
    @Path("/alvos")
    @Transactional
    public Response salvarAlvo(AlvoAvaliado alvo) {
        if (alvo.getCreatedAt() == null) {
            alvo.setCreatedAt(OffsetDateTime.now());
        }
        em.merge(alvo);
        return Response.ok(alvo).build();
    }

    @DELETE
    @Path("/alvos/{id}")
    @Transactional
    public Response removerAlvo(@PathParam("id") UUID id) {
        AlvoAvaliado a = em.find(AlvoAvaliado.class, id);
        if (a != null) {
            em.remove(a);
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    // --- PÚBLICOS AVALIADORES ---

    @GET
    @Path("/publicos")
    public List<PublicoAvaliador> listarPublicos() {
        return em.createQuery("SELECT p FROM PublicoAvaliador p ORDER BY p.nome ASC", PublicoAvaliador.class)
                .getResultList();
    }

    @POST
    @Path("/publicos")
    @Transactional
    public Response salvarPublico(PublicoAvaliador publico) {
        if (publico.getCreatedAt() == null) {
            publico.setCreatedAt(OffsetDateTime.now());
        }
        em.merge(publico);
        return Response.ok(publico).build();
    }
}
