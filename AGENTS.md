# Instruções do projeto

## Formulários

- Todo campo cujo preenchimento é obrigatório deve exibir um asterisco (`*`) vermelho imediatamente após o rótulo.
- Preserve essa indicação ao criar ou alterar formulários e mantenha o asterisco acessível, por exemplo, com `aria-hidden="true"` quando o próprio campo já comunica a obrigatoriedade.

## Navegação no frontend

- Para links internos, use `RouterLink` com `:to` apontando para uma rota nomeada e passe os valores dinâmicos em `params`, em vez de montar o caminho manualmente ou usar um link HTML comum.
