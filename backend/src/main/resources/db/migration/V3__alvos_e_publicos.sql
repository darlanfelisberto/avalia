-- =========================================================================
-- IFFar AVALIA - Migração V3: Suporte a Tipos de Público e Alvos Avaliados
-- =========================================================================

-- 1. Criação da Tabela de Catálogo de Alvos Avaliativos (Instituição, Setores, Docentes, Cursos, Turmas)
CREATE TABLE IF NOT EXISTS alvo_avaliado (
    id VARCHAR(64) PRIMARY KEY,
    tipo VARCHAR(32) NOT NULL,            -- INSTITUICAO, CAMPUS, SETOR, DOCENTE, CURSO, TURMA
    nome VARCHAR(255) NOT NULL,
    codigo VARCHAR(64),
    descricao TEXT,
    responsavel_nome VARCHAR(255),
    campus VARCHAR(100) DEFAULT 'Reitoria',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Criação da Tabela de Catálogo de Públicos Avaliadores (Segmentos Acadêmicos e Comunitários)
CREATE TABLE IF NOT EXISTS publico_avaliador (
    id VARCHAR(64) PRIMARY KEY,
    segmento VARCHAR(32) NOT NULL,        -- DISCENTE, DOCENTE, TAE, EGRESSO, SOCIEDADE_CIVIL, TODOS
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    exige_autenticacao BOOLEAN NOT NULL DEFAULT TRUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Evolução da Tabela Instrumento: Adição das colunas estruturadas tipo_alvo e tipo_publico
ALTER TABLE instrumento ADD COLUMN IF NOT EXISTS tipo_alvo VARCHAR(32) NOT NULL DEFAULT 'DOCENTE';
ALTER TABLE instrumento ADD COLUMN IF NOT EXISTS tipo_publico VARCHAR(32) NOT NULL DEFAULT 'DISCENTES';
ALTER TABLE instrumento ADD COLUMN IF NOT EXISTS alvo_especifico_id VARCHAR(64) REFERENCES alvo_avaliado(id) ON DELETE SET NULL;

-- 4. Povoamento Inicial de Alvos do IFFar (Docentes, Setores, Instituição, Cursos)
INSERT INTO alvo_avaliado (id, tipo, nome, codigo, descricao, campus, ativo) VALUES
('alvo-inst-geral', 'INSTITUICAO', 'Instituto Federal Farroupilha (Geral)', 'IFFAR-REI', 'Avaliação global do PDI, infraestrutura geral e gestão multicampi', 'Reitoria', TRUE),
('alvo-campus-sm', 'CAMPUS', 'Reitoria / Campus Santa Maria', 'IFFAR-SMA', 'Estrutura administrativa e acadêmica central', 'Santa Maria', TRUE),
('alvo-campus-ale', 'CAMPUS', 'Campus Alegrete', 'IFFAR-ALE', 'Unidade de ensino técnico e superior da fronteira oeste', 'Alegrete', TRUE),
('alvo-setor-ti', 'SETOR', 'Diretoria de Tecnologia da Informação (DTI)', 'SET-DTI', 'Sistemas acadêmicos, conectividade e suporte tecnológico', 'Reitoria', TRUE),
('alvo-setor-cain', 'SETOR', 'Coordenação de Avaliação Institucional (CAIN)', 'SET-CAIN', 'Gestão dos processos de autoavaliação institucional e SINAES', 'Reitoria', TRUE),
('alvo-setor-bib', 'SETOR', 'Biblioteca Central e Setoriais', 'SET-BIB', 'Acervo físico, digital e salas de estudos', 'Reitoria', TRUE),
('alvo-doc-roberto', 'DOCENTE', 'Prof. Dr. Roberto Alencar', 'DOC-001', 'Docente da área de Ciência da Computação / TADS', 'Alegrete', TRUE),
('alvo-doc-mariana', 'DOCENTE', 'Profa. Dra. Mariana Silveira', 'DOC-002', 'Docente da área de Banco de Dados e Informação', 'Alegrete', TRUE),
('alvo-doc-carlos', 'DOCENTE', 'Prof. Me. Carlos Eduardo Lima', 'DOC-003', 'Docente da área de Redes e Sistemas', 'Alegrete', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 5. Povoamento Inicial de Públicos Avaliadores
INSERT INTO publico_avaliador (id, segmento, nome, descricao, exige_autenticacao, ativo) VALUES
('pub-discentes', 'DISCENTE', 'Discentes Matriculados', 'Alunos com matrícula ativa em cursos FIC, Técnicos, Graduação e Pós-Graduação', TRUE, TRUE),
('pub-docentes', 'DOCENTE', 'Corpo Docente', 'Professores efetivos, substitutos e temporários do quadro do IFFar', TRUE, TRUE),
('pub-taes', 'TAE', 'Técnicos-Administrativos em Educação (TAEs)', 'Servidores técnicos de todos os campi e reitoria', TRUE, TRUE),
('pub-egressos', 'EGRESSO', 'Egressos e Ex-alunos', 'Formandos dos cursos do IFFar nos últimos anos', FALSE, TRUE),
('pub-sociedade', 'SOCIEDADE_CIVIL', 'Sociedade Civil e Comunidade Externa', 'Representantes da comunidade regional assistida pelo IFFar', FALSE, TRUE),
('pub-todos', 'TODOS', 'Toda a Comunidade Acadêmica', 'Discentes, Docentes, TAEs e Comunidade Externa integrados (SINAES)', FALSE, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 6. Atualização dos Instrumentos Existentes com o novo padrão semântico
UPDATE instrumento 
SET tipo_alvo = 'DOCENTE', tipo_publico = 'DISCENTES' 
WHERE id = 'inst-adpd-2026-1';

UPDATE instrumento 
SET tipo_alvo = 'INSTITUICAO', tipo_publico = 'TODOS' 
WHERE id = 'inst-18cc8404';

-- Índices adicionais
CREATE INDEX IF NOT EXISTS idx_instrumento_tipo_alvo ON instrumento(tipo_alvo);
CREATE INDEX IF NOT EXISTS idx_instrumento_tipo_publico ON instrumento(tipo_publico);
CREATE INDEX IF NOT EXISTS idx_alvo_tipo ON alvo_avaliado(tipo);
CREATE INDEX IF NOT EXISTS idx_publico_segmento ON publico_avaliador(segmento);
