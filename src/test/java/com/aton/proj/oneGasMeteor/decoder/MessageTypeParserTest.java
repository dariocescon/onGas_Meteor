package com.aton.proj.oneGasMeteor.decoder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.model.MessageType16Response;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;
import com.aton.proj.oneGasMeteor.model.MessageType6Response;

/**
 * Test della classe MessageTypeParser.
 *
 * Verifica il parsing dei tre tipi di messaggi speciali ASCII hex-encoded:
 *  - Tipo 6:  Device settings (coppie chiave=valore)
 *  - Tipo 16: Statistiche modem + ICCID
 *  - Tipo 17: Dati GPS
 *
 * I payload in input sono stringhe HEX che rappresentano dati ASCII codificati
 * nel formato del protocollo TEK822.
 */
class MessageTypeParserTest {

	private MessageTypeParser parser;

	@BeforeEach
	void setUp() {
		parser = new MessageTypeParser();
	}

	// ====================== parseMessageType6 (Settings) ======================

	/**
	 * Input hex: codifica ASCII di "S0=80,S1=05,S2=7F0038"
	 *   S = 53, 0 = 30, = = 3D, 8 = 38, 0 = 30  → "S0=80"
	 *   , = 2C                                    → separatore
	 *   ...
	 * Input deviceId:   "DEV001"
	 * Input deviceType: "TEK822V2"
	 *
	 * Output atteso:
	 *   - deviceId   = "DEV001"
	 *   - deviceType = "TEK822V2"
	 *   - settings.size() ≥ 1
	 *   - settings["S0"] = "80"
	 *   - settings["S1"] = "05"
	 *
	 * Verifica il parsing base di impostazioni del dispositivo.
	 */
	@Test
	void testParseMessageType6_basicSettings() {
		// ASCII "S0=80,S1=05,S2=7F0038" codificata in HEX
		String asciiPayload = "S0=80,S1=05,S2=7F0038";
		String hexPayload = asciiToHex(asciiPayload);

		MessageType6Response response = parser.parseMessageType6(hexPayload, "DEV001", "TEK822V2");

		assertNotNull(response);
		assertEquals("DEV001", response.getDeviceId());
		assertEquals("TEK822V2", response.getDeviceType());
		assertNotNull(response.getSettings());
		assertEquals(3, response.getSettings().size());
		assertEquals("80", response.getSetting("S0"));
		assertEquals("05", response.getSetting("S1"));
		assertEquals("7F0038", response.getSetting("S2"));
	}

	/**
	 * Input hex: codifica ASCII di ",S0=80,S1=05" (con virgola iniziale)
	 * Input deviceId:   "DEV002"
	 * Input deviceType: "TEK822V1"
	 *
	 * Output atteso:
	 *   - settings["S0"] = "80"
	 *   - settings["S1"] = "05"
	 *
	 * Verifica che il parser gestisca correttamente la virgola iniziale,
	 * che è presente nel formato reale del protocollo TEK822.
	 */
	@Test
	void testParseMessageType6_withLeadingComma() {
		// Payload reale: inizia con virgola
		String asciiPayload = ",S0=80,S1=05";
		String hexPayload = asciiToHex(asciiPayload);

		MessageType6Response response = parser.parseMessageType6(hexPayload, "DEV002", "TEK822V1");

		assertNotNull(response);
		assertEquals("80", response.getSetting("S0"));
		assertEquals("05", response.getSetting("S1"));
	}

	/**
	 * Input hex: codifica ASCII di un singolo setting "S4=0000"
	 * Input deviceId: "DEV003"
	 *
	 * Output atteso:
	 *   - settings.size() = 1
	 *   - settings["S4"] = "0000"
	 *
	 * Verifica il parsing di un singolo setting con valore "0000".
	 */
	@Test
	void testParseMessageType6_singleSetting() {
		String hexPayload = asciiToHex("S4=0000");

		MessageType6Response response = parser.parseMessageType6(hexPayload, "DEV003", "TEK822V2");

		assertEquals(1, response.getSettings().size());
		assertEquals("0000", response.getSetting("S4"));
	}

