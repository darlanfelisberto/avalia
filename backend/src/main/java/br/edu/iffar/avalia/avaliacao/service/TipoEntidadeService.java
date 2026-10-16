package br.edu.iffar.avalia.avaliacao.service;

import br.edu.iffar.avalia.avaliacao.dao.TipoEntidadeDao;
import br.edu.iffar.avalia.avaliacao.dto.PermissaoTipoEntidadeDTO;
import br.edu.iffar.avalia.avaliacao.dto.TipoEntidadeDTO;
import br.edu.iffar.avalia.avaliacao.model.TipoEntidadePermissao;
import br.edu.iffar.avalia.core.exception.ValidacaoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Regras de consulta e configuração dos tipos de entidade avaliadora e avaliada. */
@ApplicationScoped
@Transactional
public class TipoEntidadeService {
    @Inject
    private TipoEntidadeDao dao;

    public List<TipoEntidadeDTO> listar() {
        List<TipoEntidadeDTO> resultado = new ArrayList<>();
        for (var tipo : dao.listar()) {
            resultado.add(TipoEntidadeDTO.create(tipo));
        }
        return resultado;
    }

    public List<PermissaoTipoEntidadeDTO> listarPermissoes() {
        List<PermissaoTipoEntidadeDTO> resultado = new ArrayList<>();
        for (var permissao : dao.listarPermissoes()) {
            resultado.add(PermissaoTipoEntidadeDTO.create(permissao));
        }
        return resultado;
    }

    public void salvarPermissoes(List<PermissaoTipoEntidadeDTO> permissoes) {
        if (permissoes == null) throw new ValidacaoException("Informe a matriz de permissões.");

        Map<UUID, br.edu.iffar.avalia.avaliacao.model.TipoEntidade> tipos = new HashMap<>();
        for (var tipo : dao.listar()) tipos.put(tipo.getId(), tipo);
        Set<String> pares = new HashSet<>();
        List<UUID[]> paresValidos = new ArrayList<>();

        for (PermissaoTipoEntidadeDTO permissao : permissoes) {
            if (permissao == null || emBranco(permissao.tipoRespondenteId) || emBranco(permissao.tipoAvaliadoId))
                throw new ValidacaoException("Cada permissão precisa de um tipo respondente e um tipo avaliado.");
            UUID respondenteId = UUID.fromString(permissao.tipoRespondenteId);
            UUID avaliadoId = UUID.fromString(permissao.tipoAvaliadoId);
            if (!pares.add(respondenteId + ":" + avaliadoId))
                throw new ValidacaoException("A matriz contém uma permissão repetida.");

            var respondente = tipos.get(respondenteId);
            var avaliado = tipos.get(avaliadoId);
            if (respondente == null || !respondente.isPodeResponder())
                throw new ValidacaoException("O tipo selecionado não pode responder avaliações.");
            if (avaliado == null || !avaliado.isPodeSerAvaliada())
                throw new ValidacaoException("O tipo selecionado não pode ser avaliado.");
            paresValidos.add(new UUID[]{respondenteId, avaliadoId});
        }

        dao.removerPermissoes();
        List<TipoEntidadePermissao> entidades = new ArrayList<>();
        for (UUID[] par : paresValidos) {
            TipoEntidadePermissao permissao = new TipoEntidadePermissao();
            permissao.setRespondente(dao.referencia(par[0]));
            permissao.setAvaliado(dao.referencia(par[1]));
            entidades.add(permissao);
        }
        dao.salvarPermissoes(entidades);
    }

    private boolean emBranco(String valor) {
        return valor == null || valor.isBlank();
    }
}
