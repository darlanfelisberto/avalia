import type { EstadoPreenchimento, ManifestacaoDocente, EncaminhamentoGestao, InstrumentoAvaliativo } from '../types/avaliacao';
import { instrumentoADPDMock } from '../data/mockInstrumento';

const STORAGE_KEY_STATE = 'iffar_avalia_estado_preenchimento';
const STORAGE_KEY_MANIFESTACAO = 'iffar_avalia_manifestacoes_docentes';
const STORAGE_KEY_ENCAMINHAMENTO = 'iffar_avalia_encaminhamentos_gestao';
const STORAGE_KEY_INSTRUMENTO = 'iffar_avalia_instrumento_ativo';

export function salvarEstadoPreenchimento(estado: EstadoPreenchimento): void {
  try {
    estado.ultimaModificacao = new Date().toISOString();
    localStorage.setItem(STORAGE_KEY_STATE, JSON.stringify(estado));
  } catch (e) {
    console.error('Falha ao salvar estado no localStorage', e);
  }
}

export function carregarEstadoPreenchimento(instrumentoId: string, primeiroComponenteId: string): EstadoPreenchimento {
  try {
    const salvo = localStorage.getItem(STORAGE_KEY_STATE);
    if (salvo) {
      const parsed = JSON.parse(salvo);
      if (parsed.instrumentoId === instrumentoId) {
        return parsed;
      }
    }
  } catch (e) {
    console.warn('Erro ao restaurar estado do localStorage, iniciando novo', e);
  }

  return {
    instrumentoId,
    respostas: {},
    componenteAtivoId: primeiroComponenteId,
    etapaAtual: 'autoavaliacao',
    ultimaModificacao: new Date().toISOString(),
    status: 'EM_ANDAMENTO'
  };
}

export function limparEstadoPreenchimento(): void {
  localStorage.removeItem(STORAGE_KEY_STATE);
}

export function salvarManifestacaoDocente(manifestacao: ManifestacaoDocente): void {
  try {
    const atuais = carregarManifestacoesDocentes();
    atuais[manifestacao.componenteId] = manifestacao;
    localStorage.setItem(STORAGE_KEY_MANIFESTACAO, JSON.stringify(atuais));
  } catch (e) {
    console.error('Erro ao salvar manifestacao', e);
  }
}

export function carregarManifestacoesDocentes(): Record<string, ManifestacaoDocente> {
  try {
    const salvo = localStorage.getItem(STORAGE_KEY_MANIFESTACAO);
    return salvo ? JSON.parse(salvo) : {};
  } catch {
    return {};
  }
}

export function salvarEncaminhamentoGestao(encaminhamento: EncaminhamentoGestao): void {
  try {
    const atuais = carregarEncaminhamentosGestao();
    atuais.push(encaminhamento);
    localStorage.setItem(STORAGE_KEY_ENCAMINHAMENTO, JSON.stringify(atuais));
  } catch (e) {
    console.error('Erro ao salvar encaminhamento', e);
  }
}

export function carregarEncaminhamentosGestao(): EncaminhamentoGestao[] {
  try {
    const salvo = localStorage.getItem(STORAGE_KEY_ENCAMINHAMENTO);
    return salvo ? JSON.parse(salvo) : [];
  } catch {
    return [];
  }
}

export function salvarInstrumento(instrumento: InstrumentoAvaliativo): void {
  try {
    localStorage.setItem(STORAGE_KEY_INSTRUMENTO, JSON.stringify(instrumento));
  } catch (e) {
    console.error('Erro ao salvar instrumento', e);
  }
}

export function carregarInstrumentoAtivo(): InstrumentoAvaliativo {
  try {
    const salvo = localStorage.getItem(STORAGE_KEY_INSTRUMENTO);
    if (salvo) {
      return JSON.parse(salvo);
    }
  } catch (e) {
    console.warn('Erro ao carregar instrumento salvo, usando padrão', e);
  }
  return JSON.parse(JSON.stringify(instrumentoADPDMock));
}

export function resetarInstrumentoParaPadrao(): InstrumentoAvaliativo {
  localStorage.removeItem(STORAGE_KEY_INSTRUMENTO);
  return JSON.parse(JSON.stringify(instrumentoADPDMock));
}

