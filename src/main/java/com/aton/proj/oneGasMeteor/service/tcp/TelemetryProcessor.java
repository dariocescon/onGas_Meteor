package com.aton.proj.oneGasMeteor.service.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aton.proj.oneGasMeteor.model.tcp.CommandResponse;
import com.aton.proj.oneGasMeteor.model.tcp.TelemetryMessage;

//@Service
public class TelemetryProcessor {

	private static final Logger log = LoggerFactory.getLogger(TelemetryProcessor.class);

	/**
	 * Processa il messaggio di telemetria e genera una risposta
	 */
	public CommandResponse process(TelemetryMessage message) {
		log.info("Processing telemetry from {}: {} bytes", message.getSourceAddress(), message.getPayload().length);
		log.debug("Hex data: {}", message.getHexData());

		// Esempio: analizza il messaggio e genera risposta
		String command = generateCommand(message);

		log.info("Generated command: {}", command);

		return new CommandResponse(command);
	}

	/**
	 * Logica di generazione comandi basata sulla telemetria ricevuta
	 */
	private String generateCommand(TelemetryMessage message) {
		byte[] data = message.getPayload();

		// Esempio: se il primo byte � 0x18 (24), risponde con TEK822,S0=84
		if (data.length > 0 && data[0] == 0x18) {
			return "TEK822,S0=84";
		}

		// Esempio: estrai informazioni dal payload
		if (data.length >= 4) {
			int value = data[3] & 0xFF;
			return String.format("TEK822,S0=%d", value);
		}

		// Default fallback
		return "TEK822,OK";
	}
}