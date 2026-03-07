package com.aton.proj.oneGasMeteor.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.model.TelemetryResponse;

/**
 * Test della classe ControllerUtils.
 *
 * Verifica i metodi di utilità per:
 *  - Conversione HEX ↔ ASCII
 *  - Conversione hex string → byte array e viceversa
 *  - Concatenazione di comandi codificati TEK822
 */
class ControllerUtilsTest {

	// ====================== hexToAscii ======================

	/**
	 * Input:  "54454B383232" (hex)
	 * Output: "TEK822"       (ASCII)
	 *
	 * Verifica che una stringa HEX valida venga convertita nel corrispondente
	 * testo ASCII.
	 */
	@Test
	void testHexToAscii_validString() {
		// "TEK822" in ASCII → T=54, E=45, K=4B, 8=38, 2=32, 2=32
		String hex = "54454B383232";
		String result = ControllerUtils.hexToAscii(hex);
		assertEquals("TEK822", result);
	}

	/**
	 * Input:  "54454B3832322C53303D3830" (hex)
	 * Output: "TEK822,S0=80"             (ASCII)
	 *
	 * Verifica una stringa HEX più completa che include separatori e valori
	 * numerici tipici di un comando TEK822.
	 */
	@Test
	void testHexToAscii_commandString() {
		// "TEK822,S0=80" in hex
		String hex = "54454B3832322C53303D3830";
		String result = ControllerUtils.hexToAscii(hex);
		assertEquals("TEK822,S0=80", result);
	}

	/**
	 * Input:  "" (stringa vuota)
	 * Output: "" (stringa vuota)
	 *
	 * Verifica che una stringa vuota in input produca una stringa vuota in output
	 * senza eccezioni.
	 */
	@Test
	void testHexToAscii_emptyString() {
		String result = ControllerUtils.hexToAscii("");
		assertEquals("", result);
	}

	/**
	 * Input:  null
	 * Output: "" (stringa vuota)
	 *
	 * Verifica che un input null non causi NullPointerException ma restituisca
	 * una stringa vuota.
	 */
	@Test
	void testHexToAscii_nullInput() {
		String result = ControllerUtils.hexToAscii(null);
		assertEquals("", result);
	}

	// ====================== hexStringToByteArray ======================

	/**
	 * Input:  "080181" (hex)
	 * Output: byte[] { 0x08, 0x01, 0x81 }
	 *
	 * Verifica la conversione di una stringa esadecimale in un array di byte.
	 */
	@Test
	void testHexStringToByteArray_valid() {
		byte[] result = ControllerUtils.hexStringToByteArray("080181");
		assertArrayEquals(new byte[]{0x08, 0x01, (byte) 0x81}, result);
	}

	/**
	 * Input:  "FF00AB" (hex, con valori limite)
	 * Output: byte[] { (byte)0xFF, 0x00, (byte)0xAB }
	 *
	 * Verifica che i valori esadecimali al limite (00, FF) vengano convertiti
	 * correttamente.
	 */
	@Test
	void testHexStringToByteArray_edgeValues() {
		byte[] result = ControllerUtils.hexStringToByteArray("FF00AB");
		assertArrayEquals(new byte[]{(byte) 0xFF, 0x00, (byte) 0xAB}, result);
	}

	/**
	 * Input:  "ff00ab" (lettere minuscole)
	 * Output: byte[] { (byte)0xFF, 0x00, (byte)0xAB }
	 *
	 * Verifica che le lettere minuscole in una stringa HEX vengano accettate.
	 */
	@Test
	void testHexStringToByteArray_lowercaseHex() {
		byte[] result = ControllerUtils.hexStringToByteArray("ff00ab");
		assertArrayEquals(new byte[]{(byte) 0xFF, 0x00, (byte) 0xAB}, result);
	}

	/**
	 * Input:  "" (stringa vuota)
	 * Output: byte[] {} (array vuoto)
	 *
	 * Verifica che una stringa vuota produca un array di byte vuoto.
	 */
	@Test
	void testHexStringToByteArray_emptyString() {
		byte[] result = ControllerUtils.hexStringToByteArray("");
		assertArrayEquals(new byte[]{}, result);
	}

	/**
	 * Input:  null
	 * Output: byte[] {} (array vuoto)
	 *
	 * Verifica che un input null non generi eccezioni.
	 */
	@Test
	void testHexStringToByteArray_null() {
		byte[] result = ControllerUtils.hexStringToByteArray(null);
		assertArrayEquals(new byte[]{}, result);
	}

