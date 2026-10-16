import type { ConjuntoOpcaoDTO, InstrumentoAvaliativoDTO, PermissaoTipoEntidadeDTO, TipoEntidadeDTO } from '../types/instrumentoAvaliacao';
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
  tiposEntidade: () => chamar<TipoEntidadeDTO[]>(`${BASE}/tipos-entidade`),
  permissoesTiposEntidade: () => chamar<PermissaoTipoEntidadeDTO[]>(`${BASE}/permissoes-tipos-entidade`),
  salvarPermissoesTiposEntidade: (permissoes: PermissaoTipoEntidadeDTO[]) =>
    chamar<void>(`${BASE}/permissoes-tipos-entidade`, { method: 'PUT', body: JSON.stringify(permissoes) }),
  conjuntos: () => chamar<ConjuntoOpcaoDTO[]>(`${BASE}/conjuntos-opcao`),
  salvarConjunto: (c: ConjuntoOpcaoDTO) => chamar<ConjuntoOpcaoDTO>(`${BASE}/conjuntos-opcao`, { method: 'POST', body: JSON.stringify(c) }),
  listar: (pagina = 1, tamanho = 10) => chamar<InstrumentoAvaliativoDTO[]>(`${BASE}/instrumentos?pagina=${pagina}&tamanho=${tamanho}`),
  buscar: (id: string) => chamar<InstrumentoAvaliativoDTO>(`${BASE}/instrumentos/${id}`),
  salvar: (i: InstrumentoAvaliativoDTO, usuario?: string) =>
    chamar<InstrumentoAvaliativoDTO>(`${BASE}/instrumentos`, {
      method: 'POST',
      body: JSON.stringify(i),
      headers: usuario ? { 'X-Usuario': usuario } : {}
    }),
  publicar: (id: string) => chamar<InstrumentoAvaliativoDTO>(`${BASE}/instrumentos/${id}/publicar`, { method: 'POST' }),
  excluir: (id: string) => chamar<void>(`${BASE}/instrumentos/${id}`, { method: 'DELETE' })
};
