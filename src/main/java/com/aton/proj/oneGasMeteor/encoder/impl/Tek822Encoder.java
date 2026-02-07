package com.aton.proj.oneGasMeteor.encoder.impl;

import com.aton.proj.oneGasMeteor.encoder.DeviceEncoder;
import com.aton.proj.oneGasMeteor.exception.EncodingException;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encoder per comandi destinati a dispositivi Tekelek TEK822 e compatibili
 * 
 * Comandi supportati: - SET_INTERVAL: Imposta intervallo di logging - REBOOT:
 * Riavvia il dispositivo - REQUEST_STATUS: Richiede stato immediato -
 * SET_ALARM_THRESHOLD: Imposta soglie allarme
 */
@Component
public class Tek822Encoder implements DeviceEncoder {

	private static final Logger log = LoggerFactory.getLogger(Tek822Encoder.class);

	private static final List<String> SUPPORTED_DEVICES = Arrays.asList("TEK586", "TEK733", "TEK643", "TEK811",
			"TEK822V1", "TEK733A", "TEK871", "TEK811A", "TEK822V1BTN", "TEK822V2", "TEK900", "TEK880", "TEK898V2",
			"TEK898V1");

	@Override
	public boolean canEncode(String deviceType) {
		return deviceType != null && SUPPORTED_DEVICES.contains(deviceType);
	}

	@Override
	public List<TelemetryResponse.EncodedCommand> encode(List<DeviceCommand> commands) {
		List<TelemetryResponse.EncodedCommand> encodedCommands = new ArrayList<>();

		for (DeviceCommand command : commands) {
//			try {
				TelemetryResponse.EncodedCommand encoded = encodeCommand(command);
				if (encoded != null) {
					encodedCommands.add(encoded);
				}
//			} catch (Exception e) {
//				log.error("❌ Failed to encode command: {}", command.getCommandType(), e);
//				// Continua con gli altri comandi invece di fallire completamente
//			}
		}

		return encodedCommands;
	}

	/**
	 * Codifica un singolo comando
	 */
	private TelemetryResponse.EncodedCommand encodeCommand(DeviceCommand command) {

		String commandType = command.getCommandType();
		log.debug("🔧 Encoding command: {} for device: {}", commandType, command.getDeviceId());

		String hexData = switch (commandType) {
		case "SET_INTERVAL" -> encodeSetInterval(command);
		case "REBOOT" -> encodeReboot(command);
		case "REQUEST_STATUS" -> encodeRequestStatus(command);
		case "SET_ALARM_THRESHOLD" -> encodeSetAlarmThreshold(command);
		default -> {
			log.warn("⚠️  Unknown command type: {}", commandType);
			yield null;
		}
		};

		if (hexData != null) {
			return new TelemetryResponse.EncodedCommand(command.getId(), commandType, hexData);
		}

		return null;
	}

	/**
	 * SET_INTERVAL: Imposta l'intervallo di logging
	 * 
	 * Parametri: - interval: minuti tra le letture (1-255)
	 * 
	 * Formato messaggio TEK822: Byte 0: 0x53 ('S' - Set) Byte 1: 0x49 ('I' -
	 * Interval) Byte 2: interval in minuti Byte 3: checksum (XOR dei byte
	 * precedenti)
	 */
	private String encodeSetInterval(DeviceCommand command) {
		try {
			Integer interval = (Integer) command.getParameter("interval");
			if (interval == null || interval < 1 || interval > 255) {
				throw new EncodingException("Invalid interval: " + interval + " (must be 1-255)");
			}

			byte[] message = new byte[4];
			message[0] = 0x53; // 'S'
			message[1] = 0x49; // 'I'
			message[2] = interval.byteValue();
			message[3] = calculateChecksum(message, 3);

			String hexData = bytesToHex(message);
			log.debug("✅ Encoded SET_INTERVAL({}): {}", interval, hexData);
			return hexData;

		} catch (Exception e) {
			throw new EncodingException("Failed to encode SET_INTERVAL", e);
		}
	}

	/**
	 * REBOOT: Riavvia il dispositivo
	 * 
	 * Formato messaggio TEK822: Byte 0: 0x52 ('R' - Reboot) Byte 1: 0x42 ('B' -
	 * Boot) Byte 2: 0x00 (reserved) Byte 3: checksum
	 */
	private String encodeReboot(DeviceCommand command) {
	    try {
	        byte[] message = new byte[4];
	        message[0] = 0x52; // 'R'
	        message[1] = 0x42; // 'B'
	        message[2] = 0x00;
	        message[3] = calculateChecksum(message, 3);
	        
	        String hexData = bytesToHex(message);
	        log.debug("✅ Encoded REBOOT: {}", hexData);
	        return hexData;
	        
	    } catch (Exception e) {
	        throw new EncodingException("Failed to encode REBOOT", e);
	    }
	}

	/**
	 * REQUEST_STATUS: Richiede invio immediato dello stato
	 * 
	 * Formato messaggio TEK822: Byte 0: 0x51 ('Q' - Query) Byte 1: 0x53 ('S' -
	 * Status) Byte 2: 0x00 (reserved) Byte 3: checksum
	 */
	private String encodeRequestStatus(DeviceCommand command) {
		try {
			byte[] message = new byte[4];
			message[0] = 0x51; // 'Q'
			message[1] = 0x53; // 'S'
			message[2] = 0x00;
			message[3] = calculateChecksum(message, 3);

			String hexData = bytesToHex(message);
			log.debug("✅ Encoded REQUEST_STATUS: {}", hexData);
			return hexData;

		} catch (Exception e) {
			throw new EncodingException("Failed to encode REQUEST_STATUS", e);
		}
	}

	/**
	 * SET_ALARM_THRESHOLD: Imposta soglia per allarme distanza
	 * 
	 * Parametri: - threshold: distanza in cm (0-1023)
	 * 
	 * Formato messaggio TEK822: Byte 0: 0x41 ('A' - Alarm) Byte 1: 0x54 ('T' -
	 * Threshold) Byte 2: threshold high byte Byte 3: threshold low byte Byte 4:
	 * checksum
	 */
	private String encodeSetAlarmThreshold(DeviceCommand command) {
		try {
			Integer threshold = (Integer) command.getParameter("threshold");
			if (threshold == null || threshold < 0 || threshold > 1023) {
				throw new EncodingException("Invalid threshold: " + threshold + " (must be 0-1023)");
			}

			byte[] message = new byte[5];
			message[0] = 0x41; // 'A'
			message[1] = 0x54; // 'T'
			message[2] = (byte) ((threshold >> 8) & 0xFF); // High byte
			message[3] = (byte) (threshold & 0xFF); // Low byte
			message[4] = calculateChecksum(message, 4);

			String hexData = bytesToHex(message);
			log.debug("✅ Encoded SET_ALARM_THRESHOLD({}): {}", threshold, hexData);
			return hexData;

		} catch (Exception e) {
			throw new EncodingException("Failed to encode SET_ALARM_THRESHOLD", e);
		}
	}

	/**
	 * Calcola checksum XOR
	 */
	private byte calculateChecksum(byte[] data, int length) {
		byte checksum = 0;
		for (int i = 0; i < length; i++) {
			checksum ^= data[i];
		}
		return checksum;
	}

	/**
	 * Converte byte array in hex string
	 */
	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}

	@Override
	public List<String> getSupportedDeviceTypes() {
		return SUPPORTED_DEVICES;
	}

	@Override
	public String getEncoderName() {
		return "Tek822Encoder";
	}
}