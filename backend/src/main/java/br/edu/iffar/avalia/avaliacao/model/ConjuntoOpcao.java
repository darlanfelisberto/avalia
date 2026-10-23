package br.edu.iffar.avalia.avaliacao.model;

import br.com.feliva.sharedClass.db.Model;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conjunto_opcao", schema = "avaliacao")
public class ConjuntoOpcao extends Model<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "conjunto_opcao_id")
    private UUID conjuntoOpcaoId;

    @NotBlank(message = "Informe o nome do conjunto de opções.")
    @Size(max = 100, message = "O nome do conjunto deve ter até 100 caracteres.")
    @Column(nullable = false, length = 100, unique = true)
    private String nome;

    // NULL nos registros anteriores equivale a um conjunto reutilizável.
    @Column(name = "exclusivo")
    private Boolean exclusivo = false;

    @OneToMany(mappedBy = "conjunto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Valid
    @Size(min = 2, message = "Um conjunto precisa de ao menos duas opções.")
    private List<OpcaoQuestao> opcoes = new ArrayList<>();

    public UUID getId() { return conjuntoOpcaoId; }
    public UUID getConjuntoOpcaoId() { return conjuntoOpcaoId; }
    public void setConjuntoOpcaoId(UUID conjuntoOpcaoId) { this.conjuntoOpcaoId = conjuntoOpcaoId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public boolean isExclusivo() { return Boolean.TRUE.equals(exclusivo); }
    public void setExclusivo(boolean exclusivo) { this.exclusivo = exclusivo; }
    public List<OpcaoQuestao> getOpcoes() { return opcoes; }

    @Override
    public UUID getMMId() {
        return this.conjuntoOpcaoId;
    }
}
