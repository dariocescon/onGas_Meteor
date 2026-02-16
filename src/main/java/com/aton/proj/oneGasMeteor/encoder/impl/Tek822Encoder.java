package com.aton.proj.oneGasMeteor.encoder.impl;

import com.aton.proj.oneGasMeteor.encoder.DeviceEncoder;
import com.aton.proj.oneGasMeteor.exception.EncodingException;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encoder per comandi destinati a dispositivi Tekelek TEK822 e compatibili
 * Basato su: TEK 822 Logger NB-IoT/CAT-M1 User Manual (9-5988-07)
 */
@Component
@Order(1)
public class Tek822Encoder implements DeviceEncoder {

	private static final Logger log = LoggerFactory.getLogger(Tek822Encoder.class);

	// Device types supportati
	private static final List<String> SUPPORTED_DEVICES = Arrays.asList("TEK586", "TEK733", "TEK643", "TEK811",
			"TEK822V1", "TEK733A", "TEK871", "TEK811A", "TEK822V1BTN", "TEK822V2", "TEK900", "TEK880", "TEK898V2",
			"TEK898V1");

	// Command types (sezione 3.20 e 3.21 del manuale)
	public static final String CMD_SET_INTERVAL = "SET_INTERVAL"; // S0
	public static final String CMD_SET_LISTEN = "SET_LISTEN"; // S1
	public static final String CMD_SET_SCHEDULE = "SET_SCHEDULE"; // S2
	public static final String CMD_REBOOT = "REBOOT"; // R3=ACTIVE
	public static final String CMD_REQUEST_STATUS = "REQUEST_STATUS"; // R6=02
	public static final String CMD_SET_ALARM_THRESHOLD = "SET_ALARM_THRESHOLD"; // S4/S5/S6
	public static final String CMD_SHUTDOWN = "SHUTDOWN"; // R1=80
	public static final String CMD_SET_RTC = "SET_RTC"; // R2
	public static final String CMD_DEACTIVATE = "DEACTIVATE"; // R4=DEACT
	public static final String CMD_CLOSE_TCP = "CLOSE_TCP"; // R6=03
	public static final String CMD_REQUEST_GPS = "REQUEST_GPS"; // R7
	public static final String CMD_REQUEST_SETTINGS = "REQUEST_SETTINGS"; // R1=02/04/08
	public static final String CMD_SET_APN = "SET_APN"; // S12/S13/S14
	public static final String CMD_SET_SERVER = "SET_SERVER"; // S15/S16

	// Default password (sezione 3.9 del manuale)
	private static final String DEFAULT_PASSWORD = "TEK822";

	@Override
	public boolean canEncode(String deviceType) {
		return deviceType != null && SUPPORTED_DEVICES.contains(deviceType);
	}

	@Override
	public List<TelemetryResponse.EncodedCommand> encode(List<DeviceCommand> commands) {
		List<TelemetryResponse.EncodedCommand> encodedCommands = new ArrayList<>();

		for (DeviceCommand command : commands) {
			try {
				encodeCommand(command);

				TelemetryResponse.EncodedCommand encoded = new TelemetryResponse.EncodedCommand();
				encoded.setCommandId(command.getId());
				encoded.setCommandType(command.getCommandType());
				encoded.setEncodedData(command.getEncodedCommandASCII());

				encodedCommands.add(encoded);

				log.debug("  Encoded {}: {}", command.getCommandType(), command.getEncodedCommandASCII());

			} catch (Exception e) {
				log.error("  Failed to encode command: {}", command.getCommandType(), e);
				throw new EncodingException("Failed to encode command: " + command.getCommandType(), e);
			}
		}

		return encodedCommands;
	}

