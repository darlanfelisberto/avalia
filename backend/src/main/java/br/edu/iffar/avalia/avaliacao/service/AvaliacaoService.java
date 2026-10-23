package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dao.InstrumentoAvaliativoDAO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.*;
import br.edu.iffar.avalia.avaliacao.dao.EntidadeAcademicaDao;
import br.edu.iffar.avalia.avaliacao.dao.TipoEntidadeDao;
import br.edu.iffar.avalia.avaliacao.model.TipoEntidade;
import br.edu.iffar.avalia.avaliacao.model.ConjuntoOpcao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoRespondente;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoSessao;
import br.edu.iffar.avalia.avaliacao.model.InstrumentoTipoAlvo;
import br.edu.iffar.avalia.avaliacao.model.OpcaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.Questao;
import br.edu.iffar.avalia.avaliacao.model.SessaoQuestao;
import br.edu.iffar.avalia.avaliacao.model.StatusInstrumentoAvaliativo;
import br.edu.iffar.avalia.avaliacao.model.TipoSessao;
import br.edu.iffar.avalia.core.exception.ValidacaoException;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/** Regras de criação, publicação e composição dos instrumentos de avaliação. */
@ApplicationScoped
@Transactional
public class AvaliacaoService {

    @Inject
    private InstrumentoAvaliativoDAO instrumentoAvaliativoDAO;

    @Inject
    private EntidadeAcademicaDao entidadeAcademicaDao;

    @Inject
    private TipoEntidadeDao tiposEntidadeDao;

    @Inject
    private AuthService auth;

    @Inject
    private Validator validator;

    // ------------------------------------------------------------------ catálogos

    /** Atualiza os espelhos avaliativos a partir dos dados acadêmicos já sincronizados. */
    public void sincronizarEntidadesAcademicas() {
        entidadeAcademicaDao.sincronizar();
    }

    public List<ConjuntoOpcaoDTO> listarConjuntos() {
        List<ConjuntoOpcaoDTO> resultado = new ArrayList<>();
        for (ConjuntoOpcao entidade : instrumentoAvaliativoDAO.listarConjuntos()) {
            resultado.add(ConjuntoOpcaoDTO.create(entidade));
        }
        return resultado;
    }

    public ConjuntoOpcaoDTO salvarConjunto(ConjuntoOpcaoDTO dto) {
        if (dto == null) throw new ValidacaoException("Dados do conjunto não informados.");
        validarBean(dto);
        ConjuntoOpcao conjunto = new ConjuntoOpcao();
        conjunto.setNome(dto.nome.trim());
        int ordem = 0;
        for (OpcaoQuestaoDTO item : dto.opcoes) {
            OpcaoQuestao opcao = new OpcaoQuestao();
            opcao.setConjunto(conjunto);
            opcao.setTexto(item.texto.trim());
            opcao.setValor(item.valor == null ? null : BigDecimal.valueOf(item.valor));
            opcao.setCorreta(item.correta);
            opcao.setOrdem(ordem++);
            conjunto.getOpcoes().add(opcao);
        }
        instrumentoAvaliativoDAO.persistir(conjunto);
        instrumentoAvaliativoDAO.flush();
        return ConjuntoOpcaoDTO.create(conjunto);
    }


    public List<InstrumentoAvaliativoDTO> listarInstrumentos(int paginaSolicitada, int tamanhoSolicitado) {
        if (tamanhoSolicitado < 1 || tamanhoSolicitado > 100)
            throw new ValidacaoException("O tamanho da página deve estar entre 1 e 100.");

        List<InstrumentoAvaliativoDTO> resultado = new ArrayList<>();
        for (var instrimento : instrumentoAvaliativoDAO.listInstrumentos(paginaSolicitada, tamanhoSolicitado)) {
            resultado.add(new InstrumentoAvaliativoDTO()
                    .createResumo(instrimento));
        }
        return resultado;
    }

