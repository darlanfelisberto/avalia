# Instruções do projeto

## Formulários

- Todo campo cujo preenchimento é obrigatório deve exibir um asterisco (`*`) vermelho imediatamente após o rótulo.
- Preserve essa indicação ao criar ou alterar formulários e mantenha o asterisco acessível, por exemplo, com `aria-hidden="true"` quando o próprio campo já comunica a obrigatoriedade.
- Nomeie componentes de formulário como `<Entidade>Form`, usando o nome do domínio e sem o sufixo `View` (por exemplo, `InstrumentoAvaliativoForm.vue`).

## Nomenclatura de componentes no frontend

- Nomeie componentes de listagem como `<Entidade>List`, usando o nome do domínio e sem o sufixo `View` (por exemplo, `InstrumentoAcaliativoList.vue`).

## Navegação no frontend

- Para links internos, use `RouterLink` com `:to` apontando para uma rota nomeada e passe os valores dinâmicos em `params`, em vez de montar o caminho manualmente ou usar um link HTML comum.

## Organização do backend

- Cada tipo de dado deve ter seu próprio endpoint, service, DAO, model e DTO. Evite concentrar tipos de dados diferentes nessas mesmas classes.
