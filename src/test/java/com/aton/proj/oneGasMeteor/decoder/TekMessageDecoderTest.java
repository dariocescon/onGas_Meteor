package com.aton.proj.oneGasMeteor.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryMessage;
import com.aton.proj.oneGasMeteor.utils.ControllerUtils;

public class TekMessageDecoderTest {

	@Test
	void testDecodeDecodeProductType_1() {

		String hexString = "180A640188117C0862406075927406047B0078773652FF84002100721E31000161E0860000112233445047B00005200002C84013000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000F594A4E29F30A5029F30A5029F30A5229F30A5029F30A5029F30A4E29F30A5029F30A5029F30A5029F30A5029F30A5429F30A5629F30A5A29F30A5629F30A5229F30A4E29F30A4829F30A4629F30A4229F30A4229F30000000000000000000000000000000000000000000000000000000055F7";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeProductType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("TEK822V2", decoded.getUnitInfo().getProductType());
	}

	@Test
	void testDecodeDecodeProductType_2() {

		String hexString = "18020344891936086443104798705410462C38393838323830363636303031303637353334382C3435323237332C302C38302C323335352C31382C35383937342C3732362C362C33393439362C313639362C36302C7EE0";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeProductType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("TEK822V2", decoded.getUnitInfo().getProductType());
	}

	@Test
	void testDecodeVersions_1() {

		String hexString = "180A640188117C0862406075927406047B0078773652FF84002100721E31000161E0860000112233445047B00005200002C84013000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000F594A4E29F30A5029F30A5029F30A5229F30A5029F30A5029F30A4E29F30A5029F30A5029F30A5029F30A5029F30A5429F30A5629F30A5A29F30A5629F30A5229F30A4E29F30A4829F30A4629F30A4229F30A4229F30000000000000000000000000000000000000000000000000000000055F7";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeVersions", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("2.1", decoded.getUnitInfo().getHardwareRevision());
		assertEquals("4.3", decoded.getUnitInfo().getFirmwareRevision());
	}

	@Test
	void testDecodeVersions_2() {

		String hexString = "180203418919360864431047987054047B093248000BFF8103000A682BFE0A682BFE0A6A2BFE0A6A2BFE0A6A2BFE0A6A28000A6A2BFE0A6A28430A6A281E0A6A28620A6A2B700A6A2BFE000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002F1D";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeVersions", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("2.0", decoded.getUnitInfo().getHardwareRevision());
		assertEquals("3.0", decoded.getUnitInfo().getFirmwareRevision());
	}

	@Test
	void testDecodeImei_1() {

		String hexString = "180203418919360864431047987054047B093248000BFF8103000A682BFE0A682BFE0A6A2BFE0A6A2BFE0A6A2BFE0A6A28000A6A2BFE0A6A28430A6A281E0A6A28620A6A2B700A6A2BFE000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002F1D";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeImei", byte[].class, DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("864431047987054", decoded.getUniqueIdentifier().getImei());
	}

	@Test
	void testDecodeImei_2() {

		String hexString = "180A640188117C0862406075927406047B0078773652FF84002100721E31000161E0860000112233445047B00005200002C84013000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000F594A4E29F30A5029F30A5029F30A5229F30A5029F30A5029F30A4E29F30A5029F30A5029F30A5029F30A5029F30A5429F30A5629F30A5A29F30A5629F30A5229F30A4E29F30A4829F30A4629F30A4229F30A4229F30000000000000000000000000000000000000000000000000000000055F7";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeImei", byte[].class, DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("862406075927406", decoded.getUniqueIdentifier().getImei());
	}

	@Test
	void testDecodeMessageType_shouldBeType4_1() {

		String hexString = "180203418919360864431047987054047B093248000BFF8103000A682BFE0A682BFE0A6A2BFE0A6A2BFE0A6A2BFE0A6A28000A6A2BFE0A6A28430A6A281E0A6A28620A6A2B700A6A2BFE000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002F1D";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMessageType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("4", decoded.getMessageType());
	}

