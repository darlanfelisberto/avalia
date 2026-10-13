-- =========================================================================
-- IFFar AVALIA - Migração V4: Vínculo de Questionários ao Usuário Criador
-- =========================================================================

-- Adiciona campos de autoria do questionário
ALTER TABLE instrumento ADD COLUMN IF NOT EXISTS criador_id VARCHAR(64) NOT NULL DEFAULT 'usr-cpa-cain';
ALTER TABLE instrumento ADD COLUMN IF NOT EXISTS criador_nome VARCHAR(255) NOT NULL DEFAULT 'Comissão Própria de Avaliação (CPA)';

-- Índice para buscas rápidas por autor
CREATE INDEX IF NOT EXISTS idx_instrumento_criador_id ON instrumento(criador_id);
