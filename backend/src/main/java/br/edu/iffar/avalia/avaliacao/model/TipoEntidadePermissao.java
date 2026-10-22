package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "tipo_entidade_permissao", schema = "avaliacao",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tipo_respondente_id", "tipo_avaliado_id"}))
public class TipoEntidadePermissao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tipo_entidade_permissao_id")
    private UUID tipoEntidadePermissaoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_respondente_id", nullable = false)
    private TipoEntidade respondente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_avaliado_id", nullable = false)
    private TipoEntidade avaliado;

    public UUID getTipoEntidadePermissaoId() {
        return tipoEntidadePermissaoId;
    }

    public void setTipoEntidadePermissaoId(UUID tipoEntidadePermissaoId) {
        this.tipoEntidadePermissaoId = tipoEntidadePermissaoId;
    }

    public TipoEntidade getRespondente() {
        return respondente;
    }

    public void setRespondente(TipoEntidade respondente) {
        this.respondente = respondente;
    }

    public TipoEntidade getAvaliado() {
        return avaliado;
    }

    public void setAvaliado(TipoEntidade avaliado) {
        this.avaliado = avaliado;
    }

    @Override
    public UUID getMMId() {
        return this.tipoEntidadePermissaoId;
    }
}
