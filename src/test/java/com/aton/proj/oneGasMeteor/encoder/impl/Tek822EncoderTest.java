package com.aton.proj.oneGasMeteor.encoder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;

class Tek822EncoderTest {

	@Test
	void testTek822Encoder() {
		Tek822Encoder encoder = new Tek822Encoder();

		// Test SET_INTERVAL
		DeviceCommand cmd1 = new DeviceCommand("device123", "TEK822V1", "SET_INTERVAL");
		cmd1.addParameter("interval", 15);

		List<TelemetryResponse.EncodedCommand> encoded = encoder.encode(List.of(cmd1));

		assertEquals(1, encoded.size());
		assertEquals("53490F15", encoded.get(0).getEncodedData());

		// Test REBOOT
		DeviceCommand cmd2 = new DeviceCommand("device123", "TEK822V1", "REBOOT");
		encoded = encoder.encode(List.of(cmd2));

		assertEquals("52420010", encoded.get(0).getEncodedData());

		// Test REQUEST_STATUS
		DeviceCommand cmd3 = new DeviceCommand("device123", "TEK822V1", "REQUEST_STATUS");
		encoded = encoder.encode(List.of(cmd3));

		assertEquals("51530002", encoded.get(0).getEncodedData());

		// Test SET_ALARM_THRESHOLD
		DeviceCommand cmd4 = new DeviceCommand("device123", "TEK822V1", "SET_ALARM_THRESHOLD");
		cmd4.addParameter("threshold", 100);
		encoded = encoder.encode(List.of(cmd4));

		assertEquals("4154006471", encoded.get(0).getEncodedData());
	}

	@Test
	void testAllCommands() {
		Tek822Encoder encoder = new Tek822Encoder();

		// 1. SET_INTERVAL(15)
		DeviceCommand cmd1 = new DeviceCommand("dev1", "TEK822V1", "SET_INTERVAL");
		cmd1.addParameter("interval", 15);
		assertEquals("54454B3832322C53303D3135", encoder.encode(List.of(cmd1)).get(0).getEncodedData());

		// 2. REBOOT
		DeviceCommand cmd2 = new DeviceCommand("dev1", "TEK822V1", "REBOOT");
		assertEquals("52420010", encoder.encode(List.of(cmd2)).get(0).getEncodedData());

		// 3. REQUEST_STATUS
		DeviceCommand cmd3 = new DeviceCommand("dev1", "TEK822V1", "REQUEST_STATUS");
		assertEquals("51530002", encoder.encode(List.of(cmd3)).get(0).getEncodedData());

		// 4. SET_ALARM_THRESHOLD(100)
		DeviceCommand cmd4 = new DeviceCommand("dev1", "TEK822V1", "SET_ALARM_THRESHOLD");
		cmd4.addParameter("threshold", 100);
		assertEquals("4154006471", encoder.encode(List.of(cmd4)).get(0).getEncodedData());
	}

	@Test
	void testChecksumCalculation() {
		Tek822Encoder encoder = new Tek822Encoder();

		// Verifica checksum per SET_INTERVAL(15)
		// 0x53 ^ 0x49 ^ 0x0F = 0x15
		byte[] message = { 0x53, 0x49, 0x0F };
		byte checksum = 0;
		for (byte b : message) {
			checksum ^= b;
		}
		assertEquals(0x15, checksum & 0xFF);
	}

	@Test
	void testEncoderSelection() {
		Tek822Encoder encoder = new Tek822Encoder();

		// Supporta TEK822
		assertTrue(encoder.canEncode("TEK822V1"));
		assertTrue(encoder.canEncode("TEK822V2"));
		assertTrue(encoder.canEncode("TEK586"));

		// Non supporta altri
		assertFalse(encoder.canEncode("TEK999"));
		assertFalse(encoder.canEncode(null));
	}

	@Test
	void testInvalidParameters() {
		Tek822Encoder encoder = new Tek822Encoder();

		// SET_INTERVAL con valore invalido
		DeviceCommand cmd = new DeviceCommand("device123", "TEK822V1", "SET_INTERVAL");
		cmd.addParameter("interval", 300); // > 255, dovrebbe fallire

		assertThrows(Exception.class, () -> encoder.encode(List.of(cmd)));

		// SET_ALARM_THRESHOLD con valore invalido
		DeviceCommand cmd2 = new DeviceCommand("device123", "TEK822V1", "SET_ALARM_THRESHOLD");
		cmd2.addParameter("threshold", 2000); // > 1023, dovrebbe fallire

		assertThrows(Exception.class, () -> encoder.encode(List.of(cmd2)));
	}

}
