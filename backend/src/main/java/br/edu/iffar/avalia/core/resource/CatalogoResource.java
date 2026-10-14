package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.model.AlvoAvaliado;
import br.edu.iffar.avalia.core.model.PublicoAvaliador;
import br.edu.iffar.avalia.core.model.TipoAlvoAvaliado;
import br.edu.iffar.avalia.core.model.TipoPublico;
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
    public List<AlvoAvaliado> listarAlvos(
            @QueryParam("tipo") String tipo,
            @QueryParam("campus") String campusFiltro,
            @QueryParam("busca") String termoBusca) {

        if (tipo != null && !tipo.trim().isEmpty()) {
            try {
                TipoAlvoAvaliado tipoEnum = TipoAlvoAvaliado.valueOf(tipo.trim().toUpperCase());
                List<AlvoAvaliado> dinamicos = buscarAlvosDinamicosDoSigaa(tipoEnum, campusFiltro, termoBusca);
                if (dinamicos != null && !dinamicos.isEmpty()) {
                    return dinamicos;
                }
                return em.createQuery("SELECT a FROM AlvoAvaliado a WHERE a.tipo = :tipo ORDER BY a.nome ASC", AlvoAvaliado.class)
                        .setParameter("tipo", tipoEnum)
                        .getResultList();
            } catch (Exception e) {
                // Em caso de erro, segue busca padrão
            }
        }
        return em.createQuery("SELECT a FROM AlvoAvaliado a ORDER BY a.tipo ASC, a.nome ASC", AlvoAvaliado.class)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<AlvoAvaliado> buscarAlvosDinamicosDoSigaa(TipoAlvoAvaliado tipo, String campusFiltro, String termoBusca) {
        List<AlvoAvaliado> resultado = new java.util.ArrayList<>();
        try {
            switch (tipo) {
                case CAMPUS: {
                    String sql = "SELECT u.id::text, u.nome, COALESCE(u.sigla, 'CAMPUS'), u.nome " +
                            "FROM academico.unidade u WHERE u.id_pai = 672 ORDER BY u.nome ASC";
                    List<Object[]> rows = em.createNativeQuery(sql).getResultList();
                    for (Object[] r : rows) {
                        AlvoAvaliado a = new AlvoAvaliado();
                        a.setTipo(TipoAlvoAvaliado.CAMPUS);
                        a.setIdOrigemSigaa(String.valueOf(r[0]));
                        a.setNome(String.valueOf(r[1]));
                        a.setCodigo(r[2] != null ? String.valueOf(r[2]) : null);
                        a.setCampus(String.valueOf(r[3]));
                        a.setAlvoAvaliadoId(gerarUuidDeterministo("CAMPUS", a.getIdOrigemSigaa()));
                        resultado.add(a);
                    }
                    break;
                }
                case SETOR: {
                    String sql = "SELECT u.id::text, u.nome, COALESCE(u.sigla, u.codigo, 'SETOR'), " +
                            "COALESCE(p.sigla, p.nome, 'Reitoria') " +
                            "FROM academico.unidade u " +
                            "LEFT JOIN academico.unidade p ON u.id_pai = p.id " +
                            "WHERE u.id_pai != 672 AND u.id != 672 " +
                            "ORDER BY u.nome ASC LIMIT 300";
                    List<Object[]> rows = em.createNativeQuery(sql).getResultList();
                    for (Object[] r : rows) {
                        AlvoAvaliado a = new AlvoAvaliado();
                        a.setTipo(TipoAlvoAvaliado.SETOR);
                        a.setIdOrigemSigaa(String.valueOf(r[0]));
                        a.setNome(String.valueOf(r[1]));
                        a.setCodigo(r[2] != null ? String.valueOf(r[2]) : null);
                        a.setCampus(r[3] != null ? String.valueOf(r[3]) : "Reitoria");
                        a.setAlvoAvaliadoId(gerarUuidDeterministo("SETOR", a.getIdOrigemSigaa()));
                        resultado.add(a);
                    }
                    break;
                }
                case DOCENTE: {
                    String sql = "SELECT s.id::text, p.nome, s.siape, COALESCE(u.nome, 'Geral') " +
                            "FROM academico.servidor s " +
                            "JOIN academico.pessoa p ON s.id_pessoa = p.id " +
                            "LEFT JOIN academico.unidade u ON s.id_unidade_lotacao = u.id " +
                            "LEFT JOIN academico.cargo c ON s.id_cargo = c.id " +
                            "WHERE c.denominacao ILIKE '%PROFESSOR%' " +
                            "ORDER BY p.nome ASC LIMIT 400";
                    List<Object[]> rows = em.createNativeQuery(sql).getResultList();
                    for (Object[] r : rows) {
                        AlvoAvaliado a = new AlvoAvaliado();
                        a.setTipo(TipoAlvoAvaliado.DOCENTE);
                        a.setIdOrigemSigaa(String.valueOf(r[0]));
                        a.setNome(String.valueOf(r[1]));
                        a.setCodigo(r[2] != null ? String.valueOf(r[2]) : null);
                        a.setCampus(r[3] != null ? String.valueOf(r[3]) : "Geral");
                        a.setAlvoAvaliadoId(gerarUuidDeterministo("DOCENTE", a.getIdOrigemSigaa()));
                        resultado.add(a);
                    }
                    break;
                }
                case SERVIDOR: {
                    String sql = "SELECT s.id::text, p.nome, s.siape, COALESCE(c.denominacao, 'Técnico-Administrativo'), COALESCE(u.nome, 'Geral') " +
                            "FROM academico.servidor s " +
                            "JOIN academico.pessoa p ON s.id_pessoa = p.id " +
                            "LEFT JOIN academico.unidade u ON s.id_unidade_lotacao = u.id " +
                            "LEFT JOIN academico.cargo c ON s.id_cargo = c.id " +
                            "WHERE c.denominacao NOT ILIKE '%PROFESSOR%' OR c.denominacao IS NULL " +
                            "ORDER BY p.nome ASC LIMIT 400";
                    List<Object[]> rows = em.createNativeQuery(sql).getResultList();
                    for (Object[] r : rows) {
                        AlvoAvaliado a = new AlvoAvaliado();
                        a.setTipo(TipoAlvoAvaliado.SERVIDOR);
                        a.setIdOrigemSigaa(String.valueOf(r[0]));
                        a.setNome(String.valueOf(r[1]));
                        a.setCodigo(r[2] != null ? String.valueOf(r[2]) : null);
                        a.setDescricao(r[3] != null ? String.valueOf(r[3]) : "Técnico-Administrativo");
                        a.setCampus(r[4] != null ? String.valueOf(r[4]) : "Geral");
                        a.setAlvoAvaliadoId(gerarUuidDeterministo("SERVIDOR", a.getIdOrigemSigaa()));
                        resultado.add(a);
                    }
                    break;
                }
                case CURSO: {
                    String sql = "SELECT c.id::text, c.nome, COALESCE(c.codigo, 'CURSO'), COALESCE(u.nome, 'Geral') " +
                            "FROM academico.curso c " +
                            "LEFT JOIN academico.unidade u ON c.id_unidade = u.id " +
                            "ORDER BY c.nome ASC LIMIT 300";
                    List<Object[]> rows = em.createNativeQuery(sql).getResultList();
                    for (Object[] r : rows) {
                        AlvoAvaliado a = new AlvoAvaliado();
                        a.setTipo(TipoAlvoAvaliado.CURSO);
                        a.setIdOrigemSigaa(String.valueOf(r[0]));
                        a.setNome(String.valueOf(r[1]));
                        a.setCodigo(r[2] != null ? String.valueOf(r[2]) : null);
                        a.setCampus(r[3] != null ? String.valueOf(r[3]) : "Geral");
                        a.setAlvoAvaliadoId(gerarUuidDeterministo("CURSO", a.getIdOrigemSigaa()));
                        resultado.add(a);
                    }
                    break;
                }
                case TURMA: {
                    String sql = "SELECT t.id_turma::text, " +
                            "CONCAT(t.nome_componente, ' (Turma ', t.codigo_turma, ' - ', t.ano, '/', t.periodo, ')'), " +
                            "t.codigo_turma, COALESCE(u.nome, 'Geral') " +
                            "FROM academico.v_turma_resumo t " +
                            "LEFT JOIN academico.turma tr ON t.id_turma = tr.id " +
                            "LEFT JOIN academico.unidade u ON tr.id_unidade = u.id " +
                            "WHERE t.ano >= 2024 " +
                            "ORDER BY t.ano DESC, t.periodo DESC, t.nome_componente ASC LIMIT 300";
                    List<Object[]> rows = em.createNativeQuery(sql).getResultList();
                    for (Object[] r : rows) {
                        AlvoAvaliado a = new AlvoAvaliado();
                        a.setTipo(TipoAlvoAvaliado.TURMA);
                        a.setIdOrigemSigaa(String.valueOf(r[0]));
                        a.setNome(String.valueOf(r[1]));
                        a.setCodigo(r[2] != null ? String.valueOf(r[2]) : null);
                        a.setCampus(r[3] != null ? String.valueOf(r[3]) : "Geral");
                        a.setAlvoAvaliadoId(gerarUuidDeterministo("TURMA", a.getIdOrigemSigaa()));
                        resultado.add(a);
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            // Caso ocorra falha de consulta, retorna lista vazia para acionar fallback
        }
        return resultado;
    }

    private UUID gerarUuidDeterministo(String prefixo, String idOrigem) {
        String chave = prefixo + ":" + idOrigem;
        return UUID.nameUUIDFromBytes(chave.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

    // --- TIPOS DE PÚBLICO / ALVO AVALIADO ---

    @GET
    @Path("/tipos-publico")
    public List<TipoPublico> listarTiposPublico() {
        return em.createQuery("SELECT tp FROM TipoPublico tp WHERE tp.ativo = true ORDER BY tp.ordem ASC, tp.codigo ASC", TipoPublico.class)
                .getResultList();
    }

    @POST
    @Path("/tipos-publico")
    @Transactional
    public Response salvarTipoPublico(TipoPublico tipoPublico) {
        if (tipoPublico.getCreatedAt() == null) {
            tipoPublico.setCreatedAt(OffsetDateTime.now());
        }
        em.merge(tipoPublico);
        return Response.ok(tipoPublico).build();
    }
}
