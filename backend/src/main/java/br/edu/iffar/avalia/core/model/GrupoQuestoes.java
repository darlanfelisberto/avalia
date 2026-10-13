package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grupo_questoes")
public class GrupoQuestoes {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private Instrumento instrumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "secao", nullable = false, length = 32)
    private SecaoInstrumento secao = SecaoInstrumento.AVALIACAO_DOCENTE;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "ordem", nullable = false)
    private int ordem = 0;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    private List<Questao> questoes = new ArrayList<>();

    public GrupoQuestoes() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instrumento getInstrumento() { return instrumento; }
    public void setInstrumento(Instrumento instrumento) { this.instrumento = instrumento; }

    public SecaoInstrumento getSecao() { return secao; }
    public void setSecao(SecaoInstrumento secao) { this.secao = secao; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }

    public List<Questao> getQuestoes() { return questoes; }
    public void setQuestoes(List<Questao> questoes) { this.questoes = questoes; }
}
