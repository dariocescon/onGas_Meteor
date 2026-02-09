package com.aton.proj.oneGasMeteor.controller.utils;

import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class per operazioni comuni nel controller
 */
public class ControllerUtils {

	private static final Logger log = LoggerFactory.getLogger(ControllerUtils.class);

	/**
	 * Concatena tutti i comandi in un'unica stringa ASCII separata da virgole, poi
	 * converte in byte array.
	 * 
	 * Formato: <password>,<cmd1>,<cmd2>,<cmd3>
	 * 
	 * Dal manuale TEK822 sezione 3.21: "Commands can be concatenated (separated by
	 * commas)"
	 * 
	 * Example: Input comandi: - Command1: "54454B3832322C53303D3830" → ASCII:
	 * "TEK822,S0=80" - Command2: "54454B3832322C53313D3031" → ASCII: "TEK822,S1=01"
	 * - Command3: "54454B3832322C52333D414354495645" → ASCII: "TEK822,R3=ACTIVE"
	 * 
	 * Output concatenato: ASCII: "TEK822,S0=80,S1=01,R3=ACTIVE" Bytes: [0x54, 0x45,
	 * 0x4B, 0x38, 0x32, 0x32, 0x2C, 0x53, 0x30, ...]
	 */
	public static byte[] concatenateCommands(List<TelemetryResponse.EncodedCommand> commands) {

		if (commands == null || commands.isEmpty()) {
			return new byte[0];
		}

		List<String> asciiCommands = new ArrayList<>();

		for (int i = 0; i < commands.size(); i++) {
			TelemetryResponse.EncodedCommand cmd = commands.get(i);

			// Converti HEX → ASCII
			String asciiCmd = hexToAscii(cmd.getEncodedData());

			log.debug("   Command {}: {} → ASCII: {}", cmd.getCommandId(), cmd.getCommandType(), asciiCmd);

			if (i == 0) {
				// Primo comando: mantieni password
				asciiCommands.add(asciiCmd);
			} else {
				// Comandi successivi: rimuovi password (evita duplicati)
				String cmdWithoutPassword = removePassword(asciiCmd);
				asciiCommands.add(cmdWithoutPassword);
			}
		}

		// Concatena con virgola
		String concatenated = String.join(",", asciiCommands);

		log.debug("   Concatenated ASCII: {}", concatenated);

		// Converti ASCII → byte array
		byte[] result = concatenated.getBytes(StandardCharsets.US_ASCII);

		log.debug("   Total binary payload: {} bytes ({} commands)", result.length, commands.size());

		return result;
	}

	/**
	 * Rimuove la password da un comando
	 * 
	 * Input: "TEK822,S0=80" Output: "S0=80"
	 * 
	 * Input: "TEK822,R3=ACTIVE" Output: "R3=ACTIVE"
	 */
	private static String removePassword(String asciiCommand) {
		// Trova la prima virgola
		int commaIndex = asciiCommand.indexOf(',');

		if (commaIndex != -1 && commaIndex < asciiCommand.length() - 1) {
			// Ritorna tutto dopo la virgola
			return asciiCommand.substring(commaIndex + 1);
		}

		// Nessuna virgola trovata, ritorna il comando così com'è
		return asciiCommand;
	}

	/**
	 * Converte hex string in ASCII string
	 * 
	 * Example: "54454B383232" → "TEK822"
	 */
	public static String hexToAscii(String hexString) {
		if (hexString == null || hexString.isEmpty()) {
			return "";
		}

		StringBuilder ascii = new StringBuilder();
		for (int i = 0; i < hexString.length(); i += 2) {
			String hex = hexString.substring(i, Math.min(i + 2, hexString.length()));
			int decimal = Integer.parseInt(hex, 16);
			ascii.append((char) decimal);
		}

		return ascii.toString();
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

	/**
	 * Converte byte array in hex string (uppercase)
	 * 
	 * Example: [0x08, 0x01, 0x81] → "080181"
	 */
	public static String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			hexString.append(String.format("%02X", b));
		}
		return hexString.toString();
	}

	/**
	 * Converte comandi concatenati in string per risposta JSON/text
	 * 
	 * Questo metodo mantiene il formato hex per compatibilità, ma puoi usarlo anche
	 * per convertire in ASCII leggibile.
	 */
	public static String commandsToHexString(List<TelemetryResponse.EncodedCommand> commands) {
		byte[] concatenated = concatenateCommands(commands);
		return bytesToHex(concatenated);
	}

	/**
	 * Converte comandi concatenati in string ASCII per risposta text/plain
	 */
	public static String commandsToAsciiString(List<TelemetryResponse.EncodedCommand> commands) {
		byte[] concatenated = concatenateCommands(commands);
		return new String(concatenated, StandardCharsets.US_ASCII);
	}
}