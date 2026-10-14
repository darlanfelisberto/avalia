export type TipoQuestao = 'ESCALA' | 'ESCOLHA_UNICA' | 'MULTIPLA_ESCOLHA' | 'TEXTO_LIVRE';


export interface OpcaoResposta {
  id: string;
  opcaoQuestaoId?: string;
  texto: string;
  valor?: number;
}

export interface Questao {
  id: string;
  questaoId?: string;
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
  grupoQuestoesId?: string;
  titulo: string;
  descricao?: string;
  questoes: Questao[];
}

export interface ComponenteCurricular {
  id: string;
  componenteCurricularId?: string;
  codigo: string;
  nome: string;
  docenteNome: string;
  turma: string;
}

export type TipoAlvoAvaliado = 'INSTITUICAO' | 'CAMPUS' | 'SETOR' | 'DOCENTE' | 'SERVIDOR' | 'CURSO' | 'TURMA' | 'AUTOAVALIACAO';

export type TipoPublicoAvaliador = 'DISCENTES' | 'DOCENTES' | 'TAES' | 'EGRESSOS' | 'SOCIEDADE_CIVIL' | 'TODOS';

export type PapelUsuario = 'CPA' | 'DOCENTE' | 'DISCENTE' | 'GESTAO' | 'TAE' | 'ADMIN';

export type PermissaoUsuario =
  | 'INSTRUMENTO_CRIAR'
  | 'INSTRUMENTO_EDITAR'
  | 'INSTRUMENTO_EXCLUIR'
  | 'INSTRUMENTO_VISUALIZAR'
  | 'AVALIACAO_RESPONDER'
  | 'DOCENTE_MANIFESTAR'
  | 'GESTAO_ENCAMINHAR'
  | 'SIGAA_SINCRONIZAR'
  | 'AUDITORIA_ACESSAR';

export interface UsuarioContexto {
  id?: string;
  cpf: string;
  cpfFormatado?: string;
  nome: string;
  email?: string;
  campus: string;
  papel: PapelUsuario;
  papelAtivo?: PapelUsuario;
  papeis: PapelUsuario[];
  permissoes: PermissaoUsuario[];
  token?: string;
}

export interface AuthResponseDTO {
  autenticado: boolean;
  token?: string;
  mensagem: string;
  usuario?: UsuarioContexto;
}

export interface InstrumentoAvaliativo {
  id: string;
  instrumentoId?: string;
  titulo: string;
  subtitulo: string;
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
  anonimo: boolean;
  status?: string;
  gruposAutoavaliacaoDiscente: GrupoQuestoes[];
  gruposAvaliacaoDocente: GrupoQuestoes[];
  componentesMatriculados: ComponenteCurricular[];
}

export interface EstadoPreenchimento {
  instrumentoId: string;
  respostas: Record<string, unknown>;
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
