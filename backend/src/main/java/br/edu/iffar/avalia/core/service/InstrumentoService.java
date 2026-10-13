package br.edu.iffar.avalia.core.service;

import br.edu.iffar.avalia.core.dto.*;
import br.edu.iffar.avalia.core.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class InstrumentoService {

    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

    private final Jsonb jsonb = JsonbBuilder.create();

    public List<InstrumentoDTO> listarTodos(String criadorId) {
        String jpql = "SELECT DISTINCT i FROM Instrumento i LEFT JOIN FETCH i.grupos ";
        if (criadorId != null && !criadorId.trim().isEmpty()) {
            jpql += "WHERE i.criadorId = :criadorId ";
        }
        jpql += "ORDER BY i.createdAt DESC";

        var query = em.createQuery(jpql, Instrumento.class);
        if (criadorId != null && !criadorId.trim().isEmpty()) {
            query.setParameter("criadorId", criadorId);
        }

        List<Instrumento> instrumentos = query.getResultList();
        List<InstrumentoDTO> resultado = new ArrayList<>();
        for (Instrumento inst : instrumentos) {
            resultado.add(converterParaDTO(inst));
        }
        return resultado;
    }

    public InstrumentoDTO buscarPorId(UUID id) {
        Instrumento inst = em.find(Instrumento.class, id);
        if (inst == null) {
            return null;
        }
        return converterParaDTO(inst);
    }

    @Transactional
    public InstrumentoDTO salvarOuAtualizar(InstrumentoDTO dto) {
        Instrumento inst = null;
        UUID instId = dto.getInstrumentoId() != null ? dto.getInstrumentoId() : dto.getId();
        if (instId != null) {
            inst = em.find(Instrumento.class, instId);
        }

        if (inst == null) {
            inst = new Instrumento();
            if (instId != null) {
                inst.setInstrumentoId(instId);
            }
            inst.setCreatedAt(OffsetDateTime.now());
            em.persist(inst);
        }

        inst.setTitulo(dto.getTitulo());
        inst.setSubtitulo(dto.getSubtitulo());
        inst.setModulo(dto.getModulo() != null ? dto.getModulo() : ModuloAvaliacao.ADPD);
        inst.setAnoCiclo(dto.getAnoCiclo() != null && !dto.getAnoCiclo().trim().isEmpty()
                ? dto.getAnoCiclo()
                : String.valueOf(java.time.Year.now().getValue()));
        inst.setPublicoAlvo(dto.getPublicoAlvo());
        if (dto.getTipoAlvo() != null) {
            try {
                inst.setTipoAlvo(TipoAlvoAvaliado.valueOf(dto.getTipoAlvo()));
            } catch (Exception e) {
                inst.setTipoAlvo(TipoAlvoAvaliado.DOCENTE);
            }
        }
        if (dto.getPublicosPermitidos() != null && !dto.getPublicosPermitidos().isEmpty()) {
            inst.setPublicosPermitidos(String.join(",", dto.getPublicosPermitidos()));
            if (dto.getPublicosPermitidos().size() > 1) {
                inst.setTipoPublico(TipoPublicoAvaliador.TODOS);
            } else {
                try {
                    inst.setTipoPublico(TipoPublicoAvaliador.valueOf(dto.getPublicosPermitidos().get(0)));
                } catch (Exception e) {
                    inst.setTipoPublico(TipoPublicoAvaliador.TODOS);
                }
            }
        } else if (dto.getTipoPublico() != null) {
            try {
                inst.setTipoPublico(TipoPublicoAvaliador.valueOf(dto.getTipoPublico()));
            } catch (Exception e) {
                inst.setTipoPublico(TipoPublicoAvaliador.DISCENTES);
            }
            inst.setPublicosPermitidos(inst.getTipoPublico().name());
        }
        inst.setRestringirVinculoCampus(dto.isRestringirVinculoCampus());
        if (dto.getAlvosEspecificosIds() != null && !dto.getAlvosEspecificosIds().isEmpty()) {
            inst.setAlvosEspecificosIds(String.join(",", dto.getAlvosEspecificosIds()));
            try {
                inst.setAlvoEspecificoId(UUID.fromString(dto.getAlvosEspecificosIds().get(0)));
            } catch (Exception e) {
                inst.setAlvoEspecificoId(null);
            }
        } else if (dto.getAlvoEspecificoId() != null) {
            inst.setAlvoEspecificoId(dto.getAlvoEspecificoId());
            inst.setAlvosEspecificosIds(dto.getAlvoEspecificoId().toString());
        } else {
            inst.setAlvoEspecificoId(null);
            inst.setAlvosEspecificosIds(null);
        }
        if (dto.getCriadorId() != null && !dto.getCriadorId().trim().isEmpty()) {
            inst.setCriadorId(dto.getCriadorId());
        }
        if (dto.getCriadorNome() != null && !dto.getCriadorNome().trim().isEmpty()) {
            inst.setCriadorNome(dto.getCriadorNome());
        }
        inst.setAnonimo(dto.isAnonimo());
        inst.setStatus(dto.getStatus() != null ? dto.getStatus() : "ABERTO");
        inst.setUpdatedAt(OffsetDateTime.now());

        if (dto.getDataInicio() != null && !dto.getDataInicio().trim().isEmpty()) {
            inst.setDataInicio(converterParaOffsetDateTime(dto.getDataInicio()));
        }
        if (dto.getDataTermino() != null && !dto.getDataTermino().trim().isEmpty()) {
            inst.setDataTermino(converterParaOffsetDateTime(dto.getDataTermino()));
        } else {
            inst.setDataTermino(OffsetDateTime.now().plusDays(30));
        }

        // Sincronizar grupos sem recriar instâncias existentes (evita violação de chave primária no JPA/EclipseLink)
        Map<UUID, GrupoQuestoes> gruposExistentes = new HashMap<>();
        for (GrupoQuestoes g : inst.getGrupos()) {
            if (g.getGrupoQuestoesId() != null) {
                gruposExistentes.put(g.getGrupoQuestoesId(), g);
            }
        }

        List<GrupoQuestoes> gruposSincronizados = new ArrayList<>();
        int ordemGrupo = 1;

        // 1. Grupos de Autoavaliação Discente
        if (dto.getGruposAutoavaliacaoDiscente() != null) {
            for (GrupoDTO gDto : dto.getGruposAutoavaliacaoDiscente()) {
                GrupoQuestoes grupo = sincronizarGrupo(gDto, inst, SecaoInstrumento.AUTOAVALIACAO_DISCENTE, ordemGrupo++, gruposExistentes);
                gruposSincronizados.add(grupo);
            }
        }

        // 2. Grupos de Avaliação Docente
        if (dto.getGruposAvaliacaoDocente() != null) {
            for (GrupoDTO gDto : dto.getGruposAvaliacaoDocente()) {
                GrupoQuestoes grupo = sincronizarGrupo(gDto, inst, SecaoInstrumento.AVALIACAO_DOCENTE, ordemGrupo++, gruposExistentes);
                gruposSincronizados.add(grupo);
            }
        }

        inst.getGrupos().removeIf(g -> !gruposSincronizados.contains(g));
        for (GrupoQuestoes g : gruposSincronizados) {
            if (!inst.getGrupos().contains(g)) {
                inst.getGrupos().add(g);
            }
        }

        // 3. Componentes curriculares (para ADPD)
        Map<UUID, ComponenteCurricular> compsExistentes = new HashMap<>();
        for (ComponenteCurricular c : inst.getComponentes()) {
            if (c.getComponenteCurricularId() != null) {
                compsExistentes.put(c.getComponenteCurricularId(), c);
            }
        }

        List<ComponenteCurricular> compsSincronizados = new ArrayList<>();
        if (dto.getComponentesMatriculados() != null) {
            for (ComponenteDTO cDto : dto.getComponentesMatriculados()) {
                UUID cId = cDto.getComponenteCurricularId() != null ? cDto.getComponenteCurricularId() : cDto.getId();
                ComponenteCurricular comp = null;
                if (cId != null) {
                    comp = compsExistentes.get(cId);
                }
                if (comp == null) {
                    comp = new ComponenteCurricular();
                    if (cId != null) {
                        comp.setComponenteCurricularId(cId);
                    }
                    comp.setInstrumento(inst);
                }
                comp.setCodigo(cDto.getCodigo());
                comp.setNome(cDto.getNome());
                comp.setDocenteNome(cDto.getDocenteNome());
                comp.setTurma(cDto.getTurma());
                compsSincronizados.add(comp);
            }
        }

        inst.getComponentes().removeIf(c -> !compsSincronizados.contains(c));
        for (ComponenteCurricular c : compsSincronizados) {
            if (!inst.getComponentes().contains(c)) {
                inst.getComponentes().add(c);
            }
        }

        em.flush();
        return converterParaDTO(inst);
    }

    private GrupoQuestoes sincronizarGrupo(GrupoDTO gDto, Instrumento inst, SecaoInstrumento secao, int ordem, Map<UUID, GrupoQuestoes> existentes) {
        UUID id = gDto.getGrupoQuestoesId() != null ? gDto.getGrupoQuestoesId() : gDto.getId();
        GrupoQuestoes grupo = null;
        if (id != null) {
            grupo = existentes.get(id);
        }
        if (grupo == null) {
            grupo = new GrupoQuestoes();
            if (id != null) {
                grupo.setGrupoQuestoesId(id);
            }
            grupo.setInstrumento(inst);
        }

        grupo.setSecao(secao);
        grupo.setTitulo(gDto.getTitulo());
        grupo.setDescricao(gDto.getDescricao());
        grupo.setOrdem(ordem);

        sincronizarQuestoes(grupo, gDto.getQuestoes());
        return grupo;
    }

    private void sincronizarQuestoes(GrupoQuestoes grupo, List<QuestaoDTO> questoesDto) {
        if (questoesDto == null) {
            grupo.getQuestoes().clear();
            return;
        }

        Map<UUID, Questao> questoesExistentes = new HashMap<>();
        for (Questao q : grupo.getQuestoes()) {
            if (q.getQuestaoId() != null) {
                questoesExistentes.put(q.getQuestaoId(), q);
            }
        }

        List<Questao> novasQuestoes = new ArrayList<>();
        int ordemQuestao = 1;

        for (QuestaoDTO qDto : questoesDto) {
            UUID qId = qDto.getQuestaoId() != null ? qDto.getQuestaoId() : qDto.getId();
            Questao q = null;
            if (qId != null) {
                q = questoesExistentes.get(qId);
            }
            if (q == null) {
                q = new Questao();
                if (qId != null) {
                    q.setQuestaoId(qId);
                }
                q.setGrupoQuestoes(grupo);
            }

            q.setEnunciado(qDto.getEnunciado());
            q.setTipo(qDto.getTipo() != null ? qDto.getTipo() : TipoQuestao.ESCALA);
            q.setObrigatoria(qDto.isObrigatoria());
            q.setOrdem(ordemQuestao++);
            q.setEscalaMin(qDto.getEscalaMin() != null ? qDto.getEscalaMin() : 1);
            q.setEscalaMax(qDto.getEscalaMax() != null ? qDto.getEscalaMax() : 5);
            q.setRotuloMin(qDto.getRotuloMin());
            q.setRotuloMax(qDto.getRotuloMax());
            q.setAjuda(qDto.getAjuda());

            sincronizarOpcoes(q, qDto.getOpcoes());

            novasQuestoes.add(q);
        }

        grupo.getQuestoes().removeIf(q -> !novasQuestoes.contains(q));
        for (Questao q : novasQuestoes) {
            if (!grupo.getQuestoes().contains(q)) {
                grupo.getQuestoes().add(q);
            }
        }
    }

    private void sincronizarOpcoes(Questao questao, List<OpcaoRespostaDTO> opcoesDto) {
        if (opcoesDto == null) {
            questao.getOpcoes().clear();
            return;
        }

        Map<UUID, OpcaoQuestao> opcoesExistentes = new HashMap<>();
        for (OpcaoQuestao o : questao.getOpcoes()) {
            if (o.getOpcaoQuestaoId() != null) {
                opcoesExistentes.put(o.getOpcaoQuestaoId(), o);
            }
        }

        List<OpcaoQuestao> novasOpcoes = new ArrayList<>();
        int ordemOpcao = 0;

        for (OpcaoRespostaDTO oDto : opcoesDto) {
            UUID oId = oDto.getOpcaoQuestaoId() != null ? oDto.getOpcaoQuestaoId() : oDto.getId();
            OpcaoQuestao o = null;
            if (oId != null) {
                o = opcoesExistentes.get(oId);
            }
            if (o == null) {
                o = new OpcaoQuestao();
                if (oId != null) {
                    o.setOpcaoQuestaoId(oId);
                }
                o.setQuestao(questao);
            }

            o.setTexto(oDto.getTexto() != null ? oDto.getTexto() : "");
            o.setOrdem(ordemOpcao++);
            novasOpcoes.add(o);
        }

        questao.getOpcoes().removeIf(o -> !novasOpcoes.contains(o));
        for (OpcaoQuestao o : novasOpcoes) {
            if (!questao.getOpcoes().contains(o)) {
                questao.getOpcoes().add(o);
            }
        }
    }

    @Transactional
    public boolean remover(UUID id) {
        Instrumento inst = em.find(Instrumento.class, id);
        if (inst != null) {
            em.remove(inst);
            return true;
        }
        return false;
    }

    private InstrumentoDTO converterParaDTO(Instrumento inst) {
        InstrumentoDTO dto = new InstrumentoDTO();
        dto.setInstrumentoId(inst.getInstrumentoId());
        dto.setTitulo(inst.getTitulo());
        dto.setSubtitulo(inst.getSubtitulo());
        dto.setModulo(inst.getModulo());
        dto.setAnoCiclo(inst.getAnoCiclo());
        dto.setPublicoAlvo(inst.getPublicoAlvo());
        dto.setTipoAlvo(inst.getTipoAlvo() != null ? inst.getTipoAlvo().name() : "DOCENTE");
        dto.setTipoPublico(inst.getTipoPublico() != null ? inst.getTipoPublico().name() : "DISCENTES");
        dto.setRestringirVinculoCampus(inst.isRestringirVinculoCampus());
        if (inst.getPublicosPermitidos() != null && !inst.getPublicosPermitidos().trim().isEmpty()) {
            dto.setPublicosPermitidos(new ArrayList<>(java.util.Arrays.asList(inst.getPublicosPermitidos().split(","))));
        } else if (inst.getTipoPublico() != null) {
            dto.setPublicosPermitidos(new ArrayList<>(java.util.List.of(inst.getTipoPublico().name())));
        }
        dto.setAlvoEspecificoId(inst.getAlvoEspecificoId());
        if (inst.getAlvosEspecificosIds() != null && !inst.getAlvosEspecificosIds().trim().isEmpty()) {
            dto.setAlvosEspecificosIds(new ArrayList<>(java.util.Arrays.asList(inst.getAlvosEspecificosIds().split(","))));
        } else if (inst.getAlvoEspecificoId() != null) {
            dto.setAlvosEspecificosIds(new ArrayList<>(java.util.List.of(inst.getAlvoEspecificoId().toString())));
        } else {
            dto.setAlvosEspecificosIds(new ArrayList<>());
        }
        dto.setCriadorId(inst.getCriadorId());
        dto.setCriadorNome(inst.getCriadorNome());
        dto.setAnonimo(inst.isAnonimo());
        dto.setStatus(inst.getStatus());

        if (inst.getDataInicio() != null) {
            dto.setDataInicio(inst.getDataInicio().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
        if (inst.getDataTermino() != null) {
            dto.setDataTermino(inst.getDataTermino().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        // Grupos separados por seção
        for (GrupoQuestoes g : inst.getGrupos()) {
            GrupoDTO gDto = new GrupoDTO();
            gDto.setGrupoQuestoesId(g.getGrupoQuestoesId());
            gDto.setTitulo(g.getTitulo());
            gDto.setDescricao(g.getDescricao());
            gDto.setSecao(g.getSecao().name());

            for (Questao q : g.getQuestoes()) {
                QuestaoDTO qDto = new QuestaoDTO();
                qDto.setQuestaoId(q.getQuestaoId());
                qDto.setEnunciado(q.getEnunciado());
                qDto.setTipo(q.getTipo());
                qDto.setObrigatoria(q.isObrigatoria());
                qDto.setEscalaMin(q.getEscalaMin());
                qDto.setEscalaMax(q.getEscalaMax());
                qDto.setRotuloMin(q.getRotuloMin());
                qDto.setRotuloMax(q.getRotuloMax());
                qDto.setAjuda(q.getAjuda());

                if (q.getOpcoes() != null && !q.getOpcoes().isEmpty()) {
                    List<OpcaoRespostaDTO> opts = new ArrayList<>();
                    for (OpcaoQuestao opt : q.getOpcoes()) {
                        opts.add(new OpcaoRespostaDTO(opt.getOpcaoQuestaoId(), opt.getTexto()));
                    }
                    qDto.setOpcoes(opts);
                } else if (q.getOpcoesJson() != null && !q.getOpcoesJson().isEmpty()) {
                    try {
                        List<OpcaoRespostaDTO> opts = jsonb.fromJson(
                                q.getOpcoesJson(),
                                new ArrayList<OpcaoRespostaDTO>() {}.getClass().getGenericSuperclass()
                        );
                        qDto.setOpcoes(opts);
                    } catch (Exception ignored) {}
                }

                gDto.getQuestoes().add(qDto);
            }

            if (g.getSecao() == SecaoInstrumento.AUTOAVALIACAO_DISCENTE) {
                dto.getGruposAutoavaliacaoDiscente().add(gDto);
            } else {
                dto.getGruposAvaliacaoDocente().add(gDto);
            }
        }

        // Componentes curriculares
        for (ComponenteCurricular c : inst.getComponentes()) {
            dto.getComponentesMatriculados().add(new ComponenteDTO(
                    c.getComponenteCurricularId(), c.getCodigo(), c.getNome(), c.getDocenteNome(), c.getTurma()
            ));
        }

        return dto;
    }

    private OffsetDateTime converterParaOffsetDateTime(String dataStr) {
        if (dataStr == null || dataStr.trim().isEmpty()) {
            return null;
        }
        String str = dataStr.trim();
        java.time.ZoneId fuso = java.time.ZoneId.systemDefault();
        try {
            if (str.length() == 10 && str.contains("-")) {
                // Formato YYYY-MM-DD
                return java.time.LocalDate.parse(str).atStartOfDay(fuso).toOffsetDateTime();
            }
            if (str.length() == 16 && str.contains("T")) {
                // Formato YYYY-MM-DDTHH:mm (vindo do input type="datetime-local")
                return java.time.LocalDateTime.parse(str + ":00").atZone(fuso).toOffsetDateTime();
            }
            if (!str.contains("Z") && !str.contains("+") && str.lastIndexOf("-") <= 7) {
                // Formato sem timezone, ex: 2026-11-30T23:59:59
                return java.time.LocalDateTime.parse(str).atZone(fuso).toOffsetDateTime();
            }
            return OffsetDateTime.parse(str);
        } catch (Exception e) {
            try {
                if (str.length() >= 16 && str.contains("T")) {
                    return java.time.LocalDateTime.parse(str.substring(0, 16) + ":00").atZone(fuso).toOffsetDateTime();
                }
                return java.time.LocalDate.parse(str.substring(0, 10)).atStartOfDay(fuso).toOffsetDateTime();
            } catch (Exception ex) {
                return OffsetDateTime.now();
            }
        }
    }
}
