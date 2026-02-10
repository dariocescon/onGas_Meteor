package com.aton.proj.oneGasMeteor.decoder;

import java.util.List;

import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.tcp.TelemetryMessage;

/**
 * Interface per decoder di device specifici
 */
public interface DeviceDecoder {
    
    /**
     * Verifica se questo decoder può gestire il messaggio
     * @param payload Prime bytes del messaggio
     * @return true se può decodificare
     */
    boolean canDecode(byte[] payload);
    
    /**
     * Decodifica il messaggio raw
     * @param message Messaggio da decodificare
     * @return Dati decodificati
     */
    DecodedMessage decode(TelemetryMessage message);
    
    /**
     * Ritorna i tipi di device supportati da questo decoder
     * @return Lista di device types (es. "TEK822V1", "TEK822V2")
     */
    List<String> getSupportedDeviceTypes();
    
    /**
     * Nome del decoder
     * @return Nome identificativo
     */
    String getDecoderName();
}