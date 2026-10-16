package br.edu.iffar.avalia.core.service;

import br.edu.iffar.avalia.core.dto.AuthResponseDTO;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.*;

@RequestScoped
public class AuthService {
    private static final List<String> PAPEIS_OIDC = List.of("ADMIN", "CPA", "DOCENTE", "DISCENTE", "TAE");
    private static final List<String> PERMISSOES_OIDC = List.of(
            "INSTRUMENTO_CRIAR", "INSTRUMENTO_EDITAR", "INSTRUMENTO_EXCLUIR",
            "INSTRUMENTO_VISUALIZAR", "AVALIACAO_RESPONDER", "SIGAA_SINCRONIZAR");

    @Inject
    private JsonWebToken jwt;

    public String normalizarCpf(String cpf) {
        if (cpf == null) return "";
        String digitos = cpf.replaceAll("\\D", "").trim();
        if (digitos.length() > 0 && digitos.length() < 11) {
            digitos = String.format("%011d", Long.parseLong(digitos));
        }
        return digitos;
    }

    public String formatarCpf(String cpf) {
        String limpo = normalizarCpf(cpf);
        if (limpo.length() == 11) {
            return String.format("%s.%s.%s-%s",
                    limpo.substring(0, 3),
                    limpo.substring(3, 6),
                    limpo.substring(6, 9),
                    limpo.substring(9, 11));
        }
        return cpf != null ? cpf : "";
    }

    public AuthResponseDTO autenticarUsuarioOidc(OidcClientService.IdentidadeOidc identidade) {
        UsuarioDTO usuario = montarUsuario(identidade.cpf(), identidade.nome(), identidade.email(), identidade.grupos());
        return new AuthResponseDTO(true, "Login realizado com sucesso.", usuario);
    }

    private UsuarioDTO montarUsuario(String cpfEntrada, String nome, String email, List<String> grupos) {
        String cpf = normalizarCpf(cpfEntrada);
        if (!cpf.matches("\\d{11}")) throw new SecurityException("O token não contém um CPF válido.");
        Set<String> gruposConfiaveis = new HashSet<>(grupos);
        List<String> papeis = new ArrayList<>();
        for (String papel : PAPEIS_OIDC)
            if (gruposConfiaveis.contains(papel)) papeis.add(papel);
        if (papeis.isEmpty()) throw new SecurityException("O token não contém um perfil institucional reconhecido.");
        List<String> permissoes = PERMISSOES_OIDC.stream().filter(gruposConfiaveis::contains).toList();
        if (permissoes.isEmpty()) throw new SecurityException("Usuário sem permissões no provedor OIDC.");

        UsuarioDTO usuario = new UsuarioDTO();
        usuario.setCpf(cpf);
        usuario.setCpfFormatado(formatarCpf(cpf));
        usuario.setNome(nome == null || nome.isBlank() ? cpf : nome);
        usuario.setEmail(email);
        usuario.setCampus("Geral");
        usuario.setPapeis(papeis);
        usuario.setPapelAtivo(papeis.get(0));
        usuario.setPermissoes(permissoes);
        return usuario;
    }

    public UsuarioDTO validarJwt() {
        if (jwt == null)
            throw new SecurityException("Token de acesso inválido.");
        return montarUsuario(jwt.getClaim("preferred_username"), jwt.getClaim("name"),
                jwt.getClaim("email"), new ArrayList<>(jwt.getGroups()));
    }

}
