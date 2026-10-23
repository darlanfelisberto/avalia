package br.edu.iffar.avalia.avaliacao.dao;

import br.com.feliva.sharedClass.db.InjectEntityManagerDAO;
import br.edu.iffar.avalia.academico.model.Pessoa;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.Questao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoRespondente;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoAlvo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Acesso ao schema avaliacao. SQL nativo fica reservado a consultas de projeção e cargas em lote. */
@ApplicationScoped
public class InstrumentoAvaliativoDAO  extends InjectEntityManagerDAO<InstrumentoAvaliativo> {
    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;



    public <T> T referencia(Class<T> tipo, UUID id) {
        return em.getReference(tipo, id);
    }

    public <T> T buscar(Class<T> tipo, UUID id) {
        return em.find(tipo, id);
    }

    public void persistir(Object entidade) {
        em.persist(entidade);
    }

    public void remover(Object entidade) {
        em.remove(entidade);
    }

    public void flush() {
        em.flush();
    }

    public void removerEstrutura(InstrumentoAvaliativo instrumento) {
        List<InstrumentoSessao> sessoes = em.createQuery("""
                select s from InstrumentoSessao s
                where s.instrumentoAvaliativo = :instrumento
                """, InstrumentoSessao.class)
                .setParameter("instrumento", instrumento).getResultList();
        Set<Questao> candidatas = new HashSet<>();
        for (InstrumentoSessao sessao : sessoes) {
            List<SessaoQuestao> posicoes = em.createQuery("""
                    select sq from SessaoQuestao sq where sq.instrumentoSessao = :sessao
                    """, SessaoQuestao.class)
                    .setParameter("sessao", sessao).getResultList();
            for (SessaoQuestao posicao : posicoes) {
                candidatas.add(posicao.getQuestao());
                em.remove(posicao);
            }
        }
        em.flush();
        for (InstrumentoSessao sessao : sessoes) em.remove(sessao);
        em.flush();
        for (Questao questao : candidatas) {
            Long usos = em.createQuery("select count(sq) from SessaoQuestao sq where sq.questao = :questao", Long.class)
                    .setParameter("questao", questao).getSingleResult();
            if (usos == 0) {
                ConjuntoOpcao conjunto = questao.getConjuntoOpcao();
                em.remove(questao);
                em.flush();
                if (conjunto != null && conjunto.isExclusivo()) {
                    Long usosConjunto = em.createQuery("select count(q) from Questao q where q.conjuntoOpcao = :conjunto", Long.class)
                            .setParameter("conjunto", conjunto).getSingleResult();
                    if (usosConjunto == 0) em.remove(conjunto);
                }
            }
        }
        em.flush();
        instrumento.getInstrumentoSessoesList().clear();
    }

    public void removerRespondentes(InstrumentoAvaliativo instrumento) {
        List<InstrumentoTipoRespondente> atuais = em.createQuery("""
                select r from InstrumentoTipoRespondente r where r.instrumentoAvaliativo = :instrumento
                """, InstrumentoTipoRespondente.class)
                .setParameter("instrumento", instrumento).getResultList();
        for (InstrumentoTipoRespondente atual : atuais) em.remove(atual);
        em.flush();
        instrumento.getInstrumentoTipoRespondenteList().clear();
    }

    public void substituirTipoAlvo(InstrumentoAvaliativo instrumento, UUID tipoAlvoId) {
        List<InstrumentoTipoAlvo> atuais = em.createQuery("""
                select a from InstrumentoTipoAlvo a where a.instrumentoId = :id
                """, InstrumentoTipoAlvo.class)
                .setParameter("id", instrumento.getId()).getResultList();
        if (atuais.size() == 1 && atuais.get(0).getTipoAlvoId().equals(tipoAlvoId)) return;
        for (InstrumentoTipoAlvo atual : atuais) em.remove(atual);
        em.flush();
        em.persist(new InstrumentoTipoAlvo(instrumento.getId(), tipoAlvoId));
    }

    public List<ConjuntoOpcao> listarConjuntos() {
        return em.createQuery("""
                select distinct c from ConjuntoOpcao c left join fetch c.opcoes
                where c.exclusivo = false or c.exclusivo is null
                order by c.nome
                """, ConjuntoOpcao.class).getResultList();
    }

    public Query nativeQuery(String sql){
        return this.em.createNativeQuery(sql);
    }

    public List<InstrumentoAvaliativo> listInstrumentos(int pagina, int tamanho) {
       return em.createQuery("""
                    select i from InstrumentoAvaliativo i
                    left join fetch i.tipoEntidadeAvaliada
                    order by i.atualizadoEm desc
                """, InstrumentoAvaliativo.class)
                .setFirstResult((pagina - 1) * tamanho)
                .setMaxResults(tamanho)
                .getResultList();
    }

