-- O banco SIG conectado fornece apenas o identificador e a descrição da situação.
-- V19 já pode ter sido aplicada; esta migração mantém seu histórico intacto.
ALTER TABLE academico.situacao_servidor
    ALTER COLUMN permite_cadastro_designacoes DROP NOT NULL;
