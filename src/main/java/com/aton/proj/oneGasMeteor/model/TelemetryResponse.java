package com.aton.proj.oneGasMeteor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Risposta HTTP al device dopo elaborazione
 */
public class TelemetryResponse {

	private String status;
	private String deviceId;
	private String deviceType;
	private LocalDateTime receivedAt;
	private LocalDateTime processedAt;
	private List<EncodedCommand> commands = new ArrayList<>();
	private String message;

	public TelemetryResponse() {
	}

	public TelemetryResponse(String status, String message) {
		this.status = status;
		this.message = message;
		this.processedAt = LocalDateTime.now();
	}

	public static TelemetryResponse success(String deviceId, String deviceType) {
		TelemetryResponse response = new TelemetryResponse();
		response.setStatus("OK");
		response.setDeviceId(deviceId);
		response.setDeviceType(deviceType);
		response.setProcessedAt(LocalDateTime.now());
		return response;
	}

	public static TelemetryResponse error(String message) {
		return new TelemetryResponse("ERROR", message);
	}

	// Getters & Setters
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public List<EncodedCommand> getCommands() {
		return commands;
	}

	public void setCommands(List<EncodedCommand> commands) {
		this.commands = commands;
	}

	public void addCommand(EncodedCommand command) {
		this.commands.add(command);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Comando codificato da inviare al device
	 */
	public static class EncodedCommand {
		private Long commandId;
		private String commandType;
		private String encodedData; // Hex string

		public EncodedCommand() {
		}

		public EncodedCommand(Long commandId, String commandType, String encodedData) {
			this.commandId = commandId;
			this.commandType = commandType;
			this.encodedData = encodedData;
		}

		public Long getCommandId() {
			return commandId;
		}

		public void setCommandId(Long commandId) {
			this.commandId = commandId;
		}

		public String getCommandType() {
			return commandType;
		}

		public void setCommandType(String commandType) {
			this.commandType = commandType;
		}

		public String getEncodedData() {
			return encodedData;
		}

		public void setEncodedData(String encodedData) {
			this.encodedData = encodedData;
		}
	}
}