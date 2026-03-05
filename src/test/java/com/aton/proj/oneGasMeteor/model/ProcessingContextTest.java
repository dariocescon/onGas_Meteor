package com.aton.proj.oneGasMeteor.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;

class ProcessingContextTest {

	@Test
	void testCreation() {
		ProcessingContext context = new ProcessingContext("192.168.1.1:12345");

		assertEquals("192.168.1.1:12345", context.getClientAddress());
		assertNotNull(context.getReceivedAt());
		assertTrue(context.isSuccess());
		assertEquals(-1, context.getMessageType());
		assertEquals(0, context.getPayloadLengthBytes());
	}

	@Test
	void testTimingMethods() throws InterruptedException {
		ProcessingContext context = new ProcessingContext("test");

		context.startRead();
		Thread.sleep(10);
		context.endRead();

		context.startDecode();
		Thread.sleep(10);
		context.endDecode();

		context.startDbSave();
		Thread.sleep(10);
		context.endDbSave();

		context.startCommandQuery();
		Thread.sleep(10);
		context.endCommandQuery();

		context.startCommandEncode();
		Thread.sleep(10);
		context.endCommandEncode();

		context.startSend();
		Thread.sleep(10);
		context.endSend();

		// Total processing time should be > 0
		assertTrue(context.getTotalProcessingTimeMs() > 0);
	}

	@Test
	void testCompleteSuccess() {
		ProcessingContext context = new ProcessingContext("test");
		context.complete(true, null);

		assertTrue(context.isSuccess());
		assertNull(context.getErrorMessage());
	}

	@Test
	void testCompleteError() {
		ProcessingContext context = new ProcessingContext("test");
		context.complete(false, "Connection timeout");

		assertFalse(context.isSuccess());
		assertEquals("Connection timeout", context.getErrorMessage());
	}

	@Test
	void testSetters() {
		ProcessingContext context = new ProcessingContext("test");

		context.setDeviceId("123456789012345");
		context.setDeviceType("TEK822V2");
		context.setMessageType(4);
		context.setPayloadLengthBytes(129);
		context.setDeclaredBodyLength(112);
		context.setMeasurementCount(28);
		context.setPendingCommandsFound(3);
		context.setCommandsSent(3);
		context.setResponseSizeBytes(256);

		assertEquals("123456789012345", context.getDeviceId());
		assertEquals("TEK822V2", context.getDeviceType());
		assertEquals(4, context.getMessageType());
		assertEquals(129, context.getPayloadLengthBytes());
		assertEquals(112, context.getDeclaredBodyLength());
		assertEquals(28, context.getMeasurementCount());
		assertEquals(3, context.getPendingCommandsFound());
		assertEquals(3, context.getCommandsSent());
		assertEquals(256, context.getResponseSizeBytes());
	}

	@Test
	void testExtractFromDecoded() {
		ProcessingContext context = new ProcessingContext("test");

		DecodedMessage decoded = new DecodedMessage();

		// Set unit info
		DecodedMessage.UnitInfo unitInfo = new DecodedMessage.UnitInfo();
		unitInfo.setProductType("TEK822V2");
		unitInfo.setFirmwareRevision("1.23");
		decoded.setUnitInfo(unitInfo);

		// Set IMEI
		DecodedMessage.UniqueIdentifier uid = new DecodedMessage.UniqueIdentifier();
		uid.setImei("123456789012345");
		decoded.setUniqueIdentifier(uid);

		// Set battery
		DecodedMessage.BatteryStatus battery = new DecodedMessage.BatteryStatus();
		battery.setBatteryVoltage("3.65");
		battery.setBatteryRemainingPercentage("87");
		decoded.setBatteryStatus(battery);

		// Set signal
		DecodedMessage.SignalStrength signal = new DecodedMessage.SignalStrength();
		signal.setRssi(-75);
		decoded.setSignalStrength(signal);

		// Set contact reason
		DecodedMessage.ContactReason cr = new DecodedMessage.ContactReason();
		cr.setScheduled(true);
		cr.setAlarm(false);
		cr.setManual(false);
		cr.setServerRequest(false);
		cr.setReboot(false);
		cr.setTspRequested(false);
		cr.setDynamic1(false);
		cr.setDynamic2(false);
		decoded.setContactReason(cr);

		context.extractFromDecoded(decoded);

		assertEquals("123456789012345", context.getDeviceId());
		assertEquals("TEK822V2", context.getDeviceType());
		assertEquals("1.23", context.toEntity().getFirmwareVersion());
		assertEquals("SCHEDULED", context.toEntity().getContactReason());
	}

