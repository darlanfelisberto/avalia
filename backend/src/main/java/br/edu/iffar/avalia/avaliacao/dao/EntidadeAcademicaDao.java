package br.edu.iffar.avalia.avaliacao.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Espelha registros acadêmicos e vínculos no domínio de avaliação por operações em lote. */
@ApplicationScoped
public class EntidadeAcademicaDao {
    @Inject
    private AvaliacaoDao dao;

    @Transactional
    public void sincronizar() {
        dao.nativeQuery("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            select te.tipo_entidade_id, 'academico:aluno:' || a.id, coalesce(nullif(p.nome_social, ''), p.nome) || ' · ' || coalesce(a.matricula, a.id::text)
            from academico.aluno a join academico.pessoa p on p.id = a.id_pessoa
            cross join avaliacao.tipo_entidade te where te.codigo = 'ALUNO'
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            select te.tipo_entidade_id, 'academico:docente:' || s.id, coalesce(nullif(p.nome_social, ''), p.nome)
            from (select distinct id_servidor from academico.docente_turma where id_servidor is not null) dt
            join academico.servidor s on s.id = dt.id_servidor join academico.pessoa p on p.id = s.id_pessoa
            cross join avaliacao.tipo_entidade te where te.codigo = 'DOCENTE'
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            select te.tipo_entidade_id, 'academico:turma:' || t.id, left(concat_ws(' · ', t.codigo_turma, cc.nome, t.ano || '/' || t.periodo), 300)
            from academico.turma t join academico.componente_curricular cc on cc.id = t.id_componente_curricular
            cross join avaliacao.tipo_entidade te where te.codigo = 'TURMA'
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            select te.tipo_entidade_id, 'academico:unidade:' || u.id, coalesce(nullif(u.sigla, ''), u.nome)
            from academico.unidade u cross join avaliacao.tipo_entidade te
            where (te.codigo = 'CAMPUS' and lower(concat_ws(' ', u.nome, u.sigla, u.tipo, u.categoria)) like '%campus%')
            or (te.codigo = 'SETOR' and lower(concat_ws(' ', u.nome, u.sigla, u.tipo, u.categoria)) not like '%campus%')
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            select te.tipo_entidade_id, usuario.cpf_cnpj, usuario.rotulo
            from (
                select distinct on (p.cpf_cnpj) p.cpf_cnpj, coalesce(nullif(p.nome_social, ''), p.nome) as rotulo
                from academico.pessoa p
                where p.cpf_cnpj is not null
                  and exists (select 1 from academico.aluno a where a.id_pessoa = p.id)
                order by p.cpf_cnpj, p.id
            ) usuario
            cross join avaliacao.tipo_entidade te
            where te.codigo = 'ALUNO'
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            select te.tipo_entidade_id, usuario.cpf_cnpj, usuario.rotulo
            from (
                select distinct on (p.cpf_cnpj) p.cpf_cnpj, coalesce(nullif(p.nome_social, ''), p.nome) as rotulo
                from academico.pessoa p
                where p.cpf_cnpj is not null
                  and exists (select 1 from academico.servidor s where s.id_pessoa = p.id)
                order by p.cpf_cnpj, p.id
            ) usuario
            cross join avaliacao.tipo_entidade te
            where te.codigo = 'SERVIDOR'
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
            select te.tipo_entidade_id, usuario.cpf_cnpj, usuario.rotulo
            from (
                select distinct on (p.cpf_cnpj) p.cpf_cnpj, coalesce(nullif(p.nome_social, ''), p.nome) as rotulo
                from academico.pessoa p
                where p.cpf_cnpj is not null
                  and exists (
                      select 1 from academico.servidor s
                      join academico.docente_turma dt on dt.id_servidor = s.id
                      where s.id_pessoa = p.id
                  )
                order by p.cpf_cnpj, p.id
            ) usuario
            cross join avaliacao.tipo_entidade te
            where te.codigo = 'DOCENTE'
            on conflict (tipo_entidade_id, referencia_externa) do update set rotulo = excluded.rotulo, ativo = true
            """).executeUpdate();
        dao.nativeQuery("""
            delete from avaliacao.entidade_vinculo v using avaliacao.entidade er, avaliacao.tipo_entidade tr
            where v.entidade_origem_id = er.entidade_id and er.tipo_entidade_id = tr.tipo_entidade_id
            and tr.codigo in ('ALUNO','DOCENTE','SERVIDOR') and v.tipo_vinculo in ('MATRICULADO_EM','LECIONA','PERTENCE_A')
            and er.referencia_externa not like 'academico:%'
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select er.entidade_id, turma.entidade_id, 'MATRICULADO_EM' from academico.matricula m
            join academico.aluno a on a.id = m.id_aluno join academico.pessoa p on p.id = a.id_pessoa
            join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'ALUNO'
            join avaliacao.entidade turma on turma.referencia_externa = 'academico:turma:' || m.id_turma
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select er.entidade_id, turma.entidade_id, 'LECIONA' from academico.docente_turma dt
            join academico.servidor s on s.id = dt.id_servidor join academico.pessoa p on p.id = s.id_pessoa
            join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'DOCENTE'
            join avaliacao.entidade turma on turma.referencia_externa = 'academico:turma:' || dt.id_turma
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select distinct er.entidade_id, unit.entidade_id, 'PERTENCE_A' from academico.servidor s
            join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'SERVIDOR'
            join avaliacao.entidade unit on unit.referencia_externa in ('academico:unidade:' || s.id_unidade_lotacao, 'academico:unidade:' || s.id_unidade_exercicio)
            join avaliacao.tipo_entidade tu on tu.tipo_entidade_id = unit.tipo_entidade_id and tu.codigo in ('CAMPUS','SETOR')
            where s.id_unidade_lotacao is not null or s.id_unidade_exercicio is not null
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select distinct er.entidade_id, campus.entidade_id, 'PERTENCE_A' from academico.servidor s
            join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'SERVIDOR'
            join academico.unidade_closure uc on uc.id_descendente in (s.id_unidade_lotacao, s.id_unidade_exercicio)
            join avaliacao.entidade campus on campus.referencia_externa = 'academico:unidade:' || uc.id_ancestral
            join avaliacao.tipo_entidade tc on tc.tipo_entidade_id = campus.tipo_entidade_id and tc.codigo = 'CAMPUS'
            where p.cpf_cnpj is not null
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select distinct er.entidade_id, unit.entidade_id, 'PERTENCE_A' from academico.servidor s
            join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'DOCENTE'
            join avaliacao.entidade unit on unit.referencia_externa in ('academico:unidade:' || s.id_unidade_lotacao, 'academico:unidade:' || s.id_unidade_exercicio)
            join avaliacao.tipo_entidade tu on tu.tipo_entidade_id = unit.tipo_entidade_id and tu.codigo in ('CAMPUS','SETOR')
            where s.id_unidade_lotacao is not null or s.id_unidade_exercicio is not null
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select distinct er.entidade_id, campus.entidade_id, 'PERTENCE_A' from academico.servidor s
            join academico.pessoa p on p.id = s.id_pessoa join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'DOCENTE'
            join academico.unidade_closure uc on uc.id_descendente in (s.id_unidade_lotacao, s.id_unidade_exercicio)
            join avaliacao.entidade campus on campus.referencia_externa = 'academico:unidade:' || uc.id_ancestral
            join avaliacao.tipo_entidade tc on tc.tipo_entidade_id = campus.tipo_entidade_id and tc.codigo = 'CAMPUS'
            where p.cpf_cnpj is not null
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select distinct er.entidade_id, campus.entidade_id, 'PERTENCE_A' from academico.aluno a
            join academico.pessoa p on p.id = a.id_pessoa join academico.curso c on c.id = a.id_curso
            join academico.unidade_closure uc on uc.id_descendente = c.id_unidade
            join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'ALUNO'
            join avaliacao.entidade campus on campus.referencia_externa = 'academico:unidade:' || uc.id_ancestral
            join avaliacao.tipo_entidade tc on tc.tipo_entidade_id = campus.tipo_entidade_id and tc.codigo = 'CAMPUS'
            where p.cpf_cnpj is not null
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.entidade_vinculo (entidade_origem_id, entidade_destino_id, tipo_vinculo)
            select distinct er.entidade_id, setor.entidade_id, 'PERTENCE_A' from academico.aluno a
            join academico.pessoa p on p.id = a.id_pessoa join academico.curso c on c.id = a.id_curso
            join avaliacao.entidade er on er.referencia_externa = p.cpf_cnpj
            join avaliacao.tipo_entidade tr on tr.tipo_entidade_id = er.tipo_entidade_id and tr.codigo = 'ALUNO'
            join avaliacao.entidade setor on setor.referencia_externa = 'academico:unidade:' || c.id_unidade
            join avaliacao.tipo_entidade ts on ts.tipo_entidade_id = setor.tipo_entidade_id and ts.codigo = 'SETOR'
            where p.cpf_cnpj is not null
            on conflict (entidade_origem_id, entidade_destino_id, tipo_vinculo) do nothing
            """).executeUpdate();
        dao.nativeQuery("""
            insert into avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id)
            select i.instrumento_avaliativo_id, ita.tipo_entidade_alvo_id, e.entidade_id
            from avaliacao.instrumento_avaliativo i join avaliacao.instrumento_tipo_alvo ita
            on ita.instrumento_avaliativo_id = i.instrumento_avaliativo_id
            join avaliacao.tipo_entidade te on te.tipo_entidade_id = ita.tipo_entidade_alvo_id
            join avaliacao.entidade e on e.tipo_entidade_id = te.tipo_entidade_id
            where i.status = 'PUBLICADO' and ((te.codigo = 'ALUNO' and e.referencia_externa like 'academico:aluno:%')
            or (te.codigo = 'DOCENTE' and e.referencia_externa like 'academico:docente:%')
            or (te.codigo in ('CAMPUS','SETOR') and e.referencia_externa like 'academico:unidade:%'))
            on conflict (instrumento_avaliativo_id, entidade_id) do nothing
            """).executeUpdate();
    }
}
