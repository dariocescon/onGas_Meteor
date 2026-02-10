package com.aton.proj.oneGasMeteor.decoder;

import com.aton.proj.oneGasMeteor.decoder.impl.UnknownDeviceDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory per selezionare il decoder appropriato
 */
@Component
public class DecoderFactory {

	private static final Logger log = LoggerFactory.getLogger(DecoderFactory.class);

	private final List<DeviceDecoder> decoders;
	private final UnknownDeviceDecoder fallbackDecoder;

	public DecoderFactory(List<DeviceDecoder> decoders, UnknownDeviceDecoder fallbackDecoder) {
		this.decoders = decoders;
		this.fallbackDecoder = fallbackDecoder;

		log.info("🔧 DecoderFactory initialized with {} decoders", decoders.size());
		decoders.forEach(decoder -> log.info("  {} supports: {}", decoder.getDecoderName(),
				decoder.getSupportedDeviceTypes()));
	}

	/**
	 * Seleziona il decoder appropriato per il payload
	 */
	public DeviceDecoder getDecoder(byte[] payload) {
		if (payload == null || payload.length == 0) {
			log.warn("⚠️  Empty payload, using fallback decoder");
			return fallbackDecoder;
		}

		for (DeviceDecoder decoder : decoders) {
			if (decoder.canDecode(payload)) {
				log.debug("  Selected decoder: {} for product type: {}", decoder.getDecoderName(), payload[0] & 0xFF);
				return decoder;
			}
		}

		log.warn("  No decoder found for product type: {}, using fallback", payload[0] & 0xFF);
		return fallbackDecoder;
	}
}