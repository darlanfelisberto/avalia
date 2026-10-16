# AVALIA

## Executar o sistema

Pré-requisitos: Node.js e npm para o frontend; Java 25 e Maven para o backend; PostgreSQL com o banco `avalia` disponível localmente na porta `5432`; provedor OIDC Feliva na porta `9036`.

Configure as credenciais do banco antes de iniciar o backend:

```bash
export AVALIA_DB_USER=postgres
export AVALIA_DB_PASSWORD=postgres
export AVALIA_OIDC_PROVIDER_URL=http://localhost:9036/web/auth/realms/feliva
export AVALIA_OIDC_CLIENT_ID=avalia
export AVALIA_OIDC_REDIRECT_URI=http://localhost:5173
```

O segredo do cliente `avalia` está definido no backend e pode ser substituído pela variável `AVALIA_OIDC_CLIENT_SECRET`. O redirect URI cadastrado no provedor deve ser exatamente `http://localhost:5173`. O login usa Authorization Code com PKCE e identifica o CPF pela claim `preferred_username` do `id_token`. O provedor local publica um `issuer` externo; a aplicação valida esse valor no token e acessa os endpoints pela URL local. Para fixar o valor esperado, configure `AVALIA_OIDC_ISSUER` com o `issuer` publicado na descoberta.

Para publicar o sistema, use HTTPS, substitua e rotacione o segredo definido no código e mova a sessão do navegador para um cookie `HttpOnly`, `Secure` e `SameSite`. A implementação atual guarda o token de sessão no `localStorage` e destina-se ao ambiente local.

Use terminais separados para iniciar as partes do sistema.

### Backend

```bash
cd backend
mvn clean liberty:dev
```

A API fica em `http://localhost:9081`. Na inicialização, o Flyway aplica as migrações pendentes e invalida as sessões antigas do login por CPF.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

A aplicação fica em `http://localhost:5173` e encaminha as chamadas `/api` para o backend na porta `9081`.
