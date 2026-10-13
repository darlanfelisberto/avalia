package br.edu.iffar.avalia.core.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class FlywayMigrationService {

    private static final Logger LOGGER = Logger.getLogger(FlywayMigrationService.class.getName());

    @Resource(lookup = "jdbc/avaliaDS")
    private DataSource dataSource;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOGGER.info("Iniciando verificação e aplicação de migrações Flyway no banco 'avalia'...");
        try {
            if (dataSource != null) {
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .load();

                flyway.migrate();
                LOGGER.info("Migrações Flyway executadas com sucesso!");
            } else {
                LOGGER.warning("DataSource 'jdbc/avaliaDS' nulo na inicialização do Flyway.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao executar migrações Flyway", e);
        }
    }
}
