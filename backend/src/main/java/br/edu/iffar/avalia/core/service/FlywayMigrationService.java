package br.edu.iffar.avalia.core.service;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class FlywayMigrationService implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(FlywayMigrationService.class.getName());

    @Resource(lookup = "jdbc/avaliaDS")
    private DataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Iniciando verificação e aplicação automática de migrações pelo DataSource 'jdbc/avaliaDS'...");
        try {
            if (dataSource == null) throw new IllegalStateException("DataSource 'jdbc/avaliaDS' indisponível.");
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    // Permite executar V1.1 em bancos legados que já avançaram para V2 ou superior.
                    .outOfOrder(true)
                    .load();

            flyway.migrate();
            LOGGER.info("Verificação e aplicação das migrações Flyway concluídas com sucesso.");
        } catch (Throwable e) {
            System.out.printf("--------------------");
            e.printStackTrace();
            System.out.printf("--------------------");
            LOGGER.log(Level.SEVERE, "Erro ao executar migrações Flyway", e);
            throw new IllegalStateException("Não foi possível preparar o schema do AVALIA.", e);
        }
    }
}
