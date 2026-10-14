package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "instrumento_sessao", schema = "avaliacao")
public class InstrumentoSessao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instrumento_sessao_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_avaliativo_id", nullable = false)
    private InstrumentoAvaliativo instrumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrumento_sessao_pai_id")
    private InstrumentoSessao pai;

    @Column(nullable = false, length = 6)
    private String tipo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(nullable = false)
    private int ordem;

    public UUID getId() { return id; }
    public InstrumentoAvaliativo getInstrumento() { return instrumento; }
    public void setInstrumento(InstrumentoAvaliativo instrumento) { this.instrumento = instrumento; }
    public InstrumentoSessao getPai() { return pai; }
    public void setPai(InstrumentoSessao pai) { this.pai = pai; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
