package br.edu.iffar.avalia.core.dto;

import br.edu.iffar.avalia.core.model.ModuloAvaliacao;
import java.util.ArrayList;
import java.util.List;

public class InstrumentoDTO {
    private String id;
    private String titulo;
    private String subtitulo;
    private ModuloAvaliacao modulo;
    private String anoCiclo;
    private String periodoLetivo;
    private String dataInicio;
    private String dataTermino;
    private String publicoAlvo;
    private boolean anonimo;
    private String status;
    private List<GrupoDTO> gruposAutoavaliacaoDiscente = new ArrayList<>();
    private List<GrupoDTO> gruposAvaliacaoDocente = new ArrayList<>();
    private List<ComponenteDTO> componentesMatriculados = new ArrayList<>();

    public InstrumentoDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getSubtitulo() { return subtitulo; }
    public void setSubtitulo(String subtitulo) { this.subtitulo = subtitulo; }

    public ModuloAvaliacao getModulo() { return modulo; }
    public void setModulo(ModuloAvaliacao modulo) { this.modulo = modulo; }

    public String getAnoCiclo() { return anoCiclo; }
    public void setAnoCiclo(String anoCiclo) { this.anoCiclo = anoCiclo; }

    public String getPeriodoLetivo() { return periodoLetivo; }
    public void setPeriodoLetivo(String periodoLetivo) { this.periodoLetivo = periodoLetivo; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataTermino() { return dataTermino; }
    public void setDataTermino(String dataTermino) { this.dataTermino = dataTermino; }

    public String getPublicoAlvo() { return publicoAlvo; }
    public void setPublicoAlvo(String publicoAlvo) { this.publicoAlvo = publicoAlvo; }

    public boolean isAnonimo() { return anonimo; }
    public void setAnonimo(boolean anonimo) { this.anonimo = anonimo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<GrupoDTO> getGruposAutoavaliacaoDiscente() { return gruposAutoavaliacaoDiscente; }
    public void setGruposAutoavaliacaoDiscente(List<GrupoDTO> gruposAutoavaliacaoDiscente) { this.gruposAutoavaliacaoDiscente = gruposAutoavaliacaoDiscente; }

    public List<GrupoDTO> getGruposAvaliacaoDocente() { return gruposAvaliacaoDocente; }
    public void setGruposAvaliacaoDocente(List<GrupoDTO> gruposAvaliacaoDocente) { this.gruposAvaliacaoDocente = gruposAvaliacaoDocente; }

    public List<ComponenteDTO> getComponentesMatriculados() { return componentesMatriculados; }
    public void setComponentesMatriculados(List<ComponenteDTO> componentesMatriculados) { this.componentesMatriculados = componentesMatriculados; }
}
