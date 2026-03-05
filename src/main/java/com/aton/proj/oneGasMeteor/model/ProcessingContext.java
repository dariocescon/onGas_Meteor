package com.aton.proj.oneGasMeteor.model;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;

/**
 * Accumula metriche di performance durante l'elaborazione di un messaggio TCP.
 * Creato nel handler, attraversa il service, salvato su DB a fine elaborazione.
 */
public class ProcessingContext {

	private final LocalDateTime receivedAt;
	private final long startNanos;

	// Identification
	private String deviceId;
	private String deviceType;
	private int messageType = -1;
	private String clientAddress;

	// Payload info
	private int payloadLengthBytes;
	private int declaredBodyLength;
	private Integer measurementCount;

	// Command info
	private int pendingCommandsFound;
	private int commandsSent;
	private int responseSizeBytes;

	// Timing (nanos)
	private long readStartNanos;
	private long readTimeNanos;
	private long decodeStartNanos;
	private long decodeTimeNanos;
	private long dbSaveStartNanos;
	private long dbSaveTimeNanos;
	private long commandQueryStartNanos;
	private long commandQueryTimeNanos;
	private long commandEncodeStartNanos;
	private long commandEncodeTimeNanos;
	private long sendStartNanos;
	private long sendTimeNanos;

	// Device health snapshot
	private Double batteryVoltage;
	private Double batteryPercentage;
	private Integer signalStrength;
	private String contactReason;
	private String firmwareVersion;

	// Result
	private boolean success = true;
	private String errorMessage;
	private LocalDateTime completedAt;

	public ProcessingContext(String clientAddress) {
		this.receivedAt = LocalDateTime.now();
		this.startNanos = System.nanoTime();
		this.clientAddress = clientAddress;
	}

	// --- Timing methods ---

	public void startRead() {
		this.readStartNanos = System.nanoTime();
	}

	public void endRead() {
		this.readTimeNanos = System.nanoTime() - readStartNanos;
	}

	public void startDecode() {
		this.decodeStartNanos = System.nanoTime();
	}

	public void endDecode() {
		this.decodeTimeNanos = System.nanoTime() - decodeStartNanos;
	}

	public void startDbSave() {
		this.dbSaveStartNanos = System.nanoTime();
	}

	public void endDbSave() {
		this.dbSaveTimeNanos = System.nanoTime() - dbSaveStartNanos;
	}

	public void startCommandQuery() {
		this.commandQueryStartNanos = System.nanoTime();
	}

	public void endCommandQuery() {
		this.commandQueryTimeNanos = System.nanoTime() - commandQueryStartNanos;
	}

	public void startCommandEncode() {
		this.commandEncodeStartNanos = System.nanoTime();
	}

	public void endCommandEncode() {
		this.commandEncodeTimeNanos = System.nanoTime() - commandEncodeStartNanos;
	}

	public void startSend() {
		this.sendStartNanos = System.nanoTime();
	}

	public void endSend() {
		this.sendTimeNanos = System.nanoTime() - sendStartNanos;
	}

	/**
	 * Marca come completato (successo o errore) e calcola il tempo totale
	 */
	public void complete(boolean success, String errorMessage) {
		this.completedAt = LocalDateTime.now();
		this.success = success;
		this.errorMessage = errorMessage;
	}