	/**
	 * Input hex: "" (payload vuoto)
	 * Input deviceId: "DEV004"
	 *
	 * Output atteso:
	 *   - response non null
	 *   - settings è una mappa vuota
	 *
	 * Verifica che un payload vuoto produca una risposta valida con zero settings.
	 */
	@Test
	void testParseMessageType6_emptyPayload() {
		MessageType6Response response = parser.parseMessageType6("", "DEV004", "TEK822V2");

		assertNotNull(response);
		assertEquals(0, response.getSettings().size());
	}

	// ====================== parseMessageType16 (ICCID & Statistics) ======================

	/**
	 * Input hex: codifica ASCII CSV con 12 campi:
	 *   "89882390000028895236,19875,5,55,150,3,45000,1200,800,2500,148,2"
	 *   campi:
	 *     [0]  ICCID            = "89882390000028895236"
	 *     [1]  energyUsed       = 19875
	 *     [2]  minTemperature   = 5
	 *     [3]  maxTemperature   = 55
	 *     [4]  messageCount     = 150
	 *     [5]  deliveryFailCount= 3
	 *     [6]  totalSendTime    = 45000
	 *     [7]  maxSendTime      = 1200
	 *     [8]  minSendTime      = 800
	 *     [9]  rssiTotal        = 2500
	 *     [10] rssiValidCount   = 148
	 *     [11] rssiFailCount    = 2
	 * Input deviceId:   "DEV010"
	 * Input deviceType: "TEK822V2"
	 *
	 * Output atteso:
	 *   - iccid          = "89882390000028895236"
	 *   - energyUsed     = 19875
	 *   - minTemperature = 5
	 *   - maxTemperature = 55
	 *   - messageCount   = 150
	 *   - averageSendTime ≈ 300.0  (45000/150)
	 *   - deliverySuccessRate ≈ 98.0%  ((150-3)/150 * 100)
	 *
	 * Verifica il parsing completo di un messaggio di tipo 16 con tutte le
	 * statistiche del modem e il calcolo dei campi derivati.
	 */
	@Test
	void testParseMessageType16_fullPayload() {
		String asciiPayload = "89882390000028895236,19875,5,55,150,3,45000,1200,800,2500,148,2";
		String hexPayload = asciiToHex(asciiPayload);

		MessageType16Response response = parser.parseMessageType16(hexPayload, "DEV010", "TEK822V2");

		assertNotNull(response);
		assertEquals("DEV010", response.getDeviceId());
		assertEquals("TEK822V2", response.getDeviceType());
		assertEquals("89882390000028895236", response.getIccid());
		assertEquals(19875L, response.getEnergyUsed());
		assertEquals(5, response.getMinTemperature());
		assertEquals(55, response.getMaxTemperature());
		assertEquals(150, response.getMessageCount());
		assertEquals(3, response.getDeliveryFailCount());
		assertEquals(45000L, response.getTotalSendTime());
		assertEquals(1200L, response.getMaxSendTime());
		assertEquals(800L, response.getMinSendTime());
		assertEquals(2500L, response.getRssiTotal());
		assertEquals(148, response.getRssiValidCount());
		assertEquals(2, response.getRssiFailCount());

		// Campi derivati
		// averageSendTime = totalSendTime / messageCount = 45000 / 150 = 300.0
		assertNotNull(response.getAverageSendTime());
		assertEquals(300.0, response.getAverageSendTime(), 0.01);

		// deliverySuccessRate = ((150 - 3) / 150) * 100 = 98.0%
		assertNotNull(response.getDeliverySuccessRate());
		assertEquals(98.0, response.getDeliverySuccessRate(), 0.01);

		// averageRssi = rssiTotal / rssiValidCount = 2500 / 148 ≈ 16.89
		assertNotNull(response.getAverageRssi());
		assertEquals(2500.0 / 148, response.getAverageRssi(), 0.01);
	}

