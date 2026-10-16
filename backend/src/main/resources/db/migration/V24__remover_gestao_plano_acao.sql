-- Os usuários do antigo perfil continuam aptos a responder avaliações como servidores.
UPDATE usuario_permissao
SET papel = 'TAE'
WHERE papel = 'GESTAO';

UPDATE usuario_permissao
SET permissoes = array_to_string(
    array_remove(string_to_array(permissoes, ','), 'GESTAO_ENCAMINHAR'),
    ','
)
WHERE 'GESTAO_ENCAMINHAR' = ANY(string_to_array(permissoes, ','));

-- Sessões emitidas com o perfil removido precisam de novo login.
DELETE FROM auth_sessao
WHERE 'GESTAO' = ANY(string_to_array(papeis, ','));

DROP TRIGGER IF EXISTS trg_rf14 ON avaliacao.instrumento_avaliativo;
DROP FUNCTION IF EXISTS avaliacao.fn_exige_encaminhamento();
DROP TABLE IF EXISTS avaliacao.encaminhamento;
DROP TABLE IF EXISTS encaminhamento_decisao;
