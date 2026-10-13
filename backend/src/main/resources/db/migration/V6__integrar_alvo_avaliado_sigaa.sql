-- Migração V6: Alinhamento e integração do catálogo de alvos avaliados com o SIGAA
-- Adiciona id_origem_sigaa para rastreabilidade com as tabelas de unidade, servidor, curso e turma

ALTER TABLE alvo_avaliado ADD COLUMN IF NOT EXISTS id_origem_sigaa VARCHAR(64);

-- Índices para busca rápida no catálogo
CREATE INDEX IF NOT EXISTS idx_alvo_avaliado_origem ON alvo_avaliado(id_origem_sigaa);
CREATE INDEX IF NOT EXISTS idx_alvo_avaliado_campus ON alvo_avaliado(campus);