	@Test
	void testExtractFromDecodedNull() {
		ProcessingContext context = new ProcessingContext("test");
		// Should not throw
		context.extractFromDecoded(null);
		assertNull(context.getDeviceId());
	}

	@Test
	void testExtractMultipleContactReasons() {
		ProcessingContext context = new ProcessingContext("test");

		DecodedMessage decoded = new DecodedMessage();
		DecodedMessage.ContactReason cr = new DecodedMessage.ContactReason();
		cr.setScheduled(true);
		cr.setAlarm(true);
		cr.setManual(false);
		cr.setServerRequest(false);
		cr.setReboot(false);
		cr.setTspRequested(false);
		cr.setDynamic1(false);
		cr.setDynamic2(false);
		decoded.setContactReason(cr);

		context.extractFromDecoded(decoded);

		String reason = context.toEntity().getContactReason();
		assertTrue(reason.contains("SCHEDULED"));
		assertTrue(reason.contains("ALARM"));
	}

	@Test
	void testToEntity() {
		ProcessingContext context = new ProcessingContext("10.0.0.1:9999");
		context.setDeviceId("123456789012345");
		context.setDeviceType("TEK822V2");
		context.setMessageType(4);
		context.setPayloadLengthBytes(129);
		context.setDeclaredBodyLength(112);
		context.setMeasurementCount(28);
		context.setPendingCommandsFound(2);
		context.setCommandsSent(2);
		context.setResponseSizeBytes(128);
		context.complete(true, null);

		ProcessingMetricsEntity entity = context.toEntity();

		assertEquals("123456789012345", entity.getDeviceId());
		assertEquals("TEK822V2", entity.getDeviceType());
		assertEquals(4, entity.getMessageType());
		assertEquals("10.0.0.1:9999", entity.getClientAddress());
		assertEquals(129, entity.getPayloadLengthBytes());
		assertEquals(112, entity.getDeclaredBodyLength());
		assertEquals(28, entity.getMeasurementCount());
		assertEquals(2, entity.getPendingCommandsFound());
		assertEquals(2, entity.getCommandsSent());
		assertEquals(128, entity.getResponseSizeBytes());
		assertTrue(entity.getSuccess());
		assertNull(entity.getErrorMessage());
		assertNotNull(entity.getReceivedAt());
		assertNotNull(entity.getCompletedAt());
		assertNotNull(entity.getTotalProcessingTimeMs());
		assertTrue(entity.getTotalProcessingTimeMs() >= 0);
	}

	@Test
	void testToEntityWithError() {
		ProcessingContext context = new ProcessingContext("test");
		context.complete(false, "Timeout: read timed out");

		ProcessingMetricsEntity entity = context.toEntity();

		assertFalse(entity.getSuccess());
		assertEquals("Timeout: read timed out", entity.getErrorMessage());
	}

	@Test
	void testBatteryParsingInvalidValue() {
		ProcessingContext context = new ProcessingContext("test");

		DecodedMessage decoded = new DecodedMessage();
		DecodedMessage.BatteryStatus battery = new DecodedMessage.BatteryStatus();
		battery.setBatteryVoltage("N/A");
		battery.setBatteryRemainingPercentage("invalid");
		decoded.setBatteryStatus(battery);

		// Should not throw - invalid values are ignored
		context.extractFromDecoded(decoded);
		assertNull(context.toEntity().getBatteryVoltage());
		assertNull(context.toEntity().getBatteryPercentage());
	}

	@Test
	void testSignalStrengthCsqPreferredOverRssi() {
		ProcessingContext context = new ProcessingContext("test");

		DecodedMessage decoded = new DecodedMessage();
		DecodedMessage.SignalStrength signal = new DecodedMessage.SignalStrength();
		signal.setCsq(15);
		signal.setRssi(-80);
		decoded.setSignalStrength(signal);

		context.extractFromDecoded(decoded);

		// CSQ has priority
		assertEquals(15, context.toEntity().getSignalStrength());
	}
}
