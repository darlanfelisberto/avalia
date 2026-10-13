package br.edu.iffar.avalia.core.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "instrumento")
public class Instrumento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "subtitulo")
    private String subtitulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modulo", nullable = false, length = 32)
    private ModuloAvaliacao modulo = ModuloAvaliacao.ADPD;

    @Column(name = "ano_ciclo", nullable = false, length = 32)
    private String anoCiclo;

    @Column(name = "periodo_letivo", length = 64)
    private String periodoLetivo;

    @Column(name = "data_inicio")
    private OffsetDateTime dataInicio;

    @Column(name = "data_termino", nullable = false)
    private OffsetDateTime dataTermino;

    @Column(name = "publico_alvo")
    private String publicoAlvo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alvo", nullable = false, length = 32)
    private TipoAlvoAvaliado tipoAlvo = TipoAlvoAvaliado.DOCENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_publico", nullable = false, length = 32)
    private TipoPublicoAvaliador tipoPublico = TipoPublicoAvaliador.DISCENTES;

    @Column(name = "publicos_permitidos", length = 255)
    private String publicosPermitidos;

    @Column(name = "restringir_vinculo_campus", nullable = false)
    private boolean restringirVinculoCampus = true;

    @Column(name = "alvo_especifico_id")
    private UUID alvoEspecificoId;

    @Column(name = "alvos_especificos_ids")
    private String alvosEspecificosIds;

    @Column(name = "criador_id", nullable = false, length = 64)
    private String criadorId = "usr-cpa-cain";

    @Column(name = "criador_nome", nullable = false)
    private String criadorNome = "Comissão Própria de Avaliação (CPA)";

    @Column(name = "anonimo", nullable = false)
    private boolean anonimo = true;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ABERTO";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "instrumento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    private List<GrupoQuestoes> grupos = new ArrayList<>();

    @OneToMany(mappedBy = "instrumento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ComponenteCurricular> componentes = new ArrayList<>();

    public Instrumento() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public OffsetDateTime getDataInicio() { return dataInicio; }
    public void setDataInicio(OffsetDateTime dataInicio) { this.dataInicio = dataInicio; }

    public OffsetDateTime getDataTermino() { return dataTermino; }
    public void setDataTermino(OffsetDateTime dataTermino) { this.dataTermino = dataTermino; }

    public String getPublicoAlvo() { return publicoAlvo; }
    public void setPublicoAlvo(String publicoAlvo) { this.publicoAlvo = publicoAlvo; }

    public TipoAlvoAvaliado getTipoAlvo() { return tipoAlvo; }
    public void setTipoAlvo(TipoAlvoAvaliado tipoAlvo) { this.tipoAlvo = tipoAlvo; }

    public TipoPublicoAvaliador getTipoPublico() { return tipoPublico; }
    public void setTipoPublico(TipoPublicoAvaliador tipoPublico) { this.tipoPublico = tipoPublico; }

    public String getPublicosPermitidos() { return publicosPermitidos; }
    public void setPublicosPermitidos(String publicosPermitidos) { this.publicosPermitidos = publicosPermitidos; }

    public boolean isRestringirVinculoCampus() { return restringirVinculoCampus; }
    public void setRestringirVinculoCampus(boolean restringirVinculoCampus) { this.restringirVinculoCampus = restringirVinculoCampus; }

    public UUID getAlvoEspecificoId() { return alvoEspecificoId; }
    public void setAlvoEspecificoId(UUID alvoEspecificoId) { this.alvoEspecificoId = alvoEspecificoId; }

    public String getAlvosEspecificosIds() { return alvosEspecificosIds; }
    public void setAlvosEspecificosIds(String alvosEspecificosIds) { this.alvosEspecificosIds = alvosEspecificosIds; }

    public String getCriadorId() { return criadorId; }
    public void setCriadorId(String criadorId) { this.criadorId = criadorId; }

    public String getCriadorNome() { return criadorNome; }
    public void setCriadorNome(String criadorNome) { this.criadorNome = criadorNome; }

    public boolean isAnonimo() { return anonimo; }
    public void setAnonimo(boolean anonimo) { this.anonimo = anonimo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<GrupoQuestoes> getGrupos() { return grupos; }
    public void setGrupos(List<GrupoQuestoes> grupos) { this.grupos = grupos; }

    public List<ComponenteCurricular> getComponentes() { return componentes; }
    public void setComponentes(List<ComponenteCurricular> componentes) { this.componentes = componentes; }
}
