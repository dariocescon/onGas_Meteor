package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.config.ConditionalOnInfluxDatabase;
import com.aton.proj.oneGasMeteor.config.InfluxDBConfig.InfluxDBProperties;
import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;
import com.aton.proj.oneGasMeteor.repository.DeviceLocationRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

/**
 * Implementazione InfluxDB per DeviceLocationRepository.
 */
@Repository
@ConditionalOnInfluxDatabase
public class InfluxDBDeviceLocationRepository implements DeviceLocationRepository {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBDeviceLocationRepository.class);

    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final String bucket;
    private final String org;

    public InfluxDBDeviceLocationRepository(InfluxDBClient influxDBClient, WriteApiBlocking writeApi,
            InfluxDBProperties properties) {
        this.influxDBClient = influxDBClient;
        this.writeApi = writeApi;
        this.bucket = properties.bucket();
        this.org = properties.org();
        log.info("InfluxDBDeviceLocationRepository initialized");
    }

    @Override
    public DeviceLocationEntity save(MessageType17Response gps, String rawMessage) {
        DeviceLocationEntity entity = buildEntity(gps, rawMessage);
        try {
            writeApi.writePoint(InfluxDBPointMapper.toPoint(entity));
            log.debug("Saved device location to InfluxDB: deviceId={}", gps.getDeviceId());
            return entity;
        } catch (Exception e) {
            log.error("Failed to save device location to InfluxDB for device: {}", gps.getDeviceId(), e);
            throw new RuntimeException("Failed to save device location to InfluxDB", e);
        }
    }

    @Override
    public DeviceLocationEntity buildEntity(MessageType17Response gps, String rawMessage) {
        DeviceLocationEntity entity = new DeviceLocationEntity();
        entity.setDeviceId(gps.getDeviceId());
        entity.setDeviceType(gps.getDeviceType());
        entity.setRawMessage(rawMessage);
        entity.setReceivedAt(LocalDateTime.now());
        entity.setLatitude(gps.getLatitude());
        entity.setLongitude(gps.getLongitude());
        entity.setLatitudeRaw(gps.getLatitudeRaw());
        entity.setLongitudeRaw(gps.getLongitudeRaw());
        entity.setAltitude(gps.getAltitude());
        entity.setSpeedKmh(gps.getSpeedKmh());
        entity.setSpeedKnots(gps.getSpeedKnots());
        entity.setGroundHeading(gps.getGroundHeading());
        entity.setHorizontalPrecision(gps.getHorizontalPrecision());
        entity.setUtcTime(gps.getUtcTime());
        entity.setDate(gps.getDate());
        entity.setNumberOfSatellites(gps.getNumberOfSatellites());
        entity.setTimeToFixSeconds(gps.getTimeToFixSeconds());
        entity.setGnssPositioningMode(gps.getGnssPositioningMode());
        return entity;
    }

    @Override
    public List<DeviceLocationEntity> findByDeviceId(String deviceId) {
        String query = FluxQueryHelper.byDeviceId(bucket, InfluxDBPointMapper.MEASUREMENT_LOCATIONS, deviceId);
        List<DeviceLocationEntity> results = new ArrayList<>();
        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query, org);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    results.add(mapToEntity(record));
                }
            }
        } catch (Exception e) {
            log.error("Failed to query device locations from InfluxDB for deviceId: {}", deviceId, e);
        }
        return results;
    }

    @Override
    public void deleteOlderThan(LocalDateTime threshold) {
        try {
            OffsetDateTime start = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime stop = threshold.atOffset(ZoneOffset.UTC);
            String predicate = FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_LOCATIONS);
            influxDBClient.getDeleteApi().delete(start, stop, predicate, bucket, org);
            log.info("Deleted device locations older than {} from InfluxDB", threshold);
        } catch (Exception e) {
            log.error("Failed to delete old device locations from InfluxDB", e);
        }
    }

    private DeviceLocationEntity mapToEntity(FluxRecord record) {
        DeviceLocationEntity e = new DeviceLocationEntity();
        e.setDeviceId(getStr(record, "device_id"));
        e.setDeviceType(getStr(record, "device_type"));
        e.setRawMessage(getStr(record, "raw_message"));
        e.setLatitude(getDbl(record, "latitude"));
        e.setLongitude(getDbl(record, "longitude"));
        e.setLatitudeRaw(getStr(record, "latitude_raw"));
        e.setLongitudeRaw(getStr(record, "longitude_raw"));
        e.setAltitude(getDbl(record, "altitude"));
        e.setSpeedKmh(getDbl(record, "speed_kmh"));
        e.setSpeedKnots(getDbl(record, "speed_knots"));
        e.setGroundHeading(getDbl(record, "ground_heading"));
        e.setHorizontalPrecision(getDbl(record, "horizontal_precision"));
        String utcTimeStr = getStr(record, "utc_time");
        if (utcTimeStr != null) {
            try { e.setUtcTime(LocalTime.parse(utcTimeStr)); }
            catch (Exception ignored) { }
        }
        e.setDate(getStr(record, "gps_date"));
        e.setNumberOfSatellites(getInt(record, "number_of_satellites"));
        e.setTimeToFixSeconds(getInt(record, "time_to_fix_seconds"));
        e.setGnssPositioningMode(getInt(record, "gnss_positioning_mode"));
        if (record.getTime() != null) {
            e.setReceivedAt(LocalDateTime.ofInstant(record.getTime(), ZoneOffset.UTC));
        }
        return e;
    }

    private String getStr(FluxRecord r, String k) { Object v = r.getValueByKey(k); return v != null ? v.toString() : null; }
    private Double getDbl(FluxRecord r, String k) { Object v = r.getValueByKey(k); return v instanceof Number n ? n.doubleValue() : null; }
    private Integer getInt(FluxRecord r, String k) { Object v = r.getValueByKey(k); return v instanceof Number n ? n.intValue() : null; }
}
