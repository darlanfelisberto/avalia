package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "publico_avaliador")
public class PublicoAvaliador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "publico_avaliador_id", nullable = false, updatable = false)
    private java.util.UUID publicoAvaliadorId;

    @Column(name = "segmento", nullable = false, length = 32)
    private String segmento;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "exige_autenticacao", nullable = false)
    private boolean exigeAutenticacao = true;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public PublicoAvaliador() {}

    public java.util.UUID getPublicoAvaliadorId() { return publicoAvaliadorId; }
    public void setPublicoAvaliadorId(java.util.UUID publicoAvaliadorId) { this.publicoAvaliadorId = publicoAvaliadorId; }

    public java.util.UUID getId() { return publicoAvaliadorId; }
    public void setId(java.util.UUID id) { this.publicoAvaliadorId = id; }

    public String getSegmento() { return segmento; }
    public void setSegmento(String segmento) { this.segmento = segmento; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public boolean isExigeAutenticacao() { return exigeAutenticacao; }
    public void setExigeAutenticacao(boolean exigeAutenticacao) { this.exigeAutenticacao = exigeAutenticacao; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
