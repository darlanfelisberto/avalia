-- Página e sessão são tipos independentes da estrutura do instrumento.
-- Sessões antigas podem continuar referenciando sua página; novas sessões são
-- salvas sem pai. A FK composta continua protegendo referências legadas.
alter table avaliacao.instrumento_sessao
    drop constraint if exists instrumento_sessao_check;
