import type { ConjuntoOpcao, InstrumentoAv, PermissaoTipoEntidadeAv, TipoEntidade } from '../types/instrumentoAvaliacao';
const BASE = '/api/avaliacao';

export interface Resultado<T> {
  ok: boolean;
  dados?: T;
  erro?: string;
}

async function chamar<T>(url: string, init?: RequestInit): Promise<Resultado<T>> {
  try {
    const res = await fetch(url, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers || {})
      }
    });
    if (!res.ok) {
      return { ok: false, erro: (await res.text()) || `HTTP ${res.status}` };
    }
    const texto = await res.text();
    return { ok: true, dados: texto ? JSON.parse(texto) : undefined };
  } catch (e: any) {
    return { ok: false, erro: e?.message || 'Falha de conexão com o servidor' };
  }
}

export const avaliacaoApi = {
  tiposEntidade: () => chamar<TipoEntidade[]>(`${BASE}/tipos-entidade`),
  permissoesTiposEntidade: () => chamar<PermissaoTipoEntidadeAv[]>(`${BASE}/permissoes-tipos-entidade`),
  salvarPermissoesTiposEntidade: (permissoes: PermissaoTipoEntidadeAv[]) =>
    chamar<void>(`${BASE}/permissoes-tipos-entidade`, { method: 'PUT', body: JSON.stringify(permissoes) }),
  conjuntos: () => chamar<ConjuntoOpcao[]>(`${BASE}/conjuntos-opcao`),
  salvarConjunto: (c: ConjuntoOpcao) => chamar<ConjuntoOpcao>(`${BASE}/conjuntos-opcao`, { method: 'POST', body: JSON.stringify(c) }),
  listar: () => chamar<InstrumentoAv[]>(`${BASE}/instrumentos`),
  buscar: (id: string) => chamar<InstrumentoAv>(`${BASE}/instrumentos/${id}`),
  salvar: (i: InstrumentoAv, usuario?: string) =>
    chamar<InstrumentoAv>(`${BASE}/instrumentos`, {
      method: 'POST',
      body: JSON.stringify(i),
      headers: usuario ? { 'X-Usuario': usuario } : {}
    }),
  publicar: (id: string) => chamar<InstrumentoAv>(`${BASE}/instrumentos/${id}/publicar`, { method: 'POST' }),
  excluir: (id: string) => chamar<void>(`${BASE}/instrumentos/${id}`, { method: 'DELETE' })
};
