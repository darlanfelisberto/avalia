package br.edu.iffar.avalia.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import br.edu.iffar.avalia.core.datasource.AvaliaDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class FlywayMigrationService {

    private static final Logger LOGGER = Logger.getLogger(FlywayMigrationService.class.getName());

    @Inject
    @AvaliaDataSource
    private DataSource dataSource;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOGGER.info("Iniciando verificação e aplicação automática de migrações pelo DataSource 'jdbc/avaliaDS'...");
        try {
            if (dataSource == null) throw new IllegalStateException("DataSource 'jdbc/avaliaDS' indisponível.");
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .outOfOrder(true)
                    .load();

            flyway.migrate();
            LOGGER.info("Verificação e aplicação das migrações Flyway concluídas com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Erro ao executar migrações Flyway", e);
            throw new IllegalStateException("Não foi possível preparar o schema do AVALIA.", e);
        }
    }
}
