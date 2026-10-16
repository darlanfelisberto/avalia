import type { ConjuntoOpcao, InstrumentoAv } from '../types/instrumentoAvaliacao';
import { carregarUsuarioAtivo } from './storage';

export interface AvaliacaoDisponivel {
  instrumentoId: string;
  titulo: string;
  descricao?: string | null;
  alvoId: string;
  alvoNome: string;
  alvoTipoCodigo?: string;
  contextoId?: string | null;
  contextoNome?: string | null;
  respondido: boolean;
}

export interface RespostaEnvio {
  alvoId: string;
  contextoId?: string | null;
  respostas: { sessaoQuestaoId: string; opcaoIds: string[]; texto?: string }[];
}

async function chamar<T>(caminho: string, init?: RequestInit): Promise<T> {
  const usuario = carregarUsuarioAtivo();
  if (!usuario) throw new Error('Entre novamente para responder avaliações.');
  const res = await fetch(`/api/avaliacao/responder${caminho}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      'X-Papel-Ativo': usuario.papelAtivo || usuario.papel,
      ...init?.headers
    }
  });
  if (!res.ok) throw new Error((await res.text()) || `Erro HTTP ${res.status}`);
  return res.status === 204 ? undefined as T : res.json();
}

export const responderApi = {
  listar: () => chamar<AvaliacaoDisponivel[]>(''),
  buscar: (instrumentoId: string, alvoId: string, contextoId?: string | null) =>
    chamar<InstrumentoAv>(`/${encodeURIComponent(instrumentoId)}/${encodeURIComponent(alvoId)}${contextoId ? `?contextoId=${encodeURIComponent(contextoId)}` : ''}`),
  conjuntos: (instrumentoId: string, alvoId: string, contextoId?: string | null) =>
    chamar<ConjuntoOpcao[]>(`/${encodeURIComponent(instrumentoId)}/${encodeURIComponent(alvoId)}/conjuntos-opcao${contextoId ? `?contextoId=${encodeURIComponent(contextoId)}` : ''}`),
  enviar: (instrumentoId: string, dados: RespostaEnvio) =>
    chamar<void>(`/${encodeURIComponent(instrumentoId)}/envios`, {
      method: 'POST', body: JSON.stringify(dados)
    })
};
