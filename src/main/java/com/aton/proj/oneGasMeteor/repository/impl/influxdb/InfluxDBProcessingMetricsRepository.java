package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.config.ConditionalOnInfluxDatabase;
import com.aton.proj.oneGasMeteor.config.InfluxDBConfig.InfluxDBProperties;
import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;
import com.aton.proj.oneGasMeteor.repository.ProcessingMetricsRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;

/**
 * Implementazione InfluxDB per ProcessingMetricsRepository.
 * Attivo quando database.type=influxdb e metrics.enabled=true.
 */
@Repository
@ConditionalOnInfluxDatabase
@ConditionalOnProperty(name = "metrics.enabled", havingValue = "true", matchIfMissing = true)
public class InfluxDBProcessingMetricsRepository implements ProcessingMetricsRepository {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBProcessingMetricsRepository.class);

    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final String bucket;
    private final String org;

    public InfluxDBProcessingMetricsRepository(InfluxDBClient influxDBClient, WriteApiBlocking writeApi,
            InfluxDBProperties properties) {
        this.influxDBClient = influxDBClient;
        this.writeApi = writeApi;
        this.bucket = properties.bucket();
        this.org = properties.org();
        log.info("InfluxDBProcessingMetricsRepository initialized");
    }

    @Override
    public ProcessingMetricsEntity save(ProcessingMetricsEntity entity) {
        try {
            writeApi.writePoint(InfluxDBPointMapper.toPoint(entity));
            log.debug("Saved processing metrics to InfluxDB: deviceId={}, totalMs={}",
                    entity.getDeviceId(), entity.getTotalProcessingTimeMs());
            return entity;
        } catch (Exception e) {
            log.error("Failed to save processing metrics to InfluxDB for device: {}", entity.getDeviceId(), e);
            throw new RuntimeException("Failed to save processing metrics to InfluxDB", e);
        }
    }

    @Override
    public void deleteOlderThan(LocalDateTime threshold) {
        try {
            OffsetDateTime start = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime stop = threshold.atOffset(ZoneOffset.UTC);
            String predicate = FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_METRICS);
            influxDBClient.getDeleteApi().delete(start, stop, predicate, bucket, org);
            log.info("Deleted processing metrics older than {} from InfluxDB", threshold);
        } catch (Exception e) {
            log.error("Failed to delete old processing metrics from InfluxDB", e);
        }
    }
}
