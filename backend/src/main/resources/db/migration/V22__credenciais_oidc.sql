-- Credenciais locais e códigos de autorização de uso único do provedor OIDC.
create table usuario_credencial (
    cpf varchar(14) primary key references usuario_permissao(cpf) on delete cascade,
    salt bytea not null,
    senha_hash bytea not null,
    iteracoes integer not null,
    alterada_em timestamptz not null default now()
);

create table oidc_codigo (
    codigo_hash varchar(64) primary key,
    cpf varchar(14) not null references usuario_permissao(cpf) on delete cascade,
    client_id varchar(100) not null,
    redirect_uri varchar(500) not null,
    code_challenge varchar(128) not null,
    nonce varchar(128) not null,
    expira_em timestamptz not null
);
create index ix_oidc_codigo_expira on oidc_codigo(expira_em);

update usuario_permissao
set permissoes = concat_ws(',', nullif(permissoes, ''), 'USUARIO_CREDENCIAL_GERENCIAR')
where papel in ('CPA', 'ADMIN')
  and coalesce(permissoes, '') not like '%USUARIO_CREDENCIAL_GERENCIAR%';
