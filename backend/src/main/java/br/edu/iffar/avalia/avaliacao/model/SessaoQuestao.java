package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "A questão precisa estar vinculada a uma sessão.")
    private InstrumentoSessao instrumentoSessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    @NotNull(message = "A sessão precisa estar vinculada a uma questão.")
    private Questao questao;

    @Column(nullable = false)
    @Min(value = 0, message = "A ordem da questão não pode ser negativa.")
    private int ordem;

    @Column(nullable = false)
    private boolean obrigatoria;

    @Column(precision = 6, scale = 2)
    @Digits(integer = 4, fraction = 2, message = "O peso da questão deve ter até 4 dígitos inteiros e 2 decimais.")
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
