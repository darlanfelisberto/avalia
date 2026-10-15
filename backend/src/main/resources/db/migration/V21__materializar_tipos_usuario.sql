CREATE MATERIALIZED VIEW avaliacao.mv_tipos_usuario AS
WITH alunos_ativos AS (
    SELECT p.cpf_cnpj,
           CASE
               WHEN situacao.id IN (1, 5, 14) THEN 'ALUNO'
               WHEN situacao.id IN (3, 8, 9) THEN 'EGRESSO'
               ELSE 'OUTRO'
           END AS tipo
    FROM academico.aluno a
    JOIN academico.pessoa p ON p.id = a.id_pessoa
    JOIN academico.status_discente situacao ON situacao.id = a.id_situacao
    WHERE situacao.id IN (1, 5, 14, 3, 8, 9)
), servidores_ativos AS (
    SELECT p.cpf_cnpj,
           CASE
               WHEN c.id IN (60004, 707001, 705001, 480192, 60012,
                             60011, 702005, 702003, 707002, 702001)
                   THEN 'DOCENTE'
               ELSE 'SERVIDOR'
           END AS tipo
    FROM academico.servidor s
    JOIN academico.pessoa p ON p.id = s.id_pessoa
    JOIN academico.situacao_servidor situacao ON situacao.id_situacao = s.id_situacao
    LEFT JOIN academico.cargo c ON c.id = s.id_cargo
    WHERE situacao.id_situacao IN (1, 8)
), tipos_usuario AS (
    SELECT cpf_cnpj, tipo FROM alunos_ativos
    UNION
    SELECT cpf_cnpj, tipo FROM servidores_ativos
    UNION
    SELECT p.cpf_cnpj, 'EXTERNO' AS tipo
    FROM academico.pessoa p
    WHERE NOT EXISTS (
        SELECT 1 FROM alunos_ativos a WHERE a.cpf_cnpj = p.cpf_cnpj
    ) AND NOT EXISTS (
        SELECT 1 FROM servidores_ativos s WHERE s.cpf_cnpj = p.cpf_cnpj
    )
)
SELECT u.cpf_cnpj, t.codigo, u.tipo
FROM tipos_usuario u
LEFT JOIN avaliacao.tipo_entidade t ON t.codigo = u.tipo
WHERE u.cpf_cnpj IS NOT NULL;

CREATE UNIQUE INDEX idx_mv_tipos_usuario_cpf_tipo
    ON avaliacao.mv_tipos_usuario (cpf_cnpj, tipo);
