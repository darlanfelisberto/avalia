package br.edu.iffar.avalia.avaliacao.dto;

import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.TipoSessao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTOs do schema "avaliacao" (páginas e sessões do instrumento; questões nas sessões). Datas em ISO-8601 (UTC).
 */
public final class AvaliacaoDTO {

    private AvaliacaoDTO() {
    }

    public static class OpcaoQuestaoDTO {
        public String id;
        @NotBlank(message = "Toda opção precisa de um texto.")
        @Size(max = 300, message = "O texto da opção deve ter até 300 caracteres.")
        public String texto;
        public Double valor;
        public boolean correta;
        public int ordem;

        public static OpcaoQuestaoDTO create(br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao entidade) {
            OpcaoQuestaoDTO dto = new OpcaoQuestaoDTO();
            dto.id = entidade.getId().toString();
            dto.texto = entidade.getTexto();
            dto.valor = entidade.getValor() == null ? null : entidade.getValor().doubleValue();
            dto.correta = entidade.isCorreta();
            dto.ordem = entidade.getOrdem();
            return dto;
        }
    }

    public static class ConjuntoOpcaoDTO {
        public String id;
        @NotBlank(message = "Informe o nome do conjunto de opções.")
        @Size(max = 100, message = "O nome do conjunto deve ter até 100 caracteres.")
        public String nome;
        public boolean reutilizavel;
        @NotNull(message = "Informe as opções do conjunto.")
        @Size(min = 2, message = "Um conjunto precisa de ao menos duas opções.")
        @Valid
        public List<@NotNull(message = "Opção inválida.") OpcaoQuestaoDTO> opcoes = new ArrayList<>();

        public static ConjuntoOpcaoDTO create(br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao entidade) {
            ConjuntoOpcaoDTO dto = new ConjuntoOpcaoDTO();
            dto.id = entidade.getId().toString();
            dto.nome = entidade.getNome();
            dto.reutilizavel = !entidade.isExclusivo();
            for (var opcao : entidade.getOpcoes()) dto.opcoes.add(OpcaoQuestaoDTO.create(opcao));
            return dto;
        }
    }

    public static class InstrumentoTipoRespondenteDTO {
        @NotBlank(message = "Respondente sem tipo definido.")
        public String tipoEntidadeId;
        public String tipoVinculo;

        public static InstrumentoTipoRespondenteDTO create(br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoRespondente entidade) {
            InstrumentoTipoRespondenteDTO dto = new InstrumentoTipoRespondenteDTO();
            dto.tipoEntidadeId = entidade.getTipoEntidade().getId().toString();
            return dto;
        }
    }

    public static class SessaoQuestaoDTO {
        public String questaoId;
        public String sessaoQuestaoId;
        @NotBlank(message = "Toda questão precisa de um enunciado.")
        public String enunciado;
        /**
         * ESCOLHA_UNICA, MULTIPLA_ESCOLHA, ESCALA ou DESCRITIVA
         */
        @NotBlank(message = "Informe o tipo da questão.")
        @Pattern(regexp = "ESCOLHA_UNICA|MULTIPLA_ESCOLHA|ESCALA|DESCRITIVA", message = "Tipo de questão inválido.")
        public String tipo;
        @Size(max = 100, message = "O assunto deve ter até 100 caracteres.")
        public String assunto;
        public String conjuntoOpcaoId;
        @Valid
        public List<@NotNull(message = "Opção inválida.") OpcaoQuestaoDTO> opcoes = new ArrayList<>();
        public boolean obrigatoria;
        public Double peso;
        public int ordem;

        public static SessaoQuestaoDTO create(SessaoQuestao entidade) {
            var questao = entidade.getQuestao();
            SessaoQuestaoDTO dto = new SessaoQuestaoDTO();
            dto.questaoId = questao.getId().toString();
            dto.sessaoQuestaoId = entidade.getId().toString();
            dto.enunciado = questao.getEnunciado();
            dto.tipo = questao.getTipo();
            dto.assunto = questao.getAssunto();
            var conjunto = questao.getConjuntoOpcao();
            if (conjunto != null) {
                if (!conjunto.isExclusivo()) {
                    dto.conjuntoOpcaoId = conjunto.getId().toString();
                }
                for (var opcao : conjunto.getOpcoes()) dto.opcoes.add(OpcaoQuestaoDTO.create(opcao));
            }
            dto.obrigatoria = entidade.isObrigatoria();
            dto.peso = entidade.getPeso() == null ? null : entidade.getPeso().doubleValue();
            dto.ordem = entidade.getOrdem();
            return dto;
        }
    }

    public static class InstrumentoSessaoDTO {
        public String id;
        @NotBlank(message = "Toda sessão ou página precisa de um título.")
        @Size(max = 200, message = "O título da sessão ou página deve ter até 200 caracteres.")
        public String titulo;
        public String descricao;
        public int ordem;
        @Valid
        public List<@NotNull(message = "Questão inválida.") SessaoQuestaoDTO> questoes = new ArrayList<>();

