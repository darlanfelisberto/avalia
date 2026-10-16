-- Sessões anteriores usavam permissões locais e não podem continuar ativas.
DELETE FROM auth_sessao;
ALTER TABLE auth_sessao ADD COLUMN permissoes TEXT NOT NULL DEFAULT '';
