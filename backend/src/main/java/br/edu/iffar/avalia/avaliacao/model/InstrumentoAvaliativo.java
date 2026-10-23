package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "instrumento_avaliativo", schema = "avaliacao")
public class InstrumentoAvaliativo extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrumento_avaliativo_id")
    private UUID instrumentoAvaliativoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_avaliativo_origem_id")
    private InstrumentoAvaliativo origem;

    /**
     * é a a entidade que é avaliada, turma setor instituição
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_entidade_avaliada_id", nullable = false)
    @NotNull(message = "Informe o tipo de entidade avaliada.")
    private TipoEntidade tipoEntidadeAvaliada;

    /**
     * é o tipo de entidade que pode responder esse instrumento
     */
    @OneToMany(mappedBy = "instrumentoAvaliativo")
    private List<InstrumentoTipoRespondente> instrumentoTipoRespondenteList = new ArrayList<>();

    @OneToMany(mappedBy = "instrumentoAvaliativo")
    @OrderBy("ordem ASC")
    private List<InstrumentoSessao> instrumentoSessoesList = new ArrayList<>();

    @OneToMany(mappedBy = "instrumento")
    private List<InstrumentoTipoAlvo> tiposAlvoList = new ArrayList<>();

    @OneToMany(mappedBy = "instrumento")
    private List<InstrumentoEntidadeAvaliada> entidadesAvaliadasList = new ArrayList<>();

    @NotBlank(message = "Informe o título do instrumento.")
    @Size(max = 200, message = "O título do instrumento deve ter até 200 caracteres.")
    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @NotBlank(message = "Informe o ciclo de referência.")
    @Size(max = 20, message = "O ciclo de referência deve ter até 20 caracteres.")
    @Column(name = "ciclo_referencia", nullable = false, length = 20)
    private String cicloReferencia;

    @NotNull(message = "Informe o início da aplicação.")
    @Column(name = "inicio_aplicacao", nullable = false)
    private OffsetDateTime inicioAplicacao;

    @NotNull(message = "Informe o término da aplicação.")
    @Column(name = "fim_aplicacao", nullable = false)
    private OffsetDateTime fimAplicacao;

    @Column(nullable = false)
    private boolean anonimo;

    @Column(name = "permite_edicao", nullable = false)
    private boolean permiteEdicao = true;

    @NotNull(message = "Informe o status do instrumento.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusInstrumentoAvaliativo status = StatusInstrumentoAvaliativo.RASCUNHO;

    @Column(name = "consolidado_em")
    private OffsetDateTime consolidadoEm;

    @NotBlank(message = "Informe quem criou o instrumento.")
    @Size(max = 60, message = "O identificador do criador deve ter até 60 caracteres.")
    @Column(name = "criado_por", nullable = false, length = 60)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    public UUID getId() {
        return instrumentoAvaliativoId;
    }

    public UUID getInstrumentoAvaliativoId() {
        return instrumentoAvaliativoId;
    }

    public void setInstrumentoAvaliativoId(UUID instrumentoAvaliativoId) {
        this.instrumentoAvaliativoId = instrumentoAvaliativoId;
    }

    public InstrumentoAvaliativo getOrigem() {
        return origem;
    }

    public void setOrigem(InstrumentoAvaliativo origem) {
        this.origem = origem;
    }

    public TipoEntidade getTipoEntidadeAvaliada() {
        return tipoEntidadeAvaliada;
    }

    public void setTipoEntidadeAvaliada(TipoEntidade tipoEntidadeAvaliada) {
        this.tipoEntidadeAvaliada = tipoEntidadeAvaliada;
    }

    public List<InstrumentoTipoRespondente> getInstrumentoTipoRespondenteList() {
        return instrumentoTipoRespondenteList;
    }

    public void setInstrumentoTipoRespondenteList(List<InstrumentoTipoRespondente> respondentes) {
        this.instrumentoTipoRespondenteList = respondentes;
    }

    public List<InstrumentoSessao> getInstrumentoSessoesList() {
        return instrumentoSessoesList;
    }

    public void setInstrumentoSessoesList(List<InstrumentoSessao> sessoes) {
        this.instrumentoSessoesList = sessoes;
    }

    public int contarQuestoes() {
        if (instrumentoSessoesList == null) return 0;
        return instrumentoSessoesList.stream()
                .filter(sessao -> sessao.getTipoSessao() == TipoSessao.SESSAO)
                .mapToInt(sessao -> sessao.getSessaoQuestoesList() == null
                        ? 0
                        : sessao.getSessaoQuestoesList().size())
                .sum();
    }

    public List<InstrumentoTipoAlvo> getTiposAlvoList() { return tiposAlvoList; }

    public void setTiposAlvoList(List<InstrumentoTipoAlvo> tiposAlvo) { this.tiposAlvoList = tiposAlvo; }

    public List<InstrumentoEntidadeAvaliada> getEntidadesAvaliadasList() {
        return entidadesAvaliadasList;
    }

    public void setEntidadesAvaliadasList(List<InstrumentoEntidadeAvaliada> entidadesAvaliadas) {
        this.entidadesAvaliadasList = entidadesAvaliadas;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCicloReferencia() {
        return cicloReferencia;
    }

    public void setCicloReferencia(String cicloReferencia) {
        this.cicloReferencia = cicloReferencia;
    }

    public OffsetDateTime getInicioAplicacao() {
        return inicioAplicacao;
    }

    public void setInicioAplicacao(OffsetDateTime inicioAplicacao) {
        this.inicioAplicacao = inicioAplicacao;
    }

    public OffsetDateTime getFimAplicacao() {
        return fimAplicacao;
    }

    public void setFimAplicacao(OffsetDateTime fimAplicacao) {
        this.fimAplicacao = fimAplicacao;
    }

    @AssertTrue(message = "O término da aplicação deve ser posterior ao início.")
    public boolean isPeriodoAplicacaoValido() {
        return inicioAplicacao == null || fimAplicacao == null || fimAplicacao.isAfter(inicioAplicacao);
    }

    public boolean isAnonimo() {
        return anonimo;
    }

    public void setAnonimo(boolean anonimo) {
        this.anonimo = anonimo;
    }

    public boolean isPermiteEdicao() {
        return permiteEdicao;
    }

    public void setPermiteEdicao(boolean permiteEdicao) {
        this.permiteEdicao = permiteEdicao;
    }

    public StatusInstrumentoAvaliativo getStatus() {
        return status;
    }

    public void setStatus(StatusInstrumentoAvaliativo status) {
        this.status = status;
    }

    public OffsetDateTime getConsolidadoEm() {
        return consolidadoEm;
    }

    public void setConsolidadoEm(OffsetDateTime consolidadoEm) {
        this.consolidadoEm = consolidadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(OffsetDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public boolean isAtivo() {
        return status == StatusInstrumentoAvaliativo.PUBLICADO;
    }



    @Override
    public UUID getMMId() {
        return this.instrumentoAvaliativoId;
    }
}
