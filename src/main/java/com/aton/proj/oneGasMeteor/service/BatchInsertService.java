package com.aton.proj.oneGasMeteor.service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.aton.proj.oneGasMeteor.config.ConditionalOnJpaDatabase;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;

/**
 * Service per batch INSERT su SQL Server tramite JdbcTemplate.
 * Raccoglie le entity in code concorrenti e le persiste periodicamente.
 */
@Service
@ConditionalOnJpaDatabase
public class BatchInsertService {

    private static final Logger log = LoggerFactory.getLogger(BatchInsertService.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${batch.insert.size:100}")
    private int batchSize;

    private final ConcurrentLinkedQueue<TelemetryEntity> telemetryQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<DeviceSettingsEntity> settingsQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<DeviceStatisticsEntity> statisticsQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<DeviceLocationEntity> locationQueue = new ConcurrentLinkedQueue<>();

    /** Guarantees that at most one flush cycle runs at a time. */
    private final ReentrantLock flushLock = new ReentrantLock();

    public BatchInsertService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        log.info("BatchInsertService initialized");
    }

    public void enqueue(TelemetryEntity entity) {
        telemetryQueue.add(entity);
    }

    public void enqueue(DeviceSettingsEntity entity) {
        settingsQueue.add(entity);
    }

    public void enqueue(DeviceStatisticsEntity entity) {
        statisticsQueue.add(entity);
    }

    public void enqueue(DeviceLocationEntity entity) {
        locationQueue.add(entity);
    }

    @Scheduled(fixedDelayString = "${batch.insert.interval-ms:2000}")
    public void flushAll() {

        if (telemetryQueue.isEmpty() && settingsQueue.isEmpty()
                && statisticsQueue.isEmpty() && locationQueue.isEmpty()) {
            log.trace("All queues empty, skipping flush cycle");
            return;
        }

        if (!flushLock.tryLock()) {
            log.warn("Flush already in progress, skipping this cycle");
            return;
        }

        Instant start = Instant.now();
        int[] counts = new int[4];

        int maxBatchesPerCycle = 10;

        try {

            counts[0] += flushQueue(telemetryQueue, this::flushTelemetry, maxBatchesPerCycle);
            counts[1] += flushQueue(settingsQueue, this::flushSettings, maxBatchesPerCycle);
            counts[2] += flushQueue(statisticsQueue, this::flushStatistics, maxBatchesPerCycle);
            counts[3] += flushQueue(locationQueue, this::flushLocations, maxBatchesPerCycle);

        } finally {
            flushLock.unlock();

            long ms = Duration.between(start, Instant.now()).toMillis();

            log.info("Batch flush completed in {} ms — inserted: telemetry={}, settings={}, statistics={}, locations={}",
                    ms, counts[0], counts[1], counts[2], counts[3]);
        }
    }
    
    private int flushQueue(Queue<?> queue, Supplier<Integer> flushMethod, int maxBatches) {

        int total = 0;
        int processed = 0;

        while (!queue.isEmpty() && processed < maxBatches) {
            total += flushMethod.get();
            processed++;
        }

        return total;
    }

    // -------------------------------------------------------------------------
    // Flush methods
    // -------------------------------------------------------------------------

