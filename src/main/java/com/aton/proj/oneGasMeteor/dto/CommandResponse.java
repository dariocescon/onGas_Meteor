package com.aton.proj.oneGasMeteor.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * DTO per la risposta API dei comandi
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandResponse {

	private Long id;
	private String deviceId;
	private String deviceType;
	private String commandType;
	private Map<String, Object> parameters;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime sentAt;
	private String errorMessage;

	public CommandResponse() {
	}

	/**
	 * Converte una CommandEntity in CommandResponse
	 */
	@SuppressWarnings("unchecked")
	public static CommandResponse fromEntity(CommandEntity entity, ObjectMapper objectMapper) {
		CommandResponse response = new CommandResponse();
		response.setId(entity.getId());
		response.setDeviceId(entity.getDeviceId());
		response.setDeviceType(entity.getDeviceType());
		response.setCommandType(entity.getCommandType());
		response.setStatus(entity.getStatus().name());
		response.setCreatedAt(entity.getCreatedAt());
		response.setSentAt(entity.getSentAt());
		response.setErrorMessage(entity.getErrorMessage());

		// Deserializza parametri JSON
		if (entity.getCommandParamsJson() != null && !entity.getCommandParamsJson().isEmpty()) {
			try {
				response.setParameters(objectMapper.readValue(entity.getCommandParamsJson(), Map.class));
			} catch (Exception e) {
				response.setParameters(Collections.emptyMap());
			}
		}

		return response;
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

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
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

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