	/**
	 * Tempo totale in millisecondi dal momento della creazione
	 */
	public long getTotalProcessingTimeMs() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
	}

	/**
	 * Popola i campi dal DecodedMessage dopo la decodifica
	 */
	public void extractFromDecoded(DecodedMessage decoded) {
		if (decoded == null) return;

		if (decoded.getUniqueIdentifier() != null) {
			this.deviceId = decoded.getUniqueIdentifier().getImei();
		}
		if (decoded.getUnitInfo() != null) {
			this.deviceType = decoded.getUnitInfo().getProductType();
			this.firmwareVersion = decoded.getUnitInfo().getFirmwareRevision();
		}
		if (decoded.getBatteryStatus() != null) {
			String voltageStr = decoded.getBatteryStatus().getBatteryVoltage();
			if (voltageStr != null) {
				try {
					this.batteryVoltage = Double.parseDouble(voltageStr);
				} catch (NumberFormatException ignored) {
				}
			}
			String percentStr = decoded.getBatteryStatus().getBatteryRemainingPercentage();
			if (percentStr != null) {
				try {
					this.batteryPercentage = Double.parseDouble(percentStr);
				} catch (NumberFormatException ignored) {
				}
			}
		}
		if (decoded.getSignalStrength() != null) {
			Integer csq = decoded.getSignalStrength().getCsq();
			Integer rssi = decoded.getSignalStrength().getRssi();
			this.signalStrength = csq != null ? csq : rssi;
		}
		if (decoded.getContactReason() != null) {
			this.contactReason = buildContactReasonString(decoded.getContactReason());
		}
		if (decoded.getMeasurementData() != null) {
			this.measurementCount = decoded.getMeasurementData().size();
		}
	}

	private String buildContactReasonString(DecodedMessage.ContactReason cr) {
		StringBuilder sb = new StringBuilder();
		if (Boolean.TRUE.equals(cr.getScheduled())) sb.append("SCHEDULED,");
		if (Boolean.TRUE.equals(cr.getAlarm())) sb.append("ALARM,");
		if (Boolean.TRUE.equals(cr.getServerRequest())) sb.append("SERVER_REQUEST,");
		if (Boolean.TRUE.equals(cr.getManual())) sb.append("MANUAL,");
		if (Boolean.TRUE.equals(cr.getReboot())) sb.append("REBOOT,");
		if (Boolean.TRUE.equals(cr.getTspRequested())) sb.append("TSP_REQUESTED,");
		if (Boolean.TRUE.equals(cr.getDynamic1())) sb.append("DYNAMIC1,");
		if (Boolean.TRUE.equals(cr.getDynamic2())) sb.append("DYNAMIC2,");
		if (sb.length() > 0) sb.setLength(sb.length() - 1); // rimuovi ultima virgola
		return sb.toString();
	}

	/**
	 * Converte il contesto in entity per persistenza
	 */
	public ProcessingMetricsEntity toEntity() {
		ProcessingMetricsEntity entity = new ProcessingMetricsEntity();
		entity.setDeviceId(deviceId);
		entity.setDeviceType(deviceType);
		entity.setMessageType(messageType);
		entity.setClientAddress(clientAddress);
		entity.setPayloadLengthBytes(payloadLengthBytes);
		entity.setDeclaredBodyLength(declaredBodyLength);
		entity.setMeasurementCount(measurementCount);
		entity.setPendingCommandsFound(pendingCommandsFound);
		entity.setCommandsSent(commandsSent);
		entity.setResponseSizeBytes(responseSizeBytes);
		entity.setTotalProcessingTimeMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
		entity.setReadTimeMs(TimeUnit.NANOSECONDS.toMillis(readTimeNanos));
		entity.setDecodeTimeMs(TimeUnit.NANOSECONDS.toMillis(decodeTimeNanos));
		entity.setDbSaveTimeMs(TimeUnit.NANOSECONDS.toMillis(dbSaveTimeNanos));
		entity.setCommandQueryTimeMs(TimeUnit.NANOSECONDS.toMillis(commandQueryTimeNanos));
		entity.setCommandEncodeTimeMs(TimeUnit.NANOSECONDS.toMillis(commandEncodeTimeNanos));
		entity.setSendTimeMs(TimeUnit.NANOSECONDS.toMillis(sendTimeNanos));
		entity.setBatteryVoltage(batteryVoltage);
		entity.setBatteryPercentage(batteryPercentage);
		entity.setSignalStrength(signalStrength);
		entity.setContactReason(contactReason);
		entity.setFirmwareVersion(firmwareVersion);
		entity.setSuccess(success);
		entity.setErrorMessage(errorMessage);
		entity.setReceivedAt(receivedAt);
		entity.setCompletedAt(completedAt != null ? completedAt : LocalDateTime.now());
		return entity;
	}

	// --- Getters and Setters ---

	public LocalDateTime getReceivedAt() {
		return receivedAt;
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

	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public String getClientAddress() {
		return clientAddress;
	}

	public int getPayloadLengthBytes() {
		return payloadLengthBytes;
	}

	public void setPayloadLengthBytes(int payloadLengthBytes) {
		this.payloadLengthBytes = payloadLengthBytes;
	}

	public int getDeclaredBodyLength() {
		return declaredBodyLength;
	}

	public void setDeclaredBodyLength(int declaredBodyLength) {
		this.declaredBodyLength = declaredBodyLength;
	}

	public Integer getMeasurementCount() {
		return measurementCount;
	}

	public void setMeasurementCount(Integer measurementCount) {
		this.measurementCount = measurementCount;
	}

	public int getPendingCommandsFound() {
		return pendingCommandsFound;
	}

	public void setPendingCommandsFound(int pendingCommandsFound) {
		this.pendingCommandsFound = pendingCommandsFound;
	}

	public int getCommandsSent() {
		return commandsSent;
	}

	public void setCommandsSent(int commandsSent) {
		this.commandsSent = commandsSent;
	}

	public int getResponseSizeBytes() {
		return responseSizeBytes;
	}

	public void setResponseSizeBytes(int responseSizeBytes) {
		this.responseSizeBytes = responseSizeBytes;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
