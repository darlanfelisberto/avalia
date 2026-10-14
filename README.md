# AVALIA

## Executar o sistema

Pré-requisitos: Node.js e npm para o frontend; Java 25 e Maven para o backend; PostgreSQL com o banco `avalia` disponível localmente na porta `5432`.

Configure as credenciais do banco antes de iniciar o backend:

```bash
export APP_DB_USER=postgres
export APP_DB_PASSWORD=postgres
```

Use terminais separados para iniciar as partes do sistema.

### Backend

```bash
cd backend
mvn liberty:dev
```

A API fica em `http://localhost:9080`. Na inicialização, o Flyway aplica as migrações pendentes.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

A aplicação fica em `http://localhost:5173` e encaminha as chamadas `/api` para o backend na porta `9080`.
