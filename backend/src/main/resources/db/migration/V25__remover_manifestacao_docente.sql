UPDATE usuario_permissao
SET permissoes = array_to_string(
    array_remove(string_to_array(permissoes, ','), 'DOCENTE_MANIFESTAR'),
    ','
)
WHERE 'DOCENTE_MANIFESTAR' = ANY(string_to_array(permissoes, ','));

DROP TABLE IF EXISTS manifestacao_docente;
