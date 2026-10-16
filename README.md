# AVALIA

## Executar o sistema

Pré-requisitos: Node.js e npm para o frontend; Java 25 e Maven para o backend; PostgreSQL com o banco `avalia` disponível localmente na porta `5432`; provedor OIDC Feliva na porta `9036`.

Configure as credenciais do banco antes de iniciar o backend:

```bash
export AVALIA_DB_USER=postgres
export AVALIA_DB_PASSWORD=postgres
export AVALIA_OIDC_PROVIDER_URL=http://localhost:9036/web/auth/realms/feliva
export AVALIA_OIDC_JWKS_URI=http://localhost:9036/web/auth/realms/feliva/certs
export AVALIA_OIDC_CLIENT_ID=avalia
export AVALIA_OIDC_REDIRECT_URI=http://localhost:5173
```

Guarde `AVALIA_OIDC_CLIENT_SECRET` no arquivo local `backend/.env.local` (ignorado pelo Git, permissão 600) ou injete a variável pelo ambiente. Nesse arquivo, fixe também `AVALIA_OIDC_ISSUER=https://teste-formaliza.teste.com/web/auth/realms/feliva` enquanto o provedor local publicar esse issuer; sem essa configuração a API recusa JWTs. O redirect URI cadastrado no provedor deve ser exatamente `http://localhost:5173`. O login usa Authorization Code com PKCE e identifica o CPF pela claim `preferred_username` do `id_token`.

O backend valida o `id_token` no login e confere se o `access_token` pertence ao mesmo usuário e ao cliente `avalia`. Os papéis e as permissões vêm da claim `groups` do `access_token`. O cadastro local fornece nome, campus, vínculo acadêmico e estado ativo; seus papéis privilegiados e permissões antigas não autorizam operações. Mudanças de permissões no provedor entram em vigor quando o usuário faz novo login.

O `access_token` é mantido em cookie `HttpOnly` e `SameSite=Strict`, com `Secure` quando a aplicação usa HTTPS. O Open Liberty valida assinatura, issuer, audiência e expiração do JWT em cada rota protegida por MicroProfile JWT. Todas as chamadas da API devem passar pela mesma origem do frontend em `/api`; requisições que alteram dados exigem o cabeçalho `Origin` correspondente ao redirect URI. O logout remove o cookie no navegador, mas o token emitido continua válido até expirar. Em produção, use HTTPS, um gestor de segredos e um provedor cuja URL de descoberta coincida com o `issuer` publicado. O desvio local só é aceito para `localhost` ou loopback com o issuer esperado configurado explicitamente.

Use terminais separados para iniciar as partes do sistema.

### Backend

```bash
cd backend
./dev.sh
```

A API fica em `http://localhost:9081`. Na inicialização, o Flyway aplica as migrações pendentes; a V29 remove a tabela de sessões locais.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

A aplicação fica em `http://localhost:5173` e encaminha as chamadas `/api` para o backend na porta `9081`.
