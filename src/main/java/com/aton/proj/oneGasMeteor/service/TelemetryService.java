package com.aton.proj.oneGasMeteor.service;

import com.aton.proj.oneGasMeteor.decoder.DecoderFactory;
import com.aton.proj.oneGasMeteor.decoder.DeviceDecoder;
import com.aton.proj.oneGasMeteor.exception.DecodingException;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.TekMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service per elaborare i messaggi di telemetria
 */
@Service
public class TelemetryService {
    
    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    
    private final DecoderFactory decoderFactory;
    
    public TelemetryService(DecoderFactory decoderFactory) {
        this.decoderFactory = decoderFactory;
    }
    
    /**
     * Processa un messaggio di telemetria
     * 
     * @param hexMessage Messaggio in formato hex string
     * @return Risposta da inviare al device
     */
    public TelemetryResponse processTelemetry(String hexMessage) {
        
        LocalDateTime receivedAt = LocalDateTime.now();
        log.debug("📥 Processing telemetry message: {} chars", hexMessage.length());
        
        try {
            // 1. CONVERTI HEX STRING → BYTE ARRAY
            byte[] payload = hexStringToByteArray(hexMessage);
            
            // 2. SELEZIONA IL DECODER APPROPRIATO
            DeviceDecoder decoder = decoderFactory.getDecoder(payload);
            log.debug("🔧 Selected decoder: {}", decoder.getDecoderName());
            
            // 3. CREA TekMessage con timestamp
            TekMessage tekMessage = TekMessage.fromHexString(hexMessage, System.currentTimeMillis());
            
            // 4. DECODIFICA IL MESSAGGIO
            DecodedMessage decoded = decoder.decode(tekMessage);
            log.debug("✅ Message decoded: deviceType={}, IMEI={}", 
                decoded.getUnitInfo().getProductType(),
                decoded.getUniqueIdentifier().getImei());
            
            // 5. SALVA NEL DATABASE (TODO: implementeremo dopo)
            // telemetryRepository.save(decoded);
            
            // 6. RECUPERA COMANDI PENDENTI (TODO: implementeremo dopo)
            // List<DeviceCommand> commands = commandRepository.getPendingCommands(deviceId);
            
            // 7. CODIFICA COMANDI (TODO: implementeremo dopo con encoder)
            
            // 8. CREA RISPOSTA
            TelemetryResponse response = TelemetryResponse.success(
                decoded.getUniqueIdentifier().getImei(),
                decoded.getUnitInfo().getProductType()
            );
            response.setReceivedAt(receivedAt);
            response.setProcessedAt(LocalDateTime.now());
            
            log.info("✅ Telemetry processed successfully in {} ms", 
                java.time.Duration.between(receivedAt, LocalDateTime.now()).toMillis());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Error processing telemetry", e);
            throw new DecodingException("Failed to process telemetry: " + e.getMessage(), e);
        }
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