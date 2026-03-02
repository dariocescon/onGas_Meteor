package com.aton.proj.oneGasMeteor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity per salvare le impostazioni del device (Message Type 6)
 */
@Entity
@Table(name = "device_settings", indexes = {
        @Index(name = "idx_ds_device_id", columnList = "device_id"),
        @Index(name = "idx_ds_received_at", columnList = "received_at")
})
public class DeviceSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(name = "raw_message", columnDefinition = "NVARCHAR(MAX)")
    private String rawMessage;

    @Column(name = "settings_json", columnDefinition = "NVARCHAR(MAX)")
    private String settingsJson;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public DeviceSettingsEntity() {
    }

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

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    @Override
    public String toString() {
        return "DeviceSettingsEntity{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
