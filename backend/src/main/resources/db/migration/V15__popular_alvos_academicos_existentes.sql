-- Bancos que já tinham dados do SIGAA antes da sincronização das entidades
-- avaliativas precisam materializar esses alvos para os instrumentos publicados.
insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
select te.tipo_entidade_id, 'academico:aluno:' || a.id,
       coalesce(nullif(p.nome_social, ''), p.nome) || ' · ' || coalesce(a.matricula, a.id::text)
from academico.aluno a
join academico.pessoa p on p.id = a.id_pessoa
join avaliacao.tipo_entidade te on te.codigo = 'ALUNO'
on conflict (tipo_entidade_id, referencia_externa)
do update set rotulo = excluded.rotulo, ativo = true;

insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
select te.tipo_entidade_id, 'academico:docente:' || s.id,
       coalesce(nullif(p.nome_social, ''), p.nome)
from (select distinct id_servidor from academico.docente_turma where id_servidor is not null) dt
join academico.servidor s on s.id = dt.id_servidor
join academico.pessoa p on p.id = s.id_pessoa
join avaliacao.tipo_entidade te on te.codigo = 'DOCENTE'
on conflict (tipo_entidade_id, referencia_externa)
do update set rotulo = excluded.rotulo, ativo = true;

insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
select te.tipo_entidade_id, 'academico:turma:' || t.id,
       left(concat_ws(' · ', t.codigo_turma, cc.nome, t.ano || '/' || t.periodo), 300)
from academico.turma t
join academico.componente_curricular cc on cc.id = t.id_componente_curricular
join avaliacao.tipo_entidade te on te.codigo = 'TURMA'
on conflict (tipo_entidade_id, referencia_externa)
do update set rotulo = excluded.rotulo, ativo = true;

insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
select te.tipo_entidade_id, 'academico:unidade:' || u.id,
       coalesce(nullif(u.sigla, ''), u.nome)
from academico.unidade u
join avaliacao.tipo_entidade te
  on (te.codigo = 'CAMPUS' and lower(concat_ws(' ', u.nome, u.sigla, u.tipo, u.categoria)) like '%campus%')
  or (te.codigo = 'SETOR' and lower(concat_ws(' ', u.nome, u.sigla, u.tipo, u.categoria)) not like '%campus%')
on conflict (tipo_entidade_id, referencia_externa)
do update set rotulo = excluded.rotulo, ativo = true;

insert into avaliacao.instrumento_entidade_avaliada
    (instrumento_avaliativo_id, tipo_entidade_alvo_id, entidade_id)
select i.instrumento_avaliativo_id, ita.tipo_entidade_alvo_id, e.entidade_id
from avaliacao.instrumento_avaliativo i
join avaliacao.instrumento_tipo_alvo ita
  on ita.instrumento_avaliativo_id = i.instrumento_avaliativo_id
join avaliacao.tipo_entidade te on te.tipo_entidade_id = ita.tipo_entidade_alvo_id
join avaliacao.entidade e on e.tipo_entidade_id = te.tipo_entidade_id
where i.status = 'PUBLICADO'
  and ((te.codigo = 'ALUNO' and e.referencia_externa like 'academico:aluno:%')
    or (te.codigo = 'DOCENTE' and e.referencia_externa like 'academico:docente:%')
    or (te.codigo in ('CAMPUS', 'SETOR') and e.referencia_externa like 'academico:unidade:%'))
on conflict (instrumento_avaliativo_id, entidade_id) do nothing;
