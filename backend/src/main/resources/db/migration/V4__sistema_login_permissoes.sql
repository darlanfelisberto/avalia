-- =========================================================================
-- IFFar AVALIA - Migração V4: Tabela de Usuários, Perfis e Permissões
-- =========================================================================

CREATE TABLE IF NOT EXISTS usuario_permissao (
    usuario_permissao_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf VARCHAR(14) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    papel VARCHAR(32) NOT NULL DEFAULT 'DISCENTE', -- CPA, GESTAO, DOCENTE, DISCENTE, TAE
    campus VARCHAR(100) DEFAULT 'Reitoria',
    email VARCHAR(200),
    permissoes TEXT, -- Permissões customizadas separadas por vírgula (ex: INSTRUMENTO_CRIAR,CATALOGO_GERENCIAR)
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuario_permissao_cpf ON usuario_permissao(cpf);
CREATE INDEX IF NOT EXISTS idx_usuario_permissao_papel ON usuario_permissao(papel);

-- Povoamento de contas padrão e de demonstração institucional
INSERT INTO usuario_permissao (usuario_permissao_id, cpf, nome, papel, campus, email, permissoes, ativo)
VALUES
-- 1. Conta CPA / Administrador Geral
(
    '00000000-0000-0000-0008-000000000001',
    '00000000000',
    'Comissão Própria de Avaliação (CPA / CAIN)',
    'CPA',
    'Reitoria',
    'cpa@iffar.edu.br',
    'INSTRUMENTO_CRIAR,INSTRUMENTO_EDITAR,INSTRUMENTO_EXCLUIR,INSTRUMENTO_VISUALIZAR,CATALOGO_GERENCIAR,AVALIACAO_RESPONDER,DOCENTE_MANIFESTAR,GESTAO_ENCAMINHAR,SIGAA_SINCRONIZAR,AUDITORIA_ACESSAR',
    TRUE
),
-- 2. Servidora Real da Coordenação de Avaliação Institucional (CAIN / CPA)
(
    '00000000-0000-0000-0008-000000000002',
    '48444251020',
    'Nádia Beatriz Casani Belinazo',
    'CPA',
    'Reitoria',
    'nadia.belinazo@iffar.edu.br',
    'INSTRUMENTO_CRIAR,INSTRUMENTO_EDITAR,INSTRUMENTO_EXCLUIR,INSTRUMENTO_VISUALIZAR,CATALOGO_GERENCIAR,AVALIACAO_RESPONDER,DOCENTE_MANIFESTAR,GESTAO_ENCAMINHAR,SIGAA_SINCRONIZAR,AUDITORIA_ACESSAR',
    TRUE
),
-- 3. Docente do Quadro (Campus Panambi)
(
    '00000000-0000-0000-0008-000000000003',
    '92782396072',
    'Rafaelle Ribeiro Gonçalves',
    'DOCENTE',
    'Campus Panambi',
    'rafaelle.goncalves@iffar.edu.br',
    'DOCENTE_MANIFESTAR,AVALIACAO_RESPONDER,INSTRUMENTO_VISUALIZAR',
    TRUE
),
-- 4. Discente Regular (Licenciatura / Cursos Superiores)
(
    '00000000-0000-0000-0008-000000000004',
    '14537997702',
    'Rafael Henrique Bispo dos Santos',
    'DISCENTE',
    'Campus Frederico Westphalen',
    'rafael.santos@aluno.iffar.edu.br',
    'AVALIACAO_RESPONDER',
    TRUE
),
-- 5. Gestão / Chefia Institucional (Reitoria)
(
    '00000000-0000-0000-0008-000000000005',
    '53549406053',
    'Helena Sebastiany Coelho',
    'GESTAO',
    'Reitoria',
    'helena.coelho@iffar.edu.br',
    'GESTAO_ENCAMINHAR,INSTRUMENTO_VISUALIZAR,AVALIACAO_RESPONDER',
    TRUE
)
ON CONFLICT (cpf) DO NOTHING;
