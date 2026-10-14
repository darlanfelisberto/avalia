package br.edu.iffar.avalia.avaliacao.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** Consultas de elegibilidade que cruzam o schema avaliacao com os dados acadêmicos. */
@ApplicationScoped
public class ResponderAvaliacaoDao {
    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> listarGerais(String codigo, String cpf) {
        return em.createNativeQuery("select cast(i.instrumento_avaliativo_id as text), i.titulo, i.descricao, " +
                        "cast(alvo.entidade_id as text), alvo.rotulo, ta.codigo, null::text, null::text, exists (" +
                        "select 1 from avaliacao.participacao p join avaliacao.entidade er " +
                        "on er.entidade_id = p.entidade_respondente_id " +
                        "where p.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                        "and p.entidade_avaliada_id = alvo.entidade_id " +
                        "and er.tipo_entidade_id = ir.tipo_entidade_respondente_id " +
                        "and er.referencia_externa = ?2) " +
                        "from avaliacao.instrumento_avaliativo i " +
                        "join avaliacao.instrumento_respondente ir on ir.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                        "join avaliacao.tipo_entidade t on t.tipo_entidade_id = ir.tipo_entidade_respondente_id " +
                        "join avaliacao.instrumento_entidade_avaliada ia on ia.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                        "join avaliacao.entidade alvo on alvo.entidade_id = ia.entidade_id " +
                        "join avaliacao.tipo_entidade ta on ta.tipo_entidade_id = ia.tipo_entidade_alvo_id " +
                        "where i.status = 'PUBLICADO' and now() between i.inicio_aplicacao and i.fim_aplicacao " +
                        "and i.tipo_entidade_avaliada_id not in (select tipo_entidade_id from avaliacao.tipo_entidade where codigo in ('TURMA','DOCENTE')) " +
                        "and (ta.codigo <> 'INSTITUICAO' or alvo.referencia_externa = 'instrumento:' || i.instrumento_avaliativo_id) " +
                        "and t.codigo = ?1 and (ta.codigo not in ('CAMPUS','SETOR') or exists (" +
                        "select 1 from academico.unidade alvo_unidade where alvo.referencia_externa = 'academico:unidade:' || alvo_unidade.id " +
                        "and exists (select 1 from (" +
                        "select s.id_unidade_lotacao id_unidade from academico.servidor s join academico.pessoa p on p.id = s.id_pessoa where p.cpf_cnpj = ?2 " +
                        "union select s.id_unidade_exercicio from academico.servidor s join academico.pessoa p on p.id = s.id_pessoa where p.cpf_cnpj = ?2 " +
                        "union select c.id_unidade from academico.aluno a join academico.pessoa p on p.id = a.id_pessoa " +
                        "join academico.curso c on c.id = a.id_curso where p.cpf_cnpj = ?2) unidades_usuario " +
                        "where unidades_usuario.id_unidade is not null and ((ta.codigo = 'SETOR' and unidades_usuario.id_unidade = alvo_unidade.id) " +
                        "or (ta.codigo = 'CAMPUS' and exists (select 1 from academico.unidade_closure uc " +
                        "where uc.id_ancestral = alvo_unidade.id and uc.id_descendente = unidades_usuario.id_unidade)))))) " +
                        "and (ir.tipo_vinculo is null or ta.codigo in ('CAMPUS','SETOR') or exists (" +
                        "select 1 from avaliacao.entidade er join avaliacao.entidade_vinculo v " +
                        "on v.entidade_origem_id = er.entidade_id " +
                        "where er.tipo_entidade_id = t.tipo_entidade_id and er.referencia_externa = ?2 " +
                        "and v.entidade_destino_id = alvo.entidade_id and v.tipo_vinculo = ir.tipo_vinculo)) " +
                        "order by i.titulo, alvo.rotulo")
                .setParameter(1, codigo).setParameter(2, cpf).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> listarAlvosDeTurma(String cpf, String codigo) {
        return em.createNativeQuery(
                "select distinct cast(i.instrumento_avaliativo_id as text), i.titulo, i.descricao, " +
                "cast(alvo.entidade_id as text), alvo.rotulo, ta.codigo, cast(turma.entidade_id as text), turma.rotulo, " +
                "exists (select 1 from avaliacao.participacao p join avaliacao.entidade er " +
                "on er.entidade_id = p.entidade_respondente_id where p.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                "and p.entidade_avaliada_id = alvo.entidade_id and p.entidade_contexto_id = turma.entidade_id " +
                "and er.tipo_entidade_id = ir.tipo_entidade_respondente_id and er.referencia_externa = ?1) " +
                "from avaliacao.instrumento_avaliativo i " +
                "join avaliacao.instrumento_respondente ir on ir.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                "join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = ir.tipo_entidade_respondente_id " +
                "join avaliacao.instrumento_tipo_alvo ita on ita.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                "join avaliacao.tipo_entidade ta on ta.tipo_entidade_id = ita.tipo_entidade_alvo_id " +
                "join avaliacao.instrumento_entidade_avaliada ia on ia.instrumento_avaliativo_id = i.instrumento_avaliativo_id " +
                "and ia.tipo_entidade_alvo_id = ita.tipo_entidade_alvo_id " +
                "join avaliacao.entidade alvo on alvo.entidade_id = ia.entidade_id " +
                "join avaliacao.entidade turma on turma.tipo_entidade_id = i.tipo_entidade_avaliada_id " +
                "and turma.referencia_externa like 'academico:turma:%' " +
                "join academico.turma tc on turma.referencia_externa = 'academico:turma:' || tc.id " +
                "where i.status = 'PUBLICADO' and now() between i.inicio_aplicacao and i.fim_aplicacao " +
                "and i.ciclo_referencia = tc.ano || '/' || tc.periodo " +
                "and tr.codigo = ?2 and ta.codigo in ('DOCENTE','ALUNO') " +
                "and ((?2 = 'ALUNO' and exists (select 1 from academico.matricula m join academico.aluno a on a.id = m.id_aluno " +
                "join academico.pessoa p on p.id = a.id_pessoa where p.cpf_cnpj = ?1 " +
                "and m.id_turma = tc.id)) " +
                "or (?2 = 'DOCENTE' and exists (select 1 from academico.docente_turma dt join academico.servidor sd on sd.id = dt.id_servidor " +
                "join academico.pessoa p on p.id = sd.id_pessoa where p.cpf_cnpj = ?1 and dt.id_turma = tc.id))) " +
                "and ((ta.codigo = 'DOCENTE' and exists (select 1 from academico.docente_turma dt2 " +
                "where dt2.id_turma = tc.id and alvo.referencia_externa = 'academico:docente:' || dt2.id_servidor)) " +
                "or (ta.codigo = 'ALUNO' and exists (select 1 from academico.matricula m2 " +
                "where m2.id_turma = tc.id and alvo.referencia_externa = 'academico:aluno:' || m2.id_aluno))) " +
                "order by i.titulo, turma.rotulo, ta.codigo, alvo.rotulo")
                .setParameter(1, cpf).setParameter(2, codigo).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> listarDocentesDaTurma(String cpf) {
        return em.createNativeQuery("""
                select distinct cast(i.instrumento_avaliativo_id as text), i.titulo, i.descricao,
                    cast(alvo.entidade_id as text), alvo.rotulo, ta.codigo,
                    cast(turma.entidade_id as text), turma.rotulo,
                    exists (
                        select 1 from avaliacao.participacao p
                        join avaliacao.entidade er on er.entidade_id = p.entidade_respondente_id
                        where p.instrumento_avaliativo_id = i.instrumento_avaliativo_id
                          and p.entidade_avaliada_id = alvo.entidade_id
                          and p.entidade_contexto_id = turma.entidade_id
                          and er.tipo_entidade_id = ir.tipo_entidade_respondente_id
                          and er.referencia_externa = ?1
                    )
                from avaliacao.instrumento_avaliativo i
                join avaliacao.tipo_entidade contexto on contexto.tipo_entidade_id = i.tipo_entidade_avaliada_id
                join avaliacao.instrumento_respondente ir on ir.instrumento_avaliativo_id = i.instrumento_avaliativo_id
                join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = ir.tipo_entidade_respondente_id
                join avaliacao.instrumento_entidade_avaliada ia on ia.instrumento_avaliativo_id = i.instrumento_avaliativo_id
                join avaliacao.entidade alvo on alvo.entidade_id = ia.entidade_id
                join avaliacao.tipo_entidade ta on ta.tipo_entidade_id = ia.tipo_entidade_alvo_id
                join academico.docente_turma dt on alvo.referencia_externa = 'academico:docente:' || dt.id_servidor
                join academico.turma tc on tc.id = dt.id_turma
                join avaliacao.entidade turma on turma.referencia_externa = 'academico:turma:' || tc.id
                join avaliacao.tipo_entidade tt on tt.tipo_entidade_id = turma.tipo_entidade_id and tt.codigo = 'TURMA'
                where i.status = 'PUBLICADO'
                  and now() between i.inicio_aplicacao and i.fim_aplicacao
                  and contexto.codigo = 'DOCENTE' and ta.codigo = 'DOCENTE' and tr.codigo = 'ALUNO'
                  and i.ciclo_referencia = tc.ano || '/' || tc.periodo
                  and exists (
                      select 1 from academico.matricula m
                      join academico.aluno a on a.id = m.id_aluno
                      join academico.pessoa p on p.id = a.id_pessoa
                      where m.id_turma = tc.id and p.cpf_cnpj = ?1
                  )
                order by i.titulo, turma.rotulo, alvo.rotulo
                """)
                .setParameter(1, cpf).getResultList();
    }
}
