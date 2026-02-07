package com.aton.proj.oneGasMeteor.encoder.impl;

import com.aton.proj.oneGasMeteor.encoder.DeviceEncoder;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Encoder fallback che non codifica nessun comando
 * Usato per device sconosciuti o quando non ci sono comandi da inviare
 */
@Component
public class NoOpEncoder implements DeviceEncoder {
    
    private static final Logger log = LoggerFactory.getLogger(NoOpEncoder.class);
    
    @Override
    public boolean canEncode(String deviceType) {
        // Questo encoder accetta sempre (è il fallback)
        return true;
    }
    
    @Override
    public List<TelemetryResponse.EncodedCommand> encode(List<DeviceCommand> commands) {
        if (!commands.isEmpty()) {
            log.warn("⚠️  NoOpEncoder called with {} commands - no encoding performed", commands.size());
        }
        return Collections.emptyList();
    }
    
    @Override
    public List<String> getSupportedDeviceTypes() {
        return Collections.singletonList("*");
    }
    
    @Override
    public String getEncoderName() {
        return "NoOpEncoder";
    }
}