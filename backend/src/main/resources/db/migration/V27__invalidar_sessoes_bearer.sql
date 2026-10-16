-- O token anterior era acessível ao JavaScript; exigir novo login com cookie HttpOnly.
DELETE FROM auth_sessao;
