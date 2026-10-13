-- =========================================================================
-- IFFar AVALIA - Núcleo Genérico de Avaliação Institucional (Core Engine)
-- Migração V1: Criação das tabelas centrais do modelo relacional com UUID nativo
-- =========================================================================

-- 1. Tabela de Catálogo de Alvos Avaliativos (Instituição, Campi, Setores, Docentes, Cursos, Turmas)
CREATE TABLE IF NOT EXISTS alvo_avaliado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(32) NOT NULL,            -- INSTITUICAO, CAMPUS, SETOR, DOCENTE, CURSO, TURMA
    nome VARCHAR(255) NOT NULL,
    codigo VARCHAR(64),
    descricao TEXT,
    responsavel_nome VARCHAR(255),
    campus VARCHAR(100) DEFAULT 'Reitoria',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Catálogo de Públicos Avaliadores (Segmentos Acadêmicos e Comunitários)
CREATE TABLE IF NOT EXISTS publico_avaliador (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segmento VARCHAR(32) NOT NULL,        -- DISCENTE, DOCENTE, TAE, EGRESSO, SOCIEDADE_CIVIL, TODOS
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    exige_autenticacao BOOLEAN NOT NULL DEFAULT TRUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de Instrumentos Avaliativos
CREATE TABLE IF NOT EXISTS instrumento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(255) NOT NULL,
    subtitulo VARCHAR(255),
    modulo VARCHAR(32) NOT NULL DEFAULT 'ADPD', -- ADPD, SINAES, APRENDIZAGEM
    ano_ciclo VARCHAR(32) NOT NULL,            -- ex: 2026/1
    periodo_letivo VARCHAR(64),
    data_inicio TIMESTAMP WITH TIME ZONE,
    data_termino TIMESTAMP WITH TIME ZONE NOT NULL,
    publico_alvo VARCHAR(255),
    tipo_alvo VARCHAR(32) NOT NULL DEFAULT 'DOCENTE',
    tipo_publico VARCHAR(32) NOT NULL DEFAULT 'DISCENTES',
    publicos_permitidos VARCHAR(255),
    restringir_vinculo_campus BOOLEAN NOT NULL DEFAULT TRUE,
    alvo_especifico_id UUID REFERENCES alvo_avaliado(id) ON DELETE SET NULL,
    alvos_especificos_ids TEXT,
    criador_id VARCHAR(64) NOT NULL DEFAULT 'usr-cpa-cain',
    criador_nome VARCHAR(255) NOT NULL DEFAULT 'Comissão Própria de Avaliação (CPA)',
    anonimo BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ABERTO', -- RASCUNHO, ABERTO, ENCERRADO, CONSOLIDADO
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela de Grupos de Questões / Dimensões Temáticas
CREATE TABLE IF NOT EXISTS grupo_questoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrumento_id UUID NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    secao VARCHAR(32) NOT NULL DEFAULT 'AVALIACAO_DOCENTE', -- AUTOAVALIACAO_DISCENTE, AVALIACAO_DOCENTE, GERAL
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    ordem INTEGER NOT NULL DEFAULT 0
);

-- 5. Tabela de Questões
CREATE TABLE IF NOT EXISTS questao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grupo_id UUID NOT NULL REFERENCES grupo_questoes(id) ON DELETE CASCADE,
    enunciado TEXT NOT NULL,
    tipo VARCHAR(32) NOT NULL DEFAULT 'ESCALA', -- ESCALA, ESCOLHA_UNICA, MULTIPLA_ESCOLHA, TEXTO_LIVRE
    obrigatoria BOOLEAN NOT NULL DEFAULT TRUE,
    ordem INTEGER NOT NULL DEFAULT 0,
    escala_min INTEGER DEFAULT 1,
    escala_max INTEGER DEFAULT 5,
    rotulo_min VARCHAR(100) DEFAULT '1 - Insatisfatório',
    rotulo_max VARCHAR(100) DEFAULT '5 - Excelente',
    opcoes_json TEXT,
    ajuda VARCHAR(255)
);

-- 6. Tabela de Opções de Questões (normalizada)
CREATE TABLE IF NOT EXISTS opcao_questao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    questao_id UUID NOT NULL REFERENCES questao(id) ON DELETE CASCADE,
    texto VARCHAR(500) NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Tabela de Componentes Curriculares Avaliados (ADPD / SIGAA)
CREATE TABLE IF NOT EXISTS componente_curricular (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrumento_id UUID NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    codigo VARCHAR(64) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    docente_nome VARCHAR(255) NOT NULL,
    turma VARCHAR(64)
);

-- 8. Tabela de Encaminhamentos e Fechamento do Ciclo (RF14)
CREATE TABLE IF NOT EXISTS encaminhamento_decisao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrumento_id UUID NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    responsavel_nome VARCHAR(255) NOT NULL,
    papel VARCHAR(64) NOT NULL,                  -- COORDENADOR, DIRECAO_ENSINO, CPA
    tipo_decisao VARCHAR(64) NOT NULL,           -- PLANO_DE_ACAO, ORIENTACAO_PEDAGOGICA, SEM_ACAO_NECESSARIA, ENCAMINHAMENTO_CPPD
    detalhamento TEXT NOT NULL,
    prazo_revisao DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Tabela de Manifestações e Planos de Desenvolvimento Docente (RF-AD07)
CREATE TABLE IF NOT EXISTS manifestacao_docente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrumento_id UUID NOT NULL REFERENCES instrumento(id) ON DELETE CASCADE,
    componente_id UUID NOT NULL REFERENCES componente_curricular(id) ON DELETE CASCADE,
    docente_nome VARCHAR(255) NOT NULL,
    comentario TEXT,
    plano_desenvolvimento TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices de consulta rápida
CREATE INDEX IF NOT EXISTS idx_instrumento_tipo_alvo ON instrumento(tipo_alvo);
CREATE INDEX IF NOT EXISTS idx_instrumento_tipo_publico ON instrumento(tipo_publico);
CREATE INDEX IF NOT EXISTS idx_instrumento_criador_id ON instrumento(criador_id);
CREATE INDEX IF NOT EXISTS idx_alvo_tipo ON alvo_avaliado(tipo);
CREATE INDEX IF NOT EXISTS idx_publico_segmento ON publico_avaliador(segmento);
CREATE INDEX IF NOT EXISTS idx_grupo_instrumento ON grupo_questoes(instrumento_id);
CREATE INDEX IF NOT EXISTS idx_questao_grupo ON questao(grupo_id);
CREATE INDEX IF NOT EXISTS idx_opcao_questao_questao_id ON opcao_questao(questao_id);
CREATE INDEX IF NOT EXISTS idx_componente_instrumento ON componente_curricular(instrumento_id);
CREATE INDEX IF NOT EXISTS idx_encaminhamento_instrumento ON encaminhamento_decisao(instrumento_id);
CREATE INDEX IF NOT EXISTS idx_manifestacao_instrumento ON manifestacao_docente(instrumento_id);
