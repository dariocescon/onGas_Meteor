package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.config.InfluxDBConfig.InfluxDBProperties;
import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnInfluxDatabase;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType16Response;
import com.aton.proj.oneGasMeteor.repository.DeviceStatisticsRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

/**
 * Implementazione InfluxDB per DeviceStatisticsRepository.
 */
@Repository
@ConditionalOnInfluxDatabase
public class InfluxDBDeviceStatisticsRepository implements DeviceStatisticsRepository {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBDeviceStatisticsRepository.class);

    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private final String bucket;
    private final String org;

    public InfluxDBDeviceStatisticsRepository(InfluxDBClient influxDBClient, WriteApiBlocking writeApi,
            InfluxDBProperties properties) {
        this.influxDBClient = influxDBClient;
        this.writeApi = writeApi;
        this.bucket = properties.bucket();
        this.org = properties.org();
        log.info("InfluxDBDeviceStatisticsRepository initialized");
    }

    @Override
    public DeviceStatisticsEntity save(MessageType16Response stats, String rawMessage) {
        DeviceStatisticsEntity entity = buildEntity(stats, rawMessage);
        try {
            writeApi.writePoint(InfluxDBPointMapper.toPoint(entity));
            log.debug("Saved device statistics to InfluxDB: deviceId={}", stats.getDeviceId());
            return entity;
        } catch (Exception e) {
            log.error("Failed to save device statistics to InfluxDB for device: {}", stats.getDeviceId(), e);
            throw new RuntimeException("Failed to save device statistics to InfluxDB", e);
        }
    }

    @Override
    public DeviceStatisticsEntity buildEntity(MessageType16Response stats, String rawMessage) {
        DeviceStatisticsEntity entity = new DeviceStatisticsEntity();
        entity.setDeviceId(stats.getDeviceId());
        entity.setDeviceType(stats.getDeviceType());
        entity.setRawMessage(rawMessage);
        entity.setReceivedAt(LocalDateTime.now());
        entity.setIccid(stats.getIccid());
        entity.setEnergyUsed(stats.getEnergyUsed());
        entity.setMinTemperature(stats.getMinTemperature());
        entity.setMaxTemperature(stats.getMaxTemperature());
        entity.setMessageCount(stats.getMessageCount());
        entity.setDeliveryFailCount(stats.getDeliveryFailCount());
        entity.setTotalSendTime(stats.getTotalSendTime());
        entity.setMaxSendTime(stats.getMaxSendTime());
        entity.setMinSendTime(stats.getMinSendTime());
        entity.setRssiTotal(stats.getRssiTotal());
        entity.setRssiValidCount(stats.getRssiValidCount());
        entity.setRssiFailCount(stats.getRssiFailCount());
        entity.setAverageSendTime(stats.getAverageSendTime());
        entity.setAverageRssi(stats.getAverageRssi());
        entity.setDeliverySuccessRate(stats.getDeliverySuccessRate());
        return entity;
    }

    @Override
    public List<DeviceStatisticsEntity> findByDeviceId(String deviceId) {
        String query = FluxQueryHelper.byDeviceId(bucket, InfluxDBPointMapper.MEASUREMENT_STATISTICS, deviceId);
        List<DeviceStatisticsEntity> results = new ArrayList<>();
        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query, org);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    results.add(mapToEntity(record));
                }
            }
        } catch (Exception e) {
            log.error("Failed to query device statistics from InfluxDB for deviceId: {}", deviceId, e);
        }
        return results;
    }

    @Override
    public void deleteOlderThan(LocalDateTime threshold) {
        try {
            OffsetDateTime start = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime stop = threshold.atOffset(ZoneOffset.UTC);
            String predicate = FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_STATISTICS);
            influxDBClient.getDeleteApi().delete(start, stop, predicate, bucket, org);
            log.info("Deleted device statistics older than {} from InfluxDB", threshold);
        } catch (Exception e) {
            log.error("Failed to delete old device statistics from InfluxDB", e);
        }
    }

    private DeviceStatisticsEntity mapToEntity(FluxRecord record) {
        DeviceStatisticsEntity e = new DeviceStatisticsEntity();
        e.setDeviceId(getStr(record, "device_id"));
        e.setDeviceType(getStr(record, "device_type"));
        e.setRawMessage(getStr(record, "raw_message"));
        e.setIccid(getStr(record, "iccid"));
        e.setEnergyUsed(getLong(record, "energy_used"));
        e.setMinTemperature(getInt(record, "min_temperature"));
        e.setMaxTemperature(getInt(record, "max_temperature"));
        e.setMessageCount(getInt(record, "message_count"));
        e.setDeliveryFailCount(getInt(record, "delivery_fail_count"));
        e.setTotalSendTime(getLong(record, "total_send_time"));
        e.setMaxSendTime(getLong(record, "max_send_time"));
        e.setMinSendTime(getLong(record, "min_send_time"));
        e.setRssiTotal(getLong(record, "rssi_total"));
        e.setRssiValidCount(getInt(record, "rssi_valid_count"));
        e.setRssiFailCount(getInt(record, "rssi_fail_count"));
        e.setAverageSendTime(getDbl(record, "average_send_time"));
        e.setAverageRssi(getDbl(record, "average_rssi"));
        e.setDeliverySuccessRate(getDbl(record, "delivery_success_rate"));
        if (record.getTime() != null) {
            e.setReceivedAt(LocalDateTime.ofInstant(record.getTime(), ZoneOffset.UTC));
        }
        return e;
    }

    private String getStr(FluxRecord r, String k) { Object v = r.getValueByKey(k); return v != null ? v.toString() : null; }
    private Double getDbl(FluxRecord r, String k) { Object v = r.getValueByKey(k); return v instanceof Number n ? n.doubleValue() : null; }
    private Integer getInt(FluxRecord r, String k) { Object v = r.getValueByKey(k); return v instanceof Number n ? n.intValue() : null; }
    private Long getLong(FluxRecord r, String k) { Object v = r.getValueByKey(k); return v instanceof Number n ? n.longValue() : null; }
}
