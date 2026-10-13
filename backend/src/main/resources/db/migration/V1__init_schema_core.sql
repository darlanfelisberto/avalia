-- =========================================================================
-- IFFar AVALIA - Núcleo Genérico de Avaliação Institucional (Core Engine)
-- Migração V1: Criação das tabelas centrais do modelo relacional e JSONB
-- =========================================================================

-- 1. Tabela de Instrumentos Avaliativos (RF01, RF04, RF05, RF08)
CREATE TABLE IF NOT EXISTS instrumento (
    id VARCHAR(64) PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    subtitulo VARCHAR(255),
    modulo VARCHAR(32) NOT NULL DEFAULT 'ADPD', -- ADPD, SINAES, APRENDIZAGEM
    ano_ciclo VARCHAR(32) NOT NULL,            -- ex: 2026/1
    periodo_letivo VARCHAR(64),
    data_inicio TIMESTAMP WITH TIME ZONE,
    data_termino TIMESTAMP WITH TIME ZONE NOT NULL,
    publico_alvo VARCHAR(255),
    anonimo BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ABERTO', -- RASCUNHO, ABERTO, ENCERRADO, CONSOLIDADO
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Grupos de Questões / Dimensões Temáticas (RF01)
CREATE TABLE IF NOT EXISTS grupo_questoes (
    id VARCHAR(64) PRIMARY KEY,
    instrumento_id VARCHAR(64) NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    secao VARCHAR(32) NOT NULL DEFAULT 'AVALIACAO_DOCENTE', -- AUTOAVALIACAO_DISCENTE, AVALIACAO_DOCENTE, GERAL
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    ordem INTEGER NOT NULL DEFAULT 0
);

-- 3. Tabela de Questões (RF02, RF03)
CREATE TABLE IF NOT EXISTS questao (
    id VARCHAR(64) PRIMARY KEY,
    grupo_id VARCHAR(64) NOT NULL REFERENCES grupo_questoes(id) ON DELETE CASCADE,
    enunciado TEXT NOT NULL,
    tipo VARCHAR(32) NOT NULL DEFAULT 'ESCALA', -- ESCALA, ESCOLHA_UNICA, MULTIPLA_ESCOLHA, TEXTO_LIVRE
    obrigatoria BOOLEAN NOT NULL DEFAULT TRUE,
    ordem INTEGER NOT NULL DEFAULT 0,
    escala_min INTEGER DEFAULT 1,
    escala_max INTEGER DEFAULT 5,
    rotulo_min VARCHAR(100) DEFAULT '1 - Insatisfatório',
    rotulo_max VARCHAR(100) DEFAULT '5 - Excelente',
    opcoes_json JSONB,                          -- Array de objetos: [{"id":"opt-1", "texto":"Opção..."}]
    ajuda VARCHAR(255)
);

-- 4. Tabela de Componentes Curriculares Avaliados (ADPD / SIGAA)
CREATE TABLE IF NOT EXISTS componente_curricular (
    id VARCHAR(64) PRIMARY KEY,
    instrumento_id VARCHAR(64) NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    codigo VARCHAR(64) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    docente_nome VARCHAR(255) NOT NULL,
    turma VARCHAR(64)
);

-- 5. Tabela de Encaminhamentos e Fechamento do Ciclo (RF14 - Cipriano Luckesi)
CREATE TABLE IF NOT EXISTS encaminhamento_decisao (
    id VARCHAR(64) PRIMARY KEY,
    instrumento_id VARCHAR(64) NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    responsavel_nome VARCHAR(255) NOT NULL,
    papel VARCHAR(64) NOT NULL,                  -- COORDENADOR, DIRECAO_ENSINO, CPA
    tipo_decisao VARCHAR(64) NOT NULL,           -- PLANO_DE_ACAO, ORIENTACAO_PEDAGOGICA, SEM_ACAO_NECESSARIA, ENCAMINHAMENTO_CPPD
    detalhamento TEXT NOT NULL,
    prazo_revisao DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Tabela de Manifestações e Planos de Desenvolvimento Docente (RF-AD07 - Jussara Hoffmann)
CREATE TABLE IF NOT EXISTS manifestacao_docente (
    id VARCHAR(64) PRIMARY KEY,
    instrumento_id VARCHAR(64) NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    componente_id VARCHAR(64) NOT NULL REFERENCES componente_curricular(id) ON DELETE CASCADE,
    docente_nome VARCHAR(255) NOT NULL,
    comentario TEXT,
    plano_desenvolvimento TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices de consulta rápida
CREATE INDEX IF NOT EXISTS idx_grupo_instrumento ON grupo_questoes(instrumento_id);
CREATE INDEX IF NOT EXISTS idx_questao_grupo ON questao(grupo_id);
CREATE INDEX IF NOT EXISTS idx_componente_instrumento ON componente_curricular(instrumento_id);
CREATE INDEX IF NOT EXISTS idx_encaminhamento_instrumento ON encaminhamento_decisao(instrumento_id);
CREATE INDEX IF NOT EXISTS idx_manifestacao_instrumento ON manifestacao_docente(instrumento_id);