	/**
	 * Codifica un singolo comando
	 */
	private DeviceCommand encodeCommand(DeviceCommand command) {
		log.debug("  Encoding command: {} for device: {}", command.getCommandType(), command.getDeviceId());

		// Ottieni la password (usa default se non specificata)
		String password = command.getParameters().getOrDefault("password", DEFAULT_PASSWORD).toString();

		String asciiCommand = switch (command.getCommandType()) {
		case CMD_SET_INTERVAL -> encodeSetInterval(password, command);
		case CMD_SET_LISTEN -> encodeSetListen(password, command);
		case CMD_SET_SCHEDULE -> encodeSetSchedule(password, command);
		case CMD_REBOOT -> encodeReboot(password);
		case CMD_REQUEST_STATUS -> encodeRequestStatus(password);
		case CMD_SET_ALARM_THRESHOLD -> encodeSetAlarmThreshold(password, command);
		case CMD_SHUTDOWN -> encodeShutdown(password);
		case CMD_SET_RTC -> encodeSetRTC(password, command);
		case CMD_DEACTIVATE -> encodeDeactivate(password);
		case CMD_CLOSE_TCP -> encodeCloseTCP(password);
		case CMD_REQUEST_GPS -> encodeRequestGPS(password, command);
		case CMD_REQUEST_SETTINGS -> encodeRequestSettings(password, command);
		case CMD_SET_APN -> encodeSetAPN(password, command);
		case CMD_SET_SERVER -> encodeSetServer(password, command);
		default -> throw new EncodingException("Unknown command type: " + command.getCommandType());
		};

		// Converti ASCII in HEX (sezione 3.21: "converted to hex")
		String hexCommand = asciiToHex(asciiCommand);
		
		command.setEncodedCommandASCII(asciiCommand);
		command.setEncodedCommandHEX(hexCommand);

		log.debug("   ASCII: {}", asciiCommand);
		log.debug("   HEX: {}", hexCommand);

		return command;
	}

	/**
	 * S0: Logger Configuration (sezione 3.20.1) Example: TEK822,S0=80
	 */
	private String encodeSetInterval(String password, DeviceCommand command) {
		int interval = Integer.parseInt(command.getParameters().get("interval").toString());

		// Formula dal PDF: S0=(128 x B) + (A x 4)
		// A = logger speed in hours (0.25 increments)
		// B = sampling period (0=1min, 1=15min)

		int samplingPeriod = 1; // Default: 15 minutes
		int loggerSpeed = interval / 15; // Converti minuti in multipli di 15

		int s0Value = (128 * samplingPeriod) + (loggerSpeed * 4);

		return String.format("%s,S0=%02X", password, s0Value);
	}

	/**
	 * S1: Listen Configuration (sezione 3.20.2) Example: TEK822,S1=01
	 */
	private String encodeSetListen(String password, DeviceCommand command) {
		int listenMinutes = Integer.parseInt(command.getParameters().get("listenMinutes").toString());

		// Formula: S1 = listenMinutes / 5
		int s1Value = listenMinutes / 5;

		return String.format("%s,S1=%02X", password, s1Value);
	}

	/**
	 * S2: Schedule Configuration (sezione 3.20.3) Example: TEK822,S2=7F2056
	 */
	private String encodeSetSchedule(String password, DeviceCommand command) {
		// Implementazione complessa - per ora placeholder
		String scheduleValue = command.getParameters().getOrDefault("schedule", "7F0038").toString();
		return String.format("%s,S2=%s", password, scheduleValue);
	}

	/**
	 * R3=ACTIVE: Reboot/Activate (sezione 3.21) Example: TEK822,R3=ACTIVE
	 */
	private String encodeReboot(String password) {
		return String.format("%s,R3=ACTIVE", password);
	}

	/**
	 * R6=02: Request Status (Message Type 16) (sezione 3.21) Example: TEK822,R6=02
	 */
	private String encodeRequestStatus(String password) {
		return String.format("%s,R6=02", password);
	}

