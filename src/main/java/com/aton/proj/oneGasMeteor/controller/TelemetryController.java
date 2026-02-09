package com.aton.proj.oneGasMeteor.controller;

import com.aton.proj.oneGasMeteor.controller.utils.ControllerUtils;
import com.aton.proj.oneGasMeteor.exception.DecodingException;
import com.aton.proj.oneGasMeteor.exception.UnknownDeviceException;
import com.aton.proj.oneGasMeteor.model.RawTelemetryRequest;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.service.TelemetryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller REST per ricevere messaggi di telemetria dai dispositivi
 */
@RestController
@RequestMapping("/telemetry")
public class TelemetryController {

	private static final Logger log = LoggerFactory.getLogger(TelemetryController.class);

	private final TelemetryService telemetryService;

	@Value("${server.port}")
	private int serverPort;

	public TelemetryController(TelemetryService telemetryService) {
		this.telemetryService = telemetryService;
	}

	/**
	 * Endpoint principale per ricevere messaggi hex string dal device
	 * 
	 * POST /telemetry Content-Type: text/plain
	 * 
	 * Response: JSON con comandi concatenati
	 */
	@PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TelemetryResponse> receiveTelemetryPlainText(@RequestBody String hexMessage) {

		log.info("🚀 [PORT {}] Received telemetry message (plain text): {} bytes", serverPort,
				hexMessage != null ? hexMessage.length() / 2 : 0);

		try {
			// Valida che non sia vuoto
			if (hexMessage == null || hexMessage.trim().isEmpty()) {
				log.warn("⚠️  Empty hex message received");
				return ResponseEntity.badRequest().body(TelemetryResponse.error("Empty message"));
			}

			// Rimuovi eventuali spazi o caratteri non validi
			String cleanHex = hexMessage.trim().replaceAll("\\s+", "");

			// Processa il messaggio
			TelemetryResponse response = telemetryService.processTelemetry(cleanHex);

			// ✅ AGGIUNGI COMANDI CONCATENATI
			enrichResponseWithConcatenatedCommands(response);

			log.info("✅ [PORT {}] Telemetry processed successfully for device: {} (type: {})", serverPort,
					response.getDeviceId(), response.getDeviceType());

			return ResponseEntity.ok(response);

		} catch (UnknownDeviceException e) {
			log.error("❌ [PORT {}] Unknown device: {}", serverPort, e.getMessage());
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
					.body(TelemetryResponse.error("Unknown device type: " + e.getDeviceType()));

		} catch (DecodingException e) {
			log.error("❌ [PORT {}] Decoding error: {}", serverPort, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(TelemetryResponse.error("Failed to decode message: " + e.getMessage()));

		} catch (Exception e) {
			log.error("❌ [PORT {}] Unexpected error processing telemetry", serverPort, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(TelemetryResponse.error("Internal server error: " + e.getMessage()));
		}
	}

	/**
	 * Endpoint per device che inviano/ricevono dati binari puri
	 */
	@PostMapping(value = "/octet", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<byte[]> receiveTelemetryOctetStreamBinary(@RequestBody byte[] rawBytes) {

		log.info("🚀 [PORT {}] Received telemetry message (octet-stream binary): {} bytes", serverPort,
				rawBytes != null ? rawBytes.length : 0);

		try {
			if (rawBytes == null || rawBytes.length == 0) {
				log.warn("⚠️  Empty byte array received");
				return ResponseEntity.badRequest().body(new byte[0]);
			}

			String hexMessage = ControllerUtils.bytesToHex(rawBytes);
			log.debug("   Converted {} bytes to hex string: {} chars", rawBytes.length, hexMessage.length());

			TelemetryResponse response = telemetryService.processTelemetry(hexMessage);

			log.info("✅ [PORT {}] Telemetry processed successfully for device: {} (type: {})", serverPort,
					response.getDeviceId(), response.getDeviceType());

			if (response.getCommands() != null && !response.getCommands().isEmpty()) {

				log.info("   📤 Preparing {} commands for binary response", response.getCommands().size());

				byte[] commandsBytes = ControllerUtils.concatenateCommands(response.getCommands());

				log.info("   📦 Sending {} bytes of commands to device", commandsBytes.length);
				log.debug("   📝 Commands ASCII: {}",
						new String(commandsBytes, java.nio.charset.StandardCharsets.US_ASCII));

				return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(commandsBytes);
			}

			log.info("   ℹ️  No commands for device, sending empty response");

			return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(new byte[0]);

		} catch (UnknownDeviceException e) {
			log.error("❌ [PORT {}] Unknown device: {}", serverPort, e.getMessage());
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new byte[0]);

		} catch (DecodingException e) {
			log.error("❌ [PORT {}] Decoding error: {}", serverPort, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new byte[0]);

		} catch (Exception e) {
			log.error("❌ [PORT {}] Unexpected error processing telemetry", serverPort, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
		}
	}

	/**
	 * Endpoint alternativo per ricevere messaggi in formato JSON
	 */
	@PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TelemetryResponse> receiveTelemetryJson(@Valid @RequestBody RawTelemetryRequest request) {

		log.info("🚀 [PORT {}] Received telemetry message (JSON): deviceId={}, {} bytes", serverPort,
				request.getDeviceId(), request.getHexMessage() != null ? request.getHexMessage().length() / 2 : 0);

		try {
			TelemetryResponse response = telemetryService.processTelemetry(request.getHexMessage());

			// ✅ AGGIUNGI COMANDI CONCATENATI
			enrichResponseWithConcatenatedCommands(response);

			log.info("✅ [PORT {}] Telemetry processed successfully for device: {} (type: {})", serverPort,
					response.getDeviceId(), response.getDeviceType());

			return ResponseEntity.ok(response);

		} catch (UnknownDeviceException e) {
			log.error("❌ [PORT {}] Unknown device: {}", serverPort, e.getMessage());
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
					.body(TelemetryResponse.error("Unknown device type: " + e.getDeviceType()));

		} catch (DecodingException e) {
			log.error("❌ [PORT {}] Decoding error: {}", serverPort, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(TelemetryResponse.error("Failed to decode message: " + e.getMessage()));

		} catch (Exception e) {
			log.error("❌ [PORT {}] Unexpected error processing telemetry", serverPort, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(TelemetryResponse.error("Internal server error: " + e.getMessage()));
		}
	}

	/**
	 * Endpoint per device che si aspettano risposta in formato HEX text/plain
	 */
	@PostMapping(value = "/raw", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> receiveTelemetryRaw(@RequestBody String hexMessage) {

		log.info("🚀 [PORT {}] Received telemetry message (raw mode): {} bytes", serverPort, hexMessage.length() / 2);

		try {
			String cleanHex = hexMessage.trim().replaceAll("\\s+", "");

			TelemetryResponse response = telemetryService.processTelemetry(cleanHex);

			log.info("✅ [PORT {}] Telemetry processed successfully for device: {} (type: {})", serverPort,
					response.getDeviceId(), response.getDeviceType());

			if (response.getCommands() != null && !response.getCommands().isEmpty()) {
				String commandsHex = ControllerUtils.commandsToHexString(response.getCommands());
				String commandsAscii = ControllerUtils.commandsToAsciiString(response.getCommands());

				log.info("   📤 Sending {} commands: {}", response.getCommands().size(), commandsAscii);
				log.debug("   📤 Commands HEX: {}", commandsHex);

				return ResponseEntity.ok(commandsHex);
			}

			log.info("   ℹ️  No commands for device");
			return ResponseEntity.ok("");

		} catch (Exception e) {
			log.error("❌ [PORT {}] Error processing telemetry", serverPort, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
		}
	}

	/**
	 * ✅ METODO HELPER: Arricchisce la risposta con comandi concatenati
	 */
	private void enrichResponseWithConcatenatedCommands(TelemetryResponse response) {
		if (response.getCommands() != null && !response.getCommands().isEmpty()) {

			// Genera comandi concatenati in HEX
			String concatenatedHex = ControllerUtils.commandsToHexString(response.getCommands());
			response.setConcatenatedCommandsHex(concatenatedHex);

			// Genera comandi concatenati in ASCII (per leggibilità)
			String concatenatedAscii = ControllerUtils.commandsToAsciiString(response.getCommands());
			response.setConcatenatedCommandsAscii(concatenatedAscii);

			log.info("   📤 Sending {} commands: {}", response.getCommands().size(), concatenatedAscii);
			log.debug("   📤 Commands HEX: {}", concatenatedHex);
		}
	}

	/**
	 * Health check endpoint
	 */
	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("Telemetry service is running on port " + serverPort);
	}

	/**
	 * Endpoint di test per verificare la connessione
	 */
	@GetMapping("/ping")
	public ResponseEntity<TelemetryResponse> ping() {
		TelemetryResponse response = new TelemetryResponse("OK", "Pong from port " + serverPort);
		response.setReceivedAt(LocalDateTime.now());
		return ResponseEntity.ok(response);
	}
}