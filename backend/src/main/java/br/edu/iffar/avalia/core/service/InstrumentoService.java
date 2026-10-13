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
import java.util.List;
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

        // Limpar grupos antigos e reassociar
        inst.getGrupos().clear();
        int ordemGrupo = 1;

        // 1. Grupos de Autoavaliação Discente
        if (dto.getGruposAutoavaliacaoDiscente() != null) {
            for (GrupoDTO gDto : dto.getGruposAutoavaliacaoDiscente()) {
                GrupoQuestoes grupo = new GrupoQuestoes();
                grupo.setId(gDto.getId() != null ? gDto.getId() : "grp-" + UUID.randomUUID().toString().substring(0, 8));
                grupo.setInstrumento(inst);
                grupo.setSecao(SecaoInstrumento.AUTOAVALIACAO_DISCENTE);
                grupo.setTitulo(gDto.getTitulo());
                grupo.setDescricao(gDto.getDescricao());
                grupo.setOrdem(ordemGrupo++);

                associarQuestoes(grupo, gDto.getQuestoes());
                inst.getGrupos().add(grupo);
            }
        }

        // 2. Grupos de Avaliação Docente
        if (dto.getGruposAvaliacaoDocente() != null) {
            for (GrupoDTO gDto : dto.getGruposAvaliacaoDocente()) {
                GrupoQuestoes grupo = new GrupoQuestoes();
                grupo.setId(gDto.getId() != null ? gDto.getId() : "grp-" + UUID.randomUUID().toString().substring(0, 8));
                grupo.setInstrumento(inst);
                grupo.setSecao(SecaoInstrumento.AVALIACAO_DOCENTE);
                grupo.setTitulo(gDto.getTitulo());
                grupo.setDescricao(gDto.getDescricao());
                grupo.setOrdem(ordemGrupo++);

                associarQuestoes(grupo, gDto.getQuestoes());
                inst.getGrupos().add(grupo);
            }
        }

        // 3. Componentes curriculares (para ADPD)
        inst.getComponentes().clear();
        if (dto.getComponentesMatriculados() != null) {
            for (ComponenteDTO cDto : dto.getComponentesMatriculados()) {
                ComponenteCurricular comp = new ComponenteCurricular();
                comp.setId(cDto.getId() != null ? cDto.getId() : "comp-" + UUID.randomUUID().toString().substring(0, 8));
                comp.setInstrumento(inst);
                comp.setCodigo(cDto.getCodigo());
                comp.setNome(cDto.getNome());
                comp.setDocenteNome(cDto.getDocenteNome());
                comp.setTurma(cDto.getTurma());
                inst.getComponentes().add(comp);
            }
        }

        em.flush();
        return converterParaDTO(inst);
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

    private void associarQuestoes(GrupoQuestoes grupo, List<QuestaoDTO> questoesDto) {
        if (questoesDto == null) return;
        int ordemQuestao = 1;
        for (QuestaoDTO qDto : questoesDto) {
            Questao q = new Questao();
            q.setId(qDto.getId() != null ? qDto.getId() : "q-" + UUID.randomUUID().toString().substring(0, 8));
            q.setGrupo(grupo);
            q.setEnunciado(qDto.getEnunciado());
            q.setTipo(qDto.getTipo() != null ? qDto.getTipo() : TipoQuestao.ESCALA);
            q.setObrigatoria(qDto.isObrigatoria());
            q.setOrdem(ordemQuestao++);
            q.setEscalaMin(qDto.getEscalaMin() != null ? qDto.getEscalaMin() : 1);
            q.setEscalaMax(qDto.getEscalaMax() != null ? qDto.getEscalaMax() : 5);
            q.setRotuloMin(qDto.getRotuloMin());
            q.setRotuloMax(qDto.getRotuloMax());
            q.setAjuda(qDto.getAjuda());

            if (qDto.getOpcoes() != null && !qDto.getOpcoes().isEmpty()) {
                q.setOpcoesJson(jsonb.toJson(qDto.getOpcoes()));
            }

            grupo.getQuestoes().add(q);
        }
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

                if (q.getOpcoesJson() != null && !q.getOpcoesJson().isEmpty()) {
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
