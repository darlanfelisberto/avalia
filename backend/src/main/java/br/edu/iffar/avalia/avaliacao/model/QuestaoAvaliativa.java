package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "questao", schema = "avaliacao")
public class QuestaoAvaliativa {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "questao_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conjunto_opcao_id")
    private ConjuntoOpcao conjunto;

    @Column(nullable = false, columnDefinition = "text")
    private String enunciado;

    @Column(nullable = false, length = 16)
    private String tipo;

    @Column(length = 100)
    private String assunto;

    @Column(name = "criado_por", nullable = false, length = 60)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    public UUID getId() { return id; }
    public ConjuntoOpcao getConjunto() { return conjunto; }
    public void setConjunto(ConjuntoOpcao conjunto) { this.conjunto = conjunto; }
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getAssunto() { return assunto; }
    public void setAssunto(String assunto) { this.assunto = assunto; }
    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
}
