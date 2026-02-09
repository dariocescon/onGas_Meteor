package com.aton.proj.oneGasMeteor.model;

import java.time.LocalDateTime;

/**
 * Response model per Message Type 16 (ICCID & Modem Statistics)
 */
public class MessageType16Response {

	private String deviceId;
	private String deviceType;
	private LocalDateTime receivedAt;

	// Modem info
	private String iccid;

	// Statistics
	private Long energyUsed;
	private Integer minTemperature;
	private Integer maxTemperature;
	private Integer messageCount;
	private Integer deliveryFailCount;
	private Long totalSendTime;
	private Long maxSendTime;
	private Long minSendTime;
	private Long rssiTotal;
	private Integer rssiValidCount;
	private Integer rssiFailCount;

	// Calculated fields
	private Double averageSendTime;
	private Double averageRssi;
	private Double deliverySuccessRate;

	public MessageType16Response() {
		this.receivedAt = LocalDateTime.now();
	}

	// Calcola campi derivati
	public void calculateDerivedFields() {
		if (messageCount != null && messageCount > 0 && totalSendTime != null) {
			this.averageSendTime = (double) totalSendTime / messageCount;
		}

		if (rssiValidCount != null && rssiValidCount > 0 && rssiTotal != null) {
			this.averageRssi = (double) rssiTotal / rssiValidCount;
		}

		if (messageCount != null && messageCount > 0 && deliveryFailCount != null) {
			int successCount = messageCount - deliveryFailCount;
			this.deliverySuccessRate = ((double) successCount / messageCount) * 100.0;
		}
	}

	// Getters & Setters
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

	public LocalDateTime getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(LocalDateTime receivedAt) {
		this.receivedAt = receivedAt;
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

	@Override
	public String toString() {
		return "MessageType16Response{" + "deviceId='" + deviceId + '\'' + ", iccid='" + iccid + '\'' + ", energyUsed="
				+ energyUsed + ", messageCount=" + messageCount + ", deliverySuccessRate="
				+ String.format("%.2f%%", deliverySuccessRate) + ", averageRssi=" + String.format("%.1f", averageRssi)
				+ '}';
	}
}