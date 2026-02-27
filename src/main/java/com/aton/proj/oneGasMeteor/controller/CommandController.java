package com.aton.proj.oneGasMeteor.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aton.proj.oneGasMeteor.dto.CommandCreateRequest;
import com.aton.proj.oneGasMeteor.dto.CommandResponse;
import com.aton.proj.oneGasMeteor.service.CommandService;

import jakarta.validation.Valid;

/**
 * REST Controller per la gestione dei comandi device.
 *
 * POST /api/commands             - Crea un nuovo comando
 * GET  /api/commands/{id}        - Dettaglio singolo comando
 * GET  /api/commands/device/{id} - Comandi pendenti per device
 */
@RestController
@RequestMapping("/api/commands")
public class CommandController {

	private static final Logger log = LoggerFactory.getLogger(CommandController.class);

	private final CommandService commandService;

	public CommandController(CommandService commandService) {
		this.commandService = commandService;
		log.info("CommandController initialized");
	}

	@PostMapping
	public ResponseEntity<CommandResponse> createCommand(@Valid @RequestBody CommandCreateRequest request) {
		log.info("POST /api/commands - deviceId={}, commandType={}",
				request.getDeviceId(), request.getCommandType());

		CommandResponse response = commandService.createCommand(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CommandResponse> getCommand(@PathVariable Long id) {
		log.debug("GET /api/commands/{}", id);

		CommandResponse response = commandService.getCommandById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/device/{deviceId}")
	public ResponseEntity<List<CommandResponse>> getPendingCommands(@PathVariable String deviceId) {
		log.debug("GET /api/commands/device/{}", deviceId);

		List<CommandResponse> commands = commandService.getPendingCommands(deviceId);
		return ResponseEntity.ok(commands);
	}

	// ====================== Exception Handlers ======================

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleValidationError(IllegalArgumentException ex) {
		log.warn("Validation error: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(Map.of(
				"status", "ERROR",
				"message", ex.getMessage(),
				"timestamp", LocalDateTime.now().toString()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");

		log.warn("Bean validation error: {}", message);
		return ResponseEntity.badRequest().body(Map.of(
				"status", "ERROR",
				"message", message,
				"timestamp", LocalDateTime.now().toString()));
	}
}
