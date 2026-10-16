package br.edu.iffar.avalia.avaliacao.dto;

import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.TipoSessao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        public static Opcao create(br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao entidade) {
            Opcao dto = new Opcao();
            dto.id = entidade.getId().toString();
            dto.texto = entidade.getTexto();
            dto.valor = entidade.getValor() == null ? null : entidade.getValor().doubleValue();
            dto.correta = entidade.isCorreta();
            dto.ordem = entidade.getOrdem();
            return dto;
        }
    }

    public static class ConjuntoOpcao {
        public String id;
        public String nome;
        public List<Opcao> opcoes = new ArrayList<>();

        public static ConjuntoOpcao create(br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao entidade) {
            ConjuntoOpcao dto = new ConjuntoOpcao();
            dto.id = entidade.getId().toString();
            dto.nome = entidade.getNome();
            for (var opcao : entidade.getOpcoes()) dto.opcoes.add(Opcao.create(opcao));
            return dto;
        }
    }

    public static class InstrumentoTipoRespondente {
        public String tipoEntidadeId;
        public String tipoVinculo;

        public static InstrumentoTipoRespondente create(br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoRespondente entidade) {
            InstrumentoTipoRespondente dto = new InstrumentoTipoRespondente();
            dto.tipoEntidadeId = entidade.getTipoEntidade().getId().toString();
            return dto;
        }
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

        public static Questao create(SessaoQuestao entidade) {
            var questao = entidade.getQuestao();
            Questao dto = new Questao();
            dto.questaoId = questao.getId().toString();
            dto.sessaoQuestaoId = entidade.getId().toString();
            dto.enunciado = questao.getEnunciado();
            dto.tipo = questao.getTipo();
            dto.assunto = questao.getAssunto();
            dto.conjuntoOpcaoId = questao.getConjuntoOpcao() == null ? null : questao.getConjuntoOpcao().getId().toString();
            dto.obrigatoria = entidade.isObrigatoria();
            dto.peso = entidade.getPeso() == null ? null : entidade.getPeso().doubleValue();
            dto.ordem = entidade.getOrdem();
            return dto;
        }
    }

    public static class Sessao {
        public String id;
        public String titulo;
        public String descricao;
        public int ordem;
        public List<Questao> questoes = new ArrayList<>();

        public static Sessao create(InstrumentoSessao entidade) {
            Sessao dto = new Sessao();
            dto.id = entidade.getId().toString();
            dto.titulo = entidade.getTitulo();
            dto.descricao = entidade.getDescricao();
            dto.ordem = entidade.getOrdem();
            for (SessaoQuestao questao : entidade.getSessaoQuestoesList()) dto.questoes.add(Questao.create(questao));
            return dto;
        }
    }

    public static class Pagina {
        public String id;
        public String titulo;
        public String descricao;
        public int ordem;

        public static Pagina create(InstrumentoSessao entidade) {
            Pagina dto = new Pagina();
            dto.id = entidade.getId().toString();
            dto.titulo = entidade.getTitulo();
            dto.descricao = entidade.getDescricao();
            dto.ordem = entidade.getOrdem();
            return dto;
        }
    }

    public static class InstrumentoAvaliativoDTO {
        public String id;
        public String tipoEntidadeAvaliadaId;
        public String tipoEntidadeAvaliadaNome;
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

        public InstrumentoAvaliativoDTO createResumo(InstrumentoAvaliativo ia) {
            return comDadosBasicos(ia);
        }

        public InstrumentoAvaliativoDTO createDetalhe(InstrumentoAvaliativo ia) {
            return createResumo(ia)
                    .comRespondentes(ia)
                    .comPaginas(ia)
                    .comSessoes(ia);
        }

        public InstrumentoAvaliativoDTO comDadosBasicos(InstrumentoAvaliativo ia) {
            var tipoAvaliado = ia.getTipoEntidadeAvaliada();
            id = ia.getId().toString();
            tipoEntidadeAvaliadaId = tipoAvaliado.getId().toString();
            tipoEntidadeAvaliadaNome = tipoAvaliado.getNome();
            titulo = ia.getTitulo();
            descricao = ia.getDescricao();
            cicloReferencia = ia.getCicloReferencia();
            status = ia.getStatus();
            inicioAplicacao = ia.getInicioAplicacao().toString();
            fimAplicacao = ia.getFimAplicacao().toString();
            anonimo = ia.isAnonimo();
            permiteEdicao = ia.isPermiteEdicao();
            criadoPor = ia.getCriadoPor();
            atualizadoEm = ia.getAtualizadoEm().toString();
            return this;
        }

        public InstrumentoAvaliativoDTO comRespondentes(InstrumentoAvaliativo ia) {
            respondentes.clear();
            for (var respondente : ia.getInstrumentoTipoRespondenteList()) {
                respondentes.add(InstrumentoTipoRespondente.create(respondente));
            }
            return this;
        }

        public InstrumentoAvaliativoDTO comPaginas(InstrumentoAvaliativo ia) {
            paginas.clear();
            for (InstrumentoSessao sessao : ia.getInstrumentoSessoesList()) {
                if (sessao.getTipoSessao() == TipoSessao.PAGINA) {
                    paginas.add(Pagina.create(sessao));
                }
            }
            return this;
        }

        public InstrumentoAvaliativoDTO comSessoes(InstrumentoAvaliativo ia) {
            sessoes.clear();
            totalQuestoes = 0;
            for (InstrumentoSessao sessao : ia.getInstrumentoSessoesList()) {
                if (sessao.getTipoSessao() != TipoSessao.PAGINA) {
                    Sessao sessaoDto = Sessao.create(sessao);
                    totalQuestoes += sessaoDto.questoes.size();
                    sessoes.add(sessaoDto);
                }
            }
            return this;
        }
    }
}
