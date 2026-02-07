package com.aton.proj.oneGasMeteor.service;

import com.aton.proj.oneGasMeteor.decoder.DecoderFactory;
import com.aton.proj.oneGasMeteor.decoder.DeviceDecoder;
import com.aton.proj.oneGasMeteor.encoder.DeviceEncoder;
import com.aton.proj.oneGasMeteor.encoder.EncoderFactory;
import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.exception.DecodingException;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TekMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.aton.proj.oneGasMeteor.repository.TelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	private final ObjectMapper objectMapper;

	@Value("${command.max.per.response:10}")
	private int maxCommandsPerResponse;

	public TelemetryService(DecoderFactory decoderFactory, EncoderFactory encoderFactory,
			TelemetryRepository telemetryRepository, CommandRepository commandRepository, ObjectMapper objectMapper) {
		this.decoderFactory = decoderFactory;
		this.encoderFactory = encoderFactory;
		this.telemetryRepository = telemetryRepository;
		this.commandRepository = commandRepository;
		this.objectMapper = objectMapper;

		log.info("✅ TelemetryService initialized");
	}

	/**
	 * Processa un messaggio di telemetria
	 * 
	 * @param hexMessage Messaggio in formato hex string
	 * @return Risposta da inviare al device
	 */
	public TelemetryResponse processTelemetry(String hexMessage) {

		LocalDateTime receivedAt = LocalDateTime.now();
		log.info("📥 Processing telemetry message: {} chars", hexMessage.length());

		try {
			// 1. CONVERTI HEX STRING → BYTE ARRAY
			byte[] payload = hexStringToByteArray(hexMessage);
			log.debug("   Converted to {} bytes", payload.length);

			// 2. SELEZIONA IL DECODER APPROPRIATO
			DeviceDecoder decoder = decoderFactory.getDecoder(payload);
			log.debug("   Selected decoder: {}", decoder.getDecoderName());

			// 3. CREA TekMessage con timestamp
			TekMessage tekMessage = TekMessage.fromHexString(hexMessage, System.currentTimeMillis());

			// 4. DECODIFICA IL MESSAGGIO
			DecodedMessage decoded = decoder.decode(tekMessage);
			String deviceId = extractDeviceId(decoded);
			String deviceType = decoded.getUnitInfo().getProductType();

			log.info("   ✅ Decoded: deviceType={}, deviceId={}", deviceType, deviceId);

			// 5. SALVA NEL DATABASE
			TelemetryEntity savedEntity = telemetryRepository.save(deviceId, deviceType, hexMessage, decoded);
			log.info("   💾 Saved to database: id={}", savedEntity.getId());

			// 6. RECUPERA COMANDI PENDENTI PER QUESTO DEVICE
			List<CommandEntity> pendingCommands = commandRepository.findPendingCommands(deviceId);
			log.debug("   📋 Found {} pending commands for device {}", pendingCommands.size(), deviceId);

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

			long processingTimeMs = java.time.Duration.between(receivedAt, LocalDateTime.now()).toMillis();
			log.info("✅ Telemetry processed successfully in {} ms (commands: {})", processingTimeMs,
					encodedCommands.size());

			return response;

		} catch (Exception e) {
			log.error("❌ Error processing telemetry", e);
			throw new DecodingException("Failed to process telemetry: " + e.getMessage(), e);
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
	 * Codifica i comandi da inviare al device
	 */
	private List<TelemetryResponse.EncodedCommand> encodeCommands(List<CommandEntity> pendingCommands,
			String deviceType) {

		try {
			// Limita il numero di comandi per risposta
			List<CommandEntity> commandsToSend = pendingCommands.stream().limit(maxCommandsPerResponse).toList();

			log.debug("   🔧 Encoding {} commands for device type: {}", commandsToSend.size(), deviceType);

			// Seleziona l'encoder appropriato
			DeviceEncoder encoder = encoderFactory.getEncoder(deviceType);
			log.debug("   Selected encoder: {}", encoder.getEncoderName());

			// Converti CommandEntity → DeviceCommand
			List<DeviceCommand> deviceCommands = commandsToSend.stream().map(this::toDeviceCommand).toList();

			// Codifica i comandi
			List<TelemetryResponse.EncodedCommand> encodedCommands = encoder.encode(deviceCommands);

			// Marca i comandi come SENT
			for (CommandEntity cmd : commandsToSend) {
				commandRepository.markAsSent(cmd.getId());
			}

			log.info("   📤 Encoded {} commands successfully", encodedCommands.size());

			return encodedCommands;

		} catch (Exception e) {
			log.error("❌ Failed to encode commands", e);
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
				log.warn("⚠️  Failed to parse command params for command {}: {}", entity.getId(), e.getMessage());
			}
		}

		return command;
	}

	/**
	 * Converte hex string in byte array
	 */
	private byte[] hexStringToByteArray(String hexString) {
		if (hexString == null || hexString.isEmpty()) {
			throw new IllegalArgumentException("Hex string cannot be null or empty");
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