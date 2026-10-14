import type { InstrumentoAv } from '../types/instrumentoAvaliacao';
import { carregarTokenAtivo, carregarUsuarioAtivo } from './storage';

export interface AvaliacaoDisponivel {
  instrumentoId: string;
  titulo: string;
  descricao?: string | null;
  modulo: string;
  alvoId: string;
  alvoNome: string;
  respondido: boolean;
}

export interface RespostaEnvio {
  alvoId: string;
  respostas: { sessaoQuestaoId: string; opcaoIds: string[]; texto?: string }[];
}

async function chamar<T>(caminho: string, init?: RequestInit): Promise<T> {
  const usuario = carregarUsuarioAtivo();
  const token = carregarTokenAtivo();
  if (!usuario || !token) throw new Error('Entre novamente para responder avaliações.');
  const res = await fetch(`/api/avaliacao/responder${caminho}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      'X-Papel-Ativo': usuario.papelAtivo || usuario.papel,
      ...init?.headers
    }
  });
  if (!res.ok) throw new Error((await res.text()) || `Erro HTTP ${res.status}`);
  return res.status === 204 ? undefined as T : res.json();
}

export const responderApi = {
  listar: () => chamar<AvaliacaoDisponivel[]>(''),
  buscar: (instrumentoId: string, alvoId: string) =>
    chamar<InstrumentoAv>(`/${encodeURIComponent(instrumentoId)}/${encodeURIComponent(alvoId)}`),
  enviar: (instrumentoId: string, dados: RespostaEnvio) =>
    chamar<void>(`/${encodeURIComponent(instrumentoId)}/envios`, {
      method: 'POST', body: JSON.stringify(dados)
    })
};
