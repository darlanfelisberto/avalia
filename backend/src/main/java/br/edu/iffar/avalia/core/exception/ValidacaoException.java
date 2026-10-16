package br.edu.iffar.avalia.core.exception;

/** Exceção padronizada para rejeitar dados ou operações que não atendem às regras de validação. */
public class ValidacaoException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public ValidacaoException(String mensagem) {
        super(mensagem);
    }

    public ValidacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
