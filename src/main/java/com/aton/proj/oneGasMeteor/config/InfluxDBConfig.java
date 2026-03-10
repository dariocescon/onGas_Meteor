package com.aton.proj.oneGasMeteor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnInfluxDatabase;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;

import jakarta.annotation.PreDestroy;

/**
 * Configurazione InfluxDB 2.x.
 * Attiva solo quando database.type=influxdb.
 * Crea i bean InfluxDBClient e WriteApiBlocking.
 */
@Configuration
@ConditionalOnInfluxDatabase
public class InfluxDBConfig {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBConfig.class);

    @Value("${influxdb.url}")
    private String url;

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    private InfluxDBClient client;

    @Bean
    public InfluxDBClient influxDBClient() {
        log.info("Creating InfluxDB client: url={}, org={}, bucket={}", url, org, bucket);
        client = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
        log.info("InfluxDB client created successfully");
        return client;
    }

    @Bean
    public WriteApiBlocking writeApiBlocking(InfluxDBClient influxDBClient) {
        return influxDBClient.getWriteApiBlocking();
    }

    /**
     * Restituisce il nome del bucket configurato.
     * Usato dai repository per le query Flux.
     */
    @Bean
    public InfluxDBProperties influxDBProperties() {
        return new InfluxDBProperties(url, org, bucket);
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            log.info("Closing InfluxDB client");
            client.close();
        }
    }

    /**
     * Record con le proprietà InfluxDB necessarie ai repository.
     */
    public record InfluxDBProperties(String url, String org, String bucket) {
    }
}