    @Transactional
    public InstrumentoAvaliativo findInstrumentosById(UUID id) {

        // Inicialize cada coleção em sua própria consulta. Buscar várias List ao
        // mesmo tempo pode gerar MultipleBagFetchException no Hibernate.
        InstrumentoAvaliativo instrumento = em.createQuery("""
                    select distinct i from InstrumentoAvaliativo i
                    left join fetch i.tipoEntidadeAvaliada
                    left join fetch i.instrumentoTipoRespondenteList r
                    left join fetch r.tipoEntidade
                    where i.instrumentoAvaliativoId = :id
                """, InstrumentoAvaliativo.class)
                .setParameter("id", id)
                .getSingleResult();

        em.createQuery("""
                    select distinct i from InstrumentoAvaliativo i
                    left join fetch i.instrumentoSessoesList s
                    where i.instrumentoAvaliativoId = :id
                """, InstrumentoAvaliativo.class)
                .setParameter("id", id)
                .getSingleResult();

        em.createQuery("""
                    select distinct s from InstrumentoSessao s
                    left join fetch s.sessaoQuestoesList sq
                    left join fetch sq.questao q
                    left join fetch q.conjuntoOpcao
                    where s.instrumentoAvaliativo.instrumentoAvaliativoId = :id
                """, InstrumentoSessao.class)
                .setParameter("id", id)
                .getResultList();

        em.createQuery("""
                    select distinct c from ConjuntoOpcao c
                    left join fetch c.opcoes
                    where c.exclusivo = true and exists (
                        select 1 from SessaoQuestao sq
                        where sq.questao.conjuntoOpcao = c
                        and sq.instrumentoSessao.instrumentoAvaliativo.instrumentoAvaliativoId = :id
                    )
                """, br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao.class)
                .setParameter("id", id)
                .getResultList();

        em.createQuery("""
                    select distinct i from InstrumentoAvaliativo i
                    left join fetch i.tiposAlvoList ta
                    left join fetch ta.tipoAlvo
                    where i.instrumentoAvaliativoId = :id
                """, InstrumentoAvaliativo.class)
                .setParameter("id", id)
                .getSingleResult();

        return instrumento;
    }

//    public List<Object[]> listarIdsQuestoes(String id) {
//        return dao.rows("""
//            select cast(sq.questao_id as text) from avaliacao.sessao_questao sq
//            join avaliacao.instrumento_sessao s on s.instrumento_sessao_id = sq.instrumento_sessao_id
//            where s.instrumento_avaliativo_id = cast(?1 as uuid)
//            """, id);
//    }
//
//    public void criarEntidadeInstitucional(String tipoId, String referencia, String titulo) {
//        dao.exec("""
//            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
//            values (cast(?1 as uuid), ?2, ?3) on conflict (tipo_entidade_id, referencia_externa) do nothing
//            """,
//                    tipoId, referencia, titulo);
//    }
//
//    public void vincularEntidadeInstitucional(String id, String tipoId, String referencia) {
//        dao.exec("""
//            insert into avaliacao.instrumento_entidade_avaliada
//            (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id)
//            select cast(?1 as uuid), cast(?2 as uuid), e.entidade_id from avaliacao.entidade e
//            where e.tipo_entidade_id = cast(?2 as uuid) and e.referencia_externa = ?3
//            on conflict (instrumento_avaliativo_id, entidade_id) do nothing
//            """,
//                    id, tipoId, referencia);
//    }

//    public void popularAlvos(String id) {
//        dao.exec("""
//            insert into avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id)
//            select i.instrumento_avaliativo_id, ita.tipo_entidade_alvo_id, e.entidade_id
//            from avaliacao.instrumento_avaliativo i join avaliacao.instrumento_tipo_alvo ita
//            on ita.instrumento_avaliativo_id = i.instrumento_avaliativo_id
//            join avaliacao.tipo_entidade te on te.tipo_entidade_id = ita.tipo_entidade_alvo_id
//            join avaliacao.entidade e on e.tipo_entidade_id = ita.tipo_entidade_alvo_id
//            where i.instrumento_avaliativo_id = cast(?1 as uuid)
//            and (e.referencia_externa like 'academico:%'
//            or (te.codigo = 'INSTITUICAO' and e.referencia_externa = 'instrumento:' || i.instrumento_avaliativo_id))
//            on conflict (instrumento_avaliativo_id, entidade_id) do nothing
//            """, id);
//    }

//    public void removerQuestaoOrfa(String id) {
//        if (dao.count("""
//                select count(*) from avaliacao.sessao_questao
//                where questao_id = cast(?1 as uuid)
//                """, id) != 0) return;
//        var questao = dao.buscar(br.edu.iffar.avalia.avaliacao.model.Questao.class, UUID.fromString(id));
//        if (questao == null) return;
//        var conjunto = questao.getConjuntoOpcao();
//        dao.remover(questao);
//        dao.flush();
//        if (conjunto != null && conjunto.isExclusivo()) dao.remover(conjunto);
//    }

    public List<InstrumentoAvaliativo> listAllForUser(Pessoa pessoa){
        return em.createQuery("""
                    select i from InstrumentoAvaliativo i
                    left join fetch i.tipoEntidadeAvaliada tea 
                    where i.status = 'PUBLICADO' 
                    and CURRENT_TIMESTAMP between i.inicioAplicacao and i.fimAplicacao 
                    and i.tipoEntidadeAvaliada in (
                        select avalia from Pessoa p 
                        left join p.tipoEntidades t 
                        left join TipoEntidadePermissao permissao on permissao.respondente.tipoEntidadeId = t.id
                        left join TipoEntidade avalia on avalia.tipoEntidadeId = permissao.avaliado.tipoEntidadeId
                        where p.idPessoa = 155099
                    )                        
                """, InstrumentoAvaliativo.class)
//                .setParameter("id", pessoa.getMMId())
                .getResultList();
    }
}