	@Test
	void testDecodeMessageType_shouldBeType4_2() {

		String hexString = "180A640188117C0862406075927406047B0078773652FF84002100721E31000161E0860000112233445047B00005200002C84013000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000F594A4E29F30A5029F30A5029F30A5229F30A5029F30A5029F30A4E29F30A5029F30A5029F30A5029F30A5029F30A5429F30A5629F30A5A29F30A5629F30A5229F30A4E29F30A4829F30A4629F30A4229F30A4229F30000000000000000000000000000000000000000000000000000000055F7";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMessageType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("4", decoded.getMessageType());
	}

	@Test
	void testDecodeMessageType_shouldBeType8() {

		String hexString = "180203428918360864431047987054087B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A2800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000294A";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMessageType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("8", decoded.getMessageType());
	}

	@Test
	void testDecodeMessageType_shouldBeType9() {

		String hexString = "180203428918360864431047987054097B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A280000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004ED";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMessageType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("9", decoded.getMessageType());
	}

	@Test
	void testDecodeMessageType_shouldBeType6() {

		String hexString = "18020344891836086443104798705406DF53303D38312C53313D30302C53323D3746323030302C53333D38322C53343D464645382C53353D374331342C53363D303030302C53373D30302C53383D30302C53393D2C5331303D2C5331313D2C5331323D696F742E316E63652E6E65742C5331333D2C5331343D2C5331353D3137332E3231322E3231352E3133312C5331363D393030322C5331373D373230302C5331383D43382C5331393D30362C5332303D30302C5332313D2C5332323D2C5332333D31312C5332343D30302C5332353D30302C5332363D38302C5332373D38382C5332383D2C5332393D30302CF83B";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMessageType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("6", decoded.getMessageType());
	}

	@Test
	void testDecodeMessageType_shouldBeType16() {

		String hexString = "18020344891936086443104798705410462C38393838323830363636303031303637353334382C3435323237332C302C38302C323335352C31382C35383937342C3732362C362C33393439362C313639362C36302C7EE0";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMessageType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("16", decoded.getMessageType());
	}

	@Test
	void testDecodeMessageType_shouldBeType17() {

		String hexString = "18038204881160086443104798705411492C39352C3133343434322E302C353235352E393935304E2C30303833322E34343137572C312E392C3132372E382C322C302E30302C302E302C302E302C3032313031352C30342C8843";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMessageType", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("17", decoded.getMessageType());
	}

	@Test
	void testDecodeContactReason_1() {

		String hexString = "180203428918360864431047987054097B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A280000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004ED";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeContactReason", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals(Boolean.FALSE, decoded.getContactReason().getScheduled());
		assertEquals(Boolean.TRUE, decoded.getContactReason().getAlarm());
		assertEquals(Boolean.FALSE, decoded.getContactReason().getServerRequest());
		assertEquals(Boolean.FALSE, decoded.getContactReason().getManual());
		assertEquals(Boolean.FALSE, decoded.getContactReason().getReboot());
		assertEquals(Boolean.FALSE, decoded.getContactReason().getTspRequested());
		assertEquals(Boolean.TRUE, decoded.getContactReason().getDynamic1());
		assertEquals(Boolean.FALSE, decoded.getContactReason().getDynamic2());

	}

	@Test
	void testDecodeAlarmStatus_1() {

		String hexString = "180203428918360864431047987054097B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A280000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004ED";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeAlarmStatus", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals(Boolean.TRUE, decoded.getAlarmStatus().getActive());
		assertEquals(Boolean.TRUE, decoded.getAlarmStatus().getBund());
		assertEquals(Boolean.TRUE, decoded.getAlarmStatus().getStatic1());
		assertEquals(Boolean.FALSE, decoded.getAlarmStatus().getStatic2());
		assertEquals(Boolean.FALSE, decoded.getAlarmStatus().getStatic3());

	}

	@Test
	void testDecodeSignalStrength_1() {

		String hexString = "180203428918360864431047987054097B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A280000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004ED";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeSignalStrength", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals(24, decoded.getSignalStrength().getCsq());
		assertEquals(null, decoded.getSignalStrength().getRssi());

	}

