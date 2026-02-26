package com.aton.proj.oneGasMeteor.encoder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.utils.ControllerUtils;

class Tek822EncoderTest {

	private final Tek822Encoder encoder = new Tek822Encoder();

	// ====================== Helper ======================

	/**
	 * Verifica che la stringa HEX sia la corretta conversione ASCII-to-HEX della
	 * stringa ASCII attesa.
	 */
	private void assertHexMatchesAscii(String expectedAscii, String actualHex) {
		StringBuilder expected = new StringBuilder();
		for (byte b : expectedAscii.getBytes(StandardCharsets.US_ASCII)) {
			expected.append(String.format("%02X", b));
		}
		assertEquals(expected.toString(), actualHex,
				"HEX dovrebbe essere la conversione ASCII-to-HEX di: " + expectedAscii);
	}

	/**
	 * Codifica un singolo comando e ritorna l'EncodedCommand. Verifica anche che il
	 * risultato contenga esattamente 1 comando.
	 */
	private TelemetryResponse.EncodedCommand encodeSingle(DeviceCommand cmd) {
		List<TelemetryResponse.EncodedCommand> result = encoder.encode(List.of(cmd));
		assertEquals(1, result.size());
		return result.get(0);
	}

	// ====================== canEncode ======================

	@Test
	void testCanEncode_supportedDevices() {
		assertTrue(encoder.canEncode("TEK822V1"));
		assertTrue(encoder.canEncode("TEK822V2"));
		assertTrue(encoder.canEncode("TEK586"));
		assertTrue(encoder.canEncode("TEK733"));
		assertTrue(encoder.canEncode("TEK643"));
		assertTrue(encoder.canEncode("TEK811"));
		assertTrue(encoder.canEncode("TEK733A"));
		assertTrue(encoder.canEncode("TEK871"));
		assertTrue(encoder.canEncode("TEK811A"));
		assertTrue(encoder.canEncode("TEK822V1BTN"));
		assertTrue(encoder.canEncode("TEK900"));
		assertTrue(encoder.canEncode("TEK880"));
		assertTrue(encoder.canEncode("TEK898V2"));
		assertTrue(encoder.canEncode("TEK898V1"));
	}

	@Test
	void testCanEncode_unsupportedDevices() {
		assertFalse(encoder.canEncode("TEK999"));
		assertFalse(encoder.canEncode("UNKNOWN"));
		assertFalse(encoder.canEncode(null));
		assertFalse(encoder.canEncode(""));
	}

	// ====================== asciiToHex (verifica indiretta) ======================

