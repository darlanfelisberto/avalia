-- =====================================================================
-- Schema: avaliacao  (PostgreSQL 13+; gen_random_uuid() nativo)
-- Convenções: PK = <tabela>_id (uuid); FK com o mesmo nome da PK referenciada.
--   Quando a mesma tabela é referenciada com papéis diferentes, o nome leva o
--   papel: *_avaliada_id, *_respondente_id, *_origem_id, *_destino_id, *_pai_id.
-- Núcleo genérico reutilizado pelos módulos AUTOAVALIACAO, ADPD, APRENDIZAGEM.
-- =====================================================================
create schema if not exists avaliacao;
create extension if not exists pg_trgm;   -- busca por texto em entidade.rotulo (requer privilégio)

-- ---------------------------------------------------------------------
-- 1. Registro genérico de entidades (avaliadas e/ou respondentes)
--    As tabelas de domínio (aluno, turma, instituição, setor...) NÃO mudam:
--    'entidade' é um espelho leve delas, com rótulo e atributos para busca/relatório.
-- ---------------------------------------------------------------------
create table avaliacao.tipo_entidade (
    tipo_entidade_id uuid primary key default gen_random_uuid(),
    codigo           varchar(30)  not null unique,        -- 'ALUNO','TURMA','INSTITUICAO','SETOR'
    nome             varchar(100) not null,
    pode_ser_avaliada boolean     not null default true,
    pode_responder    boolean     not null default true
);

create table avaliacao.entidade (
    entidade_id        uuid primary key default gen_random_uuid(),
    tipo_entidade_id   uuid         not null references avaliacao.tipo_entidade(tipo_entidade_id),
    referencia_externa varchar(60)  not null,             -- id na tabela de domínio/SIGAA (qualquer formato)
    rotulo             varchar(300) not null,             -- texto para exibição e busca
    atributos          jsonb        not null default '{}'::jsonb, -- dimensões p/ filtro/relatório (campus, curso, ano...)
    ativo              boolean      not null default true,
    sincronizado_em    timestamptz  not null default now(),
    unique (tipo_entidade_id, referencia_externa),
    unique (tipo_entidade_id, entidade_id)                -- permite FK composta que garante o tipo
);

-- Vínculos entre entidades (aluno -> turma, docente -> turma, turma -> curso...)
-- Convenção: origem "tem vínculo com" destino.
create table avaliacao.entidade_vinculo (
    entidade_vinculo_id uuid primary key default gen_random_uuid(),
    entidade_origem_id  uuid        not null references avaliacao.entidade(entidade_id) on delete cascade,
    entidade_destino_id uuid        not null references avaliacao.entidade(entidade_id) on delete cascade,
    tipo_vinculo        varchar(30) not null,             -- 'MATRICULADO_EM','LECIONA','PERTENCE_A'
    check (entidade_origem_id <> entidade_destino_id),
    unique (entidade_origem_id, entidade_destino_id, tipo_vinculo)
);

-- ---------------------------------------------------------------------
-- 2. Instrumento (uma aplicação/ciclo: autoavaliação 2025, ADPD 2026/1...)
-- ---------------------------------------------------------------------
create table avaliacao.instrumento_avaliativo (
    instrumento_avaliativo_id        uuid primary key default gen_random_uuid(),
    instrumento_avaliativo_origem_id uuid references avaliacao.instrumento_avaliativo(instrumento_avaliativo_id),
    tipo_entidade_avaliada_id uuid        not null
                              references avaliacao.tipo_entidade(tipo_entidade_id), -- o que este formulário avalia
    modulo                varchar(20)  not null
                          check (modulo in ('AUTOAVALIACAO','ADPD','APRENDIZAGEM')),
    titulo                varchar(200) not null,
    descricao             text,
    ciclo_referencia      varchar(20)  not null,           -- '2025', '2026/1'
    inicio_aplicacao      timestamptz  not null,           -- RF04
    fim_aplicacao         timestamptz  not null,
    anonimo               boolean      not null default false,  -- RF08 / RNF03
    permite_edicao        boolean      not null default true,   -- RF07
    status                varchar(12)  not null default 'RASCUNHO'
                          check (status in ('RASCUNHO','PUBLICADO','ENCERRADO','CONSOLIDADO')),
    consolidado_em        timestamptz,
    criado_por            varchar(60)  not null,
    criado_em             timestamptz  not null default now(),
    atualizado_em         timestamptz  not null default now(),
    check (fim_aplicacao > inicio_aplicacao),
    check ((status = 'CONSOLIDADO') = (consolidado_em is not null)),
    unique (modulo, ciclo_referencia, titulo),
    unique (instrumento_avaliativo_id, tipo_entidade_avaliada_id)   -- p/ FK composta abaixo
);

