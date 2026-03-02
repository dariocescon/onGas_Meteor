package com.aton.proj.oneGasMeteor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity per salvare le statistiche del device (Message Type 16)
 */
@Entity
@Table(name = "device_statistics", indexes = {
        @Index(name = "idx_dst_device_id", columnList = "device_id"),
        @Index(name = "idx_dst_received_at", columnList = "received_at")
})
public class DeviceStatisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(name = "raw_message", columnDefinition = "NVARCHAR(MAX)")
    private String rawMessage;

    @Column(name = "iccid", length = 30)
    private String iccid;

    @Column(name = "energy_used")
    private Long energyUsed;

    @Column(name = "min_temperature")
    private Integer minTemperature;

    @Column(name = "max_temperature")
    private Integer maxTemperature;

    @Column(name = "message_count")
    private Integer messageCount;

    @Column(name = "delivery_fail_count")
    private Integer deliveryFailCount;

    @Column(name = "total_send_time")
    private Long totalSendTime;

    @Column(name = "max_send_time")
    private Long maxSendTime;

    @Column(name = "min_send_time")
    private Long minSendTime;

    @Column(name = "rssi_total")
    private Long rssiTotal;

    @Column(name = "rssi_valid_count")
    private Integer rssiValidCount;

    @Column(name = "rssi_fail_count")
    private Integer rssiFailCount;

    @Column(name = "average_send_time")
    private Double averageSendTime;

    @Column(name = "average_rssi")
    private Double averageRssi;

    @Column(name = "delivery_success_rate")
    private Double deliverySuccessRate;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public DeviceStatisticsEntity() {
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

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public Long getEnergyUsed() {
        return energyUsed;
    }

    public void setEnergyUsed(Long energyUsed) {
        this.energyUsed = energyUsed;
    }

    public Integer getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(Integer minTemperature) {
        this.minTemperature = minTemperature;
    }

    public Integer getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(Integer maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public Integer getDeliveryFailCount() {
        return deliveryFailCount;
    }

    public void setDeliveryFailCount(Integer deliveryFailCount) {
        this.deliveryFailCount = deliveryFailCount;
    }

    public Long getTotalSendTime() {
        return totalSendTime;
    }

    public void setTotalSendTime(Long totalSendTime) {
        this.totalSendTime = totalSendTime;
    }

    public Long getMaxSendTime() {
        return maxSendTime;
    }

    public void setMaxSendTime(Long maxSendTime) {
        this.maxSendTime = maxSendTime;
    }

    public Long getMinSendTime() {
        return minSendTime;
    }

    public void setMinSendTime(Long minSendTime) {
        this.minSendTime = minSendTime;
    }

    public Long getRssiTotal() {
        return rssiTotal;
    }

    public void setRssiTotal(Long rssiTotal) {
        this.rssiTotal = rssiTotal;
    }

    public Integer getRssiValidCount() {
        return rssiValidCount;
    }

    public void setRssiValidCount(Integer rssiValidCount) {
        this.rssiValidCount = rssiValidCount;
    }

    public Integer getRssiFailCount() {
        return rssiFailCount;
    }

    public void setRssiFailCount(Integer rssiFailCount) {
        this.rssiFailCount = rssiFailCount;
    }

    public Double getAverageSendTime() {
        return averageSendTime;
    }

    public void setAverageSendTime(Double averageSendTime) {
        this.averageSendTime = averageSendTime;
    }

    public Double getAverageRssi() {
        return averageRssi;
    }

    public void setAverageRssi(Double averageRssi) {
        this.averageRssi = averageRssi;
    }

    public Double getDeliverySuccessRate() {
        return deliverySuccessRate;
    }

    public void setDeliverySuccessRate(Double deliverySuccessRate) {
        this.deliverySuccessRate = deliverySuccessRate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    @Override
    public String toString() {
        return "DeviceStatisticsEntity{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", iccid='" + iccid + '\'' +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
