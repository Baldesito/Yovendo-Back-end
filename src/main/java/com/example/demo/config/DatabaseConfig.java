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

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${DB_HOST:}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:}")
    private String dbName;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${SPRING_DATASOURCE_URL:}")
    private String springDatasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:}")
    private String springDatasourceUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String springDatasourcePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("Configurazione DataSource con variabili d'ambiente");

        // 1. Prova prima con le variabili SPRING_DATASOURCE_*
        if (springDatasourceUrl != null && !springDatasourceUrl.isEmpty()) {
            logger.info("Usando SPRING_DATASOURCE_URL: {}", springDatasourceUrl);

            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(springDatasourceUrl);
            dataSource.setUsername(springDatasourceUsername);
            dataSource.setPassword(springDatasourcePassword);
            dataSource.setDriverClassName("org.postgresql.Driver");

            return dataSource;
        }

        // 2. Prova con DATABASE_URL se presente
        if (databaseUrl != null && !databaseUrl.isEmpty() && databaseUrl.startsWith("postgresql://")) {
            logger.info("Usando DATABASE_URL...");

            try {
                String[] userInfoHostPort = databaseUrl.substring("postgresql://".length()).split("@");
                String[] userInfo = userInfoHostPort[0].split(":");
                String username = userInfo[0];
                String password = userInfo[1];

                String[] hostPortDb = userInfoHostPort[1].split("/");
                String host = hostPortDb[0];
                String database = hostPortDb[1];

                String jdbcUrl = "jdbc:postgresql://" + host + ":5432/" + database;

                logger.info("JDBC URL costruito da DATABASE_URL: {}", jdbcUrl);

                DriverManagerDataSource dataSource = new DriverManagerDataSource();
                dataSource.setUrl(jdbcUrl);
                dataSource.setUsername(username);
                dataSource.setPassword(password);
                dataSource.setDriverClassName("org.postgresql.Driver");

                return dataSource;
            } catch (Exception e) {
                logger.error("Errore nel parsing dell'URL del database", e);
            }
        }

        // 3. Prova con DB_* variabili separate
        if (dbHost != null && !dbHost.isEmpty() &&
                dbName != null && !dbName.isEmpty() &&
                dbUsername != null && !dbUsername.isEmpty()) {

            String jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
            logger.info("JDBC URL costruito da parametri individuali: {}", jdbcUrl);

            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(jdbcUrl);
            dataSource.setUsername(dbUsername);
            dataSource.setPassword(dbPassword);
            dataSource.setDriverClassName("org.postgresql.Driver");

            return dataSource;
        }

        // 4. Fallback con un messaggio di errore chiaro
        logger.error("ERRORE: Nessuna configurazione di database valida trovata");
        logger.error("Imposta almeno una delle seguenti opzioni:");
        logger.error("1. SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD");
        logger.error("2. DATABASE_URL nel formato postgresql://username:password@host/database");
        logger.error("3. DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD");

        // Ritorna un DataSource che genererà un errore chiaro
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/missing_db_config");
        dataSource.setUsername("missing_username");
        dataSource.setPassword("missing_password");
        dataSource.setDriverClassName("org.postgresql.Driver");

        return dataSource;
    }
}