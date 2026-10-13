export type TipoQuestao = 'ESCALA' | 'ESCOLHA_UNICA' | 'MULTIPLA_ESCOLHA' | 'TEXTO_LIVRE';

export type ModuloAvaliacao = 'ADPD' | 'SINAES' | 'APRENDIZAGEM';

export interface OpcaoResposta {
  id: string;
  texto: string;
  valor?: number;
}

export interface Questao {
  id: string;
  enunciado: string;
  tipo: TipoQuestao;
  obrigatoria: boolean;
  escalaMin?: number;
  escalaMax?: number;
  rotuloMin?: string;
  rotuloMax?: string;
  opcoes?: OpcaoResposta[];
  ajuda?: string;
}

export interface GrupoQuestoes {
  id: string;
  titulo: string;
  descricao?: string;
  questoes: Questao[];
}

export interface ComponenteCurricular {
  id: string;
  codigo: string;
  nome: string;
  docenteNome: string;
  turma: string;
}

export type TipoAlvoAvaliado = 'INSTITUICAO' | 'CAMPUS' | 'SETOR' | 'DOCENTE' | 'CURSO' | 'TURMA' | 'AUTOAVALIACAO';

export type TipoPublicoAvaliador = 'DISCENTES' | 'DOCENTES' | 'TAES' | 'EGRESSOS' | 'SOCIEDADE_CIVIL' | 'TODOS';

export interface AlvoAvaliado {
  id: string;
  tipo: TipoAlvoAvaliado;
  nome: string;
  codigo?: string;
  descricao?: string;
  responsavelNome?: string;
  campus?: string;
  ativo: boolean;
  createdAt?: string;
}

export interface PublicoAvaliador {
  id: string;
  segmento: string;
  nome: string;
  descricao?: string;
  exigeAutenticacao: boolean;
  ativo: boolean;
  createdAt?: string;
}

export interface UsuarioContexto {
  id: string;
  nome: string;
  papel: 'CPA' | 'DOCENTE' | 'DISCENTE' | 'GESTAO';
  campus: string;
}

export interface InstrumentoAvaliativo {
  id: string;
  titulo: string;
  subtitulo: string;
  modulo: ModuloAvaliacao;
  tipoAlvo: TipoAlvoAvaliado;
  tipoPublico: TipoPublicoAvaliador;
  publicosPermitidos?: TipoPublicoAvaliador[];
  restringirVinculoCampus?: boolean;
  alvoEspecificoId?: string;
  alvosEspecificosIds?: string[];
  criadorId?: string;
  criadorNome?: string;
  anoCiclo: string;
  periodoLetivo: string;
  dataInicio?: string;
  dataTermino: string;
  publicoAlvo?: string;
  anonimo: boolean;
  status?: string;
  gruposAutoavaliacaoDiscente: GrupoQuestoes[];
  gruposAvaliacaoDocente: GrupoQuestoes[];
  componentesMatriculados: ComponenteCurricular[];
}

export interface RespostaItem {
  questaoId: string;
  componenteId?: string; // Opcional se for autoavaliação discente geral
  valorEscala?: number;
  opcaoId?: string;
  opcoesMultiplas?: string[];
  textoLivre?: string;
}

export interface EstadoPreenchimento {
  instrumentoId: string;
  respostas: Record<string, RespostaItem>; // chave composta: `${questaoId}_${componenteId || 'geral'}`
  componenteAtivoId: string;
  etapaAtual: 'autoavaliacao' | 'docentes' | 'finalizacao';
  ultimaModificacao: string;
  status: 'EM_ANDAMENTO' | 'CONCLUIDO';
}

export interface ManifestacaoDocente {
  docenteNome: string;
  componenteId: string;
  comentario: string;
  planoDesenvolvimento: string;
  dataRegistro: string;
}

export interface EncaminhamentoGestao {
  cicloId: string;
  responsavelNome: string;
  papel: 'COORDENADOR' | 'DIRECAO_ENSINO' | 'CPA';
  tipoDecisao: 'PLANO_DE_ACAO' | 'ORIENTACAO_PEDAGOGICA' | 'SEM_ACAO_NECESSARIA' | 'ENCAMINHAMENTO_CPPD';
  detalhamento: string;
  prazoRevisao?: string;
  dataRegistro: string;
}
