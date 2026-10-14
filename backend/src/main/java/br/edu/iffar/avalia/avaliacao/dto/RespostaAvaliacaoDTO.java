package br.edu.iffar.avalia.avaliacao.dto;

import java.util.ArrayList;
import java.util.List;

public final class RespostaAvaliacaoDTO {
    private RespostaAvaliacaoDTO() {}

    public static class Disponivel {
        public String instrumentoId;
        public String titulo;
        public String descricao;
        public String modulo;
        public String alvoId;
        public String alvoNome;
        public boolean respondido;
    }

    public static class Item {
        public String sessaoQuestaoId;
        public List<String> opcaoIds = new ArrayList<>();
        public String texto;
    }

    public static class Envio {
        public String alvoId;
        public List<Item> respostas = new ArrayList<>();
    }
}
