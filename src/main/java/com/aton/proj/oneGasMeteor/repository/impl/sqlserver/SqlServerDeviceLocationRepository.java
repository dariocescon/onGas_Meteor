package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;
import com.aton.proj.oneGasMeteor.repository.DeviceLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementazione SQL Server per DeviceLocationRepository
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public class SqlServerDeviceLocationRepository implements DeviceLocationRepository {

    private static final Logger log = LoggerFactory.getLogger(SqlServerDeviceLocationRepository.class);

    private final DeviceLocationJpaRepository jpaRepository;

    public SqlServerDeviceLocationRepository(DeviceLocationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
        log.info(" SqlServerDeviceLocationRepository initialized");
    }

    @Override
    public DeviceLocationEntity save(MessageType17Response gps, String rawMessage) {
        try {
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

            DeviceLocationEntity saved = jpaRepository.save(entity);
            log.debug(" Saved device location: id={}, deviceId={}", saved.getId(), gps.getDeviceId());

            return saved;

        } catch (Exception e) {
            log.error(" Failed to save device location for device: {}", gps.getDeviceId(), e);
            throw new RuntimeException("Failed to save device location", e);
        }
    }

    @Override
    public List<DeviceLocationEntity> findByDeviceId(String deviceId) {
        return jpaRepository.findByDeviceIdOrderByReceivedAtDesc(deviceId);
    }

    @Override
    @Transactional
    public void deleteOlderThan(LocalDateTime threshold) {
        int deleted = jpaRepository.deleteByReceivedAtBefore(threshold);
        log.info("  Deleted {} old device location records before {}", deleted, threshold);
    }
}
