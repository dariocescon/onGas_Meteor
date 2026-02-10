package com.aton.proj.oneGasMeteor.model.tcp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public class TelemetryMessage {

	private final byte[] payload;
	private final String hexData;
	private final LocalDateTime receivedAt;
	private final String sourceAddress;
	private final long serverTimeInMs;

	public TelemetryMessage(byte[] payload, String hexData, LocalDateTime receivedAt, String sourceAddress) {
		this.payload = payload;
		this.hexData = hexData;
		this.receivedAt = receivedAt;
		this.sourceAddress = sourceAddress;
		this.serverTimeInMs = System.currentTimeMillis();
	}

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

	public byte[] getPayload() {
		return payload;
	}

	public String getHexData() {
		return hexData;
	}

	public LocalDateTime getReceivedAt() {
		return receivedAt;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public long getServerTimeInMs() {
		return serverTimeInMs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(payload);
		result = prime * result + Objects.hash(hexData, receivedAt, serverTimeInMs, sourceAddress);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TelemetryMessage other = (TelemetryMessage) obj;
		return Objects.equals(hexData, other.hexData) && Arrays.equals(payload, other.payload)
				&& Objects.equals(receivedAt, other.receivedAt) && serverTimeInMs == other.serverTimeInMs
				&& Objects.equals(sourceAddress, other.sourceAddress);
	}

	@Override
	public String toString() {
		return "TelemetryMessage [payload=" + Arrays.toString(payload) + ", hexData=" + hexData + ", receivedAt="
				+ receivedAt + ", sourceAddress=" + sourceAddress + ", serverTimeInMs=" + serverTimeInMs + "]";
	}

}