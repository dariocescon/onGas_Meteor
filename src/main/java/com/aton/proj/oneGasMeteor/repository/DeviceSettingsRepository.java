package com.aton.proj.oneGasMeteor.repository;

import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType6Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository astratto per le impostazioni del device (Message Type 6)
 */
public interface DeviceSettingsRepository {

    /**
     * Salva le impostazioni del device
     */
    DeviceSettingsEntity save(MessageType6Response settings, String rawMessage);

    /**
     * Trova per device ID
     */
    List<DeviceSettingsEntity> findByDeviceId(String deviceId);

    /**
     * Elimina vecchi record (per cleanup)
     */
    void deleteOlderThan(LocalDateTime threshold);
}
