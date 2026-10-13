-- =========================================================================
-- IFFar AVALIA - Migração V2: Seed dos dados iniciais (UUID determinístico)
-- =========================================================================

-- 1. Povoamento de Públicos Avaliadores
INSERT INTO publico_avaliador (publico_avaliador_id, segmento, nome, descricao, exige_autenticacao, ativo) VALUES
('00000000-0000-0000-0001-000000000001', 'DISCENTE', 'Discentes Matriculados', 'Alunos com matrícula ativa em cursos FIC, Técnicos, Graduação e Pós-Graduação', TRUE, TRUE),
('00000000-0000-0000-0001-000000000002', 'DOCENTE', 'Corpo Docente', 'Professores efetivos, substitutos e temporários do quadro do IFFar', TRUE, TRUE),
('00000000-0000-0000-0001-000000000003', 'TAE', 'Técnicos-Administrativos em Educação (TAEs)', 'Servidores técnicos de todos os campi e reitoria', TRUE, TRUE),
('00000000-0000-0000-0001-000000000004', 'EGRESSO', 'Egressos e Ex-alunos', 'Formandos dos cursos do IFFar nos últimos anos', FALSE, TRUE),
('00000000-0000-0000-0001-000000000005', 'SOCIEDADE_CIVIL', 'Sociedade Civil e Comunidade Externa', 'Representantes da comunidade regional assistida pelo IFFar', FALSE, TRUE),
('00000000-0000-0000-0001-000000000006', 'TODOS', 'Toda a Comunidade Acadêmica', 'Discentes, Docentes, TAEs e Comunidade Externa integrados (SINAES)', FALSE, TRUE)
ON CONFLICT (publico_avaliador_id) DO NOTHING;

-- 2. Povoamento de Alvos Avaliativos (Instituição, Campi, Setores, Docentes, Cursos, Turmas)
INSERT INTO alvo_avaliado (alvo_avaliado_id, tipo, nome, codigo, descricao, campus, ativo) VALUES
-- Instituição e Reitoria
('00000000-0000-0000-0002-000000000001', 'INSTITUICAO', 'Instituto Federal Farroupilha (Geral)', 'IFFAR-REI', 'Avaliação global do PDI, infraestrutura geral e gestão multicampi', 'Reitoria', TRUE),
('00000000-0000-0000-0002-000000000002', 'CAMPUS', 'Reitoria / Campus Santa Maria', 'IFFAR-SMA', 'Estrutura administrativa e acadêmica central', 'Santa Maria', TRUE),

-- Campi do IFFar
('00000000-0000-0000-0002-000000000003', 'CAMPUS', 'Campus Alegrete', 'IFFAR-ALE', 'Unidade de ensino técnico e superior da fronteira oeste', 'Alegrete', TRUE),
('00000000-0000-0000-0002-000000000004', 'CAMPUS', 'Campus Frederico Westphalen', 'IFFAR-FW', 'Unidade de ensino técnico e superior de Frederico Westphalen', 'Frederico Westphalen', TRUE),
('00000000-0000-0000-0002-000000000005', 'CAMPUS', 'Campus Jaguari', 'IFFAR-JAG', 'Unidade de ensino de Jaguari', 'Jaguari', TRUE),
('00000000-0000-0000-0002-000000000006', 'CAMPUS', 'Campus Júlio de Castilhos', 'IFFAR-JC', 'Unidade agrícola e superior de Júlio de Castilhos', 'Júlio de Castilhos', TRUE),
('00000000-0000-0000-0002-000000000007', 'CAMPUS', 'Campus Panambi', 'IFFAR-PAN', 'Unidade industrial e tecnológica de Panambi', 'Panambi', TRUE),
('00000000-0000-0000-0002-000000000008', 'CAMPUS', 'Campus Santa Rosa', 'IFFAR-SR', 'Unidade de formação tecnológica e superior de Santa Rosa', 'Santa Rosa', TRUE),
('00000000-0000-0000-0002-000000000009', 'CAMPUS', 'Campus Santo Ângelo', 'IFFAR-SA', 'Unidade missioneira de Santo Ângelo', 'Santo Ângelo', TRUE),
('00000000-0000-0000-0002-000000000010', 'CAMPUS', 'Campus Santo Augusto', 'IFFAR-SAUG', 'Unidade de ensino técnico e superior de Santo Augusto', 'Santo Augusto', TRUE),
('00000000-0000-0000-0002-000000000011', 'CAMPUS', 'Campus São Borja', 'IFFAR-SB', 'Unidade de ensino da fronteira oeste de São Borja', 'São Borja', TRUE),
('00000000-0000-0000-0002-000000000012', 'CAMPUS', 'Campus São Vicente do Sul', 'IFFAR-SVS', 'Unidade pioneira agropecuária e superior de São Vicente do Sul', 'São Vicente do Sul', TRUE),
('00000000-0000-0000-0002-000000000013', 'CAMPUS', 'Campus Avançado Uruguaiana', 'IFFAR-URG', 'Unidade avançada da fronteira de Uruguaiana', 'Uruguaiana', TRUE),

