package com.aton.proj.oneGasMeteor.decoder;

import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.aton.proj.oneGasMeteor.model.MessageType16Response;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;
import com.aton.proj.oneGasMeteor.model.MessageType6Response;

/**
 * Parser per Message Type 6, 16 e 17 Questi messaggi contengono dati ASCII
 * hex-encoded
 */
@Component
public class MessageTypeParser {

	private static final Logger log = LoggerFactory.getLogger(MessageTypeParser.class);

	/**
	 * Converte hex string in ASCII string
	 */
	private String hexToAscii(String hexString) {
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
	 * Parse Message Type 6 (Settings) Input hex:
	 * 53303D38302C53313D30352C53323D3746303033382C... Output:
	 * S0=80,S1=05,S2=7F0038,...
	 */
	public MessageType6Response parseMessageType6(String hexPayload, String deviceId, String deviceType) {
		log.debug("🔍 Parsing Message Type 6 for device: {}", deviceId);

		MessageType6Response response = new MessageType6Response();
		response.setDeviceId(deviceId);
		response.setDeviceType(deviceType);

		try {
			// Converti hex in ASCII
			String asciiData = hexToAscii(hexPayload);
			log.debug("   ASCII data: {}", asciiData);

			// Parse settings (formato: S0=80,S1=05,S2=7F0038,...)
			String[] settings = asciiData.split(",");

			for (String setting : settings) {
				if (setting.contains("=")) {
					String[] parts = setting.split("=", 2);
					String key = parts[0].trim();
					String value = parts.length > 1 ? parts[1].trim() : "";
					response.addSetting(key, value);
				}
			}

			log.info("✅ Parsed {} settings from Message Type 6", response.getSettings().size());

		} catch (Exception e) {
			log.error("❌ Failed to parse Message Type 6", e);
		}

		return response;
	}

	/**
	 * Parse Message Type 16 (ICCID & Statistics) Input hex:
	 * 2C38393838323339303030303032383839353233362C31393837352C... Output:
	 * ICCID,energyUsed,minTemp,maxTemp,...
	 */
	public MessageType16Response parseMessageType16(String hexPayload, String deviceId, String deviceType) {
		log.debug("🔍 Parsing Message Type 16 for device: {}", deviceId);

		MessageType16Response response = new MessageType16Response();
		response.setDeviceId(deviceId);
		response.setDeviceType(deviceType);

		try {
			// Converti hex in ASCII
			String asciiData = hexToAscii(hexPayload);
			log.debug("   ASCII data: {}", asciiData);

			// Parse CSV format
			String[] fields = asciiData.split(",");

			if (fields.length >= 12) {
				response.setIccid(fields[0].trim());
				response.setEnergyUsed(parseLong(fields[1]));
				response.setMinTemperature(parseInteger(fields[2]));
				response.setMaxTemperature(parseInteger(fields[3]));
				response.setMessageCount(parseInteger(fields[4]));
				response.setDeliveryFailCount(parseInteger(fields[5]));
				response.setTotalSendTime(parseLong(fields[6]));
				response.setMaxSendTime(parseLong(fields[7]));
				response.setMinSendTime(parseLong(fields[8]));
				response.setRssiTotal(parseLong(fields[9]));
				response.setRssiValidCount(parseInteger(fields[10]));
				response.setRssiFailCount(parseInteger(fields[11]));

				// Calcola campi derivati
				response.calculateDerivedFields();

				log.info("✅ Parsed Message Type 16: ICCID={}, Energy={}mAh, SuccessRate={}%", response.getIccid(),
						response.getEnergyUsed(), String.format("%.2f", response.getDeliverySuccessRate()));
			} else {
				log.warn("⚠️  Unexpected field count in Message Type 16: {} (expected 12)", fields.length);
			}

		} catch (Exception e) {
			log.error("❌ Failed to parse Message Type 16", e);
		}

		return response;
	}

	/**
	 * Parse Message Type 17 (GPS) Input hex:
	 * 2C39352C3133343434322E302C353235352E393935304E2C... Output: GPS data fields
	 */
	public MessageType17Response parseMessageType17(String hexPayload, String deviceId, String deviceType) {
		log.debug("🔍 Parsing Message Type 17 for device: {}", deviceId);

		MessageType17Response response = new MessageType17Response();
		response.setDeviceId(deviceId);
		response.setDeviceType(deviceType);

		try {
			// Converti hex in ASCII
			String asciiData = hexToAscii(hexPayload);
			log.debug("   ASCII data: {}", asciiData);

			// Parse CSV format
			String[] fields = asciiData.split(",");

			if (fields.length >= 12) {
				response.setTimeToFixSeconds(parseInteger(fields[0]));

				// Parse UTC time (format: hhmmss.s)
				String utcTimeStr = fields[1].trim().replace(".", "");
				if (utcTimeStr.length() >= 6) {
					int hours = Integer.parseInt(utcTimeStr.substring(0, 2));
					int minutes = Integer.parseInt(utcTimeStr.substring(2, 4));
					int seconds = Integer.parseInt(utcTimeStr.substring(4, 6));
					response.setUtcTime(LocalTime.of(hours, minutes, seconds));
				}

				response.setLatitudeRaw(fields[2].trim());
				response.setLongitudeRaw(fields[3].trim());
				response.setHorizontalPrecision(parseDouble(fields[4]));
				response.setAltitude(parseDouble(fields[5]));
				response.setGnssPositioningMode(parseInteger(fields[6]));
				response.setGroundHeading(parseDouble(fields[7]));
				response.setSpeedKmh(parseDouble(fields[8]));
				response.setSpeedKnots(parseDouble(fields[9]));
				response.setDate(fields[10].trim());
				response.setNumberOfSatellites(parseInteger(fields[11]));

				log.info("✅ Parsed Message Type 17: GPS({}, {}) alt={}m, sats={}", response.getLatitude(),
						response.getLongitude(), response.getAltitude(), response.getNumberOfSatellites());
				log.info("   📍 Google Maps: {}", response.getGoogleMapsLink());
			} else {
				log.warn("⚠️  Unexpected field count in Message Type 17: {} (expected 12)", fields.length);
			}

		} catch (Exception e) {
			log.error("❌ Failed to parse Message Type 17", e);
		}

		return response;
	}

	// Helper methods
	private Integer parseInteger(String value) {
		try {
			return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Long parseLong(String value) {
		try {
			return value != null && !value.trim().isEmpty() ? Long.parseLong(value.trim()) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Double parseDouble(String value) {
		try {
			return value != null && !value.trim().isEmpty() ? Double.parseDouble(value.trim()) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}