package com.aton.proj.oneGasMeteor.repository;

import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;

import java.util.List;
import java.util.Optional;

/**
 * Repository astratto per comandi device
 * Le implementazioni concrete gestiranno SQL Server o MongoDB
 */
public interface CommandRepository {
    
    /**
     * Salva un nuovo comando
     */
    CommandEntity save(DeviceCommand command);
    
    /**
     * Trova per ID
     */
    Optional<CommandEntity> findById(Long id);
    
    /**
     * Recupera comandi pendenti per un device
     */
    List<CommandEntity> findPendingCommands(String deviceId);
    
    /**
     * Recupera comandi pendenti per device type
     */
    List<CommandEntity> findPendingCommandsByDeviceType(String deviceType);
    
    /**
     * Aggiorna lo stato di un comando
     */
    void updateStatus(Long commandId, CommandEntity.CommandStatus status);
    
    /**
     * Marca comando come inviato
     */
    void markAsSent(Long commandId);
    
    /**
     * Marca comando come consegnato
     */
    void markAsDelivered(Long commandId);
    
    /**
     * Marca comando come fallito
     */
    void markAsFailed(Long commandId, String errorMessage);
    
    /**
     * Incrementa retry count
     */
    void incrementRetryCount(Long commandId);
    
    /**
     * Elimina vecchi comandi completati
     */
    void deleteOldCompletedCommands(int daysOld);
}