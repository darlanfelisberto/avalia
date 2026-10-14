-- =========================================================================
-- IFFar AVALIA - Migração V7: Tabela de Tipos de Público / Alvo Avaliado
-- =========================================================================

CREATE TABLE IF NOT EXISTS tipo_publico (
    tipo_publico_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(32) NOT NULL UNIQUE,
    singular VARCHAR(100) NOT NULL,
    plural VARCHAR(100) NOT NULL,
    icone VARCHAR(32) NOT NULL,
    descricao TEXT,
    ordem INT NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tipo_publico_codigo ON tipo_publico(codigo);
CREATE INDEX IF NOT EXISTS idx_tipo_publico_ordem ON tipo_publico(ordem);

-- Povoamento com os tipos definidos no rotuloPorTipo
INSERT INTO tipo_publico (tipo_publico_id, codigo, singular, plural, icone, descricao, ordem, ativo)
VALUES
('00000000-0000-0000-0009-000000000001', 'CAMPUS', 'Campus', 'Campi', '📍', 'Avaliação da infraestrutura, gestão e serviços da unidade.', 1, TRUE),
('00000000-0000-0000-0009-000000000002', 'DOCENTE', 'Docente', 'Docentes', '👨‍🏫', 'Avaliação da prática didático-pedagógica e relação dialógica.', 2, TRUE),
('00000000-0000-0000-0009-000000000003', 'SERVIDOR', 'Servidor / TAE', 'Servidores / TAEs', '💼', 'Avaliação de desempenho ou atuação de servidor técnico-administrativo.', 3, TRUE),
('00000000-0000-0000-0009-000000000004', 'TURMA', 'Turma', 'Turmas', '👥', 'Avaliação de turma e componente curricular específico.', 4, TRUE),
('00000000-0000-0000-0009-000000000005', 'CURSO', 'Curso', 'Cursos', '📚', 'Avaliação do projeto pedagógico e infraestrutura do curso.', 5, TRUE),
('00000000-0000-0000-0009-000000000006', 'SETOR', 'Setor', 'Setores', '🛠️', 'Avaliação de atendimento e eficiência de setor administrativo.', 6, TRUE),
('00000000-0000-0000-0009-000000000007', 'INSTITUICAO', 'Instituição', 'Instituições', '🏛️', 'Avaliação global institucional e PDI multicampi.', 7, TRUE),
('00000000-0000-0000-0009-000000000008', 'AUTOAVALIACAO', 'Estudante', 'Estudantes', '👤', 'Autoavaliação discente individual.', 8, TRUE)
ON CONFLICT (codigo) DO NOTHING;
