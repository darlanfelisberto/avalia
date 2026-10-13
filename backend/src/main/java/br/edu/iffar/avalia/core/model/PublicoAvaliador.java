package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "publico_avaliador")
public class PublicoAvaliador {

    @Id
    @Column(name = "id", length = 64)
    private String id;

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
