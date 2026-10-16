package br.edu.iffar.avalia.avaliacao.dto;

import br.edu.iffar.avalia.avaliacao.model.TipoEntidadePermissao;

public class PermissaoTipoEntidadeDTO {
    public String tipoRespondenteId;
    public String tipoAvaliadoId;

    public static PermissaoTipoEntidadeDTO create(TipoEntidadePermissao entidade) {
        PermissaoTipoEntidadeDTO dto = new PermissaoTipoEntidadeDTO();
        dto.tipoRespondenteId = entidade.getRespondente().getId().toString();
        dto.tipoAvaliadoId = entidade.getAvaliado().getId().toString();
        return dto;
    }
}
