alter table avaliacao.instrumento_respondente
    rename to instrumento_tipo_respondente;

alter table avaliacao.instrumento_tipo_respondente
    rename column instrumento_respondente_id to instrumento_tipo_respondente_id;

alter table avaliacao.instrumento_tipo_respondente
    rename constraint instrumento_respondente_pkey to instrumento_tipo_respondente_pkey;

alter table avaliacao.instrumento_tipo_respondente
    rename constraint instrumento_respondente_instrumento_avaliativo_id_tipo_enti_key
    to instrumento_tipo_respondente_instrumento_tipo_entidade_key;

alter table avaliacao.instrumento_tipo_respondente
    rename constraint instrumento_respondente_instrumento_avaliativo_id_fkey
    to instrumento_tipo_respondente_instrumento_avaliativo_id_fkey;

alter table avaliacao.instrumento_tipo_respondente
    rename constraint instrumento_respondente_tipo_entidade_respondente_id_fkey
    to instrumento_tipo_respondente_tipo_entidade_respondente_id_fkey;
