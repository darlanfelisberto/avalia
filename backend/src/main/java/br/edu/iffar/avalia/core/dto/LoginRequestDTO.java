package br.edu.iffar.avalia.core.dto;

public class LoginRequestDTO {
    private String cpf;

    public LoginRequestDTO() {}

    public LoginRequestDTO(String cpf) {
        this.cpf = cpf;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
}