	/**
	 * Input hex: codifica ASCII di ",89882390000028895236,19875,5,55,150,3,45000,1200,800,2500,148,2"
	 *            (con virgola iniziale, come nel formato reale)
	 *
	 * Output atteso: stessa risposta del test precedente
	 *
	 * Verifica che il parser gestisca correttamente il payload con virgola iniziale.
	 */
	@Test
	void testParseMessageType16_withLeadingComma() {
		String asciiPayload = ",89882390000028895236,19875,5,55,150,3,45000,1200,800,2500,148,2";
		String hexPayload = asciiToHex(asciiPayload);

		MessageType16Response response = parser.parseMessageType16(hexPayload, "DEV011", "TEK822V2");

		assertNotNull(response);
		assertEquals("89882390000028895236", response.getIccid());
		assertEquals(19875L, response.getEnergyUsed());
		assertEquals(150, response.getMessageCount());
	}

	/**
	 * Input hex: "" (payload vuoto)
	 *
	 * Output atteso:
	 *   - response non null
	 *   - tutti i campi null (nessun parsing effettuato)
	 *
	 * Verifica che un payload vuoto produca una risposta valida senza crash.
	 */
	@Test
	void testParseMessageType16_emptyPayload() {
		MessageType16Response response = parser.parseMessageType16("", "DEV012", "TEK822V2");

		assertNotNull(response);
		assertNull(response.getIccid());
		assertNull(response.getEnergyUsed());
	}

	/**
	 * Input:  messageCount=100, deliveryFailCount=0 (zero fallimenti)
	 * Output: deliverySuccessRate = 100.0%
	 *
	 * Verifica che con zero fallimenti il tasso di successo sia 100%.
	 */
	@Test
	void testParseMessageType16_perfectDelivery() {
		// 0 fallimenti su 100 messaggi → 100% success
		String asciiPayload = "8988239000002889,1000,10,30,100,0,10000,200,50,800,100,0";
		String hexPayload = asciiToHex(asciiPayload);

		MessageType16Response response = parser.parseMessageType16(hexPayload, "DEV013", "TEK822V2");

		assertNotNull(response.getDeliverySuccessRate());
		assertEquals(100.0, response.getDeliverySuccessRate(), 0.001);
	}

	// ====================== parseMessageType17 (GPS) ======================

