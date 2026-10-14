package br.edu.iffar.avalia.avaliacao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tipo_entidade", schema = "avaliacao")
public class TipoEntidade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tipo_entidade_id")
    private UUID id;

    @Column(nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "pode_ser_avaliada", nullable = false)
    private boolean podeSerAvaliada = true;

    @Column(name = "pode_responder", nullable = false)
    private boolean podeResponder = true;

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public boolean isPodeSerAvaliada() { return podeSerAvaliada; }
    public boolean isPodeResponder() { return podeResponder; }
}