-- Quais entidades serão avaliadas neste ciclo (ex.: as 140 turmas do semestre).
-- atributos_snapshot congela as dimensões na publicação: relatório de ciclo
-- fechado não muda se o cadastro de origem mudar depois.
create table avaliacao.instrumento_entidade_avaliada (
    instrumento_entidade_avaliada_id uuid primary key default gen_random_uuid(),
    instrumento_avaliativo_id        uuid  not null,
    tipo_entidade_avaliada_id        uuid  not null,
    entidade_id                      uuid  not null,
    atributos_snapshot               jsonb not null default '{}'::jsonb,
    unique (instrumento_avaliativo_id, entidade_id),
    foreign key (instrumento_avaliativo_id, tipo_entidade_avaliada_id)   -- tipo igual ao do instrumento
        references avaliacao.instrumento_avaliativo (instrumento_avaliativo_id, tipo_entidade_avaliada_id)
        on delete cascade,
    foreign key (tipo_entidade_avaliada_id, entidade_id)                 -- entidade do tipo certo
        references avaliacao.entidade (tipo_entidade_id, entidade_id)
);

-- Quem responde: um tipo de entidade e, opcionalmente, o vínculo exigido com a
-- entidade avaliada (respondente = origem; avaliada = destino).
--   ADPD:   tipo ALUNO,    vinculo 'MATRICULADO_EM'  (aluno -> turma avaliada)
--   Autoav: tipo SERVIDOR, vinculo NULL              (qualquer servidor)
create table avaliacao.instrumento_respondente (
    instrumento_respondente_id     uuid primary key default gen_random_uuid(),
    instrumento_avaliativo_id      uuid not null
                                   references avaliacao.instrumento_avaliativo(instrumento_avaliativo_id)
                                   on delete cascade,
    tipo_entidade_respondente_id   uuid not null references avaliacao.tipo_entidade(tipo_entidade_id),
    tipo_vinculo                   varchar(30),
    unique (instrumento_avaliativo_id, tipo_entidade_respondente_id)
);

-- ---------------------------------------------------------------------
-- 3. Estrutura do instrumento: página > sessão (auto-relacionamento)
--    PAGINA  = unidade de renderização (nunca tem pai)
--    SESSAO  = agrupador de questões/respostas dentro de uma página
-- ---------------------------------------------------------------------
create table avaliacao.instrumento_sessao (
    instrumento_sessao_id     uuid primary key default gen_random_uuid(),
    instrumento_avaliativo_id uuid         not null
                              references avaliacao.instrumento_avaliativo(instrumento_avaliativo_id)
                              on delete cascade,
    instrumento_sessao_pai_id uuid,
    tipo                      varchar(6)   not null check (tipo in ('PAGINA','SESSAO')),
    titulo                    varchar(200) not null,
    descricao                 text,
    ordem                     smallint     not null,
    unique (instrumento_avaliativo_id, instrumento_sessao_id),
    foreign key (instrumento_avaliativo_id, instrumento_sessao_pai_id)   -- pai do mesmo instrumento
        references avaliacao.instrumento_sessao (instrumento_avaliativo_id, instrumento_sessao_id)
        on delete cascade,
    check ((tipo = 'PAGINA') = (instrumento_sessao_pai_id is null))
);

-- ---------------------------------------------------------------------
-- 4. Conjuntos de opções reutilizáveis (ex.: escala Likert de 5 pontos)
-- ---------------------------------------------------------------------
create table avaliacao.conjunto_opcao (
    conjunto_opcao_id uuid primary key default gen_random_uuid(),
    nome              varchar(100) not null unique
);

create table avaliacao.opcao_questao (
    opcao_questao_id  uuid primary key default gen_random_uuid(),
    conjunto_opcao_id uuid         not null
                      references avaliacao.conjunto_opcao(conjunto_opcao_id) on delete cascade,
    texto             varchar(300) not null,
    valor             numeric(6,2),                        -- peso numérico p/ relatórios (RF09)
    correta           boolean      not null default false, -- módulo APRENDIZAGEM
    ordem             smallint     not null,
    unique (conjunto_opcao_id, ordem)
);

-- ---------------------------------------------------------------------
-- 5. Questão (catálogo; pode ser reutilizada em vários instrumentos)
-- ---------------------------------------------------------------------
create table avaliacao.questao (
    questao_id        uuid primary key default gen_random_uuid(),
    conjunto_opcao_id uuid references avaliacao.conjunto_opcao(conjunto_opcao_id),
    enunciado         text        not null,
    tipo              varchar(16) not null
                      check (tipo in ('ESCOLHA_UNICA','MULTIPLA_ESCOLHA','ESCALA','DESCRITIVA')),
    assunto           varchar(100),                        -- RF01
    criado_por        varchar(60) not null,
    criado_em         timestamptz not null default now(),
    check ((tipo = 'DESCRITIVA') = (conjunto_opcao_id is null))
);