-- Setores Estratégicos
('00000000-0000-0000-0002-000000000014', 'SETOR', 'Diretoria de Tecnologia da Informação (DTI)', 'SET-DTI', 'Sistemas acadêmicos, conectividade e suporte tecnológico', 'Reitoria', TRUE),
('00000000-0000-0000-0002-000000000015', 'SETOR', 'Coordenação de Avaliação Institucional (CAIN)', 'SET-CAIN', 'Gestão dos processos de autoavaliação institucional e SINAES', 'Reitoria', TRUE),
('00000000-0000-0000-0002-000000000016', 'SETOR', 'Biblioteca Central e Setoriais', 'SET-BIB', 'Acervo físico, digital e salas de estudos', 'Reitoria', TRUE),

-- Docentes de Referência
('00000000-0000-0000-0002-000000000017', 'DOCENTE', 'Prof. Dr. Roberto Alencar', 'DOC-001', 'Docente da área de Ciência da Computação / TADS', 'Alegrete', TRUE),
('00000000-0000-0000-0002-000000000018', 'DOCENTE', 'Profa. Dra. Mariana Silveira', 'DOC-002', 'Docente da área de Banco de Dados e Informação', 'Alegrete', TRUE),
('00000000-0000-0000-0002-000000000019', 'DOCENTE', 'Prof. Me. Carlos Eduardo Lima', 'DOC-003', 'Docente da área de Redes e Sistemas', 'Alegrete', TRUE),

-- Cursos
('00000000-0000-0000-0002-000000000020', 'CURSO', 'Tecnologia em Análise e Desenvolvimento de Sistemas (TADS)', 'CURSO-TADS', 'Curso Superior de Tecnologia em ADS', 'Panambi', TRUE),
('00000000-0000-0000-0002-000000000021', 'CURSO', 'Bacharelado em Agronomia', 'CURSO-AGRO', 'Graduação em Agronomia', 'Alegrete', TRUE),
('00000000-0000-0000-0002-000000000022', 'CURSO', 'Licenciatura em Matemática', 'CURSO-MAT', 'Formação Docente em Matemática', 'Frederico Westphalen', TRUE),

-- Turmas
('00000000-0000-0000-0002-000000000023', 'TURMA', 'Turma ADS-2026/1 - Engenharia de Software', 'TURMA-ADS-ES', 'Turma presencial - 3º Semestre TADS', 'Panambi', TRUE),
('00000000-0000-0000-0002-000000000024', 'TURMA', 'Turma ADS-2026/1 - Banco de Dados Avançado', 'TURMA-ADS-BD', 'Turma presencial - 3º Semestre TADS', 'Panambi', TRUE),
('00000000-0000-0000-0002-000000000025', 'TURMA', 'Turma ADS-2026/1 - Redes e Sistemas Distribuídos', 'TURMA-ADS-REDES', 'Turma presencial - 4º Semestre TADS', 'Panambi', TRUE),
('00000000-0000-0000-0002-000000000026', 'TURMA', 'Turma AGRO-2026/1 - Manejo e Conservação do Solo', 'TURMA-AGRO-SOLOS', 'Turma 5º Semestre Agronomia', 'Alegrete', TRUE)
ON CONFLICT (alvo_avaliado_id) DO NOTHING;

