// --- Usuário Ativo e Sessão de Autenticação ---

const STORAGE_KEY_USUARIO = 'iffar_avalia_usuario_oidc';
const STORAGE_KEY_TOKEN = 'iffar_avalia_token_oidc';

export function carregarTokenAtivo(): string | null {
  return localStorage.getItem(STORAGE_KEY_TOKEN);
}


export function carregarUsuarioAtivo(): import('../types/avaliacao').UsuarioContexto | null {
  try {
    const salvo = localStorage.getItem(STORAGE_KEY_USUARIO);
    if (salvo) {
      const u = JSON.parse(salvo);
      if (u && u.cpf) {
        if (!u.papel && u.papelAtivo) u.papel = u.papelAtivo;
        const papeisValidos = ['CPA', 'ADMIN', 'DOCENTE', 'DISCENTE', 'TAE'];
        if (!papeisValidos.includes(u.papel) || (u.papelAtivo && !papeisValidos.includes(u.papelAtivo))) {
          limparUsuarioAtivo();
          return null;
        }
        return u;
      }
    }
  } catch (e) {
    console.warn('Erro ao carregar usuário ativo', e);
  }
  return null;
}

export function salvarUsuarioAtivo(usuario: import('../types/avaliacao').UsuarioContexto, token?: string) {
  try {
    if (!usuario.papel && usuario.papelAtivo) {
      usuario.papel = usuario.papelAtivo;
    }
    localStorage.setItem(STORAGE_KEY_USUARIO, JSON.stringify(usuario));
    if (token) {
      localStorage.setItem(STORAGE_KEY_TOKEN, token);
    }
  } catch (e) {
    console.error('Erro ao salvar usuário ativo', e);
  }
}

export function limparUsuarioAtivo() {
  try {
    localStorage.removeItem(STORAGE_KEY_USUARIO);
    localStorage.removeItem(STORAGE_KEY_TOKEN);
  } catch (e) {
    console.error('Erro ao limpar sessão', e);
  }
}

export function temPermissao(
  usuario: import('../types/avaliacao').UsuarioContexto | null,
  permissao: import('../types/avaliacao').PermissaoUsuario
): boolean {
  if (!usuario) return false;
  if (usuario.papel === 'CPA' || usuario.papelAtivo === 'CPA') return true;
  return Array.isArray(usuario.permissoes) && usuario.permissoes.includes(permissao);
}