-- Posicionamento da questão no instrumento
create table avaliacao.sessao_questao (
    sessao_questao_id     uuid primary key default gen_random_uuid(),
    instrumento_sessao_id uuid     not null
                          references avaliacao.instrumento_sessao(instrumento_sessao_id) on delete cascade,
    questao_id            uuid     not null references avaliacao.questao(questao_id),
    ordem                 smallint not null,
    obrigatoria           boolean  not null default false, -- RF03
    peso                  numeric(6,2),                    -- módulo APRENDIZAGEM
    unique (instrumento_sessao_id, questao_id)
);

-- ---------------------------------------------------------------------
-- 6. Participação: quem já respondeu sobre quem (SEM vínculo com a resposta)
-- ---------------------------------------------------------------------
create table avaliacao.participacao (
    participacao_id           uuid primary key default gen_random_uuid(),
    instrumento_avaliativo_id uuid        not null,
    entidade_avaliada_id      uuid        not null,
    entidade_respondente_id   uuid        not null references avaliacao.entidade(entidade_id),
    registrada_em             timestamptz not null default now(),
    unique (instrumento_avaliativo_id, entidade_avaliada_id, entidade_respondente_id),
    foreign key (instrumento_avaliativo_id, entidade_avaliada_id)   -- só entidades declaradas no ciclo
        references avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, entidade_id)
);

-- ---------------------------------------------------------------------
-- 7. Respostas
-- ---------------------------------------------------------------------
create table avaliacao.resposta_instrumento (
    resposta_instrumento_id   uuid primary key default gen_random_uuid(),
    instrumento_avaliativo_id uuid        not null,
    entidade_avaliada_id      uuid        not null,        -- turma/setor/instituição... avaliada
    entidade_respondente_id   uuid references avaliacao.entidade(entidade_id), -- nulo ao enviar, se anônimo
    status                    varchar(12) not null default 'EM_ANDAMENTO'   -- RF06
                              check (status in ('EM_ANDAMENTO','ENVIADA')),
    data_envio                date,                        -- só a data, p/ dificultar correlação
    check ((status = 'ENVIADA') = (data_envio is not null)),
    foreign key (instrumento_avaliativo_id, entidade_avaliada_id)
        references avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, entidade_id)
);

create table avaliacao.resposta_questao (
    resposta_questao_id     uuid primary key default gen_random_uuid(),
    resposta_instrumento_id uuid not null
                            references avaliacao.resposta_instrumento(resposta_instrumento_id)
                            on delete cascade,
    sessao_questao_id       uuid not null references avaliacao.sessao_questao(sessao_questao_id),
    opcao_questao_id        uuid references avaliacao.opcao_questao(opcao_questao_id),
    texto                   text,
    check (num_nonnulls(opcao_questao_id, texto) = 1)
);
-- múltipla escolha: uma linha por opção marcada, sem repetir
create unique index ux_resposta_questao_opcao
    on avaliacao.resposta_questao (resposta_instrumento_id, sessao_questao_id, opcao_questao_id)
    where opcao_questao_id is not null;
-- descritiva: uma linha por questão
create unique index ux_resposta_questao_texto
    on avaliacao.resposta_questao (resposta_instrumento_id, sessao_questao_id)
    where opcao_questao_id is null;

-- ---------------------------------------------------------------------
-- 8. Encaminhamento (RF14): nenhum ciclo é consolidado sem registro
--    entidade_avaliada_id nulo = encaminhamento do ciclo inteiro
-- ---------------------------------------------------------------------
create table avaliacao.encaminhamento (
    encaminhamento_id         uuid primary key default gen_random_uuid(),
    instrumento_avaliativo_id uuid         not null,
    entidade_avaliada_id      uuid,
    tipo                      varchar(22)  not null
                              check (tipo in ('DECISAO','PLANO_ACAO','JUSTIFICATIVA_NAO_ACAO')),
    descricao                 text         not null,
    responsavel_id            uuid         not null references avaliacao.entidade(entidade_id),
    prazo                     date,
    registrado_em             timestamptz  not null default now(),
    foreign key (instrumento_avaliativo_id)
        references avaliacao.instrumento_avaliativo (instrumento_avaliativo_id),
    foreign key (instrumento_avaliativo_id, entidade_avaliada_id)
        references avaliacao.instrumento_entidade_avaliada (instrumento_avaliativo_id, entidade_id)
);

