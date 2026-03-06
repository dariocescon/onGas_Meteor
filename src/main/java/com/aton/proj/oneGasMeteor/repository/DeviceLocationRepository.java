package com.aton.proj.oneGasMeteor.repository;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository astratto per i dati GPS del device (Message Type 17)
 */
public interface DeviceLocationRepository {

    /**
     * Salva i dati GPS del device
     */
    DeviceLocationEntity save(MessageType17Response gps, String rawMessage);

    /**
     * Costruisce un DeviceLocationEntity senza salvarlo (usato per batch insert)
     */
    DeviceLocationEntity buildEntity(MessageType17Response gps, String rawMessage);

    /**
     * Trova per device ID
     */
    List<DeviceLocationEntity> findByDeviceId(String deviceId);

    /**
     * Elimina vecchi record (per cleanup)
     */
    void deleteOlderThan(LocalDateTime threshold);
}
