package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "questao")
public class Questao {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoQuestoes grupo;

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

    @Column(name = "opcoes_json", columnDefinition = "jsonb")
    private String opcoesJson;

    @Column(name = "ajuda")
    private String ajuda;

    public Questao() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public GrupoQuestoes getGrupo() { return grupo; }
    public void setGrupo(GrupoQuestoes grupo) { this.grupo = grupo; }

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

    public String getAjuda() { return ajuda; }
    public void setAjuda(String ajuda) { this.ajuda = ajuda; }
}
