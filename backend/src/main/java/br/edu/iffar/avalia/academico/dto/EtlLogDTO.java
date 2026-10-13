package br.edu.iffar.avalia.academico.dto;

import java.time.LocalDateTime;

public class EtlLogDTO {
    public Long id;
    public String entidade;
    public LocalDateTime inicio;
    public LocalDateTime fim;
    public Integer registrosImportados;
    public String status;
    public String mensagem;
}
