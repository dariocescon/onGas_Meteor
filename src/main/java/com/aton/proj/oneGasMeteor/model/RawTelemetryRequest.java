package com.aton.proj.oneGasMeteor.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Richiesta HTTP dal device contenente il messaggio hex
 */
public class RawTelemetryRequest {
    
    @NotBlank(message = "Hex message is required")
    private String hexMessage;
    
    private String deviceId; // Opzionale, può essere estratto dal messaggio
    
    public RawTelemetryRequest() {
    }
    
    public RawTelemetryRequest(String hexMessage) {
        this.hexMessage = hexMessage;
    }
    
    public RawTelemetryRequest(String hexMessage, String deviceId) {
        this.hexMessage = hexMessage;
        this.deviceId = deviceId;
    }
    
    public String getHexMessage() {
        return hexMessage;
    }
    
    public void setHexMessage(String hexMessage) {
        this.hexMessage = hexMessage;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    @Override
    public String toString() {
        return "RawTelemetryRequest{" +
                "hexMessage='" + (hexMessage != null ? hexMessage.substring(0, Math.min(20, hexMessage.length())) + "..." : "null") + '\'' +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
}