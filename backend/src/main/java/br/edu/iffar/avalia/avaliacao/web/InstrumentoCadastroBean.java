package br.edu.iffar.avalia.avaliacao.web;

import br.edu.iffar.avalia.avaliacao.dto.InstrumentoCadastroDTO;
import br.edu.iffar.avalia.avaliacao.dto.PermissaoTipoEntidadeDTO;
import br.edu.iffar.avalia.avaliacao.dto.TipoEntidadeDTO;
import br.edu.iffar.avalia.avaliacao.service.InstrumentoCadastroService;
import br.edu.iffar.avalia.core.exception.ValidacaoException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;

@Named("instrumentoCadastro")
@RequestScoped
public class InstrumentoCadastroBean {
    @Inject private InstrumentoCadastroService service;

    private InstrumentoCadastroDTO cadastro;
    private List<TipoEntidadeDTO> tipos;
    private List<PermissaoTipoEntidadeDTO> permissoes;

    @PostConstruct
    public void iniciar() {
        cadastro = service.novo();
    }

    public void carregar() {
        if (cadastro.getId() == null || cadastro.getId().isBlank()) return;
        try {
            cadastro = service.buscar(cadastro.getId());
        } catch (RuntimeException e) {
            mensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    public void salvar() {
        try {
            cadastro = service.salvar(cadastro);
            mensagem(FacesMessage.SEVERITY_INFO, "Instrumento salvo com sucesso.");
        } catch (ValidacaoException | SecurityException e) {
            mensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            mensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível salvar o instrumento.");
        }
    }

    public void aoAlterarTipoAvaliado() {
        cadastro.setRespondentesIds(new String[0]);
    }

    public List<SelectItem> getTiposAvaliados() {
        return tipos().stream().filter(t -> t.podeSerAvaliada)
                .map(t -> new SelectItem(t.id, t.nome)).toList();
    }

    public List<SelectItem> getRespondentesDisponiveis() {
        String tipoAvaliado = cadastro.getTipoAvaliadoId();
        if (tipoAvaliado == null || tipoAvaliado.isBlank()) return List.of();
        var permitidos = permissoes().stream()
                .filter(p -> tipoAvaliado.equals(p.tipoAvaliadoId))
                .map(p -> p.tipoRespondenteId).collect(java.util.stream.Collectors.toSet());
        return tipos().stream().filter(t -> t.podeResponder && permitidos.contains(t.id))
                .map(t -> new SelectItem(t.id, t.nome)).toList();
    }

    public boolean isSomenteLeitura() {
        return cadastro.getStatus() != null && !"RASCUNHO".equals(cadastro.getStatus());
    }

    private List<TipoEntidadeDTO> tipos() {
        if (tipos == null) tipos = service.listarTipos();
        return tipos;
    }

    private List<PermissaoTipoEntidadeDTO> permissoes() {
        if (permissoes == null) permissoes = service.listarPermissoes();
        return permissoes;
    }

    private void mensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }

    public InstrumentoCadastroDTO getCadastro() { return cadastro; }
    public void setCadastro(InstrumentoCadastroDTO cadastro) { this.cadastro = cadastro; }
    public String getId() { return cadastro.getId(); }
    public void setId(String id) { cadastro.setId(id); }
}
