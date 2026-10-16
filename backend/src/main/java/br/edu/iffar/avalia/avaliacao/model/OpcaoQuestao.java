package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "opcao_questao", schema = "avaliacao")
public class OpcaoQuestao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "opcao_questao_id")
    private UUID opcaoQuestaoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conjunto_opcao_id", nullable = false)
    private ConjuntoOpcao conjunto;

    @Column(nullable = false, length = 300)
    private String texto;

    @Column(precision = 6, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private boolean correta;

    @Column(nullable = false)
    private int ordem;

    public UUID getId() { return opcaoQuestaoId; }
    public UUID getOpcaoQuestaoId() { return opcaoQuestaoId; }
    public void setOpcaoQuestaoId(UUID opcaoQuestaoId) { this.opcaoQuestaoId = opcaoQuestaoId; }
    public ConjuntoOpcao getConjunto() { return conjunto; }
    public void setConjunto(ConjuntoOpcao conjunto) { this.conjunto = conjunto; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public boolean isCorreta() { return correta; }
    public void setCorreta(boolean correta) { this.correta = correta; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }

    @Override
    public UUID getMMId() {
        return this.opcaoQuestaoId;
    }
}
