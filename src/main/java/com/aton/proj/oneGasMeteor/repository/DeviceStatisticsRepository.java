package com.aton.proj.oneGasMeteor.repository;

import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType16Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository astratto per le statistiche del device (Message Type 16)
 */
public interface DeviceStatisticsRepository {

    /**
     * Salva le statistiche del device
     */
    DeviceStatisticsEntity save(MessageType16Response stats, String rawMessage);

    /**
     * Trova per device ID
     */
    List<DeviceStatisticsEntity> findByDeviceId(String deviceId);

    /**
     * Elimina vecchi record (per cleanup)
     */
    void deleteOlderThan(LocalDateTime threshold);
}
