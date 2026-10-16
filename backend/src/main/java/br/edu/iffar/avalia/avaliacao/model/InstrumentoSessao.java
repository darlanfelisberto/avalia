package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "instrumento_sessao", schema = "avaliacao")
public class InstrumentoSessao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrumento_sessao_id")
    private UUID instrumentoSessaoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    private InstrumentoAvaliativo instrumentoAvaliativo;

    @OneToMany(mappedBy = "instrumentoSessao")
    @OrderBy("ordem ASC")
    private List<SessaoQuestao> sessaoQuestoesList = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private TipoSessao tipoSessao;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(nullable = false)
    private int ordem;

    public UUID getId() { return instrumentoSessaoId; }
    public UUID getInstrumentoSessaoId() { return instrumentoSessaoId; }
    public void setInstrumentoSessaoId(UUID instrumentoSessaoId) { this.instrumentoSessaoId = instrumentoSessaoId; }
    public InstrumentoAvaliativo getInstrumentoAvaliativo() { return instrumentoAvaliativo; }
    public void setInstrumentoAvaliativo(InstrumentoAvaliativo instrumento) { this.instrumentoAvaliativo = instrumento; }
    public List<SessaoQuestao> getSessaoQuestoesList() { return sessaoQuestoesList; }
    public void setTipoSessao(String tipo) { this.tipoSessao = tipo != null ? TipoSessao.valueOf(tipo) : null; }
    public TipoSessao getTipoSessao() { return tipoSessao; }
    public void setTipoSessao(TipoSessao tipo) { this.tipoSessao = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }

    @Override
    public UUID getMMId() {
        return this.instrumentoSessaoId;
    }
}