	@Test
	void testAsciiToHex_viaReboot() {
		// Verifica che la conversione ASCII→HEX sia coerente
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REBOOT");
		encodeSingle(cmd);
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SET_INTERVAL (S0) ======================
	// Formula PDF: S0 = (128 × B) + (A × 4)
	// A = logger speed in ore (incrementi di 0.25h)
	// B = sampling period (0=1min, 1=15min)

	@Test
	void testSetInterval_15hours_sampling15min() {
		// S0 = (128 * 1) + (15 * 4) = 188 = 0xBC
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_INTERVAL");
		cmd.addParameter("interval", 15);

		encodeSingle(cmd);

		assertEquals("TEK822,S0=BC", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetInterval_1hour_sampling15min() {
		// S0 = (128 * 1) + (1 * 4) = 132 = 0x84
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_INTERVAL");
		cmd.addParameter("interval", 1);

		encodeSingle(cmd);

		assertEquals("TEK822,S0=84", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetInterval_8hours_sampling1min() {
		// S0 = (128 * 0) + (8 * 4) = 32 = 0x20
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_INTERVAL");
		cmd.addParameter("interval", 8);
		cmd.addParameter("samplingPeriod", 0);

		encodeSingle(cmd);

		assertEquals("TEK822,S0=20", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetInterval_quarterHour_sampling1min() {
		// S0 = (128 * 0) + (0.25 * 4) = 1 = 0x01
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_INTERVAL");
		cmd.addParameter("interval", 0.25);
		cmd.addParameter("samplingPeriod", 0);

		encodeSingle(cmd);

		assertEquals("TEK822,S0=01", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetInterval_customPassword() {
		// Verifica che la password custom venga usata al posto di quella di default
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_INTERVAL");
		cmd.addParameter("interval", 1);
		cmd.addParameter("password", "MYPASS");

		encodeSingle(cmd);

		assertEquals("MYPASS,S0=84", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SET_LISTEN (S1) ======================
	// Formula PDF: S1 = listenMinutes / 5

	@Test
	void testSetListen_5min() {
		// S1 = 5 / 5 = 1 = 0x01
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_LISTEN");
		cmd.addParameter("listenMinutes", 5);

		encodeSingle(cmd);

		assertEquals("TEK822,S1=01", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetListen_30min() {
		// S1 = 30 / 5 = 6 = 0x06
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_LISTEN");
		cmd.addParameter("listenMinutes", 30);

		encodeSingle(cmd);

		assertEquals("TEK822,S1=06", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SET_SCHEDULE (S2) ======================

	@Test
	void testSetSchedule_default() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_SCHEDULE");

		encodeSingle(cmd);

		assertEquals("TEK822,S2=7F0038", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetSchedule_custom() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_SCHEDULE");
		cmd.addParameter("schedule", "7F2056");

		encodeSingle(cmd);

		assertEquals("TEK822,S2=7F2056", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== REBOOT (R3=ACTIVE) ======================

	@Test
	void testReboot() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REBOOT");

		encodeSingle(cmd);

		assertEquals("TEK822,R3=ACTIVE", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== REQUEST_STATUS (R6=02) ======================

	@Test
	void testRequestStatus() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REQUEST_STATUS");

		encodeSingle(cmd);

		assertEquals("TEK822,R6=02", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SET_ALARM_THRESHOLD (S4/S5/S6) ======================
	// Formula PDF: S4 = D + C×(2^10) + B×(2^14) + A×(2^15)
	// D = threshold (10 bit), C = hysteresis (4 bit), B = enabled, A = polarity

	@Test
	void testSetAlarmThreshold_defaults() {
		// threshold=100, hysteresis=10(def), enabled=true(def), polarity=true(def)
		// S4 = 100 + (10 * 1024) + (1 * 16384) + (1 * 32768) = 59492 = 0xE864
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");
		cmd.addParameter("threshold", 100);

		encodeSingle(cmd);

		assertEquals("TEK822,S4=E864", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetAlarmThreshold_s5_noPolarity() {
		// threshold=500, hysteresis=5, enabled=true, polarity=false, register=S5
		// S5 = 500 + (5 * 1024) + (1 * 16384) + (0 * 32768) = 22004 = 0x55F4
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");
		cmd.addParameter("threshold", 500);
		cmd.addParameter("hysteresis", 5);
		cmd.addParameter("polarity", false);
		cmd.addParameter("alarmRegister", "S5");

		encodeSingle(cmd);

		assertEquals("TEK822,S5=55F4", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetAlarmThreshold_disabled() {
		// threshold=200, hysteresis=10(def), enabled=false, polarity=true(def)
		// S4 = 200 + (10 * 1024) + (0 * 16384) + (1 * 32768) = 43208 = 0xA8C8
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");
		cmd.addParameter("threshold", 200);
		cmd.addParameter("enabled", false);

		encodeSingle(cmd);

		assertEquals("TEK822,S4=A8C8", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetAlarmThreshold_zeroThreshold() {
		// threshold=0, hysteresis=0, enabled=false, polarity=false
		// S4 = 0 + (0 * 1024) + (0 * 16384) + (0 * 32768) = 0 = 0x0000
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");
		cmd.addParameter("threshold", 0);
		cmd.addParameter("hysteresis", 0);
		cmd.addParameter("enabled", false);
		cmd.addParameter("polarity", false);

		encodeSingle(cmd);

		assertEquals("TEK822,S4=0000", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SHUTDOWN (R1=80) ======================

	@Test
	void testShutdown() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SHUTDOWN");

		encodeSingle(cmd);

		assertEquals("TEK822,R1=80", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SET_RTC (R2) ======================
	// Formato PDF: yy/mm/dd:hh/mm/ss

	@Test
	void testSetRTC() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_RTC");
		cmd.addParameter("datetime", "2026-02-07T14:30:00");

		encodeSingle(cmd);

		assertEquals("TEK822,R2=26/02/07:14/30/00", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetRTC_midnight() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_RTC");
		cmd.addParameter("datetime", "2025-12-31T00:00:00");

		encodeSingle(cmd);

		assertEquals("TEK822,R2=25/12/31:00/00/00", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== DEACTIVATE (R4=DEACT) ======================

	@Test
	void testDeactivate() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "DEACTIVATE");

		encodeSingle(cmd);

		assertEquals("TEK822,R4=DEACT", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== CLOSE_TCP (R6=03) ======================

	@Test
	void testCloseTCP() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "CLOSE_TCP");

		encodeSingle(cmd);

		assertEquals("TEK822,R6=03", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== REQUEST_GPS (R7) ======================

	@Test
	void testRequestGPS_default60s() {
		// timeout default = 60 → 0x3C
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REQUEST_GPS");

		encodeSingle(cmd);

		assertEquals("TEK822,R7=3C", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testRequestGPS_custom120s() {
		// timeout = 120 → 0x78
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REQUEST_GPS");
		cmd.addParameter("timeout", 120);

		encodeSingle(cmd);

		assertEquals("TEK822,R7=78", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testRequestGPS_maxFF() {
		// timeout = 255 → 0xFF
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REQUEST_GPS");
		cmd.addParameter("timeout", 255);

		encodeSingle(cmd);

		assertEquals("TEK822,R7=FF", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== REQUEST_SETTINGS (R1) ======================

	@Test
	void testRequestSettings_fromS0_default() {
		// startFrom default = S0 → R1=02
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REQUEST_SETTINGS");

		encodeSingle(cmd);

		assertEquals("TEK822,R1=02", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testRequestSettings_fromS12() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REQUEST_SETTINGS");
		cmd.addParameter("startFrom", "S12");

		encodeSingle(cmd);

		assertEquals("TEK822,R1=04", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testRequestSettings_fromS19() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "REQUEST_SETTINGS");
		cmd.addParameter("startFrom", "S19");

		encodeSingle(cmd);

		assertEquals("TEK822,R1=08", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SET_APN (S12/S13/S14) ======================

	@Test
	void testSetAPN_full() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_APN");
		cmd.addParameter("apn", "internet");
		cmd.addParameter("username", "user");
		cmd.addParameter("apnPassword", "pass");

		encodeSingle(cmd);

		assertEquals("TEK822,S12=internet,S13=user,S14=pass", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	@Test
	void testSetAPN_noCredentials() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_APN");
		cmd.addParameter("apn", "iot.1nce.net");

		encodeSingle(cmd);

		assertEquals("TEK822,S12=iot.1nce.net,S13=,S14=", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== SET_SERVER (S15/S16) ======================

	@Test
	void testSetServer() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SET_SERVER");
		cmd.addParameter("serverIp", "84.51.250.104");
		cmd.addParameter("serverPort", "9000");

		encodeSingle(cmd);

		assertEquals("TEK822,S15=84.51.250.104,S16=9000", cmd.getEncodedCommandASCII());
		assertHexMatchesAscii(cmd.getEncodedCommandASCII(), cmd.getEncodedCommandHEX());
	}

	// ====================== Comando sconosciuto ======================

	@Test
	void testUnknownCommand_throwsException() {
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "UNKNOWN_CMD");

		assertThrows(Exception.class, () -> encoder.encode(List.of(cmd)));
	}

	// ====================== Comandi multipli ======================

	@Test
	void testMultipleCommands() {
		DeviceCommand cmd1 = new DeviceCommand("dev1", "TEK822V2", "REBOOT");
		DeviceCommand cmd2 = new DeviceCommand("dev1", "TEK822V2", "REQUEST_STATUS");

		List<TelemetryResponse.EncodedCommand> result = encoder.encode(List.of(cmd1, cmd2));

		assertEquals(2, result.size());
		assertEquals("REBOOT", result.get(0).getCommandType());
		assertEquals("REQUEST_STATUS", result.get(1).getCommandType());

		// Verifica HEX coerente con ASCII
		assertHexMatchesAscii("TEK822,R3=ACTIVE", result.get(0).getEncodedData());
		assertHexMatchesAscii("TEK822,R6=02", result.get(1).getEncodedData());
	}

	// ====================== Concatenazione comandi (sezione 3.21 PDF)
	// ======================
	// "Commands can be concatenated (separated by commas)"
	// Formato: <password>,<cmd1>,<cmd2>,<cmd3>
	// La password appare solo nel primo comando; i successivi la rimuovono.

	@Test
	void testConcatenation_3commands_docExample() {
		// Esempio dalla documentazione del metodo concatenateCommands:
		// "TEK822,S0=80" + "TEK822,S1=01" + "TEK822,R3=ACTIVE"
		// → "TEK822,S0=80,S1=01,R3=ACTIVE"

		DeviceCommand cmd1 = new DeviceCommand("dev1", "TEK822V2", "SET_INTERVAL");
		cmd1.addParameter("interval", 0); // S0 = (128*1)+(0*4) = 128 = 0x80
		cmd1.addParameter("samplingPeriod", 1);

		DeviceCommand cmd2 = new DeviceCommand("dev1", "TEK822V2", "SET_LISTEN");
		cmd2.addParameter("listenMinutes", 5); // S1 = 5/5 = 1 = 0x01

		DeviceCommand cmd3 = new DeviceCommand("dev1", "TEK822V2", "REBOOT"); // R3=ACTIVE

		List<TelemetryResponse.EncodedCommand> encoded = encoder.encode(List.of(cmd1, cmd2, cmd3));
		assertEquals(3, encoded.size());

		// Verifica singoli comandi ASCII
		assertEquals("TEK822,S0=80", cmd1.getEncodedCommandASCII());
		assertEquals("TEK822,S1=01", cmd2.getEncodedCommandASCII());
		assertEquals("TEK822,R3=ACTIVE", cmd3.getEncodedCommandASCII());

		// Concatena e verifica
		String concatenatedAscii = ControllerUtils.commandsToAsciiString(encoded);
		assertEquals("TEK822,S0=80,S1=01,R3=ACTIVE", concatenatedAscii);
	}

	@Test
	void testConcatenation_2commands() {
		// REBOOT + REQUEST_STATUS → "TEK822,R3=ACTIVE,R6=02"
		DeviceCommand cmd1 = new DeviceCommand("dev1", "TEK822V2", "REBOOT");
		DeviceCommand cmd2 = new DeviceCommand("dev1", "TEK822V2", "REQUEST_STATUS");

		List<TelemetryResponse.EncodedCommand> encoded = encoder.encode(List.of(cmd1, cmd2));

		String concatenatedAscii = ControllerUtils.commandsToAsciiString(encoded);
		assertEquals("TEK822,R3=ACTIVE,R6=02", concatenatedAscii);

		// Verifica anche HEX concatenato
		String concatenatedHex = ControllerUtils.commandsToHexString(encoded);
		assertHexMatchesAscii("TEK822,R3=ACTIVE,R6=02", concatenatedHex);
	}

	@Test
	void testConcatenation_singleCommand() {
		// Singolo comando: nessuna rimozione password
		DeviceCommand cmd = new DeviceCommand("dev1", "TEK822V2", "SHUTDOWN");

		List<TelemetryResponse.EncodedCommand> encoded = encoder.encode(List.of(cmd));

		String concatenatedAscii = ControllerUtils.commandsToAsciiString(encoded);
		assertEquals("TEK822,R1=80", concatenatedAscii);
	}

	@Test
	void testConcatenation_emptyList() {
		// Lista vuota → byte array vuoto → stringa vuota
		byte[] result = ControllerUtils.concatenateCommands(List.of());
		assertEquals(0, result.length);
	}

	@Test
	void testConcatenation_fullScenario_alarmConfig() {
		// Scenario reale: configurazione completa allarmi S4+S5+S6 + attivazione
		// S4=E864, S5=55F4, S6=0000, R3=ACTIVE
		// → "TEK822,S4=E864,S5=55F4,S6=0000,R3=ACTIVE"

		DeviceCommand cmd1 = new DeviceCommand("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");
		cmd1.addParameter("threshold", 100); // S4 defaults → 0xE864
		cmd1.addParameter("alarmRegister", "S4");

		DeviceCommand cmd2 = new DeviceCommand("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");
		cmd2.addParameter("threshold", 500);
		cmd2.addParameter("hysteresis", 5);
		cmd2.addParameter("polarity", false);
		cmd2.addParameter("alarmRegister", "S5"); // → 0x55F4

		DeviceCommand cmd3 = new DeviceCommand("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");
		cmd3.addParameter("threshold", 0);
		cmd3.addParameter("hysteresis", 0);
		cmd3.addParameter("enabled", false);
		cmd3.addParameter("polarity", false);
		cmd3.addParameter("alarmRegister", "S6"); // → 0x0000

		DeviceCommand cmd4 = new DeviceCommand("dev1", "TEK822V2", "REBOOT");

		List<TelemetryResponse.EncodedCommand> encoded = encoder.encode(List.of(cmd1, cmd2, cmd3, cmd4));
		assertEquals(4, encoded.size());

		String concatenatedAscii = ControllerUtils.commandsToAsciiString(encoded);
		assertEquals("TEK822,S4=E864,S5=55F4,S6=0000,R3=ACTIVE", concatenatedAscii);
	}

	@Test
	void testConcatenation_enrichResponse() {
		// Test del flusso completo: encode → enrichResponse → verifica response
		DeviceCommand cmd1 = new DeviceCommand("dev1", "TEK822V2", "SET_INTERVAL");
		cmd1.addParameter("interval", 8);
		cmd1.addParameter("samplingPeriod", 0); // S0 = 0x20

		DeviceCommand cmd2 = new DeviceCommand("dev1", "TEK822V2", "REQUEST_STATUS"); // R6=02

		List<TelemetryResponse.EncodedCommand> encoded = encoder.encode(List.of(cmd1, cmd2));

		TelemetryResponse response = new TelemetryResponse();
		response.setCommands(encoded);

		ControllerUtils.enrichResponseWithConcatenatedCommands(response);

		assertEquals("TEK822,S0=20,R6=02", response.getConcatenatedCommandsAscii());
		assertHexMatchesAscii("TEK822,S0=20,R6=02", response.getConcatenatedCommandsHex());
	}

	// ================ getEncoderName / getSupportedDeviceTypes ================

	@Test
	void testGetEncoderName() {
		assertEquals("Tek822Encoder", encoder.getEncoderName());
	}

	@Test
	void testGetSupportedDeviceTypes() {
		List<String> types = encoder.getSupportedDeviceTypes();
		assertEquals(14, types.size());
		assertTrue(types.contains("TEK822V1"));
		assertTrue(types.contains("TEK822V2"));
	}

}
