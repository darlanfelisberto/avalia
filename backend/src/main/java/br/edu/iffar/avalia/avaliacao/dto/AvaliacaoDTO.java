package br.edu.iffar.avalia.avaliacao.dto;

import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;

import java.util.ArrayList;
import java.util.List;

/**
 * DTOs do schema "avaliacao" (páginas e sessões do instrumento; questões nas sessões). Datas em ISO-8601 (UTC).
 */
public final class AvaliacaoDTO {

    private AvaliacaoDTO() {
    }

    public static class Opcao {
        public String id;
        public String texto;
        public Double valor;
        public boolean correta;
        public int ordem;
    }

    public static class ConjuntoOpcao {
        public String id;
        public String nome;
        public List<Opcao> opcoes = new ArrayList<>();
    }

    public static class InstrumentoTipoRespondente {
        public String tipoEntidadeId;
        public String tipoVinculo;
    }

    public static class Respondente extends InstrumentoTipoRespondente {
    }

    public static class Questao {
        public String questaoId;
        public String sessaoQuestaoId;
        public String enunciado;
        /**
         * ESCOLHA_UNICA, MULTIPLA_ESCOLHA, ESCALA ou DESCRITIVA
         */
        public String tipo;
        public String assunto;
        public String conjuntoOpcaoId;
        public boolean obrigatoria;
        public Double peso;
        public int ordem;
    }

    public static class Sessao {
        public String id;
        public String titulo;
        public String descricao;
        public int ordem;
        public List<Questao> questoes = new ArrayList<>();
    }

    public static class Pagina {
        public String id;
        public String titulo;
        public String descricao;
        public int ordem;
    }

    public static class InstrumentoAvaliativoDTO {
        public String id;
        public String tipoEntidadeAvaliadaId;
        public String tipoEntidadeAvaliadaNome;
        public List<String> tiposEntidadeAlvoIds = new ArrayList<>();
        public List<String> tiposEntidadeAlvoNomes = new ArrayList<>();
        public String titulo;
        public String descricao;
        public String cicloReferencia;
        public String inicioAplicacao;
        public String fimAplicacao;
        public boolean anonimo;
        public boolean permiteEdicao = true;
        /**
         * RASCUNHO, PUBLICADO, ENCERRADO ou CONSOLIDADO
         */
        public String status;
        public String criadoPor;
        public String atualizadoEm;
        public int totalQuestoes;
        public List<InstrumentoTipoRespondente> respondentes = new ArrayList<>();
        public List<Pagina> paginas = new ArrayList<>();
        public List<Sessao> sessoes = new ArrayList<>();

        public static InstrumentoAvaliativoDTO create(InstrumentoAvaliativo entidade) {
            InstrumentoAvaliativoDTO i = new InstrumentoAvaliativoDTO();
            i.id = entidade.getId().toString();
            i.tipoEntidadeAvaliadaId = entidade.getTipoEntidadeAvaliada().getId().toString();
            i.tipoEntidadeAvaliadaNome = entidade.getTipoEntidadeAvaliada().getNome();
            i.titulo = entidade.getTitulo();
            i.cicloReferencia = entidade.getCicloReferencia();
            i.status = entidade.getStatus();
            i.inicioAplicacao = entidade.getInicioAplicacao().toString();
            i.fimAplicacao = entidade.getFimAplicacao().toString();
            i.anonimo = entidade.isAnonimo();
            i.permiteEdicao = entidade.isPermiteEdicao();
            i.criadoPor = entidade.getCriadoPor();
            i.atualizadoEm = entidade.getAtualizadoEm().toString();
            return i;
        }
    }
}
