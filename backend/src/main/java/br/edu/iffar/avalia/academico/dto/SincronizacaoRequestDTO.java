package br.edu.iffar.avalia.academico.dto;

import java.util.List;

public class SincronizacaoRequestDTO {
    public Integer ano;
    public Integer periodo;
    public Boolean sincronizarEstrutura; // Unidades, cargos, servidores, cursos, componentes
    public Boolean sincronizarTurmasMatriculas; // Turmas, docentes e matrículas
    public List<String> entidades; // Se quiser sincronizar entidades específicas
}