	@Test
	void testDecodeDiagnosticInfo_1() {

		String hexString = "180203428918360864431047987054097B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A280000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004ED";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeDiagnosticInfo", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals(Boolean.FALSE, decoded.getDiagnosticInfo().getLteActive());
		assertEquals(Boolean.TRUE, decoded.getDiagnosticInfo().getRtcSet());

	}

	@Test
	void testDecodeDiagnosticInfo_2() {

		String hexString = "180203428918760864431047987054097B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A280000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004ED";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeDiagnosticInfo", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals(Boolean.TRUE, decoded.getDiagnosticInfo().getLteActive());
		assertEquals(Boolean.TRUE, decoded.getDiagnosticInfo().getRtcSet());

	}

	@Test
	void testDecodeBatteryStatus_1() {

		String hexString = "180203428918760864431047987054097B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A280000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004ED";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeBatteryStatus", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals("70.97", decoded.getBatteryStatus().getBatteryRemainingPercentage());
		assertEquals(null, decoded.getBatteryStatus().getBatteryVoltage());

	}

	@Test
	void testDecodeDiagnosticData_1() {

		String hexString = "180203428918360864431047987054087B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A2800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000294A";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();
		decoded.getUnitInfo().setFirmwareRevision("3.1");

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeDiagnosticData", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals(2353, decoded.getDiagnosticInfo().getMessageCount());
		assertEquals(2, decoded.getDiagnosticInfo().getTryAttemptsRemaining());
		assertEquals(8, decoded.getDiagnosticInfo().getEnergyUsedLastContactMaSeconds());
		assertEquals(null, decoded.getDiagnosticInfo().getLastErrorCode());
	}

