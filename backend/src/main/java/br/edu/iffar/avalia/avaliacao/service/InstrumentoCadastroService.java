package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.InstrumentoAvaliativoDTO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.InstrumentoTipoRespondenteDTO;
import br.edu.iffar.avalia.avaliacao.dto.AvaliacaoDTO.TipoEntidadeAvaliadaDTO;
import br.edu.iffar.avalia.avaliacao.dto.InstrumentoCadastroDTO;
import br.edu.iffar.avalia.avaliacao.dto.PermissaoTipoEntidadeDTO;
import br.edu.iffar.avalia.avaliacao.dto.TipoEntidadeDTO;
import br.edu.iffar.avalia.core.exception.ValidacaoException;
import br.edu.iffar.avalia.core.service.AuthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Adapta o cadastro JSF ao mesmo fluxo de persistência usado pelo cadastro REST. */
@ApplicationScoped
public class InstrumentoCadastroService {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter CAMPO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Inject private AvaliacaoService avaliacaoService;
    @Inject private TipoEntidadeService tipoEntidadeService;
    @Inject private AuthService auth;

    public List<TipoEntidadeDTO> listarTipos() { return tipoEntidadeService.listar(); }
    public List<PermissaoTipoEntidadeDTO> listarPermissoes() { return tipoEntidadeService.listarPermissoes(); }

    public InstrumentoCadastroDTO novo() {
        InstrumentoCadastroDTO dto = new InstrumentoCadastroDTO();
        LocalDateTime agora = LocalDateTime.now(FUSO);
        dto.setCicloReferencia(agora.getYear() + "/" + (agora.getMonthValue() <= 6 ? 1 : 2));
        dto.setInicioAplicacao(agora.format(CAMPO_DATA));
        dto.setFimAplicacao(agora.plusDays(30).format(CAMPO_DATA));
        return dto;
    }

    public InstrumentoCadastroDTO buscar(String id) {
        exigirPermissao("INSTRUMENTO_VISUALIZAR");
        return converter(avaliacaoService.buscarCadastro(id));
    }

    public InstrumentoCadastroDTO salvar(InstrumentoCadastroDTO cadastro) {
        exigirPermissao(cadastro.getId() == null || cadastro.getId().isBlank()
                ? "INSTRUMENTO_CRIAR" : "INSTRUMENTO_EDITAR");
        if (cadastro.getId() != null && !cadastro.getId().isBlank()
                && !"RASCUNHO".equals(avaliacaoService.buscarCadastro(cadastro.getId()).status))
            throw new ValidacaoException("Apenas rascunhos podem ser editados neste formulário.");
        validarPublicos(cadastro);
        InstrumentoAvaliativoDTO dto = new InstrumentoAvaliativoDTO();
        dto.id = cadastro.getId();
        dto.titulo = cadastro.getTitulo();
        dto.descricao = cadastro.getDescricao();
        dto.cicloReferencia = cadastro.getCicloReferencia();
        dto.inicioAplicacao = converterData(cadastro.getInicioAplicacao(), "início da aplicação");
        dto.fimAplicacao = converterData(cadastro.getFimAplicacao(), "término da aplicação");
        dto.anonimo = cadastro.isAnonimo();
        dto.permiteEdicao = cadastro.isPermiteEdicao();
        dto.tipoEntidadeAvaliada = new TipoEntidadeAvaliadaDTO();
        dto.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId = cadastro.getTipoAvaliadoId();
        for (String tipoId : cadastro.getRespondentesIds()) {
            InstrumentoTipoRespondenteDTO respondente = new InstrumentoTipoRespondenteDTO();
            respondente.tipoEntidadeId = tipoId;
            dto.respondentes.add(respondente);
        }
        return converter(avaliacaoService.salvarInstrumento(dto));
    }

    private void validarPublicos(InstrumentoCadastroDTO cadastro) {
        if (cadastro.getTipoAvaliadoId() == null || cadastro.getTipoAvaliadoId().isBlank())
            throw new ValidacaoException("Selecione o que será avaliado.");
        Set<String> permitidos = listarPermissoes().stream()
                .filter(p -> cadastro.getTipoAvaliadoId().equals(p.tipoAvaliadoId))
                .map(p -> p.tipoRespondenteId).collect(Collectors.toSet());
        if (Arrays.stream(cadastro.getRespondentesIds()).distinct().count() != cadastro.getRespondentesIds().length
                || Arrays.stream(cadastro.getRespondentesIds()).anyMatch(id -> !permitidos.contains(id)))
            throw new ValidacaoException("O público respondente não é permitido para o tipo avaliado.");
    }

    private String converterData(String valor, String campo) {
        try {
            return LocalDateTime.parse(valor, CAMPO_DATA).atZone(FUSO).toOffsetDateTime().toString();
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ValidacaoException("Informe uma data válida para " + campo + ".");
        }
    }

    private InstrumentoCadastroDTO converter(InstrumentoAvaliativoDTO origem) {
        InstrumentoCadastroDTO dto = new InstrumentoCadastroDTO();
        dto.setId(origem.id);
        dto.setTitulo(origem.titulo);
        dto.setDescricao(origem.descricao);
        dto.setCicloReferencia(origem.cicloReferencia);
        dto.setInicioAplicacao(OffsetDateTime.parse(origem.inicioAplicacao).atZoneSameInstant(FUSO).format(CAMPO_DATA));
        dto.setFimAplicacao(OffsetDateTime.parse(origem.fimAplicacao).atZoneSameInstant(FUSO).format(CAMPO_DATA));
        dto.setTipoAvaliadoId(origem.tipoEntidadeAvaliada.tipoEntidadeAvaliadaId);
        dto.setRespondentesIds(origem.respondentes.stream().map(r -> r.tipoEntidadeId).toArray(String[]::new));
        dto.setAnonimo(origem.anonimo);
        dto.setPermiteEdicao(origem.permiteEdicao);
        dto.setStatus(origem.status);
        return dto;
    }

    private void exigirPermissao(String permissao) {
        if (!auth.validarJwt().getPermissoes().contains(permissao))
            throw new SecurityException("Você não tem permissão para esta operação.");
    }
}
