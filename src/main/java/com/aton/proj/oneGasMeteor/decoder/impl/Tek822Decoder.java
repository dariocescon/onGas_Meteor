package com.aton.proj.oneGasMeteor.decoder.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.aton.proj.oneGasMeteor.decoder.DeviceDecoder;
import com.aton.proj.oneGasMeteor.decoder.TekMessageDecoder;
import com.aton.proj.oneGasMeteor.exception.DecodingException;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryMessage;

/**
 * Decoder per dispositivi Tekelek famiglia TEK822 e compatibili
 */
@Component
@Order(1)
public class Tek822Decoder implements DeviceDecoder {
    
    private final TekMessageDecoder telemetryMessageDecoder = new TekMessageDecoder();
    
    // Product type codes supportati da questo decoder
    private static final List<Integer> SUPPORTED_PRODUCT_CODES = Arrays.asList(
        2,   // TEK586
        5,   // TEK733
        6,   // TEK643
        7,   // TEK811
        8,   // TEK822V1
        9,   // TEK733A
        10,  // TEK871
        11,  // TEK811A
        23,  // TEK822V1BTN
        24,  // TEK822V2
        25,  // TEK900
        26,  // TEK880
        27,  // TEK898V2
        28   // TEK898V1
    );
    
    @Override
    public boolean canDecode(byte[] payload) {
        if (payload == null || payload.length < 1) {
            return false;
        }
        
        int productType = payload[0] & 0xFF;
        return SUPPORTED_PRODUCT_CODES.contains(productType);
    }
    
    @Override
    public DecodedMessage decode(TelemetryMessage message) {
        try {
            return telemetryMessageDecoder.decode(message);
        } catch (Exception e) {
            throw new DecodingException("Failed to decode message", e);
        }
    }
    
    @Override
    public List<String> getSupportedDeviceTypes() {
        return Arrays.asList(
            "TEK586", "TEK733", "TEK643", "TEK811", "TEK822V1", 
            "TEK733A", "TEK871", "TEK811A", "TEK822V1BTN", "TEK822V2",
            "TEK900", "TEK880", "TEK898V2", "TEK898V1"
        );
    }
    
    @Override
    public String getDecoderName() {
        return "Tek822Decoder";
    }
}