-- 3. Inserir Instrumento Inicial da ADPD 2026/1
INSERT INTO instrumento (
    instrumento_id, titulo, subtitulo, modulo, ano_ciclo, periodo_letivo,
    data_inicio, data_termino, publico_alvo, tipo_alvo, tipo_publico, publicos_permitidos, restringir_vinculo_campus,
    criador_id, criador_nome, anonimo, status
) VALUES (
    '00000000-0000-0000-0003-000000000001',
    'Avaliação Docente pelo Discente (ADPD)',
    'Instituto Federal Farroupilha - Resolução CONSUP nº 104/2013',
    'ADPD',
    '2026/1',
    '1º Semestre de 2026',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    'Discentes regularmente matriculados nos Cursos Superiores do IFFar',
    'DOCENTE',
    'DISCENTES',
    'DISCENTES',
    TRUE,
    'usr-cpa-cain',
    'Comissão Própria de Avaliação (CPA)',
    TRUE,
    'ABERTO'
) ON CONFLICT (instrumento_id) DO NOTHING;

-- 4. Inserir Componentes Curriculares da ADPD
INSERT INTO componente_curricular (componente_curricular_id, instrumento_id, codigo, nome, docente_nome, turma) VALUES
('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0003-000000000001', 'IFF-ADS-01', 'Arquitetura e Engenharia de Software', 'Prof. Dr. Roberto Alencar', 'ADS-2026'),
('00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0003-000000000001', 'IFF-ADS-02', 'Banco de Dados Avançado', 'Profa. Dra. Mariana Silveira', 'ADS-2026'),
('00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0003-000000000001', 'IFF-ADS-03', 'Redes e Sistemas Distribuídos', 'Prof. Me. Carlos Eduardo Lima', 'ADS-2026')
ON CONFLICT (componente_curricular_id) DO NOTHING;

-- 5. Inserir Grupo 1: Autoavaliação do Estudante
INSERT INTO grupo_questoes (grupo_questoes_id, instrumento_id, secao, titulo, descricao, ordem) VALUES
('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0003-000000000001', 'AUTOAVALIACAO_DISCENTE', 'Etapa 1: Autoavaliação do Estudante',
 'Reflita sobre sua dedicação, pontualidade e engajamento nas atividades acadêmicas ao longo do semestre letivo.', 1)
ON CONFLICT (grupo_questoes_id) DO NOTHING;

-- Inserir Questões do Grupo 1
INSERT INTO questao (questao_id, grupo_questoes_id, enunciado, tipo, obrigatoria, ordem, escala_min, escala_max, rotulo_min, rotulo_max, ajuda) VALUES
('00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0005-000000000001', 'Mantive frequência regular, pontualidade e dedicação compatível com a carga horária das disciplinas cursadas.', 'ESCALA', TRUE, 1, 1, 5, '1 - Nunca / Raramente', '5 - Sempre / Plenamente', 'Considere presença em aulas presenciais e prazos de entregas.'),
('00000000-0000-0000-0006-000000000002', '00000000-0000-0000-0005-000000000001', 'Participei ativamente das aulas, esclarecendo dúvidas e colaborando com os colegas nos trabalhos em equipe.', 'ESCALA', TRUE, 2, 1, 5, '1 - Insatisfatório', '5 - Muito Participativo', NULL)
ON CONFLICT (questao_id) DO NOTHING;

INSERT INTO questao (questao_id, grupo_questoes_id, enunciado, tipo, obrigatoria, ordem) VALUES
('00000000-0000-0000-0006-000000000003', '00000000-0000-0000-0005-000000000001', 'Utilizei os canais de atendimento extraclasse (monitoria, atendimento ao aluno) quando necessário.', 'ESCOLHA_UNICA', TRUE, 3)
ON CONFLICT (questao_id) DO NOTHING;

