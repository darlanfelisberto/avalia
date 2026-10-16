# Backend

O backend usa Jakarta EE no Open Liberty. O Flyway mantém o schema; a unidade JPA `avaliaPU` usa o DataSource `jdbc/avaliaDS` e não cria tabelas em execução.

## Organização

- `avaliacao/model`: entidades JPA do schema `avaliacao` (instrumentos, catálogo, alvos e respostas).
- `avaliacao/dao`: consultas e persistência. `InstrumentoDao` concentra as projeções e operações em lote específicas do PostgreSQL; `ResponderAvaliacaoDao` contém as consultas de elegibilidade que cruzam os schemas `avaliacao` e `academico`.
- `avaliacao/service`: validação, composição dos DTOs e regras de publicação e envio.
- `academico/dao/SincronizacaoSigaaDao`: importação JDBC em lotes entre as duas bases; `academico/service` coordena a execução assíncrona.
- `core/dao/AuthDao`: acesso a usuários e sessões JPA, com consultas do espelho acadêmico usadas após a autenticação OIDC.

## Convenção de organização por objeto

Cada entidade ou objeto de domínio deve ter seus próprios componentes de API e persistência. Por exemplo, `TipoEntidade` deve ter `TipoEntidadeResource` para seus endpoints, `TipoEntidadeService` para suas regras e `TipoEntidadeDao` para suas consultas e persistência, além do model e DTOs próprios quando necessários. Não adicionar endpoints e consultas de um objeto a um Resource ou DAO genérico de outro objeto, como `AvaliacaoResource` ou `AvaliacaoDao`.

Os componentes devem ficar nos pacotes por camada (`model`, `resource`, `service`, `dao` e `dto`), com nomes correspondentes ao objeto de domínio. Essa separação vale também para novos objetos; ao alterar código existente, mover as operações para os componentes do objeto responsável.

O diretório `src/main/resources/db/migration` está sem migrations no momento; o dump da base será preparado posteriormente como uma migration inicial. Execute `mvn -f backend/pom.xml verify` para compilar e empacotar o backend. Para verificar os fluxos com persistência, disponibilize PostgreSQL e inicie o Liberty conforme o README principal.
