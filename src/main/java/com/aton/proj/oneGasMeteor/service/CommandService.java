package com.aton.proj.oneGasMeteor.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aton.proj.oneGasMeteor.dto.CommandCreateRequest;
import com.aton.proj.oneGasMeteor.dto.CommandResponse;
import com.aton.proj.oneGasMeteor.encoder.EncoderFactory;
import com.aton.proj.oneGasMeteor.encoder.impl.Tek822Encoder;
import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service per la gestione dei comandi device via API REST.
 * Validazione stretta di deviceType, commandType e parametri obbligatori.
 */
@Service
public class CommandService {

	private static final Logger log = LoggerFactory.getLogger(CommandService.class);

	private final CommandRepository commandRepository;
	private final EncoderFactory encoderFactory;
	private final ObjectMapper objectMapper;

	// Command types validi (dalle costanti di Tek822Encoder)
	private static final Set<String> VALID_COMMAND_TYPES = Set.of(
			Tek822Encoder.CMD_SET_INTERVAL,
			Tek822Encoder.CMD_SET_LISTEN,
			Tek822Encoder.CMD_SET_SCHEDULE,
			Tek822Encoder.CMD_REBOOT,
			Tek822Encoder.CMD_REQUEST_STATUS,
			Tek822Encoder.CMD_SET_ALARM_THRESHOLD,
			Tek822Encoder.CMD_SHUTDOWN,
			Tek822Encoder.CMD_SET_RTC,
			Tek822Encoder.CMD_DEACTIVATE,
			Tek822Encoder.CMD_CLOSE_TCP,
			Tek822Encoder.CMD_REQUEST_GPS,
			Tek822Encoder.CMD_REQUEST_SETTINGS,
			Tek822Encoder.CMD_SET_APN,
			Tek822Encoder.CMD_SET_SERVER);

	// Parametri obbligatori per ciascun commandType
	private static final Map<String, List<String>> REQUIRED_PARAMS = Map.of(
			Tek822Encoder.CMD_SET_INTERVAL, List.of("interval"),
			Tek822Encoder.CMD_SET_LISTEN, List.of("listenMinutes"),
			Tek822Encoder.CMD_SET_ALARM_THRESHOLD, List.of("threshold"),
			Tek822Encoder.CMD_SET_RTC, List.of("datetime"),
			Tek822Encoder.CMD_SET_APN, List.of("apn"),
			Tek822Encoder.CMD_SET_SERVER, List.of("serverIp", "serverPort"));

	public CommandService(CommandRepository commandRepository, EncoderFactory encoderFactory,
			ObjectMapper objectMapper) {
		this.commandRepository = commandRepository;
		this.encoderFactory = encoderFactory;
		this.objectMapper = objectMapper;
		log.info("CommandService initialized");
	}

	/**
	 * Crea un nuovo comando dopo validazione stretta
	 */
	public CommandResponse createCommand(CommandCreateRequest request) {
		// 1. Valida deviceType
		validateDeviceType(request.getDeviceType());

		// 2. Valida commandType
		validateCommandType(request.getCommandType());

		// 3. Valida parametri obbligatori
		validateRequiredParameters(request.getCommandType(), request.getParameters());

		// 4. Crea DeviceCommand e salva
		DeviceCommand deviceCommand = new DeviceCommand(
				request.getDeviceId(),
				request.getDeviceType(),
				request.getCommandType());

		if (request.getParameters() != null) {
			deviceCommand.setParameters(request.getParameters());
		}

		CommandEntity saved = commandRepository.save(deviceCommand);

		log.info("Created command: id={}, type={}, device={}",
				saved.getId(), saved.getCommandType(), saved.getDeviceId());

		return CommandResponse.fromEntity(saved, objectMapper);
	}

	/**
	 * Recupera un comando per ID
	 */
	public CommandResponse getCommandById(Long id) {
		CommandEntity entity = commandRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Command not found: " + id));
		return CommandResponse.fromEntity(entity, objectMapper);
	}

	/**
	 * Recupera i comandi pendenti per un device
	 */
	public List<CommandResponse> getPendingCommands(String deviceId) {
		return commandRepository.findPendingCommands(deviceId).stream()
				.map(entity -> CommandResponse.fromEntity(entity, objectMapper))
				.toList();
	}

	// ====================== Validazione ======================

	private void validateDeviceType(String deviceType) {
		if (!encoderFactory.getEncoder(deviceType).canEncode(deviceType)) {
			throw new IllegalArgumentException("Unsupported deviceType: " + deviceType
					+ ". Supported: TEK586, TEK733, TEK643, TEK811, TEK822V1, TEK822V2, TEK733A, "
					+ "TEK871, TEK811A, TEK822V1BTN, TEK900, TEK880, TEK898V2, TEK898V1");
		}
	}

	private void validateCommandType(String commandType) {
		if (!VALID_COMMAND_TYPES.contains(commandType)) {
			throw new IllegalArgumentException("Unknown commandType: " + commandType
					+ ". Valid types: " + VALID_COMMAND_TYPES);
		}
	}

	private void validateRequiredParameters(String commandType, Map<String, Object> parameters) {
		List<String> required = REQUIRED_PARAMS.get(commandType);
		if (required == null) {
			return; // Nessun parametro obbligatorio
		}

		for (String param : required) {
			if (parameters == null || !parameters.containsKey(param) || parameters.get(param) == null) {
				throw new IllegalArgumentException(
						"Missing required parameter '" + param + "' for command " + commandType);
			}
		}
	}
}
