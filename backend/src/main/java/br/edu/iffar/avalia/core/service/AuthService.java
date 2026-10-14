package br.edu.iffar.avalia.core.service;

import br.edu.iffar.avalia.core.dto.AuthResponseDTO;
import br.edu.iffar.avalia.core.dto.UsuarioDTO;
import br.edu.iffar.avalia.core.model.UsuarioPermissao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@ApplicationScoped
public class AuthService {

    @PersistenceContext(unitName = "avaliaPU")
    private EntityManager em;

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
    public AuthResponseDTO autenticarPorCpf(String cpfEntrada) {
        String cpfLimpo = normalizarCpf(cpfEntrada);
        if (cpfLimpo.isEmpty()) {
            return new AuthResponseDTO(false, null, "Informe o CPF para identificação.", null);
        }

        // 1. Consulta inicial na tabela de permissões do Avalia
        @SuppressWarnings("unchecked")
        List<UsuarioPermissao> permissaoList = em.createQuery(
                "SELECT u FROM UsuarioPermissao u WHERE u.cpf = :cpf AND u.ativo = TRUE", UsuarioPermissao.class)
                .setParameter("cpf", cpfLimpo)
                .getResultList();

        if (!permissaoList.isEmpty()) {
            UsuarioPermissao up = permissaoList.get(0);
            UsuarioDTO dto = montarUsuarioDTO(up);
            String token = UUID.randomUUID().toString();
            registrarSessao(token, dto);
            return new AuthResponseDTO(true, token, "Login realizado com sucesso.", dto);
        }

        // 2. Consulta de fallback no espelho acadêmico (academico.pessoa)
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> pessoas = em.createNativeQuery(
                    "SELECT p.id, p.nome, p.email, p.cpf_cnpj FROM academico.pessoa p WHERE regexp_replace(p.cpf_cnpj, '[^0-9]', '', 'g') = ?1 LIMIT 1")
                    .setParameter(1, cpfLimpo)
                    .getResultList();

            if (!pessoas.isEmpty()) {
                Object[] row = pessoas.get(0);
                Integer pessoaId = ((Number) row[0]).intValue();
                String nome = (String) row[1];
                String email = (String) row[2];

                // Identificar papéis e vínculos institucionais
                List<String> papeis = identificarPapeisAcademicos(pessoaId);
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
                em.persist(novo);

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
        } catch (Exception e) {
            // Se as tabelas de academico não estiverem disponíveis, continua
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
        em.createNativeQuery("insert into auth_sessao (token_hash, cpf, papeis, expira_em) " +
                        "values (?1, ?2, ?3, now() + interval '12 hours')")
                .setParameter(1, hashToken(token))
                .setParameter(2, usuario.getCpf())
                .setParameter(3, String.join(",", usuario.getPapeis()))
                .executeUpdate();
    }

    public UsuarioDTO validarSessao(String authorization, String papelAtivo) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new SecurityException("Sessão não informada.");
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) throw new SecurityException("Sessão não informada.");
        @SuppressWarnings("unchecked")
        List<Object[]> sessoes = em.createNativeQuery("select u.cpf, s.papeis from auth_sessao s " +
                        "join usuario_permissao u on u.cpf = s.cpf and u.ativo = true " +
                        "where s.token_hash = ?1 and s.expira_em > now()")
                .setParameter(1, hashToken(token)).getResultList();
        if (sessoes.isEmpty()) throw new SecurityException("Sessão expirada. Entre novamente.");
        Object[] sessao = sessoes.get(0);
        String cpf = (String) sessao[0];
        List<String> papeis = Arrays.asList(((String) sessao[1]).split(","));
        String papel = papelAtivo == null || papelAtivo.isBlank() ? papeis.get(0) : papelAtivo;
        if (!papeis.contains(papel)) throw new SecurityException("Perfil não autorizado para esta sessão.");
        UsuarioPermissao up = em.createQuery("select u from UsuarioPermissao u where u.cpf = :cpf", UsuarioPermissao.class)
                .setParameter("cpf", cpf).getSingleResult();
        UsuarioDTO usuario = montarUsuarioDTO(up);
        usuario.setPapelAtivo(papel);
        return usuario;
    }

    public List<UsuarioDTO> listarUsuariosDemonstracao() {
        @SuppressWarnings("unchecked")
        List<UsuarioPermissao> lista = em.createQuery(
                "SELECT u FROM UsuarioPermissao u WHERE u.ativo = TRUE ORDER BY u.papel ASC, u.nome ASC", UsuarioPermissao.class)
                .getResultList();

        List<UsuarioDTO> resultado = new ArrayList<>();
        for (UsuarioPermissao up : lista) {
            resultado.add(montarUsuarioDTO(up));
        }
        return resultado;
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
            papeis.add("GESTAO");
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
        try {
            // Checar se é aluno
            Number countAluno = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM academico.aluno WHERE id_pessoa = ?1")
                    .setParameter(1, pessoaId)
                    .getSingleResult();
            if (countAluno != null && countAluno.intValue() > 0) {
                papeis.add("DISCENTE");
            }

            // Checar se é docente em turmas
            Number countDocTurma = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM academico.docente_turma dt " +
                    "JOIN academico.servidor s ON s.id = dt.id_servidor WHERE s.id_pessoa = ?1")
                    .setParameter(1, pessoaId)
                    .getSingleResult();
            if (countDocTurma != null && countDocTurma.intValue() > 0) {
                papeis.add("DOCENTE");
            }

            // Checar se tem chefia ou coordenação de curso
            Number countChefia = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM academico.designacao d " +
                    "JOIN academico.servidor s ON s.id = d.id_servidor WHERE s.id_pessoa = ?1 AND d.chefia = TRUE")
                    .setParameter(1, pessoaId)
                    .getSingleResult();
            Number countCoord = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM academico.coordenacao_curso c " +
                    "JOIN academico.servidor s ON s.id = c.id_servidor WHERE s.id_pessoa = ?1 AND c.ativo = TRUE")
                    .setParameter(1, pessoaId)
                    .getSingleResult();
            if ((countChefia != null && countChefia.intValue() > 0) || (countCoord != null && countCoord.intValue() > 0)) {
                papeis.add("GESTAO");
            }

            // Checar se é servidor geral (TAE) caso não seja docente
            Number countServidor = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM academico.servidor WHERE id_pessoa = ?1")
                    .setParameter(1, pessoaId)
                    .getSingleResult();
            if (countServidor != null && countServidor.intValue() > 0 && !papeis.contains("DOCENTE")) {
                papeis.add("TAE");
            }
        } catch (Exception ignored) {}

        if (papeis.isEmpty()) {
            papeis.add("DISCENTE");
        }
        return new ArrayList<>(papeis);
    }

    private String identificarCampusPessoa(Integer pessoaId) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> campusList = em.createNativeQuery(
                    "SELECT u.nome FROM academico.servidor s " +
                    "JOIN academico.unidade u ON u.id = s.id_unidade_lotacao " +
                    "WHERE s.id_pessoa = ?1 LIMIT 1")
                    .setParameter(1, pessoaId)
                    .getResultList();
            if (!campusList.isEmpty() && campusList.get(0) != null) {
                return campusList.get(0).toString();
            }

            @SuppressWarnings("unchecked")
            List<Object> campusAluno = em.createNativeQuery(
                    "SELECT u.nome FROM academico.aluno a " +
                    "JOIN academico.curso c ON c.id = a.id_curso " +
                    "JOIN academico.unidade u ON u.id = c.id_unidade " +
                    "WHERE a.id_pessoa = ?1 LIMIT 1")
                    .setParameter(1, pessoaId)
                    .getResultList();
            if (!campusAluno.isEmpty() && campusAluno.get(0) != null) {
                return campusAluno.get(0).toString();
            }
        } catch (Exception ignored) {}
        return "Reitoria";
    }

    private String definirPapelPrincipal(List<String> papeis) {
        if (papeis.contains("CPA")) return "CPA";
        if (papeis.contains("GESTAO")) return "GESTAO";
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
                        "DOCENTE_MANIFESTAR",
                        "GESTAO_ENCAMINHAR",
                        "SIGAA_SINCRONIZAR",
                        "AUDITORIA_ACESSAR"
                );
            case "GESTAO":
                return List.of(
                        "GESTAO_ENCAMINHAR",
                        "INSTRUMENTO_VISUALIZAR",
                        "AVALIACAO_RESPONDER"
                );
            case "DOCENTE":
                return List.of(
                        "DOCENTE_MANIFESTAR",
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
