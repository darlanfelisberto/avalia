create table if not exists auth_sessao (
    token_hash varchar(64) primary key,
    cpf varchar(14) not null references usuario_permissao(cpf) on delete cascade,
    papeis varchar(200) not null,
    expira_em timestamptz not null
);
create index if not exists ix_auth_sessao_expira on auth_sessao(expira_em);

-- Instrumentos criados antes da tela de respostas precisam de um alvo do
-- próprio ciclo até que sejam associados a entidades institucionais reais.
insert into avaliacao.entidade (tipo_entidade_id, referencia_externa, rotulo)
select i.tipo_entidade_avaliada_id, 'instrumento:' || i.instrumento_avaliativo_id,
       i.titulo
from avaliacao.instrumento_avaliativo i
where i.status = 'PUBLICADO'
  and not exists (select 1 from avaliacao.instrumento_entidade_avaliada ia
                  where ia.instrumento_avaliativo_id = i.instrumento_avaliativo_id)
on conflict (tipo_entidade_id, referencia_externa) do nothing;

insert into avaliacao.instrumento_entidade_avaliada
    (instrumento_avaliativo_id, tipo_entidade_avaliada_id, entidade_id)
select i.instrumento_avaliativo_id, i.tipo_entidade_avaliada_id, e.entidade_id
from avaliacao.instrumento_avaliativo i
join avaliacao.entidade e on e.tipo_entidade_id = i.tipo_entidade_avaliada_id
 and e.referencia_externa = 'instrumento:' || i.instrumento_avaliativo_id
where i.status = 'PUBLICADO'
on conflict (instrumento_avaliativo_id, entidade_id) do nothing;
