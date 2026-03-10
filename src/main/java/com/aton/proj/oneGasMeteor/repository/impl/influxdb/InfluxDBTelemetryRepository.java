package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.config.InfluxDBConfig.InfluxDBProperties;
import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnInfluxDatabase;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.repository.TelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.DeleteApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

/**
 * Implementazione InfluxDB per TelemetryRepository.
 * Dati time-series di telemetria scritti come measurement "telemetry".
 */
@Repository
@ConditionalOnInfluxDatabase
public class InfluxDBTelemetryRepository implements TelemetryRepository {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBTelemetryRepository.class);

    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final ObjectMapper objectMapper;
    private final String bucket;
    private final String org;

    public InfluxDBTelemetryRepository(InfluxDBClient influxDBClient, WriteApiBlocking writeApi,
            ObjectMapper objectMapper, InfluxDBProperties properties) {
        this.influxDBClient = influxDBClient;
        this.writeApi = writeApi;
        this.objectMapper = objectMapper;
        this.bucket = properties.bucket();
        this.org = properties.org();
        log.info("InfluxDBTelemetryRepository initialized (bucket={}, org={})", bucket, org);
    }

    @Override
    public TelemetryEntity save(String deviceId, String deviceType, String rawMessage, DecodedMessage decoded) {
        TelemetryEntity entity = buildEntity(deviceId, deviceType, rawMessage, decoded);
        try {
            writeApi.writePoint(InfluxDBPointMapper.toPoint(entity));
            log.debug("Saved telemetry to InfluxDB: deviceId={}", deviceId);
            return entity;
        } catch (Exception e) {
            log.error("Failed to save telemetry to InfluxDB for device: {}", deviceId, e);
            throw new RuntimeException("Failed to save telemetry to InfluxDB", e);
        }
    }

    @Override
    public TelemetryEntity buildEntity(String deviceId, String deviceType, String rawMessage, DecodedMessage decoded) {
        try {
            TelemetryEntity entity = new TelemetryEntity();
            entity.setDeviceId(deviceId);
            entity.setDeviceType(deviceType);
            entity.setRawMessage(rawMessage);
            entity.setReceivedAt(LocalDateTime.now());
            entity.setProcessedAt(LocalDateTime.now());

            String decodedJson = objectMapper.writeValueAsString(decoded);
            entity.setDecodedDataJson(decodedJson);

            extractMainFields(entity, decoded);
            return entity;
        } catch (Exception e) {
            log.error("Failed to build telemetry entity for device: {}", deviceId, e);
            throw new RuntimeException("Failed to build telemetry entity", e);
        }
    }

    private void extractMainFields(TelemetryEntity entity, DecodedMessage decoded) {
        if (decoded.getUniqueIdentifier() != null) {
            entity.setImei(decoded.getUniqueIdentifier().getImei());
        }
        if (decoded.getUnitInfo() != null) {
            entity.setFirmwareVersion(decoded.getUnitInfo().getFirmwareRevision());
        }
        if (decoded.getBatteryStatus() != null) {
            String voltageStr = decoded.getBatteryStatus().getBatteryVoltage();
            if (voltageStr != null) {
                try { entity.setBatteryVoltage(Double.parseDouble(voltageStr)); }
                catch (NumberFormatException ignored) { }
            }
            String percentageStr = decoded.getBatteryStatus().getBatteryRemainingPercentage();
            if (percentageStr != null) {
                try { entity.setBatteryPercentage(Double.parseDouble(percentageStr)); }
                catch (NumberFormatException ignored) { }
            }
        }
        if (decoded.getSignalStrength() != null) {
            Integer csq = decoded.getSignalStrength().getCsq();
            Integer rssi = decoded.getSignalStrength().getRssi();
            entity.setSignalStrength(csq != null ? csq : rssi);
        }
        entity.setMessageType(decoded.getMessageType());
        if (decoded.getMeasurementData() != null) {
            entity.setMeasurementCount(decoded.getMeasurementData().size());
        }
    }

    @Override
    public Optional<TelemetryEntity> findById(Long id) {
        log.warn("findById() not supported in InfluxDB (no auto-increment IDs). Use findByDeviceId() instead.");
        return Optional.empty();
    }

    @Override
    public List<TelemetryEntity> findByDeviceId(String deviceId) {
        String query = FluxQueryHelper.byDeviceId(bucket, InfluxDBPointMapper.MEASUREMENT_TELEMETRY, deviceId);
        return queryTelemetry(query);
    }

    @Override
    public List<TelemetryEntity> findByDeviceIdAndDateRange(String deviceId, LocalDateTime from, LocalDateTime to) {
        String query = FluxQueryHelper.byDeviceIdAndDateRange(
                bucket, InfluxDBPointMapper.MEASUREMENT_TELEMETRY, deviceId, from, to);
        return queryTelemetry(query);
    }

    @Override
    public List<TelemetryEntity> findByImei(String imei) {
        String query = FluxQueryHelper.byImei(bucket, InfluxDBPointMapper.MEASUREMENT_TELEMETRY, imei);
        return queryTelemetry(query);
    }

    @Override
    public List<TelemetryEntity> findByDeviceType(String deviceType) {
        String query = FluxQueryHelper.byDeviceType(bucket, InfluxDBPointMapper.MEASUREMENT_TELEMETRY, deviceType);
        return queryTelemetry(query);
    }

    @Override
    public long countByDeviceId(String deviceId) {
        String query = FluxQueryHelper.countByDeviceId(
                bucket, InfluxDBPointMapper.MEASUREMENT_TELEMETRY, deviceId);
        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query, org);
            if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
                Object value = tables.get(0).getRecords().get(0).getValue();
                return value instanceof Number n ? n.longValue() : 0L;
            }
            return 0L;
        } catch (Exception e) {
            log.error("Failed to count telemetry by deviceId: {}", deviceId, e);
            return 0L;
        }
    }

    @Override
    public void deleteOlderThan(LocalDateTime threshold) {
        try {
            DeleteApi deleteApi = influxDBClient.getDeleteApi();
            OffsetDateTime start = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime stop = threshold.atOffset(ZoneOffset.UTC);
            String predicate = FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_TELEMETRY);
            deleteApi.delete(start, stop, predicate, bucket, org);
            log.info("Deleted telemetry data older than {} from InfluxDB", threshold);
        } catch (Exception e) {
            log.error("Failed to delete old telemetry from InfluxDB", e);
        }
    }

    private List<TelemetryEntity> queryTelemetry(String fluxQuery) {
        List<TelemetryEntity> results = new ArrayList<>();
        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery, org);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    results.add(mapToTelemetryEntity(record));
                }
            }
        } catch (Exception e) {
            log.error("Failed to query telemetry from InfluxDB", e);
        }
        return results;
    }

    private TelemetryEntity mapToTelemetryEntity(FluxRecord record) {
        TelemetryEntity entity = new TelemetryEntity();
        entity.setDeviceId(getStringTag(record, "device_id"));
        entity.setDeviceType(getStringTag(record, "device_type"));
        entity.setMessageType(getStringTag(record, "message_type"));
        entity.setRawMessage(getStringValue(record, "raw_message"));
        entity.setDecodedDataJson(getStringValue(record, "decoded_data"));
        entity.setImei(getStringValue(record, "imei"));
        entity.setFirmwareVersion(getStringValue(record, "firmware_version"));
        entity.setBatteryVoltage(getDoubleValue(record, "battery_voltage"));
        entity.setBatteryPercentage(getDoubleValue(record, "battery_percentage"));
        entity.setSignalStrength(getIntValue(record, "signal_strength"));
        entity.setMeasurementCount(getIntValue(record, "measurement_count"));
        if (record.getTime() != null) {
            entity.setReceivedAt(LocalDateTime.ofInstant(record.getTime(), ZoneOffset.UTC));
        }
        return entity;
    }

    // -- Helper methods for FluxRecord value extraction --

    private String getStringTag(FluxRecord record, String key) {
        Object v = record.getValueByKey(key);
        return v != null ? v.toString() : null;
    }

    private String getStringValue(FluxRecord record, String key) {
        Object v = record.getValueByKey(key);
        return v != null ? v.toString() : null;
    }

    private Double getDoubleValue(FluxRecord record, String key) {
        Object v = record.getValueByKey(key);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    private Integer getIntValue(FluxRecord record, String key) {
        Object v = record.getValueByKey(key);
        if (v instanceof Number n) return n.intValue();
        return null;
    }
}
