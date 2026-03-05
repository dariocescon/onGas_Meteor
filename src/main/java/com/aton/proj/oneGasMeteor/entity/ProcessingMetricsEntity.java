package com.aton.proj.oneGasMeteor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity per le metriche di performance delle singole elaborazioni TCP
 */
@Entity
@Table(name = "processing_metrics", indexes = {
	@Index(name = "idx_pm_device_id", columnList = "device_id"),
	@Index(name = "idx_pm_received_at", columnList = "received_at"),
	@Index(name = "idx_pm_message_type", columnList = "message_type"),
	@Index(name = "idx_pm_success", columnList = "success")
})
public class ProcessingMetricsEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "device_id", length = 50)
	private String deviceId;

	@Column(name = "device_type", length = 50)
	private String deviceType;

	@Column(name = "message_type")
	private Integer messageType;

	@Column(name = "client_address", length = 100)
	private String clientAddress;

	// Payload info
	@Column(name = "payload_length_bytes")
	private Integer payloadLengthBytes;

	@Column(name = "declared_body_length")
	private Integer declaredBodyLength;

	@Column(name = "measurement_count")
	private Integer measurementCount;

	// Command info
	@Column(name = "pending_commands_found")
	private Integer pendingCommandsFound;

	@Column(name = "commands_sent")
	private Integer commandsSent;

	@Column(name = "response_size_bytes")
	private Integer responseSizeBytes;

	// Timing (ms)
	@Column(name = "total_processing_time_ms")
	private Long totalProcessingTimeMs;

	@Column(name = "read_time_ms")
	private Long readTimeMs;

	@Column(name = "decode_time_ms")
	private Long decodeTimeMs;

	@Column(name = "db_save_time_ms")
	private Long dbSaveTimeMs;

	@Column(name = "command_query_time_ms")
	private Long commandQueryTimeMs;

	@Column(name = "command_encode_time_ms")
	private Long commandEncodeTimeMs;

	@Column(name = "send_time_ms")
	private Long sendTimeMs;

	// Device health snapshot
	@Column(name = "battery_voltage")
	private Double batteryVoltage;

	@Column(name = "battery_percentage")
	private Double batteryPercentage;

	@Column(name = "signal_strength")
	private Integer signalStrength;

	@Column(name = "contact_reason", length = 200)
	private String contactReason;

	@Column(name = "firmware_version", length = 20)
	private String firmwareVersion;

	// Result
	@Column(name = "success", nullable = false)
	private Boolean success;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	// Timestamps
	@Column(name = "received_at", nullable = false)
	private LocalDateTime receivedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	// Constructors
	public ProcessingMetricsEntity() {
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

	public Integer getMessageType() {
		return messageType;
	}

	public void setMessageType(Integer messageType) {
		this.messageType = messageType;
	}

	public String getClientAddress() {
		return clientAddress;
	}

	public void setClientAddress(String clientAddress) {
		this.clientAddress = clientAddress;
	}

	public Integer getPayloadLengthBytes() {
		return payloadLengthBytes;
	}

	public void setPayloadLengthBytes(Integer payloadLengthBytes) {
		this.payloadLengthBytes = payloadLengthBytes;
	}

	public Integer getDeclaredBodyLength() {
		return declaredBodyLength;
	}

	public void setDeclaredBodyLength(Integer declaredBodyLength) {
		this.declaredBodyLength = declaredBodyLength;
	}

	public Integer getMeasurementCount() {
		return measurementCount;
	}

	public void setMeasurementCount(Integer measurementCount) {
		this.measurementCount = measurementCount;
	}

	public Integer getPendingCommandsFound() {
		return pendingCommandsFound;
	}

	public void setPendingCommandsFound(Integer pendingCommandsFound) {
		this.pendingCommandsFound = pendingCommandsFound;
	}

	public Integer getCommandsSent() {
		return commandsSent;
	}

	public void setCommandsSent(Integer commandsSent) {
		this.commandsSent = commandsSent;
	}

	public Integer getResponseSizeBytes() {
		return responseSizeBytes;
	}

	public void setResponseSizeBytes(Integer responseSizeBytes) {
		this.responseSizeBytes = responseSizeBytes;
	}

	public Long getTotalProcessingTimeMs() {
		return totalProcessingTimeMs;
	}

	public void setTotalProcessingTimeMs(Long totalProcessingTimeMs) {
		this.totalProcessingTimeMs = totalProcessingTimeMs;
	}

	public Long getReadTimeMs() {
		return readTimeMs;
	}

	public void setReadTimeMs(Long readTimeMs) {
		this.readTimeMs = readTimeMs;
	}

	public Long getDecodeTimeMs() {
		return decodeTimeMs;
	}

	public void setDecodeTimeMs(Long decodeTimeMs) {
		this.decodeTimeMs = decodeTimeMs;
	}

	public Long getDbSaveTimeMs() {
		return dbSaveTimeMs;
	}

	public void setDbSaveTimeMs(Long dbSaveTimeMs) {
		this.dbSaveTimeMs = dbSaveTimeMs;
	}

	public Long getCommandQueryTimeMs() {
		return commandQueryTimeMs;
	}

	public void setCommandQueryTimeMs(Long commandQueryTimeMs) {
		this.commandQueryTimeMs = commandQueryTimeMs;
	}

	public Long getCommandEncodeTimeMs() {
		return commandEncodeTimeMs;
	}

	public void setCommandEncodeTimeMs(Long commandEncodeTimeMs) {
		this.commandEncodeTimeMs = commandEncodeTimeMs;
	}

	public Long getSendTimeMs() {
		return sendTimeMs;
	}

	public void setSendTimeMs(Long sendTimeMs) {
		this.sendTimeMs = sendTimeMs;
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

	public String getContactReason() {
		return contactReason;
	}

	public void setContactReason(String contactReason) {
		this.contactReason = contactReason;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public void setFirmwareVersion(String firmwareVersion) {
		this.firmwareVersion = firmwareVersion;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public LocalDateTime getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(LocalDateTime receivedAt) {
		this.receivedAt = receivedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	@Override
	public String toString() {
		return "ProcessingMetricsEntity{" +
				"id=" + id +
				", deviceId='" + deviceId + '\'' +
				", messageType=" + messageType +
				", totalMs=" + totalProcessingTimeMs +
				", success=" + success +
				'}';
	}
}
