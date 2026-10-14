package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "auth_sessao")
public class AuthSessao {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "cpf", nullable = false, length = 14)
    private String cpf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpf", referencedColumnName = "cpf", insertable = false, updatable = false)
    private UsuarioPermissao usuario;

    @Column(name = "papeis", nullable = false, length = 200)
    private String papeis;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getPapeis() { return papeis; }
    public void setPapeis(String papeis) { this.papeis = papeis; }
    public OffsetDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(OffsetDateTime expiraEm) { this.expiraEm = expiraEm; }
}
