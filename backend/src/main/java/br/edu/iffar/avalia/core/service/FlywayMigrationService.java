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
            if (dataSource == null) throw new IllegalStateException("DataSource 'jdbc/avaliaDS' indisponível.");
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .outOfOrder(true)
                    .load();

            flyway.migrate();
            LOGGER.info("Migrações Flyway executadas com sucesso!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao executar migrações Flyway", e);
            throw new IllegalStateException("Não foi possível preparar o schema do AVALIA.", e);
        }
    }
}
