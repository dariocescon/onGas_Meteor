package com.aton.proj.oneGasMeteor.model.tcp;

import java.time.LocalDateTime;

public record TelemetryMessage(byte[] rawData, String hexData, LocalDateTime receivedAt, String sourceAddress) {
	public TelemetryMessage(byte[] rawData, String sourceAddress) {
		this(rawData, bytesToHex(rawData), LocalDateTime.now(), sourceAddress);
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
}
