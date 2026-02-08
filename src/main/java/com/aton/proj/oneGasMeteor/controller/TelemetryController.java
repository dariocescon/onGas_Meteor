package com.aton.proj.oneGasMeteor.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aton.proj.oneGasMeteor.exception.DecodingException;
import com.aton.proj.oneGasMeteor.exception.UnknownDeviceException;
import com.aton.proj.oneGasMeteor.model.RawTelemetryRequest;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.service.TelemetryService;

import jakarta.validation.Valid;

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
     * POST /telemetry
     * Content-Type: text/plain
     * Body: 080181048614750861075021004551047B00019700000082010F0A5B28770A5B...
     */
    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TelemetryResponse> receiveTelemetryPlainText(@RequestBody String hexMessage) {
        
        log.info("🚀 [PORT {}] Received telemetry message (plain text): {} bytes", 
            serverPort, hexMessage != null ? hexMessage.length() / 2 : 0);
        
        try {
            // Valida che non sia vuoto
            if (hexMessage == null || hexMessage.trim().isEmpty()) {
                log.warn("⚠️  Empty hex message received");
                return ResponseEntity
                    .badRequest()
                    .body(TelemetryResponse.error("Empty message"));
            }
            
            // Rimuovi eventuali spazi o caratteri non validi
            String cleanHex = hexMessage.trim().replaceAll("\\s+", "");
            
            // Processa il messaggio
            TelemetryResponse response = telemetryService.processTelemetry(cleanHex);
            
            log.info("✅ [PORT {}] Telemetry processed successfully for device: {} (type: {})", 
                serverPort, response.getDeviceId(), response.getDeviceType());
            
            return ResponseEntity.ok(response);
            
        } catch (UnknownDeviceException e) {
            log.error("❌ [PORT {}] Unknown device: {}", serverPort, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(TelemetryResponse.error("Unknown device type: " + e.getDeviceType()));
                
        } catch (DecodingException e) {
            log.error("❌ [PORT {}] Decoding error: {}", serverPort, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(TelemetryResponse.error("Failed to decode message: " + e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ [PORT {}] Unexpected error processing telemetry", serverPort, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TelemetryResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Endpoint alternativo per ricevere messaggi in formato JSON
     * 
     * POST /telemetry/json
     * Content-Type: application/json
     * Body: {
     *   "hexMessage": "080181048614750861...",
     *   "deviceId": "optional"
     * }
     */
    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TelemetryResponse> receiveTelemetryJson(@Valid @RequestBody RawTelemetryRequest request) {
        
        log.info("🚀 [PORT {}] Received telemetry message (JSON): deviceId={}, {} bytes", 
            serverPort, request.getDeviceId(), 
            request.getHexMessage() != null ? request.getHexMessage().length() / 2 : 0);
        
        try {
            // Processa il messaggio
            TelemetryResponse response = telemetryService.processTelemetry(request.getHexMessage());
            
            log.info("✅ [PORT {}] Telemetry processed successfully for device: {} (type: {})", 
                serverPort, response.getDeviceId(), response.getDeviceType());
            
            return ResponseEntity.ok(response);
            
        } catch (UnknownDeviceException e) {
            log.error("❌ [PORT {}] Unknown device: {}", serverPort, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(TelemetryResponse.error("Unknown device type: " + e.getDeviceType()));
                
        } catch (DecodingException e) {
            log.error("❌ [PORT {}] Decoding error: {}", serverPort, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(TelemetryResponse.error("Failed to decode message: " + e.getMessage()));
                
        } catch (Exception e) {
            log.error("❌ [PORT {}] Unexpected error processing telemetry", serverPort, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TelemetryResponse.error("Internal server error: " + e.getMessage()));
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