package com.aton.proj.oneGasMeteor.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aton.proj.oneGasMeteor.decoder.DecoderFactory;
import com.aton.proj.oneGasMeteor.decoder.DeviceDecoder;
import com.aton.proj.oneGasMeteor.decoder.MessageTypeParser;
import com.aton.proj.oneGasMeteor.encoder.DeviceEncoder;
import com.aton.proj.oneGasMeteor.encoder.EncoderFactory;
import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.exception.DecodingException;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.MessageType16Response;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;
import com.aton.proj.oneGasMeteor.model.MessageType6Response;
import com.aton.proj.oneGasMeteor.model.TelemetryMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse.EncodedCommand;
import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.aton.proj.oneGasMeteor.repository.TelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service per elaborare i messaggi di telemetria
 */
@Service
public class TelemetryService {

	private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

	private final DecoderFactory decoderFactory;
	private final EncoderFactory encoderFactory;
	private final TelemetryRepository telemetryRepository;
	private final CommandRepository commandRepository;
	private final MessageTypeParser messageTypeParser;
	private final ObjectMapper objectMapper;

	@Value("${command.max.per.response:10}")
	private int maxCommandsPerResponse;

	public TelemetryService(DecoderFactory decoderFactory, EncoderFactory encoderFactory,
			TelemetryRepository telemetryRepository, CommandRepository commandRepository,
			MessageTypeParser messageTypeParser, ObjectMapper objectMapper) {
		this.decoderFactory = decoderFactory;
		this.encoderFactory = encoderFactory;
		this.telemetryRepository = telemetryRepository;
		this.commandRepository = commandRepository;
		this.messageTypeParser = messageTypeParser;
		this.objectMapper = objectMapper;

		log.info("TelemetryService initialized");
	}

	/**
	 * Processa un messaggio di telemetria
	 * 
	 * @param hexMessage Messaggio in formato hex string
	 * @return Risposta da inviare al device
	 */
	public TelemetryResponse processTelemetry(TelemetryMessage message) {

		LocalDateTime receivedAt = LocalDateTime.now();
		log.info("Processing telemetry message: {} chars", message.getHexData().length());

		try {
			// 2. SELEZIONA IL DECODER APPROPRIATO
			DeviceDecoder decoder = decoderFactory.getDecoder(message.getPayload());
			log.debug("  Selected decoder: {}", decoder.getDecoderName());

			// 4. DECODIFICA IL MESSAGGIO
			DecodedMessage decoded = decoder.decode(message);
			String deviceId = extractDeviceId(decoded);
			String deviceType = decoded.getUnitInfo().getProductType();
			int messageType = extractMessageType(message.getPayload());

			log.info("  Decoded: deviceType={}, deviceId={}, messageType={}", deviceType, deviceId, messageType);

			String hexData = message.getHexData();
			// 5. GESTISCI IN BASE AL MESSAGE TYPE
			switch (messageType) {
			case 4, 8, 9 -> {
				// Standard telemetry - salva nel DB
				TelemetryEntity savedEntity = telemetryRepository.save(deviceId, deviceType, hexData, decoded);
				log.info("  Saved to database: id={}", savedEntity.getId());
			}
			case 6 -> {
				// Settings response - parse e log
				String settingsPayload = extractPayloadAfterHeader(hexData);
				MessageType6Response settings = messageTypeParser.parseMessageType6(settingsPayload, deviceId,
						deviceType);
				log.info("  Received settings: {} parameters", settings.getSettings().size());
				// TODO: Opzionalmente salva in una tabella device_settings
			}
			case 16 -> {
				// ICCID & Statistics - parse e log
				String statsPayload = extractPayloadAfterHeader(hexData);
				MessageType16Response stats = messageTypeParser.parseMessageType16(statsPayload, deviceId, deviceType);
				log.info("  Received statistics: ICCID={}, Energy={}mAh", stats.getIccid(), stats.getEnergyUsed());
				// TODO: Opzionalmente salva in una tabella device_statistics
			}
			case 17 -> {
				// GPS data - parse e log
				String gpsPayload = extractPayloadAfterHeader(hexData);
				MessageType17Response gps = messageTypeParser.parseMessageType17(gpsPayload, deviceId, deviceType);
				log.info("  Received GPS: lat={}, lon={}, alt={}m", gps.getLatitude(), gps.getLongitude(),
						gps.getAltitude());
				log.info("  Google Maps: {}", gps.getGoogleMapsLink());
				// TODO: Opzionalmente salva in una tabella device_locations
			}
			default -> {
				log.warn("  Unknown message type: {}", messageType);
			}
			}

			// 6. RECUPERA COMANDI PENDENTI PER QUESTO DEVICE
			List<CommandEntity> pendingCommands = commandRepository.findPendingCommands(deviceId);
			log.debug("   Found {} pending commands for device {}", pendingCommands.size(), deviceId);

			// 7. CODIFICA COMANDI (se presenti)
			List<TelemetryResponse.EncodedCommand> encodedCommands = new ArrayList<>();
			if (!pendingCommands.isEmpty()) {
				encodedCommands = encodeCommands(pendingCommands, deviceType);
			}

			// 8. CREA RISPOSTA
			TelemetryResponse response = TelemetryResponse.success(deviceId, deviceType);
			response.setReceivedAt(receivedAt);
			response.setProcessedAt(LocalDateTime.now());
			response.setCommands(encodedCommands);

			long processingTimeMs = Duration.between(receivedAt, LocalDateTime.now()).toMillis();
			log.info("  Telemetry processed successfully in {} ms (commands: {})", processingTimeMs,
					encodedCommands.size());

			return response;

		} catch (Exception e) {
			log.error("  Error processing telemetry", e);
			throw new DecodingException("Failed to process telemetry: " + e.getMessage(), e);
		}
	}

