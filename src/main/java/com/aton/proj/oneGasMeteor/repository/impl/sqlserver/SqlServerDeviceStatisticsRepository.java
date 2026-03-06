package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType16Response;
import com.aton.proj.oneGasMeteor.repository.DeviceStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementazione SQL Server per DeviceStatisticsRepository
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public class SqlServerDeviceStatisticsRepository implements DeviceStatisticsRepository {

    private static final Logger log = LoggerFactory.getLogger(SqlServerDeviceStatisticsRepository.class);

    private final DeviceStatisticsJpaRepository jpaRepository;

    public SqlServerDeviceStatisticsRepository(DeviceStatisticsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
        log.info(" SqlServerDeviceStatisticsRepository initialized");
    }

    @Override
    public DeviceStatisticsEntity save(MessageType16Response stats, String rawMessage) {
        try {
            DeviceStatisticsEntity entity = buildEntity(stats, rawMessage);

            DeviceStatisticsEntity saved = jpaRepository.save(entity);
            log.debug(" Saved device statistics: id={}, deviceId={}", saved.getId(), stats.getDeviceId());

            return saved;

        } catch (Exception e) {
            log.error(" Failed to save device statistics for device: {}", stats.getDeviceId(), e);
            throw new RuntimeException("Failed to save device statistics", e);
        }
    }

    /**
     * Costruisce un DeviceStatisticsEntity senza salvarlo (usato per batch insert)
     */
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
        return jpaRepository.findByDeviceIdOrderByReceivedAtDesc(deviceId);
    }

    @Override
    @Transactional
    public void deleteOlderThan(LocalDateTime threshold) {
        int deleted = jpaRepository.deleteByReceivedAtBefore(threshold);
        log.info("  Deleted {} old device statistics records before {}", deleted, threshold);
    }
}
