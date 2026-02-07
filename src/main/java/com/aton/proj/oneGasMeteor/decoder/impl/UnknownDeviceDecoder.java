package com.aton.proj.oneGasMeteor.decoder.impl;

import com.aton.proj.oneGasMeteor.decoder.DeviceDecoder;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.TekMessage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Decoder fallback per device sconosciuti
 */
@Component
public class UnknownDeviceDecoder implements DeviceDecoder {
    
    @Override
    public boolean canDecode(byte[] payload) {
        // Questo decoder accetta sempre (è il fallback)
        return true;
    }
    
    @Override
    public DecodedMessage decode(TekMessage message) {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("UNKNOWN");
        
        // Salva almeno il raw payload
        byte[] payload = message.payload();
        if (payload != null && payload.length > 0) {
            decoded.getUnitInfo().setProductType("UNKNOWN_" + (payload[0] & 0xFF));
        }
        
        return decoded;
    }
    
    @Override
    public List<String> getSupportedDeviceTypes() {
        return Collections.singletonList("UNKNOWN");
    }
    
    @Override
    public String getDecoderName() {
        return "UnknownDeviceDecoder";
    }
}