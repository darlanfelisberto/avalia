import type { InstrumentoAvaliativo, TipoPublico } from '../types/avaliacao';
import { salvarInstrumento, carregarInstrumentoAtivo } from './storage';

const API_BASE = '/api/instrumentos';

export async function listarInstrumentosApi(criadorId?: string): Promise<InstrumentoAvaliativo[]> {
  try {
    const url = criadorId ? `${API_BASE}?criadorId=${encodeURIComponent(criadorId)}` : API_BASE;
    const res = await fetch(url);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        return data;
      }
    }
  } catch (e) {
    console.warn('API Liberty offline, usando fallback local', e);
  }
  return [carregarInstrumentoAtivo()];
}

export async function buscarInstrumentoApi(id: string): Promise<InstrumentoAvaliativo> {
  try {
    const res = await fetch(`${API_BASE}/${id}`);
    if (res.ok) {
      const data = await res.json();
      salvarInstrumento(data); // atualiza cache local
      return data;
    }
  } catch (e) {
    console.warn('API Liberty offline, usando fallback local', e);
  }
  return carregarInstrumentoAtivo();
}

export async function salvarInstrumentoApi(instrumento: InstrumentoAvaliativo): Promise<{ sucesso: boolean; instrumento: InstrumentoAvaliativo; erro?: string }> {
  // Salva no cache local primeiro para garantia offline
  salvarInstrumento(instrumento);

  try {
    const res = await fetch(API_BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(instrumento)
    });

    if (res.ok) {
      const salvo = await res.json();
      salvarInstrumento(salvo);
      return { sucesso: true, instrumento: salvo };
    } else {
      const errText = await res.text();
      console.error('Erro na resposta da API:', res.status, errText);
      return { sucesso: false, instrumento, erro: `HTTP ${res.status}: ${errText}` };
    }
  } catch (e: any) {
    console.warn('Falha na chamada REST com Open Liberty, mantido no storage local', e);
    return { sucesso: false, instrumento, erro: e?.message || 'Falha de conexão com o servidor' };
  }
}

// --- Catálogo de Alvos e Públicos ---

export async function listarAlvosApi(tipo?: string): Promise<any[]> {
  try {
    const url = tipo ? `/api/catalogo/alvos?tipo=${tipo}` : '/api/catalogo/alvos';
    const res = await fetch(url);
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Erro ao buscar alvos da API', e);
  }
  return [];
}

export async function salvarAlvoApi(alvo: any): Promise<any> {
  try {
    const res = await fetch('/api/catalogo/alvos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(alvo)
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Erro ao salvar alvo na API', e);
  }
  return alvo;
}

export async function listarPublicosApi(): Promise<any[]> {
  try {
    const res = await fetch('/api/catalogo/publicos');
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Erro ao buscar públicos da API', e);
  }
  return [];
}

export async function salvarPublicoApi(publico: any): Promise<any> {
  try {
    const res = await fetch('/api/catalogo/publicos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(publico)
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Erro ao salvar público na API', e);
  }
  return publico;
}

export async function listarTiposPublicoApi(): Promise<TipoPublico[]> {
  try {
    const res = await fetch('/api/catalogo/tipos-publico');
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        return data;
      }
    }
  } catch (e) {
    console.warn('Erro ao buscar tipos de público da API, utilizando fallback local', e);
  }
  return [];
}

export async function salvarTipoPublicoApi(tipoPublico: TipoPublico): Promise<TipoPublico> {
  try {
    const res = await fetch('/api/catalogo/tipos-publico', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(tipoPublico)
    });
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Erro ao salvar tipo de público na API', e);
  }
  return tipoPublico;
}

// --- Integração Acadêmica SIGAA ---

export interface SincronizacaoStatus {
  conectadoSigaa: boolean;
  urlSigaa: string;
  usuarioSigaa: string;
  emExecucao: boolean;
  contadoresLocais: Record<string, number>;
  ultimosLogs: Array<{
    id: number;
    entidade: string;
    inicio: string;
    fim: string;
    registrosImportados: number;
    status: string;
    mensagem: string;
  }>;
}

export async function buscarStatusSincronizacaoApi(): Promise<SincronizacaoStatus | null> {
  try {
    const res = await fetch('/api/sincronizacao/status');
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Erro ao consultar status da sincronização', e);
  }
  return null;
}

export async function executarSincronizacaoApi(params: {
  ano?: number;
  periodo?: number | null;
  sincronizarEstrutura?: boolean;
  sincronizarTurmasMatriculas?: boolean;
}): Promise<{ sucesso: boolean; mensagem: string }> {
  try {
    const res = await fetch('/api/sincronizacao/executar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    });
    if (res.ok) {
      return await res.json();
    }
    const errText = await res.text();
    return { sucesso: false, mensagem: `Erro ${res.status}: ${errText}` };
  } catch (e: any) {
    return { sucesso: false, mensagem: e?.message || 'Falha na comunicação com o servidor' };
  }
}

// --- Autenticação por CPF e Permissões ---

export async function loginPorCpfApi(cpf: string): Promise<import('../types/avaliacao').AuthResponseDTO> {
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cpf })
    });
    const data = await res.json();
    return data;
  } catch (e: any) {
    return {
      autenticado: false,
      mensagem: e?.message || 'Não foi possível conectar ao servidor para autenticação.'
    };
  }
}

export async function listarUsuariosDemonstracaoApi(): Promise<import('../types/avaliacao').UsuarioContexto[]> {
  try {
    const res = await fetch('/api/auth/demonstracao');
    if (res.ok) {
      return await res.json();
    }
  } catch (e) {
    console.warn('Falha ao obter contas de demonstração do servidor', e);
  }
  return [];
}