	@Test
	void testDecodeDiagnosticData_2() {

		String hexString = "180203428918360864431047987054087B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A2800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000294A";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();
		decoded.getUnitInfo().setFirmwareRevision("2.9");

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeDiagnosticData", byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			method.invoke(decoder, payload, decoded);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Reflection error: " + e.getMessage());
		}

		assertEquals(2353, decoded.getDiagnosticInfo().getMessageCount());
		assertEquals(2, decoded.getDiagnosticInfo().getTryAttemptsRemaining());
		assertEquals(null, decoded.getDiagnosticInfo().getEnergyUsedLastContactMaSeconds());
		assertEquals(8, decoded.getDiagnosticInfo().getLastErrorCode());
	}

	@Test
	void testDecodeMeasurementData_1() {

		String hexString = "180203428918360864431047987054087B0931470008FF810F000A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A2800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000294A";
		byte[] payload = ControllerUtils.hexStringToByteArray(hexString);

		TekMessageDecoder decoder = new TekMessageDecoder();
		DecodedMessage decoded = new DecodedMessage();

		// Crea TelemetryMessage (richiesto dal metodo)
		TelemetryMessage msg = new TelemetryMessage(payload, hexString, LocalDateTime.now(), "test");
		int msgType = payload[15] & 0x3F; // = 8

		// Use reflection to access the private method
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("decodeMeasurementData", TelemetryMessage.class,
					byte[].class, DecodedMessage.class, int.class);
			method.setAccessible(true);
			method.invoke(decoder, msg, payload, decoded, msgType);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Reflection error: " + e.getMessage());
		}

		// --- Logger Speed ---
		assertEquals(15, decoded.getUnitSetup().getLoggerSpeedMinutes());

		// --- Numero misurazioni (10 valide, le restanti 18 sono 00000000) ---
		var measurements = decoded.getMeasurementData();
		assertNotNull(measurements);
		assertEquals(10, measurements.size());

		// --- Data 0: 0A 6A 2B FE ---
		var m0 = measurements.get(0);
		assertEquals("Data 0", m0.getMeasurementNum());
		assertEquals(1022, m0.getDistanceCm());
		assertEquals(1022, m0.getMeasuredValue());
		assertEquals(10, m0.getSonicRssi());
		assertEquals(10, m0.getSonicSrc());
		assertEquals(23.0, m0.getTemperatureC(), 0.01);
		assertEquals("73.40", m0.getTemperatureF());
		assertNull(m0.getPercentageFull());
		assertNull(m0.getPayloadValue());

		// --- Data 1: 0A 6A 28 00 ---
		var m1 = measurements.get(1);
		assertEquals("Data 1", m1.getMeasurementNum());
		assertEquals(0, m1.getDistanceCm());
		assertEquals(0, m1.getMeasuredValue());
		assertEquals(10, m1.getSonicRssi());
		assertEquals(10, m1.getSonicSrc());
		assertEquals(23.0, m1.getTemperatureC(), 0.01);
		assertEquals("73.40", m1.getTemperatureF());

		// --- Data 2: 0A 6A 2B FE (uguale a Data 0) ---
		var m2 = measurements.get(2);
		assertEquals("Data 2", m2.getMeasurementNum());
		assertEquals(1022, m2.getDistanceCm());
		assertEquals(10, m2.getSonicSrc());

		// --- Data 3: 0A 6A 28 43 ---
		var m3 = measurements.get(3);
		assertEquals("Data 3", m3.getMeasurementNum());
		assertEquals(67, m3.getDistanceCm());
		assertEquals(67, m3.getMeasuredValue());
		assertEquals(10, m3.getSonicRssi());
		assertEquals(10, m3.getSonicSrc());
		assertEquals(23.0, m3.getTemperatureC(), 0.01);
		assertEquals("73.40", m3.getTemperatureF());

		// --- Data 4-8: tutti uguali a Data 3 (0A 6A 28 43) ---
		for (int i = 4; i <= 8; i++) {
			var mi = measurements.get(i);
			assertEquals("Data " + i, mi.getMeasurementNum());
			assertEquals(67, mi.getDistanceCm());
			assertEquals(23.0, mi.getTemperatureC(), 0.01);
			assertEquals(10, mi.getSonicRssi());
			assertEquals(10, mi.getSonicSrc());
		}

		// --- Data 9: 0A 6A 28 00 (uguale a Data 1) ---
		var m9 = measurements.get(9);
		assertEquals("Data 9", m9.getMeasurementNum());
		assertEquals(0, m9.getDistanceCm());
		assertEquals(23.0, m9.getTemperatureC(), 0.01);

		// --- Timestamp: ogni misura distanziata di 15 minuti dalla precedente ---
		for (int i = 1; i < measurements.size(); i++) {
			Instant t_prev = Instant.parse(measurements.get(i - 1).getTimestamp());
			Instant t_curr = Instant.parse(measurements.get(i).getTimestamp());
			System.out.println(t_prev + " - " + t_curr);
			long diffMinutes = (t_prev.toEpochMilli() - t_curr.toEpochMilli()) / 60000;
			assertEquals(15, diffMinutes, "Differenza tra Data " + (i - 1) + " e Data " + i + " deve essere 15 min");
		}
	}

	/**
	 * Helper: invoca calculateLoggerSpeed tramite reflection. Ritorna il valore in
	 * millisecondi.
	 */
	private long invokeCalculateLoggerSpeed(int msgType, byte[] payload, DecodedMessage decoded) {
		TekMessageDecoder decoder = new TekMessageDecoder();
		try {
			var method = TekMessageDecoder.class.getDeclaredMethod("calculateLoggerSpeed", int.class, byte[].class,
					DecodedMessage.class);
			method.setAccessible(true);
			return (long) method.invoke(decoder, msgType, payload, decoded);
		} catch (Exception e) {
			fail("Reflection error: " + e.getMessage());
			return -1; // mai raggiunto
		}
	}

	/**
	 * Helper: crea un payload di almeno 24 byte con byte[23] impostato al valore
	 * desiderato.
	 */
	private byte[] createPayloadWithByte23(byte value) {
		byte[] payload = new byte[24];
		payload[23] = value;
		return payload;
	}

	// --- msgType 8: manual=true → 1 secondo (1000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type8_manual() {
		byte[] payload = createPayloadWithByte23((byte) 0x80);
		DecodedMessage decoded = new DecodedMessage();
		decoded.getContactReason().setManual(Boolean.TRUE);

		long result = invokeCalculateLoggerSpeed(8, payload, decoded);
		assertEquals(1000L, result, "Type 8 con manual=true deve essere 1 secondo (1000 ms)");
	}

	// --- msgType 8: manual=false, byte[23] bit7=0 → 1 minuto (60000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type8_notManual_bit7off() {
		byte[] payload = createPayloadWithByte23((byte) 0x00);
		DecodedMessage decoded = new DecodedMessage();
		decoded.getContactReason().setManual(Boolean.FALSE);

		long result = invokeCalculateLoggerSpeed(8, payload, decoded);
		assertEquals(60000L, result, "Type 8 con manual=false e bit7=0 deve essere 1 minuto (60000 ms)");
	}

	// --- msgType 8: manual=false, byte[23] bit7=1 → 15 minuti (900000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type8_notManual_bit7on() {
		byte[] payload = createPayloadWithByte23((byte) 0x80);
		DecodedMessage decoded = new DecodedMessage();
		decoded.getContactReason().setManual(Boolean.FALSE);

		long result = invokeCalculateLoggerSpeed(8, payload, decoded);
		assertEquals(900000L, result, "Type 8 con manual=false e bit7=1 deve essere 15 minuti (900000 ms)");
	}

	// --- msgType 9: byte[23] bit7=0 → 1 minuto (60000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type9_bit7off() {
		byte[] payload = createPayloadWithByte23((byte) 0x00);
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(9, payload, decoded);
		assertEquals(60000L, result, "Type 9 con bit7=0 deve essere 1 minuto (60000 ms)");
	}

	// --- msgType 9: byte[23] bit7=1 → 15 minuti (900000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type9_bit7on() {
		byte[] payload = createPayloadWithByte23((byte) 0x80);
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(9, payload, decoded);
		assertEquals(900000L, result, "Type 9 con bit7=1 deve essere 15 minuti (900000 ms)");
	}

	// --- msgType 4: lower7bits=0, bit7=0 → 1 minuto (60000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type4_zero_bit7off() {
		byte[] payload = createPayloadWithByte23((byte) 0x00);
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(4, payload, decoded);
		assertEquals(60000L, result, "Type 4 con valore=0 e bit7=0 deve essere 1 minuto (60000 ms)");
	}

	// --- msgType 4: lower7bits=0, bit7=1 → 15 minuti (900000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type4_zero_bit7on() {
		byte[] payload = createPayloadWithByte23((byte) 0x80);
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(4, payload, decoded);
		assertEquals(900000L, result, "Type 4 con valore=0 e bit7=1 deve essere 15 minuti (900000 ms)");
	}

	// --- msgType 4: lower7bits=1 → 1 × 15 min = 15 minuti (900000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type4_value1() {
		byte[] payload = createPayloadWithByte23((byte) 0x01); // lower7=1, bit7=0
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(4, payload, decoded);
		assertEquals(900000L, result, "Type 4 con valore=1 deve essere 1 × 15 min = 900000 ms");
	}

	// --- msgType 4: lower7bits=2 → 2 × 15 min = 30 minuti (1800000 ms) ---
	@Test
	void testCalculateLoggerSpeed_type4_value2() {
		byte[] payload = createPayloadWithByte23((byte) 0x02); // lower7=2, bit7=0
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(4, payload, decoded);
		assertEquals(1800000L, result, "Type 4 con valore=2 deve essere 2 × 15 min = 1800000 ms");
	}

	// --- msgType 4: lower7bits=4 (con bit7=1) → 4 × 15 min = 60 minuti (3600000
	// ms) ---
	@Test
	void testCalculateLoggerSpeed_type4_value4_bit7on() {
		byte[] payload = createPayloadWithByte23((byte) 0x84); // lower7=4, bit7=1
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(4, payload, decoded);
		assertEquals(3600000L, result, "Type 4 con valore=4 (bit7 ignorato) deve essere 4 × 15 min = 3600000 ms");
	}

	// --- msgType 6 (default) → 15 minuti (900000 ms) ---
	@Test
	void testCalculateLoggerSpeed_defaultType() {
		byte[] payload = createPayloadWithByte23((byte) 0x00);
		DecodedMessage decoded = new DecodedMessage();

		long result = invokeCalculateLoggerSpeed(6, payload, decoded);
		assertEquals(900000L, result, "Tipo non gestito (6) deve restituire il default 15 min (900000 ms)");
	}
}