    private int flushTelemetry() {
        List<TelemetryEntity> batch = drain(telemetryQueue);
        if (batch.isEmpty()) return 0;

        log.debug("Flushing {} telemetry records via batch INSERT", batch.size());
        String sql = "INSERT INTO telemetry_data " +
                "(device_id, device_type, raw_message, decoded_data, received_at, processed_at, " +
                "imei, firmware_version, battery_voltage, battery_percentage, signal_strength, " +
                "message_type, measurement_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    TelemetryEntity e = batch.get(i);
                    ps.setString(1, e.getDeviceId());
                    ps.setString(2, e.getDeviceType());
                    ps.setString(3, e.getRawMessage());
                    ps.setString(4, e.getDecodedDataJson());
                    ps.setTimestamp(5, toTimestamp(e.getReceivedAt()));
                    ps.setTimestamp(6, toTimestamp(e.getProcessedAt()));
                    ps.setString(7, e.getImei());
                    ps.setString(8, e.getFirmwareVersion());
                    setNullableDouble(ps, 9, e.getBatteryVoltage());
                    setNullableDouble(ps, 10, e.getBatteryPercentage());
                    setNullableInt(ps, 11, e.getSignalStrength());
                    ps.setString(12, e.getMessageType());
                    setNullableInt(ps, 13, e.getMeasurementCount());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
            log.info("Batch INSERT telemetry: {} records persisted", batch.size());
            return batch.size();
        } catch (Exception ex) {
            log.error("Batch INSERT failed for telemetry ({} records), re-enqueuing: {}", batch.size(), ex.getMessage(), ex);
            telemetryQueue.addAll(batch);
            return 0;
        }
    }

    private int flushSettings() {
        List<DeviceSettingsEntity> batch = drain(settingsQueue);
        if (batch.isEmpty()) return 0;

        log.debug("Flushing {} device_settings records via batch INSERT", batch.size());
        String sql = "INSERT INTO device_settings " +
                "(device_id, device_type, raw_message, settings_json, received_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DeviceSettingsEntity e = batch.get(i);
                    ps.setString(1, e.getDeviceId());
                    ps.setString(2, e.getDeviceType());
                    ps.setString(3, e.getRawMessage());
                    ps.setString(4, e.getSettingsJson());
                    ps.setTimestamp(5, toTimestamp(e.getReceivedAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
            log.info("Batch INSERT device_settings: {} records persisted", batch.size());
            return batch.size();
        } catch (Exception ex) {
            log.error("Batch INSERT failed for device_settings ({} records), re-enqueuing: {}", batch.size(), ex.getMessage(), ex);
            settingsQueue.addAll(batch);
            return 0;
        }
    }

    private int flushStatistics() {
        List<DeviceStatisticsEntity> batch = drain(statisticsQueue);
        if (batch.isEmpty()) return 0;

        log.debug("Flushing {} device_statistics records via batch INSERT", batch.size());
        String sql = "INSERT INTO device_statistics " +
                "(device_id, device_type, raw_message, iccid, energy_used, min_temperature, " +
                "max_temperature, message_count, delivery_fail_count, total_send_time, " +
                "max_send_time, min_send_time, rssi_total, rssi_valid_count, rssi_fail_count, " +
                "average_send_time, average_rssi, delivery_success_rate, received_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DeviceStatisticsEntity e = batch.get(i);
                    ps.setString(1, e.getDeviceId());
                    ps.setString(2, e.getDeviceType());
                    ps.setString(3, e.getRawMessage());
                    ps.setString(4, e.getIccid());
                    setNullableLong(ps, 5, e.getEnergyUsed());
                    setNullableInt(ps, 6, e.getMinTemperature());
                    setNullableInt(ps, 7, e.getMaxTemperature());
                    setNullableInt(ps, 8, e.getMessageCount());
                    setNullableInt(ps, 9, e.getDeliveryFailCount());
                    setNullableLong(ps, 10, e.getTotalSendTime());
                    setNullableLong(ps, 11, e.getMaxSendTime());
                    setNullableLong(ps, 12, e.getMinSendTime());
                    setNullableLong(ps, 13, e.getRssiTotal());
                    setNullableInt(ps, 14, e.getRssiValidCount());
                    setNullableInt(ps, 15, e.getRssiFailCount());
                    setNullableDouble(ps, 16, e.getAverageSendTime());
                    setNullableDouble(ps, 17, e.getAverageRssi());
                    setNullableDouble(ps, 18, e.getDeliverySuccessRate());
                    ps.setTimestamp(19, toTimestamp(e.getReceivedAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
            log.info("Batch INSERT device_statistics: {} records persisted", batch.size());
            return batch.size();
        } catch (Exception ex) {
            log.error("Batch INSERT failed for device_statistics ({} records), re-enqueuing: {}", batch.size(), ex.getMessage(), ex);
            statisticsQueue.addAll(batch);
            return 0;
        }
    }

    private int flushLocations() {
        List<DeviceLocationEntity> batch = drain(locationQueue);
        if (batch.isEmpty()) return 0;

        log.debug("Flushing {} device_locations records via batch INSERT", batch.size());
        String sql = "INSERT INTO device_locations " +
                "(device_id, device_type, raw_message, latitude, longitude, latitude_raw, " +
                "longitude_raw, altitude, speed_kmh, speed_knots, ground_heading, " +
                "horizontal_precision, utc_time, gps_date, number_of_satellites, " +
                "time_to_fix_seconds, gnss_positioning_mode, received_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DeviceLocationEntity e = batch.get(i);
                    ps.setString(1, e.getDeviceId());
                    ps.setString(2, e.getDeviceType());
                    ps.setString(3, e.getRawMessage());
                    setNullableDouble(ps, 4, e.getLatitude());
                    setNullableDouble(ps, 5, e.getLongitude());
                    ps.setString(6, e.getLatitudeRaw());
                    ps.setString(7, e.getLongitudeRaw());
                    setNullableDouble(ps, 8, e.getAltitude());
                    setNullableDouble(ps, 9, e.getSpeedKmh());
                    setNullableDouble(ps, 10, e.getSpeedKnots());
                    setNullableDouble(ps, 11, e.getGroundHeading());
                    setNullableDouble(ps, 12, e.getHorizontalPrecision());
                    if (e.getUtcTime() != null) {
                        ps.setTime(13, java.sql.Time.valueOf(e.getUtcTime()));
                    } else {
                        ps.setNull(13, Types.TIME);
                    }
                    ps.setString(14, e.getDate());
                    setNullableInt(ps, 15, e.getNumberOfSatellites());
                    setNullableInt(ps, 16, e.getTimeToFixSeconds());
                    setNullableInt(ps, 17, e.getGnssPositioningMode());
                    ps.setTimestamp(18, toTimestamp(e.getReceivedAt()));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
            log.info("Batch INSERT device_locations: {} records persisted", batch.size());
            return batch.size();
        } catch (Exception ex) {
            log.error("Batch INSERT failed for device_locations ({} records), re-enqueuing: {}", batch.size(), ex.getMessage(), ex);
            locationQueue.addAll(batch);
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Drain up to batchSize elements from the queue into a list.
     */
    private <T> List<T> drain(ConcurrentLinkedQueue<T> queue) {
        List<T> list = new ArrayList<>(batchSize);
        T item;
        int count = 0;
        while (count < batchSize && (item = queue.poll()) != null) {
            list.add(item);
            count++;
        }
        return list;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime ldt) {
        return ldt != null ? Timestamp.valueOf(ldt) : null;
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(index, value);
        } else {
            ps.setNull(index, Types.DOUBLE);
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.BIGINT);
        }
    }
}
