package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.config.ConditionalOnInfluxDatabase;
import com.aton.proj.oneGasMeteor.config.InfluxDBConfig.InfluxDBProperties;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType6Response;
import com.aton.proj.oneGasMeteor.repository.DeviceSettingsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

/**
 * Implementazione InfluxDB per DeviceSettingsRepository.
 */
@Repository
@ConditionalOnInfluxDatabase
public class InfluxDBDeviceSettingsRepository implements DeviceSettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBDeviceSettingsRepository.class);

    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final ObjectMapper objectMapper;
    private final String bucket;
    private final String org;

    public InfluxDBDeviceSettingsRepository(InfluxDBClient influxDBClient, WriteApiBlocking writeApi,
            ObjectMapper objectMapper, InfluxDBProperties properties) {
        this.influxDBClient = influxDBClient;
        this.writeApi = writeApi;
        this.objectMapper = objectMapper;
        this.bucket = properties.bucket();
        this.org = properties.org();
        log.info("InfluxDBDeviceSettingsRepository initialized");
    }

    @Override
    public DeviceSettingsEntity save(MessageType6Response settings, String rawMessage) {
        DeviceSettingsEntity entity = buildEntity(settings, rawMessage);
        try {
            writeApi.writePoint(InfluxDBPointMapper.toPoint(entity));
            log.debug("Saved device settings to InfluxDB: deviceId={}", settings.getDeviceId());
            return entity;
        } catch (Exception e) {
            log.error("Failed to save device settings to InfluxDB for device: {}", settings.getDeviceId(), e);
            throw new RuntimeException("Failed to save device settings to InfluxDB", e);
        }
    }

    @Override
    public DeviceSettingsEntity buildEntity(MessageType6Response settings, String rawMessage) {
        try {
            DeviceSettingsEntity entity = new DeviceSettingsEntity();
            entity.setDeviceId(settings.getDeviceId());
            entity.setDeviceType(settings.getDeviceType());
            entity.setRawMessage(rawMessage);
            entity.setReceivedAt(LocalDateTime.now());
            entity.setSettingsJson(objectMapper.writeValueAsString(settings.getSettings()));
            return entity;
        } catch (Exception e) {
            log.error("Failed to build device settings entity for device: {}", settings.getDeviceId(), e);
            throw new RuntimeException("Failed to build device settings entity", e);
        }
    }

    @Override
    public List<DeviceSettingsEntity> findByDeviceId(String deviceId) {
        String query = FluxQueryHelper.byDeviceId(bucket, InfluxDBPointMapper.MEASUREMENT_SETTINGS, deviceId);
        List<DeviceSettingsEntity> results = new ArrayList<>();
        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query, org);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    DeviceSettingsEntity entity = new DeviceSettingsEntity();
                    entity.setDeviceId(getStringValue(record, "device_id"));
                    entity.setDeviceType(getStringValue(record, "device_type"));
                    entity.setRawMessage(getStringValue(record, "raw_message"));
                    entity.setSettingsJson(getStringValue(record, "settings_json"));
                    if (record.getTime() != null) {
                        entity.setReceivedAt(LocalDateTime.ofInstant(record.getTime(), ZoneOffset.UTC));
                    }
                    results.add(entity);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query device settings from InfluxDB for deviceId: {}", deviceId, e);
        }
        return results;
    }

    @Override
    public void deleteOlderThan(LocalDateTime threshold) {
        try {
            OffsetDateTime start = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime stop = threshold.atOffset(ZoneOffset.UTC);
            String predicate = FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_SETTINGS);
            influxDBClient.getDeleteApi().delete(start, stop, predicate, bucket, org);
            log.info("Deleted device settings older than {} from InfluxDB", threshold);
        } catch (Exception e) {
            log.error("Failed to delete old device settings from InfluxDB", e);
        }
    }

    private String getStringValue(FluxRecord record, String key) {
        Object v = record.getValueByKey(key);
        return v != null ? v.toString() : null;
    }
}
