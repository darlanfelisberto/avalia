package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "opcao_questao")
public class OpcaoQuestao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "opcao_questao_id", nullable = false, updatable = false)
    private UUID opcaoQuestaoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @Column(name = "texto", nullable = false, length = 500)
    private String texto;

    @Column(name = "ordem", nullable = false)
    private int ordem = 0;

    public OpcaoQuestao() {}

    public OpcaoQuestao(UUID opcaoQuestaoId, Questao questao, String texto, int ordem) {
        this.opcaoQuestaoId = opcaoQuestaoId;
        this.questao = questao;
        this.texto = texto;
        this.ordem = ordem;
    }

    public UUID getOpcaoQuestaoId() { return opcaoQuestaoId; }
    public void setOpcaoQuestaoId(UUID opcaoQuestaoId) { this.opcaoQuestaoId = opcaoQuestaoId; }

    public UUID getId() { return opcaoQuestaoId; }
    public void setId(UUID id) { this.opcaoQuestaoId = id; }

    public Questao getQuestao() { return questao; }
    public void setQuestao(Questao questao) { this.questao = questao; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
