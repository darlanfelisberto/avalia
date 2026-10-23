package br.edu.iffar.avalia.core.datasource;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import javax.sql.DataSource;

/** Expõe os DataSources gerenciados pelo Open Liberty para injeção CDI tipada. */
@ApplicationScoped
public class DataSourceProducer {

    @Resource(lookup = "jdbc/avaliaDS")
    private DataSource avaliaDataSource;

    @Resource(lookup = "jdbc/sigaaDS")
    private DataSource sigaaDataSource;

    @Produces
    @ApplicationScoped
    @AvaliaDataSource
    public DataSource produzirAvalia() {
        return avaliaDataSource;
    }

    @Produces
    @ApplicationScoped
    @SigaaDataSource
    public DataSource produzirSigaa() {
        return sigaaDataSource;
    }

    @Produces
    @ApplicationScoped
    public DataSource produzircommun() {
        return avaliaDataSource;
    }
}
