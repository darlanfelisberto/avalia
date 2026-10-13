import type { InstrumentoAvaliativo } from '../types/avaliacao';
import { salvarInstrumento, carregarInstrumentoAtivo } from './storage';

const API_BASE = '/api/instrumentos';

export async function listarInstrumentosApi(): Promise<InstrumentoAvaliativo[]> {
  try {
    const res = await fetch(API_BASE);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
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

export async function salvarInstrumentoApi(instrumento: InstrumentoAvaliativo): Promise<InstrumentoAvaliativo> {
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
      return salvo;
    }
  } catch (e) {
    console.warn('Falha na chamada REST com Open Liberty, mantido no storage local', e);
  }

  return instrumento;
}