	/**
	 * S4/S5/S6: Static Alarm Configuration (sezione 3.20.5) Example: TEK822,S4=E896
	 */
	private String encodeSetAlarmThreshold(String password, DeviceCommand command) {
		int threshold = Integer.parseInt(command.getParameters().get("threshold").toString());
		int hysteresis = Integer.parseInt(command.getParameters().getOrDefault("hysteresis", "10").toString());
		boolean enabled = Boolean.parseBoolean(command.getParameters().getOrDefault("enabled", "true").toString());
		boolean polarity = Boolean.parseBoolean(command.getParameters().getOrDefault("polarity", "true").toString());

		String alarmReg = command.getParameters().getOrDefault("alarmRegister", "S4").toString();

		// Formula: S4 = D + C x (2^10) + B x (2^14) + A x (2^15)
		int s4Value = threshold + (hysteresis * (1 << 10)) + ((enabled ? 1 : 0) * (1 << 14))
				+ ((polarity ? 1 : 0) * (1 << 15));

		return String.format("%s,%s=%04X", password, alarmReg, s4Value);
	}

	/**
	 * R1=80: Shutdown modem and sleep (sezione 3.21) Example: TEK822,R1=80
	 */
	private String encodeShutdown(String password) {
		return String.format("%s,R1=80", password);
	}

	/**
	 * R2: Set RTC (sezione 3.21) Example: TEK822,R2=26/02/07:14/30/00
	 */
	private String encodeSetRTC(String password, DeviceCommand command) {
		LocalDateTime dateTime = LocalDateTime.parse(command.getParameters().get("datetime").toString());

		// Format: yy/mm/dd:hh/mm/ss
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd:HH/mm/ss");
		String formattedTime = dateTime.format(formatter);

		return String.format("%s,R2=%s", password, formattedTime);
	}

	/**
	 * R4=DEACT: Deactivate scheduled uploads (sezione 3.21) Example:
	 * TEK822,R4=DEACT
	 */
	private String encodeDeactivate(String password) {
		return String.format("%s,R4=DEACT", password);
	}

	/**
	 * R6=03: Close TCP connection (sezione 3.21) Example: TEK822,R6=03
	 */
	private String encodeCloseTCP(String password) {
		return String.format("%s,R6=03", password);
	}

	/**
	 * R7=FF: Request GPS (Message Type 17) (sezione 3.21) Example: TEK822,R7=3C
	 * (timeout 60 seconds in hex)
	 */
	private String encodeRequestGPS(String password, DeviceCommand command) {
		int timeout = Integer.parseInt(command.getParameters().getOrDefault("timeout", "60").toString());
		return String.format("%s,R7=%02X", password, timeout);
	}

	/**
	 * R1=02/04/08: Request Settings (Message Type 6) (sezione 3.21) Example:
	 * TEK822,R1=02 (settings from S0)
	 */
	private String encodeRequestSettings(String password, DeviceCommand command) {
		String startFrom = command.getParameters().getOrDefault("startFrom", "S0").toString();

		String r1Value = switch (startFrom) {
		case "S0" -> "02";
		case "S12" -> "04";
		case "S19" -> "08";
		default -> "02";
		};

		return String.format("%s,R1=%s", password, r1Value);
	}

	/**
	 * S12/S13/S14: Set APN (sezione 3.20.7) Example:
	 * TEK822,S12=internet,S13=user,S14=pass
	 */
	private String encodeSetAPN(String password, DeviceCommand command) {
		String apn = command.getParameters().get("apn").toString();
		String username = command.getParameters().getOrDefault("username", "").toString();
		String pass = command.getParameters().getOrDefault("password", "").toString();

		return String.format("%s,S12=%s,S13=%s,S14=%s", password, apn, username, pass);
	}

	/**
	 * S15/S16: Set Server (sezione 3.20.7) Example:
	 * TEK822,S15=84.51.250.104,S16=9000
	 */
	private String encodeSetServer(String password, DeviceCommand command) {
		String serverIp = command.getParameters().get("serverIp").toString();
		String serverPort = command.getParameters().get("serverPort").toString();

		return String.format("%s,S15=%s,S16=%s", password, serverIp, serverPort);
	}

	/**
	 * Converte stringa ASCII in HEX string (sezione 3.21: "Commands can be sent via
	 * SMS or via GPRS (converted to hex)")
	 */
	private String asciiToHex(String ascii) {
		byte[] bytes = ascii.getBytes(StandardCharsets.US_ASCII);
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02X", b));
		}
		return hex.toString();
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