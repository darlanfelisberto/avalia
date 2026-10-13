package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questao")
public class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "questao_id", nullable = false, updatable = false)
    private UUID questaoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_questoes_id", nullable = false)
    private GrupoQuestoes grupoQuestoes;

    @Column(name = "enunciado", nullable = false)
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 32)
    private TipoQuestao tipo = TipoQuestao.ESCALA;

    @Column(name = "obrigatoria", nullable = false)
    private boolean obrigatoria = true;

    @Column(name = "ordem", nullable = false)
    private int ordem = 0;

    @Column(name = "escala_min")
    private Integer escalaMin = 1;

    @Column(name = "escala_max")
    private Integer escalaMax = 5;

    @Column(name = "rotulo_min", length = 100)
    private String rotuloMin;

    @Column(name = "rotulo_max", length = 100)
    private String rotuloMax;

    @Column(name = "opcoes_json", columnDefinition = "TEXT")
    private String opcoesJson;

    @OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    private List<OpcaoQuestao> opcoes = new ArrayList<>();

    @Column(name = "ajuda")
    private String ajuda;

    public Questao() {}

    public UUID getQuestaoId() { return questaoId; }
    public void setQuestaoId(UUID questaoId) { this.questaoId = questaoId; }

    public UUID getId() { return questaoId; }
    public void setId(UUID id) { this.questaoId = id; }

    public GrupoQuestoes getGrupoQuestoes() { return grupoQuestoes; }
    public void setGrupoQuestoes(GrupoQuestoes grupoQuestoes) { this.grupoQuestoes = grupoQuestoes; }

    public GrupoQuestoes getGrupo() { return grupoQuestoes; }
    public void setGrupo(GrupoQuestoes grupo) { this.grupoQuestoes = grupo; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public TipoQuestao getTipo() { return tipo; }
    public void setTipo(TipoQuestao tipo) { this.tipo = tipo; }

    public boolean isObrigatoria() { return obrigatoria; }
    public void setObrigatoria(boolean obrigatoria) { this.obrigatoria = obrigatoria; }

    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }

    public Integer getEscalaMin() { return escalaMin; }
    public void setEscalaMin(Integer escalaMin) { this.escalaMin = escalaMin; }

    public Integer getEscalaMax() { return escalaMax; }
    public void setEscalaMax(Integer escalaMax) { this.escalaMax = escalaMax; }

    public String getRotuloMin() { return rotuloMin; }
    public void setRotuloMin(String rotuloMin) { this.rotuloMin = rotuloMin; }

    public String getRotuloMax() { return rotuloMax; }
    public void setRotuloMax(String rotuloMax) { this.rotuloMax = rotuloMax; }

    public String getOpcoesJson() { return opcoesJson; }
    public void setOpcoesJson(String opcoesJson) { this.opcoesJson = opcoesJson; }

    public List<OpcaoQuestao> getOpcoes() { return opcoes; }
    public void setOpcoes(List<OpcaoQuestao> opcoes) { this.opcoes = opcoes; }

    public String getAjuda() { return ajuda; }
    public void setAjuda(String ajuda) { this.ajuda = ajuda; }
}
