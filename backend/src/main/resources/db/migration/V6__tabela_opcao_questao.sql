-- V6: Tabela relacional normalizada para opções de questões (escolha única / múltipla escolha)
CREATE TABLE IF NOT EXISTS opcao_questao (
    id VARCHAR(64) PRIMARY KEY,
    questao_id VARCHAR(64) NOT NULL REFERENCES questao(id) ON DELETE CASCADE,
    texto VARCHAR(500) NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_opcao_questao_questao_id ON opcao_questao(questao_id);

-- Migrar dados existentes de opcoes_json para a tabela opcao_questao
INSERT INTO opcao_questao (id, questao_id, texto, ordem)
SELECT 
    opt->>'id' AS id,
    q.id AS questao_id,
    opt->>'texto' AS texto,
    (row_number() OVER (PARTITION BY q.id ORDER BY (opt->>'id')))::int - 1 AS ordem
FROM questao q,
     jsonb_array_elements(q.opcoes_json::jsonb) AS opt
WHERE q.opcoes_json IS NOT NULL AND q.opcoes_json <> ''
ON CONFLICT (id) DO NOTHING;
