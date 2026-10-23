package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotNull(message = "A opção precisa estar vinculada a um conjunto.")
    private ConjuntoOpcao conjunto;

    @NotBlank(message = "Toda opção precisa de um texto.")
    @Size(max = 300, message = "O texto da opção deve ter até 300 caracteres.")
    @Column(nullable = false, length = 300)
    private String texto;

    @Digits(integer = 4, fraction = 2, message = "O valor da opção deve ter até 4 dígitos inteiros e 2 decimais.")
    @Column(precision = 6, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private boolean correta;

    @Column(nullable = false)
    @Min(value = 0, message = "A ordem da opção não pode ser negativa.")
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
