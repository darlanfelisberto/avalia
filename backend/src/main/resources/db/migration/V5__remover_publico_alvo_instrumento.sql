-- Migração V5: Remoção do campo textual redundante publico_alvo da tabela instrumento
ALTER TABLE instrumento DROP COLUMN IF EXISTS publico_alvo;
