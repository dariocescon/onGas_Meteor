package com.aton.proj.oneGasMeteor.model;

public record TekMessage(byte[] payload, long timestampInMs) {
	public static TekMessage fromHexString(String hexString, long timestampInMs) {
		
		byte[] payload = new byte[hexString.length() / 2];
		for (int i = 0; i < payload.length; i++) {
			payload[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
		}
		return new TekMessage(payload, timestampInMs);
		
	}
}