	public void markCommandsAsSent(List<EncodedCommand> commands) {
		// Marca i comandi come SENT (skip comandi sintetici con id null, es.
		// auto-appended REBOOT)
		for (EncodedCommand cmd : commands) {
			if (cmd.getCommandId() != null) {
				commandRepository.markAsSent(cmd.getCommandId());
			}
		}
	}

	/**
	 * Estrae il device ID dal messaggio decodificato (usa IMEI)
	 */
	private String extractDeviceId(DecodedMessage decoded) {
		if (decoded.getUniqueIdentifier() != null && decoded.getUniqueIdentifier().getImei() != null) {
			return decoded.getUniqueIdentifier().getImei();
		}

		// Fallback: usa product type + timestamp
		String productType = decoded.getUnitInfo() != null ? decoded.getUnitInfo().getProductType() : "UNKNOWN";
		return productType + "-" + System.currentTimeMillis();
	}

	/**
	 * Estrae il message type dal byte 15
	 */
	private int extractMessageType(byte[] payload) {
		if (payload.length > 15) {
			return payload[15] & 0x3F; // Minor 6 bits
		}
		return -1;
	}

	/**
	 * Estrae il payload dopo l'header (da byte 17 in poi)
	 */
	private String extractPayloadAfterHeader(String hexMessage) {
		// Header = 17 bytes = 34 hex chars
		if (hexMessage.length() > 34) {
			return hexMessage.substring(34);
		}
		return "";
	}

	/**
	 * Codifica i comandi da inviare al device
	 */
	private List<TelemetryResponse.EncodedCommand> encodeCommands(List<CommandEntity> pendingCommands,
			String deviceType) {

		try {
			// Limita il numero di comandi per risposta
			List<CommandEntity> commandsToSend = pendingCommands.stream().limit(maxCommandsPerResponse).toList();

			log.debug("   Encoding {} commands for device type: {}", commandsToSend.size(), deviceType);

			// Seleziona l'encoder appropriato
			DeviceEncoder encoder = encoderFactory.getEncoder(deviceType);
			log.debug("   Selected encoder: {}", encoder.getEncoderName());

			// Converti CommandEntity → DeviceCommand
			List<DeviceCommand> deviceCommands = commandsToSend.stream().map(this::toDeviceCommand).toList();

			// Codifica i comandi
			List<TelemetryResponse.EncodedCommand> encodedCommands = encoder.encode(deviceCommands);

			log.info("   Encoded {} commands successfully", encodedCommands.size());

			return encodedCommands;

		} catch (Exception e) {
			log.error(" Failed to encode commands", e);
			return new ArrayList<>(); // Ritorna lista vuota invece di fallire
		}
	}

	/**
	 * Converte CommandEntity in DeviceCommand
	 */
	private DeviceCommand toDeviceCommand(CommandEntity entity) {
		DeviceCommand command = new DeviceCommand(entity.getDeviceId(), entity.getDeviceType(),
				entity.getCommandType());
		command.setId(entity.getId());

		// Deserializza i parametri JSON
		if (entity.getCommandParamsJson() != null && !entity.getCommandParamsJson().isEmpty()) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> params = objectMapper.readValue(entity.getCommandParamsJson(), Map.class);
				command.setParameters(params);
			} catch (Exception e) {
				log.warn("  Failed to parse command params for command {}: {}", entity.getId(), e.getMessage());
			}
		}

		return command;
	}
}