	/**
	 * Input hex: codifica ASCII CSV con 12 campi GPS:
	 *   "95,134442.0,5255.9950N,00013.4000E,1.2,42.5,3,180.0,0.5,0.3,070325,8"
	 *   campi:
	 *     [0]  timeToFixSeconds    = 95
	 *     [1]  utcTime             = "134442.0" → 13:44:42
	 *     [2]  latitudeRaw         = "5255.9950N"
	 *     [3]  longitudeRaw        = "00013.4000E"
	 *     [4]  horizontalPrecision = 1.2
	 *     [5]  altitude            = 42.5
	 *     [6]  gnssPositioningMode = 3
	 *     [7]  groundHeading       = 180.0
	 *     [8]  speedKmh            = 0.5
	 *     [9]  speedKnots          = 0.3
	 *     [10] date                = "070325"
	 *     [11] numberOfSatellites  = 8
	 * Input deviceId:   "DEV020"
	 * Input deviceType: "TEK822V2"
	 *
	 * Output atteso:
	 *   - timeToFixSeconds = 95
	 *   - utcTime          = 13:44:42
	 *   - latitudeRaw      = "5255.9950N"
	 *   - latitude         ≈ 52.93325 (gradi decimali)
	 *   - longitudeRaw     = "00013.4000E"
	 *   - altitude         = 42.5
	 *   - numberOfSatellites = 8
	 *   - googleMapsLink   non null
	 *
	 * Verifica il parsing completo di un messaggio GPS di tipo 17.
	 */
	@Test
	void testParseMessageType17_fullPayload() {
		String asciiPayload = "95,134442.0,5255.9950N,00013.4000E,1.2,42.5,3,180.0,0.5,0.3,070325,8";
		String hexPayload = asciiToHex(asciiPayload);

		MessageType17Response response = parser.parseMessageType17(hexPayload, "DEV020", "TEK822V2");

		assertNotNull(response);
		assertEquals("DEV020", response.getDeviceId());
		assertEquals("TEK822V2", response.getDeviceType());
		assertEquals(95, response.getTimeToFixSeconds());
		assertNotNull(response.getUtcTime());
		assertEquals(13, response.getUtcTime().getHour());
		assertEquals(44, response.getUtcTime().getMinute());
		assertEquals(42, response.getUtcTime().getSecond());
		assertEquals("5255.9950N", response.getLatitudeRaw());
		assertEquals("00013.4000E", response.getLongitudeRaw());
		assertEquals(1.2, response.getHorizontalPrecision(), 0.001);
		assertEquals(42.5, response.getAltitude(), 0.001);
		assertEquals(3, response.getGnssPositioningMode());
		assertEquals(180.0, response.getGroundHeading(), 0.001);
		assertEquals(0.5, response.getSpeedKmh(), 0.001);
		assertEquals(0.3, response.getSpeedKnots(), 0.001);
		assertEquals("070325", response.getDate());
		assertEquals(8, response.getNumberOfSatellites());

		// Conversione automatica in gradi decimali: lat = 52 + 55.9950/60 ≈ 52.9332...
		assertNotNull(response.getLatitude());
		assertEquals(52.0 + 55.9950 / 60.0, response.getLatitude(), 0.0001);

		// Google Maps link deve essere presente perché lat/lon sono validi
		assertNotNull(response.getGoogleMapsLink());
		assertTrue(response.getGoogleMapsLink().contains("maps?q="));
	}

	/**
	 * Input hex: codifica ASCII di ",95,134442.0,5255.9950N,00013.4000E,1.2,42.5,3,180.0,0.5,0.3,070325,8"
	 *            (con virgola iniziale)
	 *
	 * Output atteso: stessa risposta del test precedente
	 *
	 * Verifica la gestione della virgola iniziale nel payload GPS.
	 */
	@Test
	void testParseMessageType17_withLeadingComma() {
		String asciiPayload = ",95,134442.0,5255.9950N,00013.4000E,1.2,42.5,3,180.0,0.5,0.3,070325,8";
		String hexPayload = asciiToHex(asciiPayload);

		MessageType17Response response = parser.parseMessageType17(hexPayload, "DEV021", "TEK822V2");

		assertNotNull(response);
		assertEquals(95, response.getTimeToFixSeconds());
		assertEquals("5255.9950N", response.getLatitudeRaw());
	}

	/**
	 * Input hex: "" (payload vuoto)
	 *
	 * Output atteso:
	 *   - response non null
	 *   - latitude  = null (nessun GPS fix)
	 *   - longitude = null
	 *   - googleMapsLink = null
	 *
	 * Verifica che un payload vuoto produca una risposta valida senza GPS fix.
	 */
	@Test
	void testParseMessageType17_emptyPayload() {
		MessageType17Response response = parser.parseMessageType17("", "DEV022", "TEK822V2");

		assertNotNull(response);
		assertNull(response.getLatitude());
		assertNull(response.getLongitude());
		assertNull(response.getGoogleMapsLink());
	}

	// ====================== Helper ======================

	/**
	 * Converte una stringa ASCII in stringa HEX (per costruire i payload di test).
	 *
	 * Esempio: "TEK" → "54454B"
	 */
	private static String asciiToHex(String ascii) {
		StringBuilder hex = new StringBuilder();
		for (byte b : ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII)) {
			hex.append(String.format("%02X", b));
		}
		return hex.toString();
	}
}
