package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sessao_questao", schema = "avaliacao")
public class SessaoQuestao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sessao_questao_id")
    private UUID sessaoQuestaoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_sessao_id", nullable = false)
    private InstrumentoSessao instrumentoSessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private boolean obrigatoria;

    @Column(precision = 6, scale = 2)
    private BigDecimal peso;

    public UUID getId() { return sessaoQuestaoId; }
    public UUID getSessaoQuestaoId() { return sessaoQuestaoId; }
    public void setSessaoQuestaoId(UUID sessaoQuestaoId) { this.sessaoQuestaoId = sessaoQuestaoId; }
    public InstrumentoSessao getInstrumentoSessao() { return instrumentoSessao; }
    public void setInstrumentoSessao(InstrumentoSessao sessao) { this.instrumentoSessao = sessao; }
    public Questao getQuestao() { return questao; }
    public void setQuestao(Questao questao) { this.questao = questao; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public boolean isObrigatoria() { return obrigatoria; }
    public void setObrigatoria(boolean obrigatoria) { this.obrigatoria = obrigatoria; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }

    @Override
    public UUID getMMId() {
        return this.sessaoQuestaoId;
    }
}
