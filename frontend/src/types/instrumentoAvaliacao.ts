export type StatusInstrumento = 'RASCUNHO' | 'PUBLICADO' | 'ENCERRADO' | 'CONSOLIDADO';
export type TipoQuestaoAv = 'ESCOLHA_UNICA' | 'MULTIPLA_ESCOLHA' | 'ESCALA' | 'DESCRITIVA';

export interface TipoEntidade {
  id: string;
  codigo: string;
  nome: string;
  podeSerAvaliada: boolean;
  podeResponder: boolean;
}

export interface OpcaoAv {
  id?: string;
  texto: string;
  valor?: number | null;
  correta: boolean;
  ordem?: number;
}

export interface ConjuntoOpcao {
  id?: string;
  nome: string;
  opcoes: OpcaoAv[];
}

export interface RespondenteAv {
  tipoEntidadeId: string;
  tipoVinculo?: string | null;
}

export interface QuestaoAv {
  questaoId?: string;
  sessaoQuestaoId?: string;
  enunciado: string;
  tipo: TipoQuestaoAv;
  assunto?: string | null;
  conjuntoOpcaoId?: string | null;
  obrigatoria: boolean;
  peso?: number | null;
  ordem?: number;
}

export interface SessaoAv {
  id?: string;
  titulo: string;
  descricao?: string | null;
  questoes: QuestaoAv[];
}

export interface PaginaAv {
  id?: string;
  titulo: string;
  descricao?: string | null;
}

export interface InstrumentoAv {
  id?: string;
  tipoEntidadeAvaliadaId: string;
  tipoEntidadeAvaliadaNome?: string;
  /** Tipos de entidade avaliados; em instrumentos legados, omitido significa o tipo principal. */
  tiposEntidadeAlvoIds?: string[];
  tiposEntidadeAlvoNomes?: string[];
  titulo: string;
  descricao?: string | null;
  cicloReferencia: string;
  /** ISO-8601; no formulário é convertido de/para datetime-local */
  inicioAplicacao: string;
  fimAplicacao: string;
  anonimo: boolean;
  permiteEdicao: boolean;
  status?: StatusInstrumento;
  totalQuestoes?: number;
  respondentes: RespondenteAv[];
  paginas: PaginaAv[];
  sessoes: SessaoAv[];
}
