package com.aton.proj.oneGasMeteor.controller.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aton.proj.oneGasMeteor.model.TelemetryResponse;

public class ControllerUtils {

	private static final Logger log = LoggerFactory.getLogger(ControllerUtils.class);

	/**
	 * Concatena tutti i comandi in un'unica stringa ASCII separata da virgole Poi
	 * converte in byte array
	 * 
	 * Format: <password>,<cmd1>,<cmd2>,<cmd3>
	 * 
	 * Example: Command1 = SET_INTERVAL (S0=80) Command2 = REBOOT (R3=ACTIVE)
	 * 
	 * Result ASCII: TEK822,S0=80,R3=ACTIVE Result Hex:
	 * 54454B3832322C53303D38302C52333D414354495645 Result Bytes: [0x54, 0x45, 0x4B,
	 * 0x38, 0x32, 0x32, 0x2C, ...]
	 */
	public static byte[] concatenateCommands(List<TelemetryResponse.EncodedCommand> commands) {

		if (commands == null || commands.isEmpty()) {
			return new byte[0];
		}

		// Tutti i comandi sono già in formato HEX con password inclusa
		// Esempio: "54454B3832322C53303D3830" = "TEK822,S0=80"

		// Dobbiamo:
		// 1. Convertire ogni comando da HEX ad ASCII
		// 2. Rimuovere il password duplicato (tutti tranne il primo)
		// 3. Concatenare con virgola
		// 4. Riconvertire in HEX/byte array

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
				// Comandi successivi: rimuovi password
				// "TEK822,S0=80" → "S0=80"
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
	 * Input:  "TEK822,S0=80"
	 * Output: "S0=80"
	 * 
	 * Input:  "TEK822,R3=ACTIVE"
	 * Output: "R3=ACTIVE"
	 */
	public static String removePassword(String asciiCommand) {
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
	 * Converte hex string in ASCII string
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

}
