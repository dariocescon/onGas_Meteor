package com.aton.proj.oneGasMeteor.repository;

import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository astratto per dati di telemetria
 * Le implementazioni concrete gestiranno SQL Server o MongoDB
 */
public interface TelemetryRepository {
    
    /**
     * Salva i dati di telemetria decodificati
     */
    TelemetryEntity save(String deviceId, String deviceType, String rawMessage, DecodedMessage decoded);
    
    /**
     * Trova per ID
     */
    Optional<TelemetryEntity> findById(Long id);
    
    /**
     * Trova per device ID
     */
    List<TelemetryEntity> findByDeviceId(String deviceId);
    
    /**
     * Trova per device ID in un range temporale
     */
    List<TelemetryEntity> findByDeviceIdAndDateRange(String deviceId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Trova per IMEI
     */
    List<TelemetryEntity> findByImei(String imei);
    
    /**
     * Trova tutti i messaggi di un certo tipo
     */
    List<TelemetryEntity> findByDeviceType(String deviceType);
    
    /**
     * Conta messaggi per device
     */
    long countByDeviceId(String deviceId);
    
    /**
     * Elimina vecchi record (per cleanup)
     */
    void deleteOlderThan(LocalDateTime threshold);
}