package com.aton.proj.oneGasMeteor.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;

@RestController
public class MeteorController {

	@Value("${server.port}")
	private int serverPort;

	@Value("${spring.application.name}")
	private String applicationName;

	@PostConstruct
	public void init() {
		System.out.println("========================================");
		System.out.println("🚀 MeteorController initialized");
		System.out.println("   Application Name: " + applicationName);
		System.out.println("   Server Port: " + serverPort);
		System.out.println("========================================");
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
		System.out.println("🚀 [PORT " + serverPort + "] Request received at " + LocalDateTime.now());

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

		System.out.println("⚙️  [PORT " + serverPort + "] Processing request...");

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