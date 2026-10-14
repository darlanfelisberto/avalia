package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
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
    public void setTipoEntidadeAvaliada(TipoEntidade tipoEntidadeAvaliada) { this.tipoEntidadeAvaliada = tipoEntidadeAvaliada; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCicloReferencia() { return cicloReferencia; }
    public void setCicloReferencia(String cicloReferencia) { this.cicloReferencia = cicloReferencia; }
    public OffsetDateTime getInicioAplicacao() { return inicioAplicacao; }
    public void setInicioAplicacao(OffsetDateTime inicioAplicacao) { this.inicioAplicacao = inicioAplicacao; }
    public OffsetDateTime getFimAplicacao() { return fimAplicacao; }
    public void setFimAplicacao(OffsetDateTime fimAplicacao) { this.fimAplicacao = fimAplicacao; }
    public boolean isAnonimo() { return anonimo; }
    public void setAnonimo(boolean anonimo) { this.anonimo = anonimo; }
    public boolean isPermiteEdicao() { return permiteEdicao; }
    public void setPermiteEdicao(boolean permiteEdicao) { this.permiteEdicao = permiteEdicao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(OffsetDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
