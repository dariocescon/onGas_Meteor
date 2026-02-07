package com.aton.proj.oneGasMeteor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity per gestire i comandi da inviare ai dispositivi
 */
@Entity
@Table(name = "device_commands", indexes = {
    @Index(name = "idx_device_status", columnList = "device_id, status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class CommandEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;
    
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;
    
    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;
    
    @Column(name = "command_params", columnDefinition = "TEXT")
    private String commandParamsJson; // JSON serializzato dei parametri
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommandStatus status = CommandStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    // Constructors
    public CommandEntity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public CommandEntity(String deviceId, String deviceType, String commandType) {
        this();
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.commandType = commandType;
    }
    
    // Getters & Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public String getCommandType() {
        return commandType;
    }
    
    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }
    
    public String getCommandParamsJson() {
        return commandParamsJson;
    }
    
    public void setCommandParamsJson(String commandParamsJson) {
        this.commandParamsJson = commandParamsJson;
    }
    
    public CommandStatus getStatus() {
        return status;
    }
    
    public void setStatus(CommandStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
    
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    /**
     * Enum per lo stato del comando
     */
    public enum CommandStatus {
        PENDING,    // In attesa di essere inviato
        SENT,       // Inviato al device
        DELIVERED,  // Confermato dal device
        FAILED,     // Fallito dopo max retry
        EXPIRED     // Scaduto
    }
    
    @Override
    public String toString() {
        return "CommandEntity{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", commandType='" + commandType + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}