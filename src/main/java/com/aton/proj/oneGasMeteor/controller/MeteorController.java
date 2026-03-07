package com.aton.proj.oneGasMeteor.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;

@RestController
public class MeteorController {

	private static final Logger log = LoggerFactory.getLogger(MeteorController.class);

	@Value("${server.port}")
	private int serverPort;

	@Value("${spring.application.name}")
	private String applicationName;

	@PostConstruct
	public void init() {
		log.info("========================================");
		log.info("MeteorController initialized");
		log.info("   Application Name: {}", applicationName);
		log.info("   Server Port: {}", serverPort);
		log.info("========================================");
	}

	/**
	 * Endpoint di test per verificare il load balancing
	 */
	@GetMapping("/status")
	public Map<String, Object> status(
			@RequestHeader(value = "X-Gateway-Source", required = false) String gatewaySource) {

		Map<String, Object> response = new HashMap<>();
		response.put("service", applicationName);
		response.put("instance", "Instance on port " + serverPort);
		response.put("port", serverPort);
		response.put("timestamp", LocalDateTime.now());
		response.put("message", "Hello from " + applicationName + " on port " + serverPort);
		response.put("gatewaySource", gatewaySource);

		// Log per vedere quale istanza risponde
		log.info(" Meteor [PORT {}] Request received at {}", serverPort, LocalDateTime.now());

		return response;
	}

	/**
	 * Endpoint per simulare elaborazione
	 */
	@GetMapping("/process")
	public Map<String, Object> process() {
		Map<String, Object> response = new HashMap<>();
		response.put("service", applicationName);
		response.put("port", serverPort);
		response.put("message", "Processing request on port " + serverPort);
		response.put("timestamp", LocalDateTime.now());

		log.info(" Meteor [PORT {}] Processing request...", serverPort);

		return response;
	}

	/**
	 * Health check
	 */
	@GetMapping("/health")
	public Map<String, Object> health() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("port", serverPort);
		return response;
	}
}