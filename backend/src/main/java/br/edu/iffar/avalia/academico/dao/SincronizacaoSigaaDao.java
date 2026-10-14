package br.edu.iffar.avalia.academico.dao;

import br.edu.iffar.avalia.academico.dto.EtlLogDTO;
import br.edu.iffar.avalia.academico.service.SigaaConnectionProvider;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Operações JDBC em lote entre o SIGAA e o espelho acadêmico local. */
@ApplicationScoped
public class SincronizacaoSigaaDao {
    private static final Logger LOGGER = Logger.getLogger(SincronizacaoSigaaDao.class.getName());

    @Resource(lookup = "jdbc/avaliaDS")
    private DataSource localDataSource;

    @Inject
    private SigaaConnectionProvider connectionProvider;

    public void sincronizar(int ano, Integer periodo, boolean syncEstrutura, boolean syncTurmas) throws SQLException {
        try (Connection connSigaa = connectionProvider.getConnection();
             Connection connLocal = localDataSource.getConnection()) {
            connLocal.setAutoCommit(false);
            try {
                if (syncEstrutura) {
                    sincronizarPessoas(connSigaa, connLocal);
                    sincronizarUnidades(connSigaa, connLocal);
                    recalcularUnidadeClosure(connLocal);
                    sincronizarCargos(connSigaa, connLocal);
                    sincronizarServidores(connSigaa, connLocal);
                    sincronizarDesignacoes(connSigaa, connLocal);
                    sincronizarCursos(connSigaa, connLocal);
                    sincronizarAlunos(connSigaa, connLocal);
                    sincronizarComponentesCurriculares(connSigaa, connLocal);
                    sincronizarCoordenacaoCurso(connSigaa, connLocal);
                }
                if (syncTurmas) {
                    sincronizarTurmas(connSigaa, connLocal, ano, periodo);
                    sincronizarDocenteTurma(connSigaa, connLocal, ano, periodo);
                    sincronizarMatriculas(connSigaa, connLocal, ano, periodo);
                }
                connLocal.commit();
            } catch (SQLException | RuntimeException e) {
                connLocal.rollback();
                throw e;
            }
        }
    }

    private void sincronizarPessoas(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = """
            SELECT id_pessoa, nome, nome_oficial, cpf_cnpj, email, telefone, celular, data_nascimento, sexo, data_cadastro
            FROM comum.pessoa
            WHERE id_pessoa IN (SELECT id_pessoa FROM rh.servidor)
            OR id_pessoa IN (SELECT id_pessoa FROM public.discente WHERE ano_ingresso >= 2020)
            """;

        String sqlDestino = """
            INSERT INTO academico.pessoa (id, nome, nome_social, cpf_cnpj, email, telefone, celular, data_nascimento, sexo, data_cadastro, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            nome = EXCLUDED.nome, nome_social = EXCLUDED.nome_social, cpf_cnpj = EXCLUDED.cpf_cnpj,
            email = EXCLUDED.email, telefone = EXCLUDED.telefone, celular = EXCLUDED.celular,
            data_nascimento = EXCLUDED.data_nascimento, sexo = EXCLUDED.sexo, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_pessoa"));
            ps.setString(2, rs.getString("nome"));
            ps.setString(3, rs.getString("nome_oficial"));
            long cpf = rs.getLong("cpf_cnpj");
            if (rs.wasNull() || cpf == 0) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, String.format("%011d", cpf));
            }
            ps.setString(5, rs.getString("email"));
            ps.setString(6, rs.getString("telefone"));
            ps.setString(7, rs.getString("celular"));
            ps.setDate(8, rs.getDate("data_nascimento"));
            ps.setString(9, rs.getString("sexo"));
            ps.setTimestamp(10, rs.getTimestamp("data_cadastro"));
        });
        gravarLog("pessoa", total, "SUCESSO", "Importadas " + total + " pessoas (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarUnidades(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        // Passo 1: Inserir todas as unidades com id_pai = null para garantir que todas as PKs existam
        String sqlOrigem = "SELECT id_unidade, codigo_unidade, nome, sigla, hierarquia, unidade_responsavel FROM comum.unidade";
        String sqlPasso1 = """
            INSERT INTO academico.unidade (id, codigo, nome, sigla, path, atualizado_em)
            VALUES (?, ?, ?, ?, ?::ltree, now())
            ON CONFLICT (id) DO UPDATE SET
            codigo = EXCLUDED.codigo, nome = EXCLUDED.nome, sigla = EXCLUDED.sigla,
            path = EXCLUDED.path, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlPasso1, (rs, ps) -> {
            int id = rs.getInt("id_unidade");
            ps.setInt(1, id);
            ps.setString(2, rs.getString("codigo_unidade"));
            ps.setString(3, rs.getString("nome"));
            ps.setString(4, rs.getString("sigla"));
            String hierarquia = rs.getString("hierarquia");
            String ltreeStr = null;
            if (hierarquia != null && !hierarquia.trim().isEmpty()) {
                ltreeStr = hierarquia.replaceAll("^\\.+|\\.+$", "");
            }
            if (ltreeStr == null || ltreeStr.isEmpty()) {
                ltreeStr = String.valueOf(id);
            }
            ps.setString(5, ltreeStr);
        });

