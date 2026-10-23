export type StatusInstrumento = 'RASCUNHO' | 'PUBLICADO' | 'ENCERRADO' | 'CONSOLIDADO';
export type TipoQuestaoDTO = 'ESCOLHA_UNICA' | 'MULTIPLA_ESCOLHA' | 'ESCALA' | 'DESCRITIVA';

export interface TipoEntidadeDTO {
  id: string;
  codigo: string;
  nome: string;
  podeSerAvaliada: boolean;
  podeResponder: boolean;
}

export interface TipoEntidadeAvaliadaDTO extends Omit<TipoEntidadeDTO, 'id'> {
  tipoEntidadeAvaliadaId: string;
}

export interface PermissaoTipoEntidadeDTO {
  tipoRespondenteId: string;
  tipoAvaliadoId: string;
}

export interface OpcaoQuestaoDTO {
  id?: string;
  texto: string;
  valor?: number | null;
  correta: boolean;
  ordem?: number;
}

export interface ConjuntoOpcaoDTO {
  id?: string;
  nome: string;
  reutilizavel?: boolean;
  opcoes: OpcaoQuestaoDTO[];
}

export interface InstrumentoTipoRespondenteDTO {
  tipoEntidadeId: string;
  tipoVinculo?: string | null;
}

export interface SessaoQuestaoDTO {
  questaoId?: string;
  sessaoQuestaoId?: string;
  enunciado: string;
  tipo: TipoQuestaoDTO;
  assunto?: string | null;
  conjuntoOpcaoId?: string | null;
  opcoes?: OpcaoQuestaoDTO[];
  obrigatoria: boolean;
  peso?: number | null;
  ordem?: number;
}

export interface InstrumentoSessaoDTO {
  id?: string;
  titulo: string;
  descricao?: string | null;
  questoes: SessaoQuestaoDTO[];
}

export interface InstrumentoSessaoPaginaDTO {
  id?: string;
  titulo: string;
  descricao?: string | null;
}

export interface InstrumentoAvaliativoDTO {
  id?: string;
  tipoEntidadeAvaliada: TipoEntidadeAvaliadaDTO;
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
  respondentes: InstrumentoTipoRespondenteDTO[];
  paginas: InstrumentoSessaoPaginaDTO[];
  sessoes: InstrumentoSessaoDTO[];
}