        public static InstrumentoSessaoDTO create(InstrumentoSessao entidade) {
            InstrumentoSessaoDTO dto = new InstrumentoSessaoDTO();
            dto.id = entidade.getId().toString();
            dto.titulo = entidade.getTitulo();
            dto.descricao = entidade.getDescricao();
            dto.ordem = entidade.getOrdem();
            for (SessaoQuestao questao : entidade.getSessaoQuestoesList()) dto.questoes.add(SessaoQuestaoDTO.create(questao));
            return dto;
        }
    }

    public static class InstrumentoSessaoPaginaDTO {
        public String id;
        @NotBlank(message = "Toda sessão ou página precisa de um título.")
        @Size(max = 200, message = "O título da sessão ou página deve ter até 200 caracteres.")
        public String titulo;
        public String descricao;
        public int ordem;

        public static InstrumentoSessaoPaginaDTO create(InstrumentoSessao entidade) {
            InstrumentoSessaoPaginaDTO dto = new InstrumentoSessaoPaginaDTO();
            dto.id = entidade.getId().toString();
            dto.titulo = entidade.getTitulo();
            dto.descricao = entidade.getDescricao();
            dto.ordem = entidade.getOrdem();
            return dto;
        }
    }

    public static class InstrumentoAvaliativoDTO {
        public String id;
        @NotNull(message = "Informe o que será avaliado.")
        @Valid
        public TipoEntidadeAvaliadaDTO tipoEntidadeAvaliada;
        @NotBlank(message = "Informe o título do instrumento.")
        @Size(max = 200, message = "O título do instrumento deve ter até 200 caracteres.")
        public String titulo;
        public String descricao;
        @NotBlank(message = "Informe o ciclo de referência.")
        @Size(max = 20, message = "O ciclo de referência deve ter até 20 caracteres.")
        public String cicloReferencia;
        @NotBlank(message = "Informe o início da aplicação.")
        public String inicioAplicacao;
        @NotBlank(message = "Informe o término da aplicação.")
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
        @Valid
        public List<@NotNull(message = "Público respondente inválido.") InstrumentoTipoRespondenteDTO> respondentes = new ArrayList<>();
        @Valid
        public List<@NotNull(message = "Página inválida.") InstrumentoSessaoPaginaDTO> paginas = new ArrayList<>();
        @Valid
        public List<@NotNull(message = "Sessão inválida.") InstrumentoSessaoDTO> sessoes = new ArrayList<>();

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
            tipoEntidadeAvaliada = TipoEntidadeAvaliadaDTO.create(tipoAvaliado);
            titulo = ia.getTitulo();
            descricao = ia.getDescricao();
            cicloReferencia = ia.getCicloReferencia();
            status = ia.getStatus().name();
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
                respondentes.add(InstrumentoTipoRespondenteDTO.create(respondente));
            }
            return this;
        }

        public InstrumentoAvaliativoDTO comPaginas(InstrumentoAvaliativo ia) {
            paginas.clear();
            for (InstrumentoSessao sessao : ia.getInstrumentoSessoesList()) {
                if (sessao.getTipoSessao() == TipoSessao.PAGINA) {
                    paginas.add(InstrumentoSessaoPaginaDTO.create(sessao));
                }
            }
            return this;
        }

        public InstrumentoAvaliativoDTO comSessoes(InstrumentoAvaliativo ia) {
            sessoes.clear();
            totalQuestoes = 0;
            for (InstrumentoSessao sessao : ia.getInstrumentoSessoesList()) {
                if (sessao.getTipoSessao() != TipoSessao.PAGINA) {
                    InstrumentoSessaoDTO sessaoDto = InstrumentoSessaoDTO.create(sessao);
                    totalQuestoes += sessaoDto.questoes.size();
                    sessoes.add(sessaoDto);
                }
            }
            return this;
        }
    }

    public static class TipoEntidadeAvaliadaDTO {
        @NotBlank(message = "Informe o que será avaliado.")
        public String tipoEntidadeAvaliadaId;
        public String codigo;
        public String nome;
        public boolean podeSerAvaliada;
        public boolean podeResponder;

        public static TipoEntidadeAvaliadaDTO create(br.edu.iffar.avalia.avaliacao.model.TipoEntidade entidade) {
            TipoEntidadeAvaliadaDTO dto = new TipoEntidadeAvaliadaDTO();
            dto.tipoEntidadeAvaliadaId = entidade.getId().toString();
            dto.codigo = entidade.getCodigo();
            dto.nome = entidade.getNome();
            dto.podeSerAvaliada = entidade.isPodeSerAvaliada();
            dto.podeResponder = entidade.isPodeResponder();
            return dto;
        }
    }
}