-- ---------------------------------------------------------------------
-- 9. Regras no banco
-- ---------------------------------------------------------------------
-- RNF03: anônimo => respondente nulo ao enviar; identificado => obrigatório
create function avaliacao.fn_garante_anonimato() returns trigger
language plpgsql as $$
declare v_anonimo boolean;
begin
    if new.status = 'ENVIADA' then
        select anonimo into v_anonimo
          from avaliacao.instrumento_avaliativo
         where instrumento_avaliativo_id = new.instrumento_avaliativo_id;
        if v_anonimo and new.entidade_respondente_id is not null then
            raise exception 'Instrumento anônimo: entidade_respondente_id deve ser nulo ao enviar';
        elsif not v_anonimo and new.entidade_respondente_id is null then
            raise exception 'Instrumento identificado: entidade_respondente_id é obrigatório';
        end if;
    end if;
    return new;
end $$;

create trigger trg_anonimato
    before insert or update on avaliacao.resposta_instrumento
    for each row execute function avaliacao.fn_garante_anonimato();

-- RF14: só consolida com ao menos um encaminhamento
create function avaliacao.fn_exige_encaminhamento() returns trigger
language plpgsql as $$
begin
    if new.status = 'CONSOLIDADO' and old.status is distinct from 'CONSOLIDADO'
       and not exists (select 1 from avaliacao.encaminhamento e
                        where e.instrumento_avaliativo_id = new.instrumento_avaliativo_id) then
        raise exception 'RF14: registre ao menos um encaminhamento antes de consolidar';
    end if;
    return new;
end $$;

create trigger trg_rf14
    before update of status on avaliacao.instrumento_avaliativo
    for each row execute function avaliacao.fn_exige_encaminhamento();

-- ---------------------------------------------------------------------
-- 10. Visão base para relatórios (só respostas enviadas, questões de opção)
--     Médias ponderadas: some soma_valores / total_respostas ao agrupar.
-- ---------------------------------------------------------------------
create view avaliacao.vw_resultado_questao as
select ri.instrumento_avaliativo_id,
       ri.entidade_avaliada_id,
       rq.sessao_questao_id,
       count(*)      as total_respostas,
       sum(oq.valor) as soma_valores,
       avg(oq.valor) as media
  from avaliacao.resposta_questao rq
  join avaliacao.resposta_instrumento ri
    on ri.resposta_instrumento_id = rq.resposta_instrumento_id
   and ri.status = 'ENVIADA'
  join avaliacao.opcao_questao oq
    on oq.opcao_questao_id = rq.opcao_questao_id
 group by ri.instrumento_avaliativo_id, ri.entidade_avaliada_id, rq.sessao_questao_id;

-- ---------------------------------------------------------------------
-- 11. Consulta de elegibilidade (referência): quem pode responder sobre a entidade :avaliada?
-- ---------------------------------------------------------------------
-- select v.entidade_origem_id
--   from avaliacao.instrumento_respondente ir
--   join avaliacao.entidade e
--     on e.tipo_entidade_id = ir.tipo_entidade_respondente_id
--   join avaliacao.entidade_vinculo v
--     on v.entidade_origem_id = e.entidade_id
--    and v.entidade_destino_id = :avaliada
--    and v.tipo_vinculo = ir.tipo_vinculo
--  where ir.instrumento_avaliativo_id = :instrumento;

-- ---------------------------------------------------------------------
-- 12. Índices de apoio
-- ---------------------------------------------------------------------
create index ix_entidade_rotulo_trgm
    on avaliacao.entidade using gin (rotulo gin_trgm_ops);
create index ix_entidade_atributos
    on avaliacao.entidade using gin (atributos);
create index ix_entidade_vinculo_destino
    on avaliacao.entidade_vinculo (entidade_destino_id, tipo_vinculo);
create index ix_instrumento_entidade_avaliada_snapshot
    on avaliacao.instrumento_entidade_avaliada using gin (atributos_snapshot);
create index ix_instrumento_sessao_instrumento
    on avaliacao.instrumento_sessao (instrumento_avaliativo_id, instrumento_sessao_pai_id, ordem);
create index ix_sessao_questao_questao
    on avaliacao.sessao_questao (questao_id);
create index ix_questao_conjunto
    on avaliacao.questao (conjunto_opcao_id);
create index ix_resposta_instrumento_avaliada
    on avaliacao.resposta_instrumento (instrumento_avaliativo_id, entidade_avaliada_id);
create index ix_resposta_questao_sessao_questao
    on avaliacao.resposta_questao (sessao_questao_id);
create index ix_encaminhamento_instrumento
    on avaliacao.encaminhamento (instrumento_avaliativo_id, entidade_avaliada_id);