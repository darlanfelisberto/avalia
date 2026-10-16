package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "instrumento_avaliativo", schema = "avaliacao")
public class InstrumentoAvaliativo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrumento_avaliativo_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_avaliativo_origem_id")
    private InstrumentoAvaliativo origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_entidade_avaliada_id", nullable = false)
    private TipoEntidade tipoEntidadeAvaliada;

    @OneToMany(mappedBy = "instrumento")
    private List<InstrumentoRespondente> respondentes = new ArrayList<>();

    @OneToMany(mappedBy = "instrumento")
    @OrderBy("ordem ASC")
    private List<InstrumentoSessao> sessoes = new ArrayList<>();

    @OneToMany(mappedBy = "instrumento")
    private List<InstrumentoTipoAlvo> tiposAlvo = new ArrayList<>();

    @OneToMany(mappedBy = "instrumento")
    private List<InstrumentoEntidadeAvaliada> entidadesAvaliadas = new ArrayList<>();

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "ciclo_referencia", nullable = false, length = 20)
    private String cicloReferencia;

    @Column(name = "inicio_aplicacao", nullable = false)
    private OffsetDateTime inicioAplicacao;

    @Column(name = "fim_aplicacao", nullable = false)
    private OffsetDateTime fimAplicacao;

    @Column(nullable = false)
    private boolean anonimo;

    @Column(name = "permite_edicao", nullable = false)
    private boolean permiteEdicao = true;

    @Column(nullable = false, length = 12)
    private String status = "RASCUNHO";

    @Column(name = "consolidado_em")
    private OffsetDateTime consolidadoEm;

    @Column(name = "criado_por", nullable = false, length = 60)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    public UUID getId() { return id; }
    public InstrumentoAvaliativo getOrigem() { return origem; }
    public TipoEntidade getTipoEntidadeAvaliada() { return tipoEntidadeAvaliada; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getCicloReferencia() { return cicloReferencia; }
    public OffsetDateTime getInicioAplicacao() { return inicioAplicacao; }
    public OffsetDateTime getFimAplicacao() { return fimAplicacao; }
    public boolean isAnonimo() { return anonimo; }
    public boolean isPermiteEdicao() { return permiteEdicao; }
    public String getStatus() { return status; }
    public OffsetDateTime getConsolidadoEm() { return consolidadoEm; }
    public String getCriadoPor() { return criadoPor; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public List<InstrumentoRespondente> getRespondentes() { return respondentes; }
    public List<InstrumentoSessao> getSessoes() { return sessoes; }
    public List<InstrumentoTipoAlvo> getTiposAlvo() { return tiposAlvo; }
    public List<InstrumentoEntidadeAvaliada> getEntidadesAvaliadas() { return entidadesAvaliadas; }
    public void setTipoEntidadeAvaliada(TipoEntidade tipoEntidadeAvaliada) { this.tipoEntidadeAvaliada = tipoEntidadeAvaliada; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setCicloReferencia(String cicloReferencia) { this.cicloReferencia = cicloReferencia; }
    public void setInicioAplicacao(OffsetDateTime inicioAplicacao) { this.inicioAplicacao = inicioAplicacao; }
    public void setFimAplicacao(OffsetDateTime fimAplicacao) { this.fimAplicacao = fimAplicacao; }
    public void setAnonimo(boolean anonimo) { this.anonimo = anonimo; }
    public void setPermiteEdicao(boolean permiteEdicao) { this.permiteEdicao = permiteEdicao; }
    public void setStatus(String status) { this.status = status; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
    public void setAtualizadoEm(OffsetDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
