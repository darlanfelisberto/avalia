// --- Integração Acadêmica SIGAA ---

export interface SincronizacaoStatus {
  conectadoSigaa: boolean;
  urlSigaa: string;
  usuarioSigaa: string;
  emExecucao: boolean;
  contadoresLocais: Record<string, number>;
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