        // Passo 2: Atualizar id_pai agora que todas as PKs existem (ignorando auto-referências)
        String sqlPasso2 = "UPDATE academico.unidade SET id_pai = ? WHERE id = ? AND ? <> ?";
        try (Statement st = sig.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_unidade, unidade_responsavel FROM comum.unidade WHERE unidade_responsavel IS NOT NULL AND unidade_responsavel > 0");
             PreparedStatement ps = loc.prepareStatement(sqlPasso2)) {
            int countPai = 0;
            while (rs.next()) {
                int idPai = rs.getInt("unidade_responsavel");
                int idUnid = rs.getInt("id_unidade");
                ps.setInt(1, idPai);
                ps.setInt(2, idUnid);
                ps.setInt(3, idPai);
                ps.setInt(4, idUnid);
                ps.addBatch();
                countPai++;
                if (countPai % 1000 == 0) {
                    ps.executeBatch();
                    loc.commit();
                }
            }
            ps.executeBatch();
            loc.commit();
        }

        gravarLog("unidade", total, "SUCESSO", "Importadas " + total + " unidades e atualizadas hierarquias (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void recalcularUnidadeClosure(Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        try (Statement st = loc.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE academico.unidade_closure");
            String sqlClosure = """
                INSERT INTO academico.unidade_closure (id_ancestral, id_descendente, profundidade)
                SELECT
                subltree(u.path, idx, idx + 1)::text::integer AS id_ancestral,
                u.id AS id_descendente,
                (nlevel(u.path) - 1 - idx) AS profundidade
                FROM academico.unidade u,
                LATERAL generate_series(0, nlevel(u.path) - 1) AS idx
                WHERE u.path IS NOT NULL
                AND subltree(u.path, idx, idx + 1)::text::integer IN (SELECT id FROM academico.unidade)
                ON CONFLICT (id_ancestral, id_descendente) DO NOTHING
                """;
            int count = st.executeUpdate(sqlClosure);
            loc.commit();
            gravarLog("unidade_closure", count, "SUCESSO", "Recalculada tabela closure de unidades (" + count + " pares em " + (System.currentTimeMillis() - inicio) + "ms)");
        }
    }

    private void sincronizarCargos(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = "SELECT id, denominacao, sigla, inativo FROM rh.cargo";
        String sqlDestino = """
            INSERT INTO academico.cargo (id, denominacao, sigla, inativo)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET denominacao = EXCLUDED.denominacao, sigla = EXCLUDED.sigla, inativo = EXCLUDED.inativo
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id"));
            ps.setString(2, rs.getString("denominacao"));
            ps.setString(3, rs.getString("sigla"));
            ps.setBoolean(4, rs.getBoolean("inativo"));
        });
        gravarLog("cargo", total, "SUCESSO", "Importados " + total + " cargos (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarServidores(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = """
            SELECT id_servidor, id_pessoa, siape, id_cargo, id_unidade_lotacao, id_unidade_exercicio,
            admissao, data_desligamento FROM rh.servidor
            """;
        String sqlDestino = """
            INSERT INTO academico.servidor (id, id_pessoa, siape, id_cargo, id_unidade_lotacao, id_unidade_exercicio, admissao, data_desligamento, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            id_pessoa = EXCLUDED.id_pessoa, siape = EXCLUDED.siape, id_cargo = EXCLUDED.id_cargo,
            id_unidade_lotacao = EXCLUDED.id_unidade_lotacao, id_unidade_exercicio = EXCLUDED.id_unidade_exercicio,
            admissao = EXCLUDED.admissao, data_desligamento = EXCLUDED.data_desligamento, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_servidor"));
            ps.setInt(2, rs.getInt("id_pessoa"));
            ps.setString(3, String.valueOf(rs.getInt("siape")));
            setIntOrNull(ps, 4, rs.getInt("id_cargo"), rs.wasNull());
            setIntOrNull(ps, 5, rs.getInt("id_unidade_lotacao"), rs.wasNull());
            setIntOrNull(ps, 6, rs.getInt("id_unidade_exercicio"), rs.wasNull());
            ps.setDate(7, rs.getDate("admissao"));
            ps.setDate(8, rs.getDate("data_desligamento"));
        });
        gravarLog("servidor", total, "SUCESSO", "Importados " + total + " servidores (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarDesignacoes(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = "SELECT id_designacao, id_servidor, id_unidade, observacoes, inicio, fim FROM rh.designacao";
        String sqlDestino = """
            INSERT INTO academico.designacao (id, id_servidor, id_unidade, funcao, chefia, data_inicio, data_fim, atualizado_em)
            VALUES (?, ?, ?, ?, TRUE, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            id_servidor = EXCLUDED.id_servidor, id_unidade = EXCLUDED.id_unidade, funcao = EXCLUDED.funcao,
            data_inicio = EXCLUDED.data_inicio, data_fim = EXCLUDED.data_fim, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_designacao"));
            ps.setInt(2, rs.getInt("id_servidor"));
            ps.setInt(3, rs.getInt("id_unidade"));
            ps.setString(4, rs.getString("observacoes"));
            ps.setDate(5, rs.getDate("inicio"));
            ps.setDate(6, rs.getDate("fim"));
        });
        gravarLog("designacao", total, "SUCESSO", "Importadas " + total + " designações (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarCursos(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = "SELECT id_curso, codigo, nome, nivel, id_unidade, ativo FROM public.curso";
        String sqlDestino = """
            INSERT INTO academico.curso (id, codigo, nome, nivel, id_unidade, ativo, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            codigo = EXCLUDED.codigo, nome = EXCLUDED.nome, nivel = EXCLUDED.nivel,
            id_unidade = EXCLUDED.id_unidade, ativo = EXCLUDED.ativo, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_curso"));
            ps.setString(2, rs.getString("codigo"));
            ps.setString(3, rs.getString("nome"));
            ps.setString(4, rs.getString("nivel"));
            setIntOrNull(ps, 5, rs.getInt("id_unidade"), rs.wasNull());
            ps.setBoolean(6, rs.getBoolean("ativo"));
        });
        gravarLog("curso", total, "SUCESSO", "Importados " + total + " cursos (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarAlunos(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = """
            SELECT id_discente, id_pessoa, matricula, id_curso, nivel, ano_ingresso, periodo_ingresso
            FROM public.discente WHERE ano_ingresso >= 2020
            """;
        String sqlDestino = """
            INSERT INTO academico.aluno (id, id_pessoa, matricula, id_curso, nivel, ano_ingresso, periodo_ingresso, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            id_pessoa = EXCLUDED.id_pessoa, matricula = EXCLUDED.matricula, id_curso = EXCLUDED.id_curso,
            nivel = EXCLUDED.nivel, ano_ingresso = EXCLUDED.ano_ingresso, periodo_ingresso = EXCLUDED.periodo_ingresso, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_discente"));
            ps.setInt(2, rs.getInt("id_pessoa"));
            ps.setString(3, String.valueOf(rs.getLong("matricula")));
            setIntOrNull(ps, 4, rs.getInt("id_curso"), rs.wasNull());
            ps.setString(5, rs.getString("nivel"));
            setIntOrNull(ps, 6, rs.getInt("ano_ingresso"), rs.wasNull());
            setIntOrNull(ps, 7, rs.getInt("periodo_ingresso"), rs.wasNull());
        });
        gravarLog("aluno", total, "SUCESSO", "Importados " + total + " alunos recentes (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarComponentesCurriculares(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = """
            SELECT cc.id_disciplina, cc.codigo, ccd.nome, cc.id_unidade, cc.nivel,
            ccd.ch_total, ccd.ch_teorico, ccd.ch_pratico, ccd.ementa
            FROM ensino.componente_curricular cc
            JOIN ensino.componente_curricular_detalhes ccd ON ccd.id_componente_detalhes = cc.id_detalhe
            """;
        String sqlDestino = """
            INSERT INTO academico.componente_curricular (id, codigo, nome, id_unidade, nivel, ch_total, ch_teorica, ch_pratica, ementa, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            codigo = EXCLUDED.codigo, nome = EXCLUDED.nome, id_unidade = EXCLUDED.id_unidade,
            nivel = EXCLUDED.nivel, ch_total = EXCLUDED.ch_total, ch_teorica = EXCLUDED.ch_teorica,
            ch_pratica = EXCLUDED.ch_pratica, ementa = EXCLUDED.ementa, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_disciplina"));
            ps.setString(2, rs.getString("codigo"));
            ps.setString(3, rs.getString("nome"));
            setIntOrNull(ps, 4, rs.getInt("id_unidade"), rs.wasNull());
            ps.setString(5, rs.getString("nivel"));
            setIntOrNull(ps, 6, rs.getInt("ch_total"), rs.wasNull());
            setIntOrNull(ps, 7, rs.getInt("ch_teorico"), rs.wasNull());
            setIntOrNull(ps, 8, rs.getInt("ch_pratico"), rs.wasNull());
            ps.setString(9, rs.getString("ementa"));
        });
        gravarLog("componente_curricular", total, "SUCESSO", "Importados " + total + " componentes (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarCoordenacaoCurso(Connection sig, Connection loc) throws SQLException {
        long inicio = System.currentTimeMillis();
        String sqlOrigem = "SELECT id_coordenacao_curso, id_servidor, id_curso, data_inicio_mandato, data_fim_mandato, ativo FROM ensino.coordenacao_curso";
        String sqlDestino = """
            INSERT INTO academico.coordenacao_curso (id, id_servidor, id_curso, data_inicio, data_fim, ativo, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            id_servidor = EXCLUDED.id_servidor, id_curso = EXCLUDED.id_curso, data_inicio = EXCLUDED.data_inicio,
            data_fim = EXCLUDED.data_fim, ativo = EXCLUDED.ativo, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_coordenacao_curso"));
            setIntOrNull(ps, 2, rs.getInt("id_servidor"), rs.wasNull());
            setIntOrNull(ps, 3, rs.getInt("id_curso"), rs.wasNull());
            ps.setDate(4, rs.getDate("data_inicio_mandato"));
            ps.setDate(5, rs.getDate("data_fim_mandato"));
            ps.setBoolean(6, rs.getBoolean("ativo"));
        });
        gravarLog("coordenacao_curso", total, "SUCESSO", "Importadas " + total + " coordenações (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarTurmas(Connection sig, Connection loc, int ano, Integer periodo) throws SQLException {
        long inicio = System.currentTimeMillis();
        String filtroPeriodo = (periodo != null) ? " AND t.periodo = " + periodo : "";
        String sqlOrigem = """
            SELECT t.id_turma, t.id_disciplina, t.codigo, t.ano, t.periodo, t.capacidade_aluno, st.descricao AS situacao
            FROM ensino.turma t
            LEFT JOIN ensino.situacao_turma st ON st.id_situacao_turma = t.id_situacao_turma
            WHERE t.ano >= %d%s
              AND t.id_disciplina IS NOT NULL AND t.id_disciplina > 0
              AND t.id_disciplina IN (SELECT id_disciplina FROM ensino.componente_curricular)
            """.formatted(ano, filtroPeriodo);
        String sqlDestino = """
            INSERT INTO academico.turma (id, id_componente_curricular, codigo_turma, ano, periodo, capacidade, situacao, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            id_componente_curricular = EXCLUDED.id_componente_curricular, codigo_turma = EXCLUDED.codigo_turma,
            ano = EXCLUDED.ano, periodo = EXCLUDED.periodo, capacidade = EXCLUDED.capacidade,
            situacao = EXCLUDED.situacao, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_turma"));
            ps.setInt(2, rs.getInt("id_disciplina"));
            ps.setString(3, rs.getString("codigo"));
            ps.setInt(4, rs.getInt("ano"));
            ps.setInt(5, rs.getInt("periodo"));
            setIntOrNull(ps, 6, rs.getInt("capacidade_aluno"), rs.wasNull());
            ps.setString(7, rs.getString("situacao"));
        });
        gravarLog("turma", total, "SUCESSO", "Importadas " + total + " turmas (ano " + ano + ") (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarDocenteTurma(Connection sig, Connection loc, int ano, Integer periodo) throws SQLException {
        long inicio = System.currentTimeMillis();
        String filtroPeriodo = (periodo != null) ? " AND t.periodo = " + periodo : "";
        String sqlOrigem = """
            SELECT dt.id_docente_turma, dt.id_docente, dt.id_turma, dt.ch_dedicada_periodo
            FROM ensino.docente_turma dt
            JOIN ensino.turma t ON t.id_turma = dt.id_turma
            WHERE t.ano >= %d%s
              AND t.id_disciplina IS NOT NULL AND t.id_disciplina > 0
              AND t.id_disciplina IN (SELECT id_disciplina FROM ensino.componente_curricular)
              AND (dt.id_docente IS NULL OR dt.id_docente IN (SELECT id_servidor FROM rh.servidor))
            """.formatted(ano, filtroPeriodo);
        String sqlDestino = """
            INSERT INTO academico.docente_turma (id, id_servidor, id_turma, ch_dedicada, atualizado_em)
            VALUES (?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            id_servidor = EXCLUDED.id_servidor, id_turma = EXCLUDED.id_turma,
            ch_dedicada = EXCLUDED.ch_dedicada, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_docente_turma"));
            setIntOrNull(ps, 2, rs.getInt("id_docente"), rs.wasNull());
            ps.setInt(3, rs.getInt("id_turma"));
            setIntOrNull(ps, 4, rs.getInt("ch_dedicada_periodo"), rs.wasNull());
        });
        gravarLog("docente_turma", total, "SUCESSO", "Importados " + total + " vínculos docente-turma (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    private void sincronizarMatriculas(Connection sig, Connection loc, int ano, Integer periodo) throws SQLException {
        long inicio = System.currentTimeMillis();
        String filtroPeriodo = (periodo != null) ? " AND t.periodo = " + periodo : "";
        String sqlOrigem = """
            SELECT mc.id_matricula_componente, mc.id_discente, mc.id_turma, mc.media_final, mc.numero_faltas, mc.ano, mc.periodo, mc.data_cadastro
            FROM ensino.matricula_componente mc
            JOIN ensino.turma t ON t.id_turma = mc.id_turma
            WHERE t.ano >= %d%s
              AND t.id_disciplina IS NOT NULL AND t.id_disciplina > 0
              AND t.id_disciplina IN (SELECT id_disciplina FROM ensino.componente_curricular)
              AND (mc.id_discente IS NULL OR mc.id_discente IN (SELECT id_discente FROM public.discente WHERE ano_ingresso >= 2020))
            """.formatted(ano, filtroPeriodo);
        String sqlDestino = """
            INSERT INTO academico.matricula (id, id_aluno, id_turma, media_final, numero_faltas, ano, periodo, data_matricula, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (id) DO UPDATE SET
            id_aluno = EXCLUDED.id_aluno, id_turma = EXCLUDED.id_turma, media_final = EXCLUDED.media_final,
            numero_faltas = EXCLUDED.numero_faltas, ano = EXCLUDED.ano, periodo = EXCLUDED.periodo, atualizado_em = now()
            """;

        int total = executarCarga(sig, loc, sqlOrigem, sqlDestino, (rs, ps) -> {
            ps.setInt(1, rs.getInt("id_matricula_componente"));
            setIntOrNull(ps, 2, rs.getInt("id_discente"), rs.wasNull());
            setIntOrNull(ps, 3, rs.getInt("id_turma"), rs.wasNull());
            double media = rs.getDouble("media_final");
            if (rs.wasNull()) {
                ps.setNull(4, Types.NUMERIC);
            } else {
                ps.setDouble(4, media);
            }
            setIntOrNull(ps, 5, rs.getInt("numero_faltas"), rs.wasNull());
            setIntOrNull(ps, 6, rs.getInt("ano"), rs.wasNull());
            setIntOrNull(ps, 7, rs.getInt("periodo"), rs.wasNull());
            ps.setDate(8, rs.getDate("data_cadastro"));
        });
        gravarLog("matricula", total, "SUCESSO", "Importadas " + total + " matrículas (" + (System.currentTimeMillis() - inicio) + "ms)");
    }

    // =========================================================================
    // UTILITÁRIOS DE BATCH PROCESSING E AUDITORIA
    // =========================================================================

    @FunctionalInterface
    private interface RowMapper {
        void map(ResultSet rs, PreparedStatement ps) throws SQLException;
    }

    private int executarCarga(Connection sig, Connection loc, String sqlOrigem, String sqlDestino, RowMapper mapper) throws SQLException {
        int contador = 0;
        int batchSize = 1000;

        try (Statement stOrigem = sig.createStatement();
             ResultSet rs = stOrigem.executeQuery(sqlOrigem);
             PreparedStatement psDestino = loc.prepareStatement(sqlDestino)) {

            while (rs.next()) {
                mapper.map(rs, psDestino);
                psDestino.addBatch();
                contador++;

                if (contador % batchSize == 0) {
                    psDestino.executeBatch();
                    loc.commit();
                }
            }
            psDestino.executeBatch();
            loc.commit();
        }
        return contador;
    }

    private void setIntOrNull(PreparedStatement ps, int paramIndex, int value, boolean wasNull) throws SQLException {
        if (wasNull || value == 0) {
            ps.setNull(paramIndex, Types.INTEGER);
        } else {
            ps.setInt(paramIndex, value);
        }
    }

    public void gravarLog(String entidade, int total, String status, String mensagem) {
        try (Connection loc = localDataSource.getConnection();
             PreparedStatement ps = loc.prepareStatement(
                     """
                         INSERT INTO academico.etl_log (entidade, inicio, fim, registros_importados, status, mensagem)
                         VALUES (?, now(), now(), ?, ?, ?)
                         """)) {
            ps.setString(1, entidade);
            ps.setInt(2, total);
            ps.setString(3, status);
            ps.setString(4, mensagem);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao gravar etl_log: " + e.getMessage());
        }
    }

    public Map<String, Long> obterContadoresLocais() {
        Map<String, Long> mapa = new LinkedHashMap<>();
        String[] tabelas = {"pessoa", "unidade", "cargo", "servidor", "designacao", "curso", "aluno", "componente_curricular", "turma", "docente_turma", "matricula"};
        try (Connection loc = localDataSource.getConnection();
             Statement st = loc.createStatement()) {
            for (String t : tabelas) {
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM academico." + t)) {
                    if (rs.next()) {
                        mapa.put(t, rs.getLong(1));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao obter contadores locais: " + e.getMessage());
        }
        return mapa;
    }

    public List<EtlLogDTO> obterUltimosLogs(int limite) {
        List<EtlLogDTO> lista = new ArrayList<>();
        try (Connection loc = localDataSource.getConnection();
             PreparedStatement ps = loc.prepareStatement(
                     """
                         SELECT id, entidade, inicio, fim, registros_importados, status, mensagem
                         FROM academico.etl_log ORDER BY id DESC LIMIT ?
                         """)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EtlLogDTO dto = new EtlLogDTO();
                    dto.id = rs.getLong("id");
                    dto.entidade = rs.getString("entidade");
                    Timestamp ini = rs.getTimestamp("inicio");
                    if (ini != null) dto.inicio = ini.toLocalDateTime();
                    Timestamp fim = rs.getTimestamp("fim");
                    if (fim != null) dto.fim = fim.toLocalDateTime();
                    dto.registrosImportados = rs.getInt("registros_importados");
                    dto.status = rs.getString("status");
                    dto.mensagem = rs.getString("mensagem");
                    lista.add(dto);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao obter logs de ETL: " + e.getMessage());
        }
        return lista;
    }
}
