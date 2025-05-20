package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@Profile("prod")
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${SPRING_DATASOURCE_URL:}")
    private String springDatasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:}")
    private String springDatasourceUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String springDatasourcePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("=== CONFIGURAZIONE DATABASE ===");
        logger.info("SPRING_DATASOURCE_URL: {}", springDatasourceUrl);
        logger.info("SPRING_DATASOURCE_USERNAME: {}", springDatasourceUsername);
        logger.info("(La password è nascosta per sicurezza)");

        // Verifica se le variabili sono presenti
        if (springDatasourceUrl == null || springDatasourceUrl.isEmpty() ||
                springDatasourceUsername == null || springDatasourceUsername.isEmpty()) {
            logger.error("ERRORE: Variabili d'ambiente del database mancanti o non impostate!");
            logger.error("Assicurati di aver impostato SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME e SPRING_DATASOURCE_PASSWORD");

            // Non sollevare eccezioni qui, ma restituisci un DataSource di fallback
            // che causerà un errore più descrittivo
        }

        // Crea il DataSource con le variabili d'ambiente
        // Anche in caso di variabili mancanti, questo genererà un errore più chiaro
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(springDatasourceUrl);
        dataSource.setUsername(springDatasourceUsername);
        dataSource.setPassword(springDatasourcePassword);
        dataSource.setDriverClassName("org.postgresql.Driver");

        return dataSource;
    }
}