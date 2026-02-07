package com.aton.proj.oneGasMeteor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity per salvare i dati di telemetria decodificati
 */
@Entity
@Table(name = "telemetry_data", indexes = {
    @Index(name = "idx_device_id", columnList = "device_id"),
    @Index(name = "idx_device_type", columnList = "device_type"),
    @Index(name = "idx_received_at", columnList = "received_at")
})
public class TelemetryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;
    
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;
    
    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;
    
    @Column(name = "decoded_data", columnDefinition = "TEXT")
    private String decodedDataJson; // JSON serializzato di DecodedMessage
    
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    // Campi estratti per query veloci
    @Column(name = "imei", length = 20)
    private String imei;
    
    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;
    
    @Column(name = "battery_voltage")
    private Double batteryVoltage;
    
    @Column(name = "battery_percentage")
    private Double batteryPercentage;
    
    @Column(name = "signal_strength")
    private Integer signalStrength;
    
    @Column(name = "message_type", length = 50)
    private String messageType;
    
    @Column(name = "measurement_count")
    private Integer measurementCount;
    
    // Constructors
    public TelemetryEntity() {
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
    
    public String getRawMessage() {
        return rawMessage;
    }
    
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }
    
    public String getDecodedDataJson() {
        return decodedDataJson;
    }
    
    public void setDecodedDataJson(String decodedDataJson) {
        this.decodedDataJson = decodedDataJson;
    }
    
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getImei() {
        return imei;
    }
    
    public void setImei(String imei) {
        this.imei = imei;
    }
    
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    
    public Double getBatteryVoltage() {
        return batteryVoltage;
    }
    
    public void setBatteryVoltage(Double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }
    
    public Double getBatteryPercentage() {
        return batteryPercentage;
    }
    
    public void setBatteryPercentage(Double batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }
    
    public Integer getSignalStrength() {
        return signalStrength;
    }
    
    public void setSignalStrength(Integer signalStrength) {
        this.signalStrength = signalStrength;
    }
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public Integer getMeasurementCount() {
        return measurementCount;
    }
    
    public void setMeasurementCount(Integer measurementCount) {
        this.measurementCount = measurementCount;
    }
    
    @Override
    public String toString() {
        return "TelemetryEntity{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", imei='" + imei + '\'' +
                ", receivedAt=" + receivedAt +
                '}';
    }
}