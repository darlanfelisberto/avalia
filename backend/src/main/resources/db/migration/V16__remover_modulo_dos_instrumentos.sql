-- O tipo de entidade avaliada define o contexto do instrumento.
-- A remoção da coluna descarta também a antiga restrição única que incluía módulo.
alter table avaliacao.instrumento_avaliativo drop column modulo;

alter table avaliacao.instrumento_avaliativo
    add constraint uq_instrumento_tipo_ciclo_titulo
    unique (tipo_entidade_avaliada_id, ciclo_referencia, titulo);

alter table public.instrumento drop column modulo;
