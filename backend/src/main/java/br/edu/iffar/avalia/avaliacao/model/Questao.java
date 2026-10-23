package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "questao", schema = "avaliacao")
public class Questao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "questao_id")
    private UUID questaoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conjunto_opcao_id")
    private ConjuntoOpcao conjuntoOpcao;

    @NotBlank(message = "Toda questão precisa de um enunciado.")
    @Column(nullable = false, columnDefinition = "text")
    private String enunciado;

    @NotBlank(message = "Informe o tipo da questão.")
    @Pattern(regexp = "ESCOLHA_UNICA|MULTIPLA_ESCOLHA|ESCALA|DESCRITIVA", message = "Tipo de questão inválido.")
    @Column(nullable = false, length = 16)
    private String tipo;

    @Size(max = 100, message = "O assunto deve ter até 100 caracteres.")
    @Column(length = 100)
    private String assunto;

    @NotBlank(message = "Informe quem criou a questão.")
    @Size(max = 60, message = "O identificador do criador deve ter até 60 caracteres.")
    @Column(name = "criado_por", nullable = false, length = 60)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    public UUID getId() { return questaoId; }
    public UUID getQuestaoId() { return questaoId; }
    public void setQuestaoId(UUID questaoId) { this.questaoId = questaoId; }
    public ConjuntoOpcao getConjuntoOpcao() { return conjuntoOpcao; }
    public void setConjuntoOpcao(ConjuntoOpcao conjunto) { this.conjuntoOpcao = conjunto; }
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getAssunto() { return assunto; }
    public void setAssunto(String assunto) { this.assunto = assunto; }
    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }

    @AssertTrue(message = "Questões descritivas não podem ter opções; questões objetivas precisam de um conjunto de opções.")
    public boolean isConjuntoCompativelComTipo() {
        if (tipo == null || tipo.isBlank()) return true;
        return switch (tipo) {
            case "DESCRITIVA" -> conjuntoOpcao == null;
            case "ESCOLHA_UNICA", "MULTIPLA_ESCOLHA", "ESCALA" -> conjuntoOpcao != null;
            default -> true;
        };
    }

    @Override
    public UUID getMMId() {
        return this.questaoId;
    }
}
