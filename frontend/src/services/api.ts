import type { InstrumentoAvaliativo } from '../types/avaliacao';
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

