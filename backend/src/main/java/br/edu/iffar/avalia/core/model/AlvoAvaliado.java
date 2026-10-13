package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alvo_avaliado")
public class AlvoAvaliado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alvo_avaliado_id", nullable = false, updatable = false)
    private java.util.UUID alvoAvaliadoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 32)
    private TipoAlvoAvaliado tipo;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "codigo", length = 64)
    private String codigo;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "responsavel_nome")
    private String responsavelNome;

    @Column(name = "campus", length = 100)
    private String campus;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public AlvoAvaliado() {}

    public java.util.UUID getAlvoAvaliadoId() { return alvoAvaliadoId; }
    public void setAlvoAvaliadoId(java.util.UUID alvoAvaliadoId) { this.alvoAvaliadoId = alvoAvaliadoId; }

    public java.util.UUID getId() { return alvoAvaliadoId; }
    public void setId(java.util.UUID id) { this.alvoAvaliadoId = id; }

    public TipoAlvoAvaliado getTipo() { return tipo; }
    public void setTipo(TipoAlvoAvaliado tipo) { this.tipo = tipo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getResponsavelNome() { return responsavelNome; }
    public void setResponsavelNome(String responsavelNome) { this.responsavelNome = responsavelNome; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
