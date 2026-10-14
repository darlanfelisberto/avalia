package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tipo_publico")
public class TipoPublico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tipo_publico_id", nullable = false, updatable = false)
    private UUID tipoPublicoId;

    @Column(name = "codigo", nullable = false, unique = true, length = 32)
    private String codigo;

    @Column(name = "singular", nullable = false, length = 100)
    private String singular;

    @Column(name = "plural", nullable = false, length = 100)
    private String plural;

    @Column(name = "icone", nullable = false, length = 32)
    private String icone;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "ordem", nullable = false)
    private int ordem = 0;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TipoPublico() {}

    public TipoPublico(String codigo, String singular, String plural, String icone, String descricao, int ordem) {
        this.codigo = codigo;
        this.singular = singular;
        this.plural = plural;
        this.icone = icone;
        this.descricao = descricao;
        this.ordem = ordem;
    }

    public UUID getTipoPublicoId() {
        return tipoPublicoId;
    }

    public void setTipoPublicoId(UUID tipoPublicoId) {
        this.tipoPublicoId = tipoPublicoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getSingular() {
        return singular;
    }

    public void setSingular(String singular) {
        this.singular = singular;
    }

    public String getPlural() {
        return plural;
    }

    public void setPlural(String plural) {
        this.plural = plural;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
