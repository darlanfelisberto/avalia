-- =========================================================================
-- IFFar AVALIA - Migração V2: Seed dos dados oficiais da ADPD 2026/1
-- =========================================================================

-- Inserir Instrumento Inicial
INSERT INTO instrumento (
    id, titulo, subtitulo, modulo, ano_ciclo, periodo_letivo,
    data_inicio, data_termino, publico_alvo, anonimo, status
) VALUES (
    'inst-adpd-2026-1',
    'Avaliação Docente pelo Discente (ADPD)',
    'Instituto Federal Farroupilha - Resolução CONSUP nº 104/2013',
    'ADPD',
    '2026/1',
    '1º Semestre de 2026',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    'Discentes regularmente matriculados nos Cursos Superiores do IFFar',
    TRUE,
    'ABERTO'
) ON CONFLICT (id) DO NOTHING;

-- Inserir Componentes Curriculares Exemplo
INSERT INTO componente_curricular (id, instrumento_id, codigo, nome, docente_nome, turma) VALUES
('comp-1', 'inst-adpd-2026-1', 'IFF-ADS-01', 'Arquitetura e Engenharia de Software', 'Prof. Dr. Roberto Alencar', 'ADS-2026'),
('comp-2', 'inst-adpd-2026-1', 'IFF-ADS-02', 'Banco de Dados Avançado', 'Profa. Dra. Mariana Silveira', 'ADS-2026'),
('comp-3', 'inst-adpd-2026-1', 'IFF-ADS-03', 'Redes e Sistemas Distribuídos', 'Prof. Me. Carlos Eduardo Lima', 'ADS-2026')
ON CONFLICT (id) DO NOTHING;

-- Inserir Grupo 1: Autoavaliação do Estudante
INSERT INTO grupo_questoes (id, instrumento_id, secao, titulo, descricao, ordem) VALUES
('grp-auto-1', 'inst-adpd-2026-1', 'AUTOAVALIACAO_DISCENTE', 'Etapa 1: Autoavaliação do Estudante',
 'Reflita sobre sua dedicação, pontualidade e engajamento nas atividades acadêmicas ao longo do semestre letivo.', 1)
ON CONFLICT (id) DO NOTHING;

-- Inserir Questões do Grupo 1
INSERT INTO questao (id, grupo_id, enunciado, tipo, obrigatoria, ordem, escala_min, escala_max, rotulo_min, rotulo_max, ajuda) VALUES
('q-auto-01', 'grp-auto-1', 'Mantive frequência regular, pontualidade e dedicação compatível com a carga horária das disciplinas cursadas.', 'ESCALA', TRUE, 1, 1, 5, '1 - Nunca / Raramente', '5 - Sempre / Plenamente', 'Considere presença em aulas presenciais e prazos de entregas.'),
('q-auto-02', 'grp-auto-1', 'Participei ativamente das aulas, esclarecendo dúvidas e colaborando com os colegas nos trabalhos em equipe.', 'ESCALA', TRUE, 2, 1, 5, '1 - Insatisfatório', '5 - Muito Participativo', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO questao (id, grupo_id, enunciado, tipo, obrigatoria, ordem, opcoes_json) VALUES
('q-auto-03', 'grp-auto-1', 'Utilizei os canais de atendimento extraclasse (monitoria, atendimento ao aluno) quando necessário.', 'ESCOLHA_UNICA', TRUE, 3, '[
  {"id": "opt-sim", "texto": "Sim, utilizei com frequência"},
  {"id": "opt-as-vezes", "texto": "Ocasionalmente, quando tive dúvidas pontuais"},
  {"id": "opt-nao-precisei", "texto": "Não senti necessidade"},
  {"id": "opt-nao-consegui", "texto": "Tive dificuldades de horário para acessar"}
]'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- Inserir Grupos da Avaliação Docente
INSERT INTO grupo_questoes (id, instrumento_id, secao, titulo, descricao, ordem) VALUES
('grp-doc-didatica', 'inst-adpd-2026-1', 'AVALIACAO_DOCENTE', 'Dimensão Pedagógica e Prática Docente',
 'Avalie a condução da disciplina, planejamento, métodos de ensino e domínio do conteúdo pelo docente.', 2),
('grp-doc-relacionamento', 'inst-adpd-2026-1', 'AVALIACAO_DOCENTE', 'Avaliação da Aprendizagem e Relação Dialógica',
 'Avalie a pontualidade na devolutiva das avaliações e a disponibilidade para esclarecimento de dúvidas.', 3)
ON CONFLICT (id) DO NOTHING;

-- Inserir Questões da Dimensão Pedagógica
INSERT INTO questao (id, grupo_id, enunciado, tipo, obrigatoria, ordem, escala_min, escala_max, rotulo_min, rotulo_max) VALUES
('q-doc-01', 'grp-doc-didatica', 'O docente apresentou o plano de ensino (objetivos, metodologia e critérios de avaliação) no início das aulas.', 'ESCALA', TRUE, 1, 1, 5, '1 - Não apresentou', '5 - Apresentou com clareza'),
('q-doc-02', 'grp-doc-didatica', 'Demonstra domínio aprofundado dos conteúdos e relaciona a teoria com aplicações práticas relevantes.', 'ESCALA', TRUE, 2, 1, 5, '1 - Pouco domínio', '5 - Excelente domínio'),
('q-doc-03', 'grp-doc-didatica', 'Utiliza recursos e estratégias didáticas que facilitam a compreensão e incentivam o pensamento crítico.', 'ESCALA', TRUE, 3, 1, 5, '1 - Rígido / Pouco claro', '5 - Dinâmico e Eficaz'),
('q-doc-04', 'grp-doc-relacionamento', 'Fornece devolutivas (feedback) construtivas sobre as atividades e provas em tempo hábil para o aprendizado.', 'ESCALA', TRUE, 4, 1, 5, '1 - Sem devolutiva', '5 - Feedback pontual e detalhado'),
('q-doc-05', 'grp-doc-relacionamento', 'Mantém postura ética, respeitosa e acessível para o diálogo acadêmico com a turma.', 'ESCALA', TRUE, 5, 1, 5, '1 - Inadequado', '5 - Sempre exemplar')
ON CONFLICT (id) DO NOTHING;

INSERT INTO questao (id, grupo_id, enunciado, tipo, obrigatoria, ordem, ajuda) VALUES
('q-doc-06', 'grp-doc-relacionamento', 'Comentários abertos e sugestões para o docente (opcional e preservado o anonimato):', 'TEXTO_LIVRE', FALSE, 6, 'Escreva observações construtivas que possam auxiliar no aprimoramento pedagógico da disciplina.')
ON CONFLICT (id) DO NOTHING;
