export type PapelUsuario = 'CPA' | 'DOCENTE' | 'DISCENTE' | 'TAE' | 'ADMIN';

export type PermissaoUsuario =
  | 'INSTRUMENTO_CRIAR'
  | 'INSTRUMENTO_EDITAR'
  | 'INSTRUMENTO_EXCLUIR'
  | 'INSTRUMENTO_VISUALIZAR'
  | 'AVALIACAO_RESPONDER'
  | 'SIGAA_SINCRONIZAR';

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
}

export interface AuthResponseDTO {
  autenticado: boolean;
  mensagem: string;
  usuario?: UsuarioContexto;
}
