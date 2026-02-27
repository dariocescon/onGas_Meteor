package com.aton.proj.oneGasMeteor.dto;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO per la richiesta di creazione comando via API REST
 */
public class CommandCreateRequest {

	@NotBlank(message = "deviceId is required")
	private String deviceId;

	@NotBlank(message = "deviceType is required")
	private String deviceType;

	@NotBlank(message = "commandType is required")
	private String commandType;

	private Map<String, Object> parameters = new HashMap<>();

	public CommandCreateRequest() {
	}

	public CommandCreateRequest(String deviceId, String deviceType, String commandType) {
		this.deviceId = deviceId;
		this.deviceType = deviceType;
		this.commandType = commandType;
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

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}
}