	/**
	 * Input:  "A XB" (stringa con spazio)
	 * Comportamento: lo spazio viene rimosso, rimane "AB" → byte[] { (byte)0xAB }
	 *
	 * Verifica che i caratteri non-esadecimali vengano rimossi prima della
	 * conversione.
	 */
	@Test
	void testHexStringToByteArray_nonHexCharsStripped() {
		byte[] result = ControllerUtils.hexStringToByteArray("A B");
		assertArrayEquals(new byte[]{(byte) 0xAB}, result);
	}

	/**
	 * Input:  "ABC" (lunghezza dispari → 3 caratteri hex validi)
	 * Output: IllegalArgumentException
	 *
	 * Verifica che una stringa HEX di lunghezza dispari (dopo la rimozione di
	 * caratteri non-hex) lanci un'eccezione, poiché non è possibile formare
	 * una coppia di nibble completa.
	 */
	@Test
	void testHexStringToByteArray_oddLength_throwsException() {
		assertThrows(IllegalArgumentException.class,
				() -> ControllerUtils.hexStringToByteArray("ABC"));
	}

	// ====================== bytesToHex ======================

	/**
	 * Input:  byte[] { 0x08, 0x01, (byte)0x81 }
	 * Output: "080181" (hex uppercase)
	 *
	 * Verifica la conversione di un array di byte in stringa HEX maiuscola.
	 */
	@Test
	void testBytesToHex_valid() {
		String result = ControllerUtils.bytesToHex(new byte[]{0x08, 0x01, (byte) 0x81});
		assertEquals("080181", result);
	}

	/**
	 * Input:  byte[] {} (array vuoto)
	 * Output: "" (stringa vuota)
	 *
	 * Verifica che un array di byte vuoto produca una stringa vuota.
	 */
	@Test
	void testBytesToHex_emptyArray() {
		String result = ControllerUtils.bytesToHex(new byte[]{});
		assertEquals("", result);
	}

	/**
	 * Input:  byte[] { (byte)0xFF, 0x00 }
	 * Output: "FF00"
	 *
	 * Verifica che i valori limite (0x00, 0xFF) vengano formattati con zero-padding
	 * in maiuscolo.
	 */
	@Test
	void testBytesToHex_edgeValues() {
		String result = ControllerUtils.bytesToHex(new byte[]{(byte) 0xFF, 0x00});
		assertEquals("FF00", result);
	}

	// ====================== Round-trip hex ↔ byte[] ======================

	/**
	 * Input:  "54454B3832322C52333D414354495645"
	 * Output: stessa stringa (dopo due conversioni: hex→byte[]→hex)
	 *
	 * Verifica che la conversione hex→byte[]→hex sia invertibile (round-trip),
	 * ovvero che non si perda informazione in entrambe le direzioni.
	 */
	@Test
	void testRoundTrip_hexToByteArrayAndBack() {
		String originalHex = "54454B3832322C52333D414354495645"; // "TEK822,R3=ACTIVE"
		byte[] bytes = ControllerUtils.hexStringToByteArray(originalHex);
		String result = ControllerUtils.bytesToHex(bytes);
		assertEquals(originalHex, result);
	}

	// ====================== concatenateCommands ======================

	/**
	 * Input:  lista vuota di comandi
	 * Output: byte[] vuoto (lunghezza 0)
	 *
	 * Verifica che una lista vuota di comandi produca un array di byte vuoto.
	 */
	@Test
	void testConcatenateCommands_emptyList() {
		byte[] result = ControllerUtils.concatenateCommands(List.of());
		assertArrayEquals(new byte[]{}, result);
	}

	/**
	 * Input:  null
	 * Output: byte[] vuoto (lunghezza 0)
	 *
	 * Verifica che un input null non generi NullPointerException.
	 */
	@Test
	void testConcatenateCommands_nullList() {
		byte[] result = ControllerUtils.concatenateCommands(null);
		assertArrayEquals(new byte[]{}, result);
	}

	/**
	 * Input:  un singolo comando con encodedData="54454B3832322C53303D3830" → ASCII "TEK822,S0=80"
	 * Output: byte[] corrispondente alla stringa ASCII "TEK822,S0=80"
	 *
	 * Verifica che un singolo comando venga convertito correttamente mantenendo
	 * la password nel risultato finale.
	 */
	@Test
	void testConcatenateCommands_singleCommand() {
		// "TEK822,S0=80" in hex
		TelemetryResponse.EncodedCommand cmd = new TelemetryResponse.EncodedCommand(
				1L, "SET_INTERVAL", "54454B3832322C53303D3830", "TEK822,S0=80");

		byte[] result = ControllerUtils.concatenateCommands(List.of(cmd));

		String ascii = new String(result, java.nio.charset.StandardCharsets.US_ASCII);
		assertEquals("TEK822,S0=80", ascii);
	}

