package com.aton.proj.oneGasMeteor.repository.impl.sql;

import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnJpaDatabase;
import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;
import com.aton.proj.oneGasMeteor.repository.DeviceLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementazione SQL per DeviceLocationRepository
 */
@Repository
@ConditionalOnJpaDatabase
public class JpaDeviceLocationRepository implements DeviceLocationRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaDeviceLocationRepository.class);

    private final DeviceLocationJpaRepository jpaRepository;

    public JpaDeviceLocationRepository(DeviceLocationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
        log.info("JpaDeviceLocationRepository initialized");
    }

    @Override
    public DeviceLocationEntity save(MessageType17Response gps, String rawMessage) {
        try {
            DeviceLocationEntity entity = buildEntity(gps, rawMessage);

            DeviceLocationEntity saved = jpaRepository.save(entity);
            log.debug("Saved device location: id={}, deviceId={}", saved.getId(), gps.getDeviceId());

            return saved;

        } catch (Exception e) {
            log.error("Failed to save device location for device: {}", gps.getDeviceId(), e);
            throw new RuntimeException("Failed to save device location", e);
        }
    }

    /**
     * Costruisce un DeviceLocationEntity senza salvarlo (usato per batch insert)
     */
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
        return jpaRepository.findByDeviceIdOrderByReceivedAtDesc(deviceId);
    }

    @Override
    @Transactional
    public void deleteOlderThan(LocalDateTime threshold) {
        int deleted = jpaRepository.deleteByReceivedAtBefore(threshold);
        log.info(" Deleted {} old device location records before {}", deleted, threshold);
    }
}
