UPDATE usuario_permissao
SET permissoes = array_to_string(
    array_remove(string_to_array(permissoes, ','), 'AUDITORIA_ACESSAR'),
    ','
)
WHERE 'AUDITORIA_ACESSAR' = ANY(string_to_array(permissoes, ','));