	/**
	 * Input:  due comandi:
	 *   - Command1: encodedData="54454B3832322C53303D3830"     → ASCII "TEK822,S0=80"
	 *   - Command2: encodedData="54454B3832322C52333D414354495645" → ASCII "TEK822,R3=ACTIVE"
	 * Output: byte[] per la stringa ASCII "TEK822,S0=80,R3=ACTIVE"
	 *
	 * Verifica che il secondo comando perda il prefisso "TEK822," evitando
	 * duplicazione della password. I comandi sono separati da virgola.
	 */
	@Test
	void testConcatenateCommands_twoCommands_removesPasswordFromSecond() {
		TelemetryResponse.EncodedCommand cmd1 = new TelemetryResponse.EncodedCommand(
				1L, "SET_INTERVAL", "54454B3832322C53303D3830", "TEK822,S0=80");
		TelemetryResponse.EncodedCommand cmd2 = new TelemetryResponse.EncodedCommand(
				2L, "REBOOT", "54454B3832322C52333D414354495645", "TEK822,R3=ACTIVE");

		byte[] result = ControllerUtils.concatenateCommands(List.of(cmd1, cmd2));

		String ascii = new String(result, java.nio.charset.StandardCharsets.US_ASCII);
		assertEquals("TEK822,S0=80,R3=ACTIVE", ascii);
	}

	/**
	 * Input:  tre comandi:
	 *   - Command1: "TEK822,S0=80"
	 *   - Command2: "TEK822,S1=01"
	 *   - Command3: "TEK822,R3=ACTIVE"
	 * Output: byte[] per "TEK822,S0=80,S1=01,R3=ACTIVE"
	 *
	 * Verifica che tre comandi vengano concatenati con la password solo all'inizio
	 * e separati da virgole. Questo è il formato TEK822 standard per comandi multipli.
	 */
	@Test
	void testConcatenateCommands_threeCommands() {
		TelemetryResponse.EncodedCommand cmd1 = new TelemetryResponse.EncodedCommand(
				1L, "SET_INTERVAL", "54454B3832322C53303D3830", "TEK822,S0=80");
		TelemetryResponse.EncodedCommand cmd2 = new TelemetryResponse.EncodedCommand(
				2L, "SET_LISTEN", "54454B3832322C53313D3031", "TEK822,S1=01");
		TelemetryResponse.EncodedCommand cmd3 = new TelemetryResponse.EncodedCommand(
				3L, "REBOOT", "54454B3832322C52333D414354495645", "TEK822,R3=ACTIVE");

		byte[] result = ControllerUtils.concatenateCommands(List.of(cmd1, cmd2, cmd3));

		String ascii = new String(result, java.nio.charset.StandardCharsets.US_ASCII);
		assertEquals("TEK822,S0=80,S1=01,R3=ACTIVE", ascii);
	}

	// ====================== commandsToAsciiString ======================

	/**
	 * Input:  due comandi: "TEK822,S0=80" e "TEK822,R3=ACTIVE"
	 * Output: "TEK822,S0=80,R3=ACTIVE" (stringa ASCII leggibile)
	 *
	 * Verifica che commandsToAsciiString produca la stringa ASCII leggibile
	 * con i comandi concatenati correttamente, omettendo la password duplicata.
	 */
	@Test
	void testCommandsToAsciiString_twoCommands() {
		TelemetryResponse.EncodedCommand cmd1 = new TelemetryResponse.EncodedCommand(
				1L, "SET_INTERVAL", "54454B3832322C53303D3830", "TEK822,S0=80");
		TelemetryResponse.EncodedCommand cmd2 = new TelemetryResponse.EncodedCommand(
				2L, "REBOOT", "54454B3832322C52333D414354495645", "TEK822,R3=ACTIVE");

		String result = ControllerUtils.commandsToAsciiString(List.of(cmd1, cmd2));

		assertEquals("TEK822,S0=80,R3=ACTIVE", result);
	}

	// ====================== commandsToHexString ======================

	/**
	 * Input:  un singolo comando con encodedData="54454B3832322C53303D3830" → ASCII "TEK822,S0=80"
	 * Output: "54454B3832322C53303D3830" (stringa HEX della concatenazione)
	 *
	 * Verifica che commandsToHexString produca la stringa HEX corrispondente
	 * ai byte ASCII del risultato concatenato.
	 */
	@Test
	void testCommandsToHexString_singleCommand() {
		TelemetryResponse.EncodedCommand cmd = new TelemetryResponse.EncodedCommand(
				1L, "SET_INTERVAL", "54454B3832322C53303D3830", "TEK822,S0=80");

		String result = ControllerUtils.commandsToHexString(List.of(cmd));

		// "TEK822,S0=80" → bytes ASCII → back to HEX = "54454B3832322C53303D3830"
		assertEquals("54454B3832322C53303D3830", result);
	}
}
