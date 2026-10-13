-- ============================================================================
-- V7: SCHEMA ACADÊMICO DE IMPORTAÇÃO (INTEGRAÇÃO COM SIGAA / SIGRH)
-- Modelo Híbrido Relacional + Árvore de Unidades para o AVALIA
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS ltree;
CREATE SCHEMA IF NOT EXISTS academico;

-- 1. DADOS PESSOAIS
CREATE TABLE IF NOT EXISTS academico.pessoa (
    id                  INTEGER PRIMARY KEY,
    nome                VARCHAR(200)    NOT NULL,
    nome_social         VARCHAR(200),
    cpf_cnpj            VARCHAR(14),
    email               VARCHAR(200),
    telefone            VARCHAR(30),
    celular             VARCHAR(30),
    data_nascimento     DATE,
    sexo                CHAR(1),
    data_cadastro       TIMESTAMP,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_pessoa_cpf  ON academico.pessoa(cpf_cnpj);
CREATE INDEX IF NOT EXISTS idx_academico_pessoa_nome ON academico.pessoa(nome);

-- 2. ESTRUTURA ORGANIZACIONAL (RH / LOTAÇÃO / SETORES / CAMPI)
CREATE TABLE IF NOT EXISTS academico.unidade (
    id                  INTEGER PRIMARY KEY,
    codigo              VARCHAR(20),
    nome                VARCHAR(200)    NOT NULL,
    sigla               VARCHAR(30),
    tipo                VARCHAR(50),
    categoria           VARCHAR(50),
    id_pai              INTEGER REFERENCES academico.unidade(id),
    path                LTREE,
    hierarquia_texto    VARCHAR(500),
    ativo               BOOLEAN DEFAULT TRUE,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_unidade_path   ON academico.unidade USING GIST(path);
CREATE INDEX IF NOT EXISTS idx_academico_unidade_id_pai ON academico.unidade(id_pai);

-- Tabela Closure para navegação hierárquica em 1 único JOIN
CREATE TABLE IF NOT EXISTS academico.unidade_closure (
    id_ancestral        INTEGER REFERENCES academico.unidade(id),
    id_descendente      INTEGER REFERENCES academico.unidade(id),
    profundidade        INTEGER NOT NULL,
    PRIMARY KEY (id_ancestral, id_descendente)
);

CREATE INDEX IF NOT EXISTS idx_academico_closure_descendente ON academico.unidade_closure(id_descendente);

-- 3. CARGOS, SERVIDORES E CHEFIAS (DOCENTES E TAES)
CREATE TABLE IF NOT EXISTS academico.cargo (
    id                  INTEGER PRIMARY KEY,
    denominacao         VARCHAR(200)    NOT NULL,
    sigla               VARCHAR(20),
    categoria           VARCHAR(100),   -- 'Docente', 'Técnico-Administrativo'
    nivel_funcional     VARCHAR(50),
    inativo             BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS academico.servidor (
    id                  INTEGER PRIMARY KEY,
    id_pessoa           INTEGER NOT NULL REFERENCES academico.pessoa(id),
    siape               VARCHAR(20),
    matricula_interna   VARCHAR(20),
    id_cargo            INTEGER REFERENCES academico.cargo(id),
    id_unidade_lotacao  INTEGER REFERENCES academico.unidade(id),
    id_unidade_exercicio INTEGER REFERENCES academico.unidade(id),
    categoria           VARCHAR(100),   -- 'Docente', 'Técnico-Administrativo'
    situacao            VARCHAR(100),   -- 'Ativo Permanente', 'Aposentado'...
    regime_trabalho     VARCHAR(50),    -- 'DE', '40h', '20h'
    regime_juridico     VARCHAR(50),    -- 'RJU', 'CLT'
    admissao            DATE,
    data_desligamento   DATE,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_servidor_pessoa            ON academico.servidor(id_pessoa);
CREATE INDEX IF NOT EXISTS idx_academico_servidor_unidade_lotacao   ON academico.servidor(id_unidade_lotacao);
CREATE INDEX IF NOT EXISTS idx_academico_servidor_unidade_exercicio ON academico.servidor(id_unidade_exercicio);
CREATE INDEX IF NOT EXISTS idx_academico_servidor_categoria         ON academico.servidor(categoria);

CREATE TABLE IF NOT EXISTS academico.designacao (
    id                  INTEGER PRIMARY KEY,
    id_servidor         INTEGER REFERENCES academico.servidor(id),
    id_unidade          INTEGER REFERENCES academico.unidade(id),
    funcao              VARCHAR(200),
    nivel               VARCHAR(20),
    chefia              BOOLEAN DEFAULT FALSE,
    data_inicio         DATE NOT NULL,
    data_fim            DATE,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_designacao_unidade ON academico.designacao(id_unidade);
CREATE INDEX IF NOT EXISTS idx_academico_designacao_servidor ON academico.designacao(id_servidor);
CREATE INDEX IF NOT EXISTS idx_academico_designacao_chefe_ativo 
    ON academico.designacao(id_unidade) WHERE chefia = TRUE AND data_fim IS NULL;

-- 4. CURSOS E ALUNOS (DISCENTES)
CREATE TABLE IF NOT EXISTS academico.curso (
    id                  INTEGER PRIMARY KEY,
    codigo              VARCHAR(30),
    nome                VARCHAR(300)    NOT NULL,
    nivel               CHAR(1),        -- 'G'=Grad, 'M'=Mest, 'D'=Dout, 'E'=Espec, 'T'=Tec, 'I'=Infantil
    modalidade          VARCHAR(50),
    id_unidade          INTEGER REFERENCES academico.unidade(id),
    ativo               BOOLEAN DEFAULT TRUE,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_curso_unidade ON academico.curso(id_unidade);

CREATE TABLE IF NOT EXISTS academico.aluno (
    id                  INTEGER PRIMARY KEY,
    id_pessoa           INTEGER NOT NULL REFERENCES academico.pessoa(id),
    matricula           VARCHAR(30),
    id_curso            INTEGER REFERENCES academico.curso(id),
    nivel               CHAR(1),
    situacao            VARCHAR(100),
    ano_ingresso        INTEGER,
    periodo_ingresso    INTEGER,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_aluno_pessoa ON academico.aluno(id_pessoa);
CREATE INDEX IF NOT EXISTS idx_academico_aluno_curso  ON academico.aluno(id_curso);
CREATE INDEX IF NOT EXISTS idx_academico_aluno_matricula ON academico.aluno(matricula);

CREATE TABLE IF NOT EXISTS academico.coordenacao_curso (
    id                  INTEGER PRIMARY KEY,
    id_servidor         INTEGER REFERENCES academico.servidor(id),
    id_curso            INTEGER REFERENCES academico.curso(id),
    cargo               VARCHAR(100),
    data_inicio         DATE,
    data_fim            DATE,
    ativo               BOOLEAN DEFAULT FALSE,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_coord_curso_ativo 
    ON academico.coordenacao_curso(id_curso) WHERE ativo = TRUE;

-- 5. COMPONENTES CURRICULARES, TURMAS E MATRÍCULAS
CREATE TABLE IF NOT EXISTS academico.componente_curricular (
    id                  INTEGER PRIMARY KEY,
    codigo              VARCHAR(30)     NOT NULL,
    nome                VARCHAR(300)    NOT NULL,
    id_unidade          INTEGER REFERENCES academico.unidade(id),
    nivel               CHAR(1),
    ch_total            INTEGER,
    ch_teorica          INTEGER,
    ch_pratica          INTEGER,
    ementa              TEXT,
    ativo               BOOLEAN DEFAULT TRUE,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_componente_codigo  ON academico.componente_curricular(codigo);
CREATE INDEX IF NOT EXISTS idx_academico_componente_unidade ON academico.componente_curricular(id_unidade);

CREATE TABLE IF NOT EXISTS academico.turma (
    id                          INTEGER PRIMARY KEY,
    id_componente_curricular    INTEGER NOT NULL REFERENCES academico.componente_curricular(id),
    codigo_turma                VARCHAR(20)     NOT NULL,
    ano                         INTEGER         NOT NULL,
    periodo                     INTEGER         NOT NULL,
    id_curso                    INTEGER REFERENCES academico.curso(id),
    capacidade                  INTEGER,
    situacao                    VARCHAR(50),
    id_unidade                  INTEGER REFERENCES academico.unidade(id),
    atualizado_em               TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_turma_componente   ON academico.turma(id_componente_curricular);
CREATE INDEX IF NOT EXISTS idx_academico_turma_ano_periodo  ON academico.turma(ano, periodo);
CREATE INDEX IF NOT EXISTS idx_academico_turma_curso        ON academico.turma(id_curso);

CREATE TABLE IF NOT EXISTS academico.docente_turma (
    id                  INTEGER PRIMARY KEY,
    id_servidor         INTEGER REFERENCES academico.servidor(id),
    id_turma            INTEGER NOT NULL REFERENCES academico.turma(id),
    ch_dedicada         NUMERIC(6,2),
    tipo_docente        VARCHAR(30),
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_docente_turma_servidor ON academico.docente_turma(id_servidor);
CREATE INDEX IF NOT EXISTS idx_academico_docente_turma_turma    ON academico.docente_turma(id_turma);

CREATE TABLE IF NOT EXISTS academico.matricula (
    id                  INTEGER PRIMARY KEY,
    id_aluno            INTEGER REFERENCES academico.aluno(id),
    id_turma            INTEGER REFERENCES academico.turma(id),
    situacao            VARCHAR(100),
    media_final         NUMERIC(5,2),
    numero_faltas       INTEGER,
    frequencia          NUMERIC(5,2),
    ano                 INTEGER,
    periodo             INTEGER,
    data_matricula      DATE,
    atualizado_em       TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_academico_matricula_aluno ON academico.matricula(id_aluno);
CREATE INDEX IF NOT EXISTS idx_academico_matricula_turma ON academico.matricula(id_turma);
CREATE INDEX IF NOT EXISTS idx_academico_matricula_ano_periodo ON academico.matricula(ano, periodo);

-- 6. LOGS DE EXECUÇÃO DO ETL DA APLICAÇÃO
CREATE TABLE IF NOT EXISTS academico.etl_log (
    id                  SERIAL PRIMARY KEY,
    entidade            VARCHAR(100),
    inicio              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fim                 TIMESTAMP,
    registros_importados INTEGER DEFAULT 0,
    status              VARCHAR(20) DEFAULT 'EM_ANDAMENTO', -- 'SUCESSO', 'ERRO', 'EM_ANDAMENTO'
    mensagem            TEXT
);

-- 7. VIEWS DE SUPORTE AO SISTEMA AVALIA
CREATE OR REPLACE VIEW academico.v_docente_componente_turma AS
SELECT
    cc.id           AS id_componente,
    cc.codigo       AS codigo_componente,
    cc.nome         AS nome_componente,
    cc.ch_total,
    u_depto.nome    AS departamento_responsavel,
    t.id            AS id_turma,
    t.codigo_turma,
    t.ano,
    t.periodo,
    t.situacao      AS situacao_turma,
    s.id            AS id_servidor,
    s.siape,
    p.nome          AS docente,
    dt.ch_dedicada  AS ch_dedicada_docente,
    dt.tipo_docente
FROM academico.componente_curricular cc
LEFT JOIN academico.unidade u_depto   ON u_depto.id = cc.id_unidade
JOIN academico.turma t                ON t.id_componente_curricular = cc.id
JOIN academico.docente_turma dt       ON dt.id_turma = t.id
JOIN academico.servidor s             ON s.id = dt.id_servidor
JOIN academico.pessoa p               ON p.id = s.id_pessoa;

CREATE OR REPLACE VIEW academico.v_turma_resumo AS
SELECT
    t.id            AS id_turma,
    t.ano,
    t.periodo,
    cc.codigo       AS codigo_componente,
    cc.nome         AS nome_componente,
    t.codigo_turma,
    t.capacidade,
    t.situacao      AS situacao_turma,
    STRING_AGG(DISTINCT p.nome, ', ') AS docentes,
    COUNT(DISTINCT m.id_aluno)        AS total_alunos_matriculados
FROM academico.turma t
JOIN academico.componente_curricular cc ON cc.id = t.id_componente_curricular
LEFT JOIN academico.docente_turma dt    ON dt.id_turma = t.id
LEFT JOIN academico.servidor s          ON s.id = dt.id_servidor
LEFT JOIN academico.pessoa p            ON p.id = s.id_pessoa
LEFT JOIN academico.matricula m         ON m.id_turma = t.id
GROUP BY t.id, t.ano, t.periodo, cc.codigo, cc.nome,
         t.codigo_turma, t.capacidade, t.situacao;

CREATE OR REPLACE VIEW academico.v_chefe_unidade AS
SELECT
    u.id            AS id_unidade,
    u.nome          AS unidade,
    u.sigla,
    p.nome          AS chefe,
    p.email         AS email_chefe,
    s.siape,
    d.funcao,
    d.nivel,
    d.data_inicio
FROM academico.designacao d
JOIN academico.unidade u  ON u.id = d.id_unidade
JOIN academico.servidor s ON s.id = d.id_servidor
JOIN academico.pessoa p   ON p.id = s.id_pessoa
WHERE d.chefia = TRUE AND d.data_fim IS NULL;

CREATE OR REPLACE VIEW academico.v_servidor_completo AS
SELECT
    s.id            AS id_servidor,
    s.siape,
    p.nome,
    p.nome_social,
    p.email,
    p.telefone,
    p.celular,
    c.denominacao   AS cargo,
    s.categoria,
    s.situacao,
    s.regime_trabalho,
    ul.nome         AS unidade_lotacao,
    ul.sigla        AS sigla_lotacao,
    ue.nome         AS unidade_exercicio
FROM academico.servidor s
JOIN academico.pessoa p   ON p.id = s.id_pessoa
LEFT JOIN academico.cargo c ON c.id = s.id_cargo
LEFT JOIN academico.unidade ul ON ul.id = s.id_unidade_lotacao
LEFT JOIN academico.unidade ue ON ue.id = s.id_unidade_exercicio;
