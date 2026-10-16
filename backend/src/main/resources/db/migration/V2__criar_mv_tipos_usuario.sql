CREATE MATERIALIZED VIEW avaliacao.mv_pessoa_tipos_entidade
TABLESPACE pg_default AS
WITH alunos_ativos AS (
    SELECT p.cpf_cnpj, a.id_pessoa,
           CASE
               WHEN situacao.id IN (1, 5, 14) THEN 'ALUNO'
               WHEN situacao.id IN (3, 8, 9) THEN 'EGRESSO'
               ELSE 'OUTRO'
           END AS tipo
    FROM academico.aluno a
    JOIN academico.pessoa p ON p.id_pessoa = a.id_pessoa
    JOIN academico.status_discente situacao ON situacao.id = a.id_situacao
    WHERE situacao.id IN (1, 5, 14, 3, 8, 9)
), servidores_ativos AS (
    SELECT p.cpf_cnpj, s.id_pessoa,
           CASE
               WHEN c.id IN (60004, 707001, 705001, 480192, 60012, 60011, 702005, 702003, 707002, 702001)
                   THEN 'DOCENTE'
               ELSE 'SERVIDOR'
           END AS tipo
    FROM academico.servidor s
    JOIN academico.pessoa p ON p.id_pessoa = s.id_pessoa
    JOIN academico.situacao_servidor situacao ON situacao.id_situacao = s.id_situacao
    LEFT JOIN academico.cargo c ON c.id = s.id_cargo
    WHERE situacao.id_situacao IN (1, 8)
), tipos_usuario AS (
    SELECT cpf_cnpj, id_pessoa, tipo FROM alunos_ativos
    UNION
    SELECT cpf_cnpj, id_pessoa, tipo FROM servidores_ativos
    UNION
    SELECT p.cpf_cnpj, p.id_pessoa, 'EXTERNO' AS tipo
    FROM academico.pessoa p
    LEFT JOIN alunos_ativos aa ON aa.id_pessoa = p.id_pessoa
    LEFT JOIN servidores_ativos sa ON sa.id_pessoa = p.id_pessoa
    WHERE aa.cpf_cnpj IS NULL AND sa.cpf_cnpj IS NULL
)
SELECT u.id_pessoa, u.cpf_cnpj, t.tipo_entidade_id, t.codigo
FROM tipos_usuario u
LEFT JOIN avaliacao.tipo_entidade t ON t.codigo = u.tipo
WITH DATA;

CREATE UNIQUE INDEX idx_mv_pessoa_tipos_entidade
    ON avaliacao.mv_pessoa_tipos_entidade USING btree (id_pessoa, tipo_entidade_id);
