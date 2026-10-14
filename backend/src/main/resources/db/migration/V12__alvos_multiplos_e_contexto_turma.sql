-- Permite que um instrumento avalie vários tipos de entidade, mantendo o tipo
-- original como contexto (por exemplo, TURMA com alvos DOCENTE e ALUNO).
create table avaliacao.instrumento_tipo_alvo (
    instrumento_avaliativo_id uuid not null
        references avaliacao.instrumento_avaliativo(instrumento_avaliativo_id) on delete cascade,
    tipo_entidade_alvo_id uuid not null
        references avaliacao.tipo_entidade(tipo_entidade_id),
    primary key (instrumento_avaliativo_id, tipo_entidade_alvo_id)
);

insert into avaliacao.instrumento_tipo_alvo (instrumento_avaliativo_id, tipo_entidade_alvo_id)
select instrumento_avaliativo_id, tipo_entidade_avaliada_id
from avaliacao.instrumento_avaliativo
on conflict do nothing;

-- O alvo avaliado agora pode ter tipo diferente do contexto principal.
do $$
declare c record;
begin
    for c in select conname from pg_constraint
             where conrelid = 'avaliacao.instrumento_entidade_avaliada'::regclass
               and confrelid = 'avaliacao.instrumento_avaliativo'::regclass
    loop
        execute format('alter table avaliacao.instrumento_entidade_avaliada drop constraint %I', c.conname);
    end loop;
end $$;
alter table avaliacao.instrumento_entidade_avaliada
    rename column tipo_entidade_avaliada_id to tipo_entidade_alvo_id;
alter table avaliacao.instrumento_entidade_avaliada
    add constraint fk_instrumento_entidade_alvo_instrumento
        foreign key (instrumento_avaliativo_id)
        references avaliacao.instrumento_avaliativo(instrumento_avaliativo_id) on delete cascade;
alter table avaliacao.instrumento_entidade_avaliada
    add constraint fk_instrumento_entidade_alvo_tipo
        foreign key (instrumento_avaliativo_id, tipo_entidade_alvo_id)
        references avaliacao.instrumento_tipo_alvo(instrumento_avaliativo_id, tipo_entidade_alvo_id) on delete cascade;

-- A turma é contexto da resposta; o alvo continua sendo o docente ou aluno.
alter table avaliacao.participacao
    add column entidade_contexto_id uuid references avaliacao.entidade(entidade_id);
do $$
declare c record;
begin
    for c in select conname from pg_constraint
             where conrelid = 'avaliacao.participacao'::regclass
               and contype = 'u' and cardinality(conkey) = 3
    loop
        execute format('alter table avaliacao.participacao drop constraint %I', c.conname);
    end loop;
end $$;
create unique index ux_participacao_contexto
    on avaliacao.participacao (
        instrumento_avaliativo_id,
        entidade_avaliada_id,
        entidade_respondente_id,
        coalesce(entidade_contexto_id, '00000000-0000-0000-0000-000000000000'::uuid)
    );

alter table avaliacao.resposta_instrumento
    add column entidade_contexto_id uuid references avaliacao.entidade(entidade_id);

