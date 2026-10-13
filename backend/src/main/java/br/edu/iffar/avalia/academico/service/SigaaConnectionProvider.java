package br.edu.iffar.avalia.academico.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provedor de conexões com a base do SIGAA através do DataSource gerenciado pelo Open Liberty (JNDI: jdbc/sigaaDS).
 * Nenhuma credencial, IP ou senha é mantida no código-fonte; os parâmetros são injetados pelo runtime.
 */
@ApplicationScoped
public class SigaaConnectionProvider {

    private static final Logger LOGGER = Logger.getLogger(SigaaConnectionProvider.class.getName());

    @Resource(lookup = "jdbc/sigaaDS")
    private DataSource sigaaDataSource;

    /**
     * Retorna a descrição de identificação do recurso para fins de diagnóstico.
     */
    public String getRecursoIdentificador() {
        return "jdbc/sigaaDS (Open Liberty Managed DataSource)";
    }

    /**
     * Obtém uma conexão com a base de dados do SIGAA a partir do pool gerenciado pelo Open Liberty.
     */
    public Connection getConnection() throws SQLException {
        if (sigaaDataSource == null) {
            throw new SQLException("DataSource 'jdbc/sigaaDS' não está disponível no servidor de aplicação.");
        }
        return sigaaDataSource.getConnection();
    }

    /**
     * Testa a conectividade com o DataSource do SIGAA.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao validar DataSource 'jdbc/sigaaDS': " + e.getMessage());
            return false;
        }
    }
}
