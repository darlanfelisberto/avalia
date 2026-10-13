package br.edu.iffar.avalia.core.dto;

import java.util.ArrayList;
import java.util.List;

public class UsuarioDTO {
    private String cpf;
    private String cpfFormatado;
    private String nome;
    private String email;
    private String campus;
    private String papelAtivo;
    private List<String> papeis = new ArrayList<>();
    private List<String> permissoes = new ArrayList<>();

    public UsuarioDTO() {}

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getCpfFormatado() { return cpfFormatado; }
    public void setCpfFormatado(String cpfFormatado) { this.cpfFormatado = cpfFormatado; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getPapelAtivo() { return papelAtivo; }
    public void setPapelAtivo(String papelAtivo) { this.papelAtivo = papelAtivo; }

    public List<String> getPapeis() { return papeis; }
    public void setPapeis(List<String> papeis) { this.papeis = papeis; }

    public List<String> getPermissoes() { return permissoes; }
    public void setPermissoes(List<String> permissoes) { this.permissoes = permissoes; }
}
