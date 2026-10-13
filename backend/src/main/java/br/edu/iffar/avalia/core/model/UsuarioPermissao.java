package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario_permissao")
public class UsuarioPermissao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usuario_permissao_id", nullable = false, updatable = false)
    private UUID usuarioPermissaoId;

    @Column(name = "cpf", nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "papel", nullable = false, length = 32)
    private String papel = "DISCENTE";

    @Column(name = "campus", length = 100)
    private String campus = "Reitoria";

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "permissoes", columnDefinition = "TEXT")
    private String permissoes;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UsuarioPermissao() {}

    public UsuarioPermissao(String cpf, String nome, String papel, String campus, String permissoes) {
        this.cpf = cpf;
        this.nome = nome;
        this.papel = papel;
        this.campus = campus;
        this.permissoes = permissoes;
    }

    public UUID getUsuarioPermissaoId() { return usuarioPermissaoId; }
    public void setUsuarioPermissaoId(UUID usuarioPermissaoId) { this.usuarioPermissaoId = usuarioPermissaoId; }

    public UUID getId() { return usuarioPermissaoId; }
    public void setId(UUID id) { this.usuarioPermissaoId = id; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPermissoes() { return permissoes; }
    public void setPermissoes(String permissoes) { this.permissoes = permissoes; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
