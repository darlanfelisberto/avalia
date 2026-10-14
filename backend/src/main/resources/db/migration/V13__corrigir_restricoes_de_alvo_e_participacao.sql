-- Remove a chave composta antiga que limitava o tipo de alvo ao tipo de
-- entidade principal do instrumento. A FK simples para o instrumento e a FK
-- composta para instrumento_tipo_alvo permanecem ativas.
do $$
declare c record;
begin
    for c in
        select conname
        from pg_constraint
        where conrelid = 'avaliacao.instrumento_entidade_avaliada'::regclass
          and confrelid = 'avaliacao.instrumento_avaliativo'::regclass
          and cardinality(conkey) > 1
    loop
        execute format(
            'alter table avaliacao.instrumento_entidade_avaliada drop constraint %I',
            c.conname
        );
    end loop;
end $$;

-- A unicidade agora inclui o contexto opcional e é garantida pelo índice
-- ux_participacao_contexto criado na V12.
do $$
declare c record;
begin
    for c in
        select conname
        from pg_constraint
        where conrelid = 'avaliacao.participacao'::regclass
          and contype = 'u'
          and cardinality(conkey) = 3
    loop
        execute format(
            'alter table avaliacao.participacao drop constraint %I',
            c.conname
        );
    end loop;
end $$;