    public InstrumentoAvaliativoDTO buscarInstrumento(String id) {
        InstrumentoAvaliativo inst = instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(id));
        return new InstrumentoAvaliativoDTO().createDetalhe(inst);
    }

    public InstrumentoAvaliativoDTO buscarCadastro(String id) {
        InstrumentoAvaliativo inst = instrumentoAvaliativoDAO.buscar(InstrumentoAvaliativo.class, UUID.fromString(id));
        if (inst == null) throw new ValidacaoException("Instrumento não encontrado.");
        return new InstrumentoAvaliativoDTO().comDadosBasicos(inst).comRespondentes(inst);
    }

    public InstrumentoAvaliativoDTO salvarInstrumento(InstrumentoAvaliativoDTO dto) {
        if (dto == null)
            throw new ValidacaoException("Dados do instrumento não informados.");
        validarBean(dto);

        InstrumentoAvaliativo entidade = blank(dto.id) ? null : instrumentoAvaliativoDAO.buscar(
                InstrumentoAvaliativo.class, UUID.fromString(dto.id));

        if (!blank(dto.id) && entidade == null)
            throw new ValidacaoException("Instrumento não encontrado.");

        boolean rascunho = entidade == null || entidade.getStatus() == StatusInstrumentoAvaliativo.RASCUNHO;

        boolean novo = entidade == null;
        if (novo) {
            entidade = new InstrumentoAvaliativo();
            entidade.setCriadoPor(auth.getCpf());
        }
        if (rascunho) entidade.setTipoEntidadeAvaliada(tiposEntidadeDao.referencia(UUID.fromString(dto.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId)));
        entidade.setTitulo(trimOrNull(dto.titulo));
        entidade.setDescricao(dto.descricao);
        entidade.setCicloReferencia(trimOrNull(dto.cicloReferencia));
        entidade.setInicioAplicacao(parseData(dto.inicioAplicacao, "início da aplicação"));
        entidade.setFimAplicacao(parseData(dto.fimAplicacao, "término da aplicação"));
        entidade.setAnonimo(dto.anonimo);
        entidade.setPermiteEdicao(dto.permiteEdicao);
        entidade.setAtualizadoEm(OffsetDateTime.now());
        if (novo) instrumentoAvaliativoDAO.persistir(entidade);
        instrumentoAvaliativoDAO.flush();
        String id = entidade.getId().toString();
        if (rascunho) instrumentoAvaliativoDAO.substituirTipoAlvo(entidade,
                UUID.fromString(dto.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId));

        // Respondentes e estrutura só podem mudar enquanto o instrumento é rascunho
        if (rascunho) {
            substituirRespondentes(entidade, dto.respondentes);
//            substituirEstrutura(entidade, dto.paginas, dto.sessoes, usuario);
        }
        return buscarCadastro(id);
    }

    public InstrumentoAvaliativoDTO salvarEstrutura(String id, EstruturaInstrumentoDTO estrutura) {
        if (estrutura == null) throw new ValidacaoException("Estrutura não informada.");
        validarBean(estrutura);
        InstrumentoAvaliativo instrumento = instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(id));
        if (instrumento == null) throw new ValidacaoException("Instrumento não encontrado.");
        if (instrumento.getStatus() != StatusInstrumentoAvaliativo.RASCUNHO)
            throw new ValidacaoException("A estrutura só pode ser editada enquanto o instrumento é rascunho.");

        InstrumentoAvaliativoDTO dados = new InstrumentoAvaliativoDTO().createDetalhe(instrumento);
        dados.paginas = estrutura.paginas == null ? new ArrayList<>() : estrutura.paginas;
        dados.sessoes = estrutura.sessoes == null ? new ArrayList<>() : estrutura.sessoes;

        instrumento.setAtualizadoEm(OffsetDateTime.now());
        instrumentoAvaliativoDAO.flush();
        return new InstrumentoAvaliativoDTO().createDetalhe(instrumento);
    }

    @Transactional
    public InstrumentoAvaliativoDTO publicar(String id) {
        return new InstrumentoAvaliativoDTO();
    }

    public boolean excluir(String id) {
        InstrumentoAvaliativoDTO i = buscarInstrumento(id);
        if (i == null) return false;
        if (!StatusInstrumentoAvaliativo.RASCUNHO.name().equals(i.status)) throw new ValidacaoException("Só é possível excluir instrumentos em rascunho.");
//        List<String> questoes = idsQuestoes(id);
        instrumentoAvaliativoDAO.remover(instrumentoAvaliativoDAO.findInstrumentosById(UUID.fromString(id)));
        instrumentoAvaliativoDAO.flush();
//        removerQuestoesOrfas(questoes);
        return true;
    }

    // ------------------------------------------------------------------ internos

    private void substituirRespondentes(InstrumentoAvaliativo instrumento,
                                        List<AvaliacaoDTO.InstrumentoTipoRespondenteDTO> respondentes) {
        instrumentoAvaliativoDAO.removerRespondentes(instrumento);
        for (AvaliacaoDTO.InstrumentoTipoRespondenteDTO r : respondentes) {
            InstrumentoTipoRespondente entidade = new InstrumentoTipoRespondente();
            entidade.setInstrumentoAvaliativo(instrumento);
            entidade.setTipoEntidade(tiposEntidadeDao.referencia(UUID.fromString(r.tipoEntidadeId)));
            instrumentoAvaliativoDAO.persistir(entidade);
            instrumento.getInstrumentoTipoRespondenteList().add(entidade);
        }
        instrumentoAvaliativoDAO.flush();
    }

    private InstrumentoSessao criarSessaoValidacao(String titulo, TipoSessao tipo,
                                                    InstrumentoAvaliativo instrumento, int ordem) {
        InstrumentoSessao sessao = new InstrumentoSessao();
        sessao.setInstrumentoAvaliativo(instrumento);
        sessao.setTipoSessao(tipo);
        sessao.setTitulo(titulo);
        sessao.setOrdem(ordem);
        return sessao;
    }

    private <T> void validarBean(T objeto) {
        var violacoes = validator.validate(objeto);
        if (!violacoes.isEmpty()) {
            String mensagens = violacoes.stream()
                    .map(violacao -> violacao.getMessage())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(" "));
            throw new ValidacaoException(mensagens);
        }
    }

    private OffsetDateTime parseData(String valor, String campo) {
        if (blank(valor)) return null;
        try {
            return OffsetDateTime.parse(valor);
        } catch (java.time.format.DateTimeParseException e) {
            throw new ValidacaoException("Formato inválido para " + campo + ". Use uma data ISO-8601.", e);
        }
    }

    private static String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

}
