package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sessao_questao", schema = "avaliacao")
public class SessaoQuestao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sessao_questao_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_sessao_id", nullable = false)
    private InstrumentoSessao sessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private QuestaoAvaliativa questao;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private boolean obrigatoria;

    @Column(precision = 6, scale = 2)
    private BigDecimal peso;

    public UUID getId() { return id; }
    public InstrumentoSessao getSessao() { return sessao; }
    public void setSessao(InstrumentoSessao sessao) { this.sessao = sessao; }
    public QuestaoAvaliativa getQuestao() { return questao; }
    public void setQuestao(QuestaoAvaliativa questao) { this.questao = questao; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public boolean isObrigatoria() { return obrigatoria; }
    public void setObrigatoria(boolean obrigatoria) { this.obrigatoria = obrigatoria; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
}
