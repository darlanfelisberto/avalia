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

    public InstrumentoDTO buscarPorId(String id) {
        Instrumento inst = em.find(Instrumento.class, id);
        if (inst == null) {
            return null;
        }
        return converterParaDTO(inst);
    }

    @Transactional
    public InstrumentoDTO salvarOuAtualizar(InstrumentoDTO dto) {
        if (dto.getId() == null || dto.getId().trim().isEmpty()) {
            dto.setId("inst-" + UUID.randomUUID().toString().substring(0, 8));
        }

        Instrumento inst = em.find(Instrumento.class, dto.getId());
        if (inst == null) {
            inst = new Instrumento();
            inst.setId(dto.getId());
            inst.setCreatedAt(OffsetDateTime.now());
            em.persist(inst);
        }

        inst.setTitulo(dto.getTitulo());
        inst.setSubtitulo(dto.getSubtitulo());
        inst.setModulo(dto.getModulo() != null ? dto.getModulo() : ModuloAvaliacao.ADPD);
        inst.setAnoCiclo(dto.getAnoCiclo());
        inst.setPublicoAlvo(dto.getPublicoAlvo());
        if (dto.getTipoAlvo() != null) {
            try {
                inst.setTipoAlvo(TipoAlvoAvaliado.valueOf(dto.getTipoAlvo()));
            } catch (Exception e) {
                inst.setTipoAlvo(TipoAlvoAvaliado.DOCENTE);
            }
        }
        if (dto.getTipoPublico() != null) {
            try {
                inst.setTipoPublico(TipoPublicoAvaliador.valueOf(dto.getTipoPublico()));
            } catch (Exception e) {
                inst.setTipoPublico(TipoPublicoAvaliador.DISCENTES);
            }
        }
        inst.setAlvoEspecificoId(dto.getAlvoEspecificoId());
        if (dto.getCriadorId() != null && !dto.getCriadorId().trim().isEmpty()) {
            inst.setCriadorId(dto.getCriadorId());
        }
        if (dto.getCriadorNome() != null && !dto.getCriadorNome().trim().isEmpty()) {
            inst.setCriadorNome(dto.getCriadorNome());
        }
        inst.setAnonimo(dto.isAnonimo());
        inst.setStatus(dto.getStatus() != null ? dto.getStatus() : "ABERTO");
        inst.setUpdatedAt(OffsetDateTime.now());

        if (dto.getDataInicio() != null && !dto.getDataInicio().isEmpty()) {
            inst.setDataInicio(OffsetDateTime.parse(formatarIso(dto.getDataInicio())));
        }
        if (dto.getDataTermino() != null && !dto.getDataTermino().isEmpty()) {
            inst.setDataTermino(OffsetDateTime.parse(formatarIso(dto.getDataTermino())));
        } else {
            inst.setDataTermino(OffsetDateTime.now().plusDays(30));
        }

        // Sincronizar grupos sem recriar instâncias existentes (evita violação de chave primária no JPA/EclipseLink)
        Map<String, GrupoQuestoes> gruposExistentes = new HashMap<>();
        for (GrupoQuestoes g : inst.getGrupos()) {
            if (g.getId() != null) {
                gruposExistentes.put(g.getId(), g);
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
        Map<String, ComponenteCurricular> compsExistentes = new HashMap<>();
        for (ComponenteCurricular c : inst.getComponentes()) {
            if (c.getId() != null) {
                compsExistentes.put(c.getId(), c);
            }
        }

        List<ComponenteCurricular> compsSincronizados = new ArrayList<>();
        if (dto.getComponentesMatriculados() != null) {
            for (ComponenteDTO cDto : dto.getComponentesMatriculados()) {
                String cId = cDto.getId();
                if (cId == null || cId.trim().isEmpty()) {
                    cId = "comp-" + UUID.randomUUID().toString().substring(0, 8);
                    cDto.setId(cId);
                }

                ComponenteCurricular comp = compsExistentes.get(cId);
                if (comp == null) {
                    comp = new ComponenteCurricular();
                    comp.setId(cId);
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

    private GrupoQuestoes sincronizarGrupo(GrupoDTO gDto, Instrumento inst, SecaoInstrumento secao, int ordem, Map<String, GrupoQuestoes> existentes) {
        String id = gDto.getId();
        if (id == null || id.trim().isEmpty()) {
            id = "grp-" + UUID.randomUUID().toString().substring(0, 8);
            gDto.setId(id);
        }

        GrupoQuestoes grupo = existentes.get(id);
        if (grupo == null) {
            grupo = new GrupoQuestoes();
            grupo.setId(id);
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

        Map<String, Questao> questoesExistentes = new HashMap<>();
        for (Questao q : grupo.getQuestoes()) {
            if (q.getId() != null) {
                questoesExistentes.put(q.getId(), q);
            }
        }

        List<Questao> novasQuestoes = new ArrayList<>();
        int ordemQuestao = 1;

        for (QuestaoDTO qDto : questoesDto) {
            String qId = qDto.getId();
            if (qId == null || qId.trim().isEmpty()) {
                qId = "q-" + UUID.randomUUID().toString().substring(0, 8);
                qDto.setId(qId);
            }

            Questao q = questoesExistentes.get(qId);
            if (q == null) {
                q = new Questao();
                q.setId(qId);
                q.setGrupo(grupo);
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

        Map<String, OpcaoQuestao> opcoesExistentes = new HashMap<>();
        for (OpcaoQuestao o : questao.getOpcoes()) {
            if (o.getId() != null) {
                opcoesExistentes.put(o.getId(), o);
            }
        }

        List<OpcaoQuestao> novasOpcoes = new ArrayList<>();
        int ordemOpcao = 0;

        for (OpcaoRespostaDTO oDto : opcoesDto) {
            String oId = oDto.getId();
            if (oId == null || oId.trim().isEmpty()) {
                oId = "opt-" + UUID.randomUUID().toString().substring(0, 8);
                oDto.setId(oId);
            }

            OpcaoQuestao o = opcoesExistentes.get(oId);
            if (o == null) {
                o = new OpcaoQuestao();
                o.setId(oId);
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
    public boolean remover(String id) {
        Instrumento inst = em.find(Instrumento.class, id);
        if (inst != null) {
            em.remove(inst);
            return true;
        }
        return false;
    }

    private InstrumentoDTO converterParaDTO(Instrumento inst) {
        InstrumentoDTO dto = new InstrumentoDTO();
        dto.setId(inst.getId());
        dto.setTitulo(inst.getTitulo());
        dto.setSubtitulo(inst.getSubtitulo());
        dto.setModulo(inst.getModulo());
        dto.setAnoCiclo(inst.getAnoCiclo());
        dto.setPublicoAlvo(inst.getPublicoAlvo());
        dto.setTipoAlvo(inst.getTipoAlvo() != null ? inst.getTipoAlvo().name() : "DOCENTE");
        dto.setTipoPublico(inst.getTipoPublico() != null ? inst.getTipoPublico().name() : "DISCENTES");
        dto.setAlvoEspecificoId(inst.getAlvoEspecificoId());
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
            gDto.setId(g.getId());
            gDto.setTitulo(g.getTitulo());
            gDto.setDescricao(g.getDescricao());
            gDto.setSecao(g.getSecao().name());

            for (Questao q : g.getQuestoes()) {
                QuestaoDTO qDto = new QuestaoDTO();
                qDto.setId(q.getId());
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
                        opts.add(new OpcaoRespostaDTO(opt.getId(), opt.getTexto()));
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
                    c.getId(), c.getCodigo(), c.getNome(), c.getDocenteNome(), c.getTurma()
            ));
        }

        return dto;
    }

    private String formatarIso(String dataStr) {
        if (dataStr.length() == 10) { // Formato YYYY-MM-DD
            return dataStr + "T00:00:00Z";
        }
        return dataStr;
    }
}
