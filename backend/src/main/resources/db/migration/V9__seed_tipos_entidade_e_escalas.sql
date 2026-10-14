-- Migração V9: seed do schema "avaliacao" (tipos de entidade e conjuntos de opções padrão).
-- Executa após a criação do schema na migração V8.
DO $$
BEGIN
    IF to_regclass('avaliacao.tipo_entidade') IS NOT NULL THEN
        INSERT INTO avaliacao.tipo_entidade (codigo, nome, pode_ser_avaliada, pode_responder) VALUES
            ('INSTITUICAO', 'Instituição', TRUE, FALSE),
            ('CAMPUS',      'Campus',      TRUE, FALSE),
            ('SETOR',       'Setor',       TRUE, FALSE),
            ('CURSO',       'Curso',       TRUE, FALSE),
            ('TURMA',       'Turma',       TRUE, FALSE),
            ('DOCENTE',     'Docente',     TRUE, TRUE),
            ('SERVIDOR',    'Servidor / TAE', TRUE, TRUE),
            ('ALUNO',       'Aluno / Discente', FALSE, TRUE),
            ('EGRESSO',     'Egresso',     FALSE, TRUE),
            ('COMUNIDADE',  'Comunidade externa', FALSE, TRUE)
        ON CONFLICT (codigo) DO NOTHING;
    END IF;

    IF to_regclass('avaliacao.conjunto_opcao') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM avaliacao.conjunto_opcao WHERE nome = 'Escala de concordância (5 pontos)') THEN
        WITH c AS (
            INSERT INTO avaliacao.conjunto_opcao (nome) VALUES ('Escala de concordância (5 pontos)') RETURNING conjunto_opcao_id
        )
        INSERT INTO avaliacao.opcao_questao (conjunto_opcao_id, texto, valor, ordem)
        SELECT c.conjunto_opcao_id, o.texto, o.valor, o.ordem FROM c, (VALUES
            ('Discordo totalmente', 1, 1), ('Discordo', 2, 2), ('Neutro', 3, 3),
            ('Concordo', 4, 4), ('Concordo totalmente', 5, 5)) AS o(texto, valor, ordem);
    END IF;

    IF to_regclass('avaliacao.conjunto_opcao') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM avaliacao.conjunto_opcao WHERE nome = 'Sim / Não') THEN
        WITH c AS (
            INSERT INTO avaliacao.conjunto_opcao (nome) VALUES ('Sim / Não') RETURNING conjunto_opcao_id
        )
        INSERT INTO avaliacao.opcao_questao (conjunto_opcao_id, texto, valor, ordem)
        SELECT c.conjunto_opcao_id, o.texto, o.valor, o.ordem FROM c, (VALUES
            ('Sim', 1, 1), ('Não', 0, 2)) AS o(texto, valor, ordem);
    END IF;
END $$;
