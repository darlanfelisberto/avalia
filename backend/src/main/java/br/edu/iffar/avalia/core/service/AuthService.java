package br.edu.iffar.avalia.core.service;

import br.edu.iffar.avalia.core.dto.AuthResponseDTO;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import br.edu.iffar.avalia.core.dao.AuthDao;
import br.edu.iffar.avalia.core.model.AuthSessao;
import br.edu.iffar.avalia.core.model.UsuarioPermissao;
import br.edu.iffar.avalia.academico.model.PessoaAcademica;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@ApplicationScoped
public class AuthService {

    @Inject
    private AuthDao dao;

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

    @Transactional
    public AuthResponseDTO autenticarUsuarioOidc(String cpfEntrada) {
        String cpfLimpo = normalizarCpf(cpfEntrada);
        if (cpfLimpo.isEmpty()) {
            return new AuthResponseDTO(false, null, "Informe o CPF para identificação.", null);
        }

        // 1. Consulta inicial na tabela de permissões do Avalia
        UsuarioPermissao up = dao.buscarUsuarioAtivo(cpfLimpo);
        if (up != null) {
            UsuarioDTO dto = montarUsuarioDTO(up);
            String token = UUID.randomUUID().toString();
            registrarSessao(token, dto);
            return new AuthResponseDTO(true, token, "Login realizado com sucesso.", dto);
        }

        // 2. Consulta de fallback no espelho acadêmico (academico.pessoa)
        PessoaAcademica pessoa = dao.buscarPessoaPorCpf(cpfLimpo);
        if (pessoa != null) {
            Integer pessoaId = pessoa.getId();
            String nome = pessoa.getNome();
            String email = pessoa.getEmail();

            // Identificar papéis e vínculos institucionais
            List<String> papeis = identificarPapeisAcademicos(pessoaId);
            if (papeis.isEmpty()) {
                return new AuthResponseDTO(false, null, "CPF sem vínculo acadêmico identificado para acesso ao AVALIA.", null);
            }
            String campus = identificarCampusPessoa(pessoaId);

            String papelPrincipal = definirPapelPrincipal(papeis);
            List<String> permissoes = obterPermissoesPadrao(papelPrincipal);

            // Cadastrar automaticamente em usuario_permissao para persistência
            UsuarioPermissao novo = new UsuarioPermissao(
                    cpfLimpo,
                    nome,
                    papelPrincipal,
                    campus != null ? campus : "Reitoria",
                    String.join(",", permissoes)
            );
            novo.setEmail(email);
            dao.persistirUsuario(novo);

            UsuarioDTO dto = new UsuarioDTO();
            dto.setCpf(cpfLimpo);
            dto.setCpfFormatado(formatarCpf(cpfLimpo));
            dto.setNome(nome);
            dto.setEmail(email);
            dto.setCampus(novo.getCampus());
            dto.setPapelAtivo(papelPrincipal);
            dto.setPapeis(papeis);
            dto.setPermissoes(permissoes);

            String token = UUID.randomUUID().toString();
            registrarSessao(token, dto);
            return new AuthResponseDTO(true, token, "Login realizado com sucesso.", dto);
        }

        return new AuthResponseDTO(false, null, "CPF não localizado no cadastro institucional do IFFar.", null);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void registrarSessao(String token, UsuarioDTO usuario) {
        dao.registrarSessao(hashToken(token), usuario.getCpf(), String.join(",", usuario.getPapeis()));
    }

    @Transactional
    public void encerrarSessao(String sessionToken) {
        if (sessionToken != null && sessionToken.matches("[0-9a-f-]{36}"))
            dao.encerrarSessao(hashToken(sessionToken));
    }

    public UsuarioDTO validarSessao(String sessionToken, String papelAtivo) {
        if (sessionToken == null || !sessionToken.matches("[0-9a-f-]{36}"))
            throw new SecurityException("Sessão não informada.");
        AuthSessao sessao = dao.buscarSessaoValida(hashToken(sessionToken));
        if (sessao == null) throw new SecurityException("Sessão expirada. Entre novamente.");
        String cpf = sessao.getCpf();
        List<String> papeis = Arrays.asList(sessao.getPapeis().split(","));
        String papel = papelAtivo == null || papelAtivo.isBlank() ? papeis.get(0) : papelAtivo;
        if (!papeis.contains(papel)) throw new SecurityException("Perfil não autorizado para esta sessão.");
        UsuarioPermissao up = dao.buscarUsuarioAtivo(cpf);
        if (up == null) throw new SecurityException("Sessão expirada. Entre novamente.");
        UsuarioDTO usuario = montarUsuarioDTO(up);
        if (!usuario.getPapeis().contains(papel))
            throw new SecurityException("Perfil não autorizado para esta sessão.");
        usuario.setPapelAtivo(papel);
        return usuario;
    }

    private UsuarioDTO montarUsuarioDTO(UsuarioPermissao up) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setCpf(up.getCpf());
        dto.setCpfFormatado(formatarCpf(up.getCpf()));
        dto.setNome(up.getNome());
        dto.setEmail(up.getEmail());
        dto.setCampus(up.getCampus() != null ? up.getCampus() : "Reitoria");
        dto.setPapelAtivo(up.getPapel());

        List<String> papeis = new ArrayList<>();
        papeis.add(up.getPapel());
        if ("CPA".equals(up.getPapel())) {
            papeis.add("DOCENTE");
        }
        dto.setPapeis(papeis);

        if (up.getPermissoes() != null && !up.getPermissoes().trim().isEmpty()) {
            dto.setPermissoes(new ArrayList<>(Arrays.asList(up.getPermissoes().split(","))));
        } else {
            dto.setPermissoes(obterPermissoesPadrao(up.getPapel()));
        }
        return dto;
    }

    private List<String> identificarPapeisAcademicos(Integer pessoaId) {
        Set<String> papeis = new LinkedHashSet<>();
        if (dao.existeAluno(pessoaId)) papeis.add("DISCENTE");
        if (dao.existeDocenteTurma(pessoaId)) papeis.add("DOCENTE");
        if (dao.existeServidor(pessoaId) && !papeis.contains("DOCENTE")) papeis.add("TAE");
        return new ArrayList<>(papeis);
    }

    private String identificarCampusPessoa(Integer pessoaId) {
        String campus = dao.campusServidor(pessoaId);
        if (campus == null) campus = dao.campusAluno(pessoaId);
        return campus == null ? "Reitoria" : campus;
    }

    private String definirPapelPrincipal(List<String> papeis) {
        if (papeis.contains("CPA")) return "CPA";
        if (papeis.contains("DOCENTE")) return "DOCENTE";
        if (papeis.contains("DISCENTE")) return "DISCENTE";
        if (papeis.contains("TAE")) return "TAE";
        return "DISCENTE";
    }

    public List<String> obterPermissoesPadrao(String papel) {
        if (papel == null) return List.of("AVALIACAO_RESPONDER");
        switch (papel.toUpperCase()) {
            case "CPA":
            case "ADMIN":
                return List.of(
                        "INSTRUMENTO_CRIAR",
                        "INSTRUMENTO_EDITAR",
                        "INSTRUMENTO_EXCLUIR",
                        "INSTRUMENTO_VISUALIZAR",
                        "AVALIACAO_RESPONDER",
                        "SIGAA_SINCRONIZAR"
                );
            case "DOCENTE":
                return List.of(
                        "AVALIACAO_RESPONDER",
                        "INSTRUMENTO_VISUALIZAR"
                );
            case "TAE":
            case "DISCENTE":
            default:
                return List.of(
                        "AVALIACAO_RESPONDER"
                );
        }
    }
}
