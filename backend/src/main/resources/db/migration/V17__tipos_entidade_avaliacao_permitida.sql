create table avaliacao.tipo_entidade_permissao (
    tipo_entidade_permissao_id uuid primary key default gen_random_uuid(),
    tipo_respondente_id uuid not null references avaliacao.tipo_entidade(tipo_entidade_id) on delete cascade,
    tipo_avaliado_id uuid not null references avaliacao.tipo_entidade(tipo_entidade_id) on delete cascade,
    unique (tipo_respondente_id, tipo_avaliado_id)
);

-- Preserva a configuração atual para os tipos já habilitados nos catálogos.
insert into avaliacao.tipo_entidade_permissao (tipo_respondente_id, tipo_avaliado_id)
select respondente.tipo_entidade_id, avaliado.tipo_entidade_id
from avaliacao.tipo_entidade respondente
cross join avaliacao.tipo_entidade avaliado
where respondente.pode_responder
  and avaliado.pode_ser_avaliada
  and respondente.codigo in ('ALUNO', 'DOCENTE', 'SERVIDOR')
on conflict (tipo_respondente_id, tipo_avaliado_id) do nothing;
