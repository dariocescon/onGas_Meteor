package com.aton.proj.oneGasMeteor.controller.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aton.proj.oneGasMeteor.model.TelemetryResponse;

public class ControllerUtils {

	private static final Logger log = LoggerFactory.getLogger(ControllerUtils.class);
	
	/**
	 * Converte byte array in hex string
	 */
	public static String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			hexString.append(String.format("%02X", b));
		}
		return hexString.toString();
	}

	/**
	 * Concatena tutti i comandi in un unico byte array
	 * 
	 * Format: [length1][command1_bytes][length2][command2_bytes]...
	 * 
	 * Ogni comando è preceduto da 2 bytes che indicano la lunghezza: - Byte 0: MSB
	 * (Most Significant Byte) - Byte 1: LSB (Least Significant Byte)
	 * 
	 * Example: Command1 = "54454B3832322C53303D3830" (12 bytes) Command2 =
	 * "54454B3832322C52333D414354495645" (18 bytes)
	 * 
	 * Result: [0x00][0x0C][...12 bytes of cmd1...][0x00][0x12][...18 bytes of
	 * cmd2...]
	 */
	public static byte[] concatenateCommands(List<TelemetryResponse.EncodedCommand> commands) {

		List<byte[]> commandBytesList = new ArrayList<>();
		int totalLength = 0;

		// Converti ogni comando hex → byte array
		for (TelemetryResponse.EncodedCommand cmd : commands) {
			byte[] cmdBytes = hexStringToByteArray(cmd.getEncodedData());

			// 2 bytes per la lunghezza + comando
			totalLength += 2 + cmdBytes.length;
			commandBytesList.add(cmdBytes);

			log.debug("   Command {}: {} → {} bytes", cmd.getCommandId(), cmd.getCommandType(), cmdBytes.length);
		}

		// Crea il buffer finale
		byte[] result = new byte[totalLength];
		int offset = 0;

		// Scrivi ogni comando con il suo length prefix
		for (byte[] cmdBytes : commandBytesList) {
			int length = cmdBytes.length;

			// Length prefix (2 bytes, big-endian)
			result[offset++] = (byte) ((length >> 8) & 0xFF); // MSB
			result[offset++] = (byte) (length & 0xFF); // LSB

			// Command bytes
			System.arraycopy(cmdBytes, 0, result, offset, cmdBytes.length);
			offset += cmdBytes.length;
		}

		log.debug("   Total binary payload: {} bytes ({} commands)", totalLength, commands.size());

		return result;
	}

	/**
	 * Converte hex string in byte array
	 * 
	 * Example: "080181" → [0x08, 0x01, 0x81]
	 */
	public static byte[] hexStringToByteArray(String hexString) {
		if (hexString == null || hexString.isEmpty()) {
			return new byte[0];
		}

		// Rimuovi spazi e caratteri non validi
		hexString = hexString.replaceAll("[^0-9A-Fa-f]", "");

		if (hexString.length() % 2 != 0) {
			throw new IllegalArgumentException("Invalid hex string length: " + hexString.length());
		}

		int len = hexString.length();
		byte[] data = new byte[len / 2];

		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
					+ Character.digit(hexString.charAt(i + 1), 16));
		}

		return data;
	}

}
