package com.aton.proj.oneGasMeteor.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.decoder.impl.Tek822Decoder;
import com.aton.proj.oneGasMeteor.decoder.impl.UnknownDeviceDecoder;

class DecoderFactoryTest {

	@Test
	void testDecoderFactorySelection() {
	    // Setup
	    DecoderFactory factory = new DecoderFactory(
	        List.of(new Tek822Decoder(), new UnknownDeviceDecoder()),
	        new UnknownDeviceDecoder()
	    );
	    
	    // Test 1: TEK822V1 (product type = 8)
	    byte[] tek822Payload = new byte[]{0x08, 0x01, (byte)0x81};
	    DeviceDecoder decoder1 = factory.getDecoder(tek822Payload);
	    assertEquals("Tek822Decoder", decoder1.getDecoderName());
	    
	    // Test 2: Device sconosciuto (product type = 255)
	    byte[] unknownPayload = new byte[]{(byte)0xFF, 0x01, (byte)0x81};
	    DeviceDecoder decoder2 = factory.getDecoder(unknownPayload);
	    assertEquals("UnknownDeviceDecoder", decoder2.getDecoderName());
	}

}
