package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType6Response;
import com.aton.proj.oneGasMeteor.repository.DeviceSettingsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementazione SQL Server per DeviceSettingsRepository
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public class SqlServerDeviceSettingsRepository implements DeviceSettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(SqlServerDeviceSettingsRepository.class);

    private final DeviceSettingsJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public SqlServerDeviceSettingsRepository(DeviceSettingsJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
        log.info(" SqlServerDeviceSettingsRepository initialized");
    }

    @Override
    public DeviceSettingsEntity save(MessageType6Response settings, String rawMessage) {
        try {
            DeviceSettingsEntity entity = new DeviceSettingsEntity();
            entity.setDeviceId(settings.getDeviceId());
            entity.setDeviceType(settings.getDeviceType());
            entity.setRawMessage(rawMessage);
            entity.setReceivedAt(LocalDateTime.now());

            String settingsJson = objectMapper.writeValueAsString(settings.getSettings());
            entity.setSettingsJson(settingsJson);

            DeviceSettingsEntity saved = jpaRepository.save(entity);
            log.debug(" Saved device settings: id={}, deviceId={}", saved.getId(), settings.getDeviceId());

            return saved;

        } catch (Exception e) {
            log.error(" Failed to save device settings for device: {}", settings.getDeviceId(), e);
            throw new RuntimeException("Failed to save device settings", e);
        }
    }

    @Override
    public List<DeviceSettingsEntity> findByDeviceId(String deviceId) {
        return jpaRepository.findByDeviceIdOrderByReceivedAtDesc(deviceId);
    }

    @Override
    @Transactional
    public void deleteOlderThan(LocalDateTime threshold) {
        int deleted = jpaRepository.deleteByReceivedAtBefore(threshold);
        log.info("  Deleted {} old device settings records before {}", deleted, threshold);
    }
}
