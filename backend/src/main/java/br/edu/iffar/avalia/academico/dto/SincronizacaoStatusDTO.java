package br.edu.iffar.avalia.academico.dto;

import java.util.Map;

public class SincronizacaoStatusDTO {
    public boolean conectadoSigaa;
    public String urlSigaa;
    public String usuarioSigaa;
    public boolean emExecucao;
    public Map<String, Long> contadoresLocais;
}
