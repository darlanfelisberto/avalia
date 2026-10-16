package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "resposta_questao", schema = "avaliacao")
public class RespostaQuestao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "resposta_questao_id")
    private UUID respostaQuestaoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resposta_instrumento_id", nullable = false)
    private RespostaInstrumento resposta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_questao_id", nullable = false)
    private SessaoQuestao questao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcao_questao_id")
    private OpcaoQuestao opcao;

    @Column(columnDefinition = "text")
    private String texto;

    public UUID getId() { return respostaQuestaoId; }
    public UUID getRespostaQuestaoId() { return respostaQuestaoId; }
    public void setRespostaQuestaoId(UUID respostaQuestaoId) { this.respostaQuestaoId = respostaQuestaoId; }
    public RespostaInstrumento getResposta() { return resposta; }
    public void setResposta(RespostaInstrumento resposta) { this.resposta = resposta; }
    public SessaoQuestao getQuestao() { return questao; }
    public void setQuestao(SessaoQuestao questao) { this.questao = questao; }
    public OpcaoQuestao getOpcao() { return opcao; }
    public void setOpcao(OpcaoQuestao opcao) { this.opcao = opcao; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    @Override
    public UUID getMMId() {
        return this.respostaQuestaoId;
    }
}
