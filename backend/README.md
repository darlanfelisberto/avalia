# Backend

O backend usa Jakarta EE no Open Liberty. O Flyway mantém o schema; a unidade JPA `avaliaPU` usa o DataSource `jdbc/avaliaDS` e não cria tabelas em execução.

## Organização

- `avaliacao/model`: entidades JPA do schema `avaliacao` (instrumentos, catálogo, alvos e respostas).
- `avaliacao/dao`: consultas e persistência. `InstrumentoDao` concentra as projeções e operações em lote específicas do PostgreSQL; `ResponderAvaliacaoDao` contém as consultas de elegibilidade que cruzam os schemas `avaliacao` e `academico`.
- `avaliacao/service`: validação, composição dos DTOs e regras de publicação e envio.
- `academico/dao/SincronizacaoSigaaDao`: importação JDBC em lotes entre as duas bases; `academico/service` coordena a execução assíncrona.
- `core/dao/AuthDao`: acesso a usuários e sessões JPA, com consultas do espelho acadêmico usadas no login.

As migrações ficam em `src/main/resources/db/migration`. A V8 cria o schema de avaliação e a V16 remove a coluna legada de módulo. Execute `mvn -f backend/pom.xml verify` para compilar e empacotar o backend. Para verificar os fluxos com persistência, disponibilize PostgreSQL e inicie o Liberty conforme o README principal.
