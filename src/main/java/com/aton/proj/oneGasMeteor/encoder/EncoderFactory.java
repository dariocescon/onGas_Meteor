package com.aton.proj.oneGasMeteor.encoder;

import com.aton.proj.oneGasMeteor.encoder.impl.NoOpEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory per selezionare l'encoder appropriato
 */
@Component
public class EncoderFactory {
    
    private static final Logger log = LoggerFactory.getLogger(EncoderFactory.class);
    
    private final List<DeviceEncoder> encoders;
    private final NoOpEncoder fallbackEncoder;
    
    public EncoderFactory(List<DeviceEncoder> encoders, NoOpEncoder fallbackEncoder) {
        this.encoders = encoders;
        this.fallbackEncoder = fallbackEncoder;
        
        log.info("🔧 EncoderFactory initialized with {} encoders", encoders.size());
        encoders.forEach(encoder -> 
            log.info("   ✅ {} supports: {}", encoder.getEncoderName(), encoder.getSupportedDeviceTypes())
        );
    }
    
    /**
     * Seleziona l'encoder appropriato per il device type
     */
    public DeviceEncoder getEncoder(String deviceType) {
        if (deviceType == null || deviceType.isEmpty()) {
            log.warn("⚠️  Null/empty device type, using fallback encoder");
            return fallbackEncoder;
        }
        
        for (DeviceEncoder encoder : encoders) {
            if (encoder.canEncode(deviceType)) {
                log.debug("✅ Selected encoder: {} for device type: {}", 
                    encoder.getEncoderName(), deviceType);
                return encoder;
            }
        }
        
        log.warn("⚠️  No encoder found for device type: {}, using fallback", deviceType);
        return fallbackEncoder;
    }
}