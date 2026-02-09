package com.aton.proj.oneGasMeteor.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Response model per Message Type 6 (Device Settings)
 */
public class MessageType6Response {

	private String deviceId;
	private String deviceType;
	private LocalDateTime receivedAt;
	private Map<String, String> settings;

	public MessageType6Response() {
		this.settings = new HashMap<>();
		this.receivedAt = LocalDateTime.now();
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

	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public void addSetting(String key, String value) {
		this.settings.put(key, value);
	}

	public String getSetting(String key) {
		return this.settings.get(key);
	}

	@Override
	public String toString() {
		return "MessageType6Response{" + "deviceId='" + deviceId + '\'' + ", deviceType='" + deviceType + '\''
				+ ", receivedAt=" + receivedAt + ", settingsCount=" + settings.size() + '}';
	}
}