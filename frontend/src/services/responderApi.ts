import type { ConjuntoOpcaoDTO, InstrumentoAvaliativoDTO } from '../types/instrumentoAvaliacao';

export interface RespostaEnvio {
  alvoId: string;
  contextoId?: string | null;
  respostas: { sessaoQuestaoId: string; opcaoIds: string[]; texto?: string }[];
}

async function chamar<T>(caminho: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api/avaliacao/responder${caminho}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers
    }
  });
  if (!res.ok) throw new Error((await res.text()) || `Erro HTTP ${res.status}`);
  return res.status === 204 ? undefined as T : res.json();
}

export const responderApi = {
  listar: () => chamar<InstrumentoAvaliativoDTO[]>(''),
  formulario: (instrumentoId: string) =>
    chamar<InstrumentoAvaliativoDTO>(`/${encodeURIComponent(instrumentoId)}/formulario`),
  buscar: (instrumentoId: string, alvoId: string, contextoId?: string | null) =>
    chamar<InstrumentoAvaliativoDTO>(`/${encodeURIComponent(instrumentoId)}/${encodeURIComponent(alvoId)}${contextoId ? `?contextoId=${encodeURIComponent(contextoId)}` : ''}`),
  conjuntos: (instrumentoId: string, alvoId: string, contextoId?: string | null) =>
    chamar<ConjuntoOpcaoDTO[]>(`/${encodeURIComponent(instrumentoId)}/${encodeURIComponent(alvoId)}/conjuntos-opcao${contextoId ? `?contextoId=${encodeURIComponent(contextoId)}` : ''}`),
  enviar: (instrumentoId: string, dados: RespostaEnvio) =>
    chamar<void>(`/${encodeURIComponent(instrumentoId)}/envios`, {
      method: 'POST', body: JSON.stringify(dados)
    })
};
