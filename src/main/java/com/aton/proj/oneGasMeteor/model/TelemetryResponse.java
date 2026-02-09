package com.aton.proj.oneGasMeteor.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response model per endpoint telemetria
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelemetryResponse {

	private String status;
	private String deviceId;
	private String deviceType;
	private LocalDateTime receivedAt;
	private LocalDateTime processedAt;

	// Array di comandi (per backward compatibility e debug)
	private List<EncodedCommand> commands;

	// ✅ NUOVO: Comandi concatenati in formato TEK822
	private String concatenatedCommandsHex; // Formato HEX
	private String concatenatedCommandsAscii; // Formato ASCII leggibile

	private String message;

	public TelemetryResponse() {
		this.commands = new ArrayList<>();
	}

	public TelemetryResponse(String status, String message) {
		this.status = status;
		this.message = message;
		this.commands = new ArrayList<>();
	}

	// Factory methods
	public static TelemetryResponse success(String deviceId, String deviceType) {
		TelemetryResponse response = new TelemetryResponse();
		response.setStatus("OK");
		response.setDeviceId(deviceId);
		response.setDeviceType(deviceType);
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

	public String getConcatenatedCommandsHex() {
		return concatenatedCommandsHex;
	}

	public void setConcatenatedCommandsHex(String concatenatedCommandsHex) {
		this.concatenatedCommandsHex = concatenatedCommandsHex;
	}

	public String getConcatenatedCommandsAscii() {
		return concatenatedCommandsAscii;
	}

	public void setConcatenatedCommandsAscii(String concatenatedCommandsAscii) {
		this.concatenatedCommandsAscii = concatenatedCommandsAscii;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Nested class per comandi codificati
	 */
	public static class EncodedCommand {
		private Long commandId;
		private String commandType;
		private String encodedData;

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