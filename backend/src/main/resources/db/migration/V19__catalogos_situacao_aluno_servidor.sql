-- O SIGAA guarda o status do discente como inteiro em public.discente.status.
CREATE TABLE academico.status_discente (
    id INTEGER PRIMARY KEY,
    descricao VARCHAR(100) NOT NULL
);

INSERT INTO academico.status_discente (id, descricao) VALUES
    (1, 'ATIVO'),
    (2, 'CADASTRADO'),
    (3, 'CONCLUÍDO'),
    (4, 'AFASTADO'),
    (5, 'TRANCADO'),
    (6, 'CANCELADO'),
    (7, 'JUBILADO'),
    (8, 'FORMANDO'),
    (9, 'FORMADO'),
    (10, 'EXCLUÍDO'),
    (11, 'EM HOMOLOGAÇÃO'),
    (12, 'DEFENDIDO'),
    (13, 'PENDENTE CADASTRO'),
    (14, 'ATIVO EM DEPENDÊNCIA'),
    (15, 'PRÉ-CADASTRADO'),
    (16, 'FALECIDO');

-- Mesmo catálogo de rh.situacao_servidor na origem.
CREATE TABLE academico.situacao_servidor (
    id_situacao INTEGER PRIMARY KEY,
    descricao VARCHAR(100) NOT NULL,
    permite_cadastro_designacoes BOOLEAN NOT NULL,
    codigo_siape INTEGER,
    atualizado_em TIMESTAMP DEFAULT now()
);

ALTER TABLE academico.aluno
    ADD COLUMN id_situacao INTEGER REFERENCES academico.status_discente(id);

ALTER TABLE academico.servidor
    ADD COLUMN id_situacao INTEGER REFERENCES academico.situacao_servidor(id_situacao);

UPDATE academico.aluno a
SET id_situacao = status.id
FROM academico.status_discente status
WHERE a.situacao = status.descricao;

CREATE INDEX idx_academico_aluno_situacao ON academico.aluno(id_situacao);
CREATE INDEX idx_academico_servidor_situacao ON academico.servidor(id_situacao);
