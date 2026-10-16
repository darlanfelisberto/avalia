package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "entidade", schema = "avaliacao")
public class Entidade extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entidade_id")
    private UUID entidadeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_entidade_id", nullable = false)
    private TipoEntidade tipo;

    @Column(name = "referencia_externa", nullable = false, length = 60)
    private String referenciaExterna;

    @Column(nullable = false, length = 300)
    private String rotulo;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "sincronizado_em", nullable = false)
    private OffsetDateTime sincronizadoEm = OffsetDateTime.now();

    public UUID getId() { return entidadeId; }
    public UUID getEntidadeId() { return entidadeId; }
    public void setEntidadeId(UUID entidadeId) { this.entidadeId = entidadeId; }
    public TipoEntidade getTipo() { return tipo; }
    public void setTipo(TipoEntidade tipo) { this.tipo = tipo; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }
    public String getRotulo() { return rotulo; }
    public void setRotulo(String rotulo) { this.rotulo = rotulo; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    @Override
    public UUID getMMId() {
        return this.entidadeId;
    }
}