INSERT INTO opcao_questao (opcao_questao_id, questao_id, texto, ordem) VALUES
('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0006-000000000003', 'Sim, utilizei com frequência', 0),
('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0006-000000000003', 'Ocasionalmente, quando tive dúvidas pontuais', 1),
('00000000-0000-0000-0007-000000000003', '00000000-0000-0000-0006-000000000003', 'Não senti necessidade', 2),
('00000000-0000-0000-0007-000000000004', '00000000-0000-0000-0006-000000000003', 'Tive dificuldades de horário para acessar', 3)
ON CONFLICT (opcao_questao_id) DO NOTHING;

-- 6. Inserir Grupos da Avaliação Docente
INSERT INTO grupo_questoes (grupo_questoes_id, instrumento_id, secao, titulo, descricao, ordem) VALUES
('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0003-000000000001', 'AVALIACAO_DOCENTE', 'Dimensão Pedagógica e Prática Docente',
 'Avalie a condução da disciplina, planejamento, métodos de ensino e domínio do conteúdo pelo docente.', 2),
('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0003-000000000001', 'AVALIACAO_DOCENTE', 'Avaliação da Aprendizagem e Relação Dialógica',
 'Avalie a pontualidade na devolutiva das avaliações e a disponibilidade para esclarecimento de dúvidas.', 3)
ON CONFLICT (grupo_questoes_id) DO NOTHING;

-- Inserir Questões da Dimensão Pedagógica
INSERT INTO questao (questao_id, grupo_questoes_id, enunciado, tipo, obrigatoria, ordem, escala_min, escala_max, rotulo_min, rotulo_max) VALUES
('00000000-0000-0000-0006-000000000004', '00000000-0000-0000-0005-000000000002', 'O docente apresentou o plano de ensino (objetivos, metodologia e critérios de avaliação) no início das aulas.', 'ESCALA', TRUE, 1, 1, 5, '1 - Não apresentou', '5 - Apresentou com clareza'),
('00000000-0000-0000-0006-000000000005', '00000000-0000-0000-0005-000000000002', 'Demonstra domínio aprofundado dos conteúdos e relaciona a teoria com aplicações práticas relevantes.', 'ESCALA', TRUE, 2, 1, 5, '1 - Pouco domínio', '5 - Excelente domínio'),
('00000000-0000-0000-0006-000000000006', '00000000-0000-0000-0005-000000000002', 'Utiliza recursos e estratégias didáticas que facilitam a compreensão e incentivam o pensamento crítico.', 'ESCALA', TRUE, 3, 1, 5, '1 - Rígido / Pouco claro', '5 - Dinâmico e Eficaz'),
('00000000-0000-0000-0006-000000000007', '00000000-0000-0000-0005-000000000003', 'Fornece devolutivas (feedback) construtivas sobre as atividades e provas em tempo hábil para o aprendizado.', 'ESCALA', TRUE, 4, 1, 5, '1 - Sem devolutiva', '5 - Feedback pontual e detalhado'),
('00000000-0000-0000-0006-000000000008', '00000000-0000-0000-0005-000000000003', 'Mantém postura ética, respeitosa e acessível para o diálogo acadêmico com a turma.', 'ESCALA', TRUE, 5, 1, 5, '1 - Inadequado', '5 - Sempre exemplar')
ON CONFLICT (questao_id) DO NOTHING;

INSERT INTO questao (questao_id, grupo_questoes_id, enunciado, tipo, obrigatoria, ordem, ajuda) VALUES
('00000000-0000-0000-0006-000000000009', '00000000-0000-0000-0005-000000000003', 'Comentários abertos e sugestões para o docente (opcional e preservado o anonimato):', 'TEXTO_LIVRE', FALSE, 6, 'Escreva observações construtivas que possam auxiliar no aprimoramento pedagógico da disciplina.')
ON CONFLICT (questao_id) DO NOTHING;
