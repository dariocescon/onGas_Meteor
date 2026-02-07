package com.aton.proj.oneGasMeteor.controller;

import com.aton.proj.oneGasMeteor.service.DataCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller per gestire il cleanup manuale dei dati
 */
@RestController
@RequestMapping("/admin/cleanup")
@ConditionalOnProperty(name = "cleanup.enabled", havingValue = "true")
public class CleanupAdminController {
    
    private static final Logger log = LoggerFactory.getLogger(CleanupAdminController.class);
    
    private final DataCleanupService cleanupService;
    
    public CleanupAdminController(DataCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }
    
    /**
     * Trigger manuale del cleanup
     * 
     * POST /admin/cleanup/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<DataCleanupService.CleanupReport> triggerCleanup() {
        log.info("🧹 Manual cleanup triggered via API");
        
        DataCleanupService.CleanupReport report = cleanupService.manualCleanup();
        
        if (report.isSuccess()) {
            return ResponseEntity.ok(report);
        } else {
            return ResponseEntity.status(500).body(report);
        }
    }
    
    /**
     * Trigger cleanup solo telemetry
     * 
     * POST /admin/cleanup/telemetry
     */
    @PostMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> cleanupTelemetry() {
        log.info("🧹 Manual telemetry cleanup triggered via API");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        try {
            cleanupService.cleanupOldTelemetry();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Trigger cleanup solo commands
     * 
     * POST /admin/cleanup/commands
     */
    @PostMapping("/commands")
    public ResponseEntity<Map<String, Object>> cleanupCommands() {
        log.info("🧹 Manual commands cleanup triggered via API");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        try {
            cleanupService.cleanupOldCommands();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Status del cleanup service
     * 
     * GET /admin/cleanup/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", true);
        status.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(status);
    }
}