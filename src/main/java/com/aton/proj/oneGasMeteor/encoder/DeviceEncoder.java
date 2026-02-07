package com.aton.proj.oneGasMeteor.encoder;

import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;

import java.util.List;

/**
 * Interface per encoder di comandi specifici per device
 */
public interface DeviceEncoder {
    
    /**
     * Verifica se questo encoder può gestire il device type
     * @param deviceType Tipo di device (es. "TEK822V1")
     * @return true se può codificare
     */
    boolean canEncode(String deviceType);
    
    /**
     * Codifica una lista di comandi in formato device-specific
     * @param commands Lista di comandi da codificare
     * @return Lista di comandi codificati pronti per essere inviati
     */
    List<TelemetryResponse.EncodedCommand> encode(List<DeviceCommand> commands);
    
    /**
     * Ritorna i tipi di device supportati da questo encoder
     * @return Lista di device types
     */
    List<String> getSupportedDeviceTypes();
    
    /**
     * Nome dell'encoder
     * @return Nome identificativo
     */
    String getEncoderName();
}