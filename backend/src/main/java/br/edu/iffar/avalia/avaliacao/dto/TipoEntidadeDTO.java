package br.edu.iffar.avalia.avaliacao.dto;

import br.edu.iffar.avalia.avaliacao.model.TipoEntidade;

public class TipoEntidadeDTO {
    public String id;
    public String codigo;
    public String nome;
    public boolean podeSerAvaliada;
    public boolean podeResponder;

    public static TipoEntidadeDTO create(TipoEntidade entidade) {
        TipoEntidadeDTO dto = new TipoEntidadeDTO();
        dto.id = entidade.getId().toString();
        dto.codigo = entidade.getCodigo();
        dto.nome = entidade.getNome();
        dto.podeSerAvaliada = entidade.isPodeSerAvaliada();
        dto.podeResponder = entidade.isPodeResponder();
        return dto;
    }
}
