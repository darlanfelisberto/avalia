import type { AuthResponseDTO, UsuarioContexto } from '../types/avaliacao';

interface OidcConfig {
  authorizationEndpoint: string;
  endSessionEndpoint: string;
  clientId: string;
  redirectUri: string;
}

interface FluxoOidc {
  state: string;
  nonce: string;
  verifier: string;
  redirectUri: string;
  iniciadoEm: number;
}

const CHAVE_FLUXO = 'avalia_oidc_fluxo';

function base64url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function aleatorio(): string {
  return base64url(crypto.getRandomValues(new Uint8Array(32)));
}

async function configuracao(): Promise<OidcConfig> {
  const res = await fetch('/api/auth/oidc/config', { cache: 'no-store' });
  if (!res.ok) throw new Error('Provedor de identidade indisponível. Tente novamente mais tarde.');
  return res.json();
}

export async function iniciarLoginOidc(): Promise<void> {
  const config = await configuracao();
  if (config.redirectUri !== window.location.origin) {
    throw new Error('A URL de retorno OIDC não corresponde à origem desta aplicação.');
  }
  const fluxo: FluxoOidc = {
    state: aleatorio(),
    nonce: aleatorio(),
    verifier: aleatorio(),
    redirectUri: config.redirectUri,
    iniciadoEm: Date.now()
  };
  const digest = new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(fluxo.verifier)));
  const url = new URL(config.authorizationEndpoint);
  url.searchParams.set('scope', 'openid');
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', config.clientId);
  url.searchParams.set('redirect_uri', fluxo.redirectUri);
  url.searchParams.set('state', fluxo.state);
  url.searchParams.set('nonce', fluxo.nonce);
  url.searchParams.set('code_challenge', base64url(digest));
  url.searchParams.set('code_challenge_method', 'S256');
  sessionStorage.setItem(CHAVE_FLUXO, JSON.stringify(fluxo));
  window.location.assign(url.toString());
}

export function recebeuRetornoOidc(): boolean {
  const params = new URLSearchParams(window.location.search);
  return params.has('code') || params.has('error');
}

export async function concluirLoginOidc(): Promise<AuthResponseDTO> {
  const params = new URLSearchParams(window.location.search);
  const salvo = sessionStorage.getItem(CHAVE_FLUXO);
  sessionStorage.removeItem(CHAVE_FLUXO);
  window.history.replaceState({}, document.title, window.location.pathname + window.location.hash);
  if (params.has('error')) throw new Error('Autenticação cancelada ou recusada pelo provedor.');
  if (!salvo) throw new Error('Sessão de autenticação não encontrada. Inicie o login novamente.');
  const fluxo = JSON.parse(salvo) as FluxoOidc;
  if (params.get('state') !== fluxo.state || Date.now() - fluxo.iniciadoEm > 10 * 60_000) {
    throw new Error('Resposta de autenticação inválida ou expirada. Inicie o login novamente.');
  }
  const code = params.get('code');
  if (!code) throw new Error('O provedor não retornou um código de autenticação.');
  const res = await fetch('/api/auth/oidc/callback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, codeVerifier: fluxo.verifier, nonce: fluxo.nonce, redirectUri: fluxo.redirectUri })
  });
  const resposta = await res.json() as AuthResponseDTO;
  if (!res.ok || !resposta.autenticado || !resposta.usuario || !resposta.token) {
    throw new Error(resposta.mensagem || 'Não foi possível concluir a autenticação.');
  }
  return resposta;
}

export async function validarSessaoAtual(token: string, papel: string): Promise<UsuarioContexto | null> {
  try {
    const res = await fetch('/api/auth/me', {
      headers: { Authorization: `Bearer ${token}`, 'X-Papel-Ativo': papel },
      cache: 'no-store'
    });
    return res.ok ? await res.json() as UsuarioContexto : null;
  } catch {
    return null;
  }
}

export async function encerrarLoginOidc(token?: string): Promise<void> {
  if (token) {
    try {
      await fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } });
    } catch {
      // A sessão local já foi removida; o provedor pode estar indisponível.
    }
  }
  try {
    const config = await configuracao();
    const url = new URL(config.endSessionEndpoint);
    url.searchParams.set('client_id', config.clientId);
    url.searchParams.set('post_logout_redirect_uri', config.redirectUri);
    window.location.assign(url.toString());
  } catch {
    window.location.assign('/');
  }
}
