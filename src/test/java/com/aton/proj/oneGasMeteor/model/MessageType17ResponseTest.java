package com.aton.proj.oneGasMeteor.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test della classe MessageType17Response.
 *
 * Verifica:
 *  - Conversione coordinate GPS dal formato NMEA (ddmm.mmmm) ai gradi decimali
 *  - Comportamento dei setter setLatitudeRaw/setLongitudeRaw (auto-conversione)
 *  - Gestione di coordinate non valide (GPS fix non disponibile)
 *  - Generazione del link Google Maps
 */
class MessageType17ResponseTest {

	// ====================== coordinateToDecimal (latitudine Nord) ======================

	/**
	 * Input:  coordinate = "5255.9950N", direction = 'N'
	 * Output: ≈ 52.9332 gradi decimali
	 *
	 * Formula: gradi = 52, minuti = 55.9950
	 * Decimale = 52 + 55.9950/60 = 52 + 0.93325 = 52.93325
	 *
	 * Verifica la conversione di una latitudine Nord dal formato NMEA ddmm.mmmm
	 * ai gradi decimali. Valore tipico per il nord Europa.
	 */
	@Test
	void testCoordinateToDecimal_latitudeNorth() {
		// 52°55.9950'N → gradi decimali
		double result = MessageType17Response.coordinateToDecimal("5255.9950N", 'N');
		assertEquals(52.0 + 55.9950 / 60.0, result, 0.0001);
	}

	/**
	 * Input:  coordinate = "5255.9950S", direction = 'S'
	 * Output: ≈ -52.9332 gradi decimali (negativo per Sud)
	 *
	 * Verifica che le latitudini Sud vengano rappresentate con segno negativo.
	 */
	@Test
	void testCoordinateToDecimal_latitudeSouth() {
		double result = MessageType17Response.coordinateToDecimal("5255.9950S", 'S');
		assertEquals(-(52.0 + 55.9950 / 60.0), result, 0.0001);
	}

	// ====================== coordinateToDecimal (longitudine Est/Ovest) ======================

	/**
	 * Input:  coordinate = "00013.4000E", direction = 'E'
	 * Output: ≈ 0.2233 gradi decimali
	 *
	 * Formula: gradi = 0, minuti = 13.4000
	 * Decimale = 0 + 13.4000/60 = 0.22333...
	 *
	 * Verifica la conversione di una longitudine Est con 3 cifre per i gradi
	 * (formato dddmm.mmmm).
	 */
	@Test
	void testCoordinateToDecimal_longitudeEast() {
		// 0°13.4000'E → gradi decimali
		double result = MessageType17Response.coordinateToDecimal("00013.4000E", 'E');
		assertEquals(0.0 + 13.4000 / 60.0, result, 0.0001);
	}

	/**
	 * Input:  coordinate = "07430.0000W", direction = 'W'
	 * Output: ≈ -74.5 gradi decimali (negativo per Ovest)
	 *
	 * Formula: gradi = 74, minuti = 30.0000
	 * Decimale = -(74 + 30/60) = -74.5
	 *
	 * Verifica la conversione di una longitudine Ovest (es. New York area).
	 */
	@Test
	void testCoordinateToDecimal_longitudeWest() {
		double result = MessageType17Response.coordinateToDecimal("07430.0000W", 'W');
		assertEquals(-74.5, result, 0.0001);
	}

	/**
	 * Input:  coordinate = "01200.0000E", direction = 'E'
	 * Output: 12.0 gradi decimali esatti
	 *
	 * Formula: gradi = 12, minuti = 0 → esattamente 12.0
	 *
	 * Verifica la conversione quando i minuti sono zero.
	 */
	@Test
	void testCoordinateToDecimal_exactDegrees() {
		double result = MessageType17Response.coordinateToDecimal("01200.0000E", 'E');
		assertEquals(12.0, result, 0.0001);
	}

	// ====================== Gestione input non validi ======================

	/**
	 * Input:  coordinate = null, direction = 'N'
	 * Output: 0.0
	 *
	 * Verifica che un input null venga gestito restituendo 0.0 senza eccezioni.
	 */
	@Test
	void testCoordinateToDecimal_nullInput() {
		double result = MessageType17Response.coordinateToDecimal(null, 'N');
		assertEquals(0.0, result, 0.0001);
	}

	/**
	 * Input:  coordinate = "", direction = 'N'
	 * Output: 0.0
	 *
	 * Verifica che una stringa vuota venga gestita restituendo 0.0.
	 */
	@Test
	void testCoordinateToDecimal_emptyString() {
		double result = MessageType17Response.coordinateToDecimal("", 'N');
		assertEquals(0.0, result, 0.0001);
	}

	/**
	 * Input:  coordinate = "0" (troppo corta per la latitudine, min 3 chars)
	 * Output: 0.0
	 *
	 * Verifica che una coordinata troppo corta (GPS fix fallito) venga gestita
	 * restituendo 0.0.
	 */
	@Test
	void testCoordinateToDecimal_tooShortForLatitude() {
		double result = MessageType17Response.coordinateToDecimal("0", 'N');
		assertEquals(0.0, result, 0.0001);
	}

	/**
	 * Input:  coordinate = "000" (troppo corta per la longitudine, min 4 chars)
	 * Output: 0.0
	 *
	 * Verifica che una coordinata troppo corta per la longitudine venga
	 * gestita correttamente.
	 */
	@Test
	void testCoordinateToDecimal_tooShortForLongitude() {
		double result = MessageType17Response.coordinateToDecimal("000", 'E');
		assertEquals(0.0, result, 0.0001);
	}

	// ====================== setLatitudeRaw (auto-conversione) ======================

	/**
	 * Input:  latitudeRaw = "5255.9950N"
	 * Output:
	 *   - latitudeRaw = "5255.9950N"
	 *   - latitude    ≈ 52.9332 (calcolato automaticamente)
	 *
	 * Verifica che il setter setLatitudeRaw() converta automaticamente la
	 * coordinata grezza in gradi decimali e la salvi nel campo latitude.
	 */
	@Test
	void testSetLatitudeRaw_autoConvertsToDecimal() {
		MessageType17Response response = new MessageType17Response();
		response.setLatitudeRaw("5255.9950N");

		assertEquals("5255.9950N", response.getLatitudeRaw());
		assertNotNull(response.getLatitude());
		assertEquals(52.0 + 55.9950 / 60.0, response.getLatitude(), 0.0001);
	}

	/**
	 * Input:  latitudeRaw = "4500.0000S"
	 * Output: latitude ≈ -45.0 (Sud = negativo)
	 *
	 * Verifica che la latitudine Sud venga convertita con segno negativo.
	 */
	@Test
	void testSetLatitudeRaw_southNegative() {
		MessageType17Response response = new MessageType17Response();
		response.setLatitudeRaw("4500.0000S");

		assertNotNull(response.getLatitude());
		assertEquals(-45.0, response.getLatitude(), 0.0001);
	}

	/**
	 * Input:  latitudeRaw = "0" (GPS fix non disponibile, valore inatteso)
	 * Output: latitude = null (non si può calcolare una latitudine valida)
	 *
	 * Verifica che una coordinata non valida (es. GPS fix fallito) produca
	 * latitude=null invece di un valore 0.0 fuorviante.
	 */
	@Test
	void testSetLatitudeRaw_invalidCoordinate_setsNullLatitude() {
		MessageType17Response response = new MessageType17Response();
		response.setLatitudeRaw("0");

		// Una coordinata non valida non deve produrre un lat/lon fuorviante
		assertNull(response.getLatitude());
	}

	// ====================== setLongitudeRaw (auto-conversione) ======================

	/**
	 * Input:  longitudeRaw = "00013.4000E"
	 * Output:
	 *   - longitudeRaw = "00013.4000E"
	 *   - longitude    ≈ 0.2233 (calcolato automaticamente)
	 *
	 * Verifica che il setter setLongitudeRaw() converta automaticamente la
	 * coordinata grezza in gradi decimali.
	 */
	@Test
	void testSetLongitudeRaw_autoConvertsToDecimal() {
		MessageType17Response response = new MessageType17Response();
		response.setLongitudeRaw("00013.4000E");

		assertEquals("00013.4000E", response.getLongitudeRaw());
		assertNotNull(response.getLongitude());
		assertEquals(13.4000 / 60.0, response.getLongitude(), 0.0001);
	}

	/**
	 * Input:  longitudeRaw = "07430.0000W"
	 * Output: longitude ≈ -74.5 (Ovest = negativo)
	 *
	 * Verifica che la longitudine Ovest venga convertita con segno negativo.
	 */
	@Test
	void testSetLongitudeRaw_westNegative() {
		MessageType17Response response = new MessageType17Response();
		response.setLongitudeRaw("07430.0000W");

		assertNotNull(response.getLongitude());
		assertEquals(-74.5, response.getLongitude(), 0.0001);
	}

	// ====================== getGoogleMapsLink ======================

	/**
	 * Input:
	 *   - latitudeRaw  = "5255.9950N" → latitude  ≈ 52.9332
	 *   - longitudeRaw = "00013.4000E" → longitude ≈ 0.2233
	 *
	 * Output: URL Google Maps contenente lat/lon formattati a 6 decimali
	 *
	 * Verifica che getGoogleMapsLink() generi un URL valido quando
	 * latitudine e longitudine sono disponibili.
	 */
	@Test
	void testGetGoogleMapsLink_validCoordinates() {
		MessageType17Response response = new MessageType17Response();
		response.setLatitudeRaw("5255.9950N");
		response.setLongitudeRaw("00013.4000E");

		String link = response.getGoogleMapsLink();

		assertNotNull(link);
		assertTrue(link.startsWith("https://www.google.com/maps?q="));
		// Verifica che lat e lon siano presenti nel link
		assertTrue(link.contains("52.9"));
	}

	/**
	 * Input: latitudeRaw = null, longitudeRaw = null (nessun GPS fix)
	 *
	 * Output: null (non è possibile generare un link senza coordinate)
	 *
	 * Verifica che getGoogleMapsLink() restituisca null quando
	 * le coordinate non sono disponibili.
	 */
	@Test
	void testGetGoogleMapsLink_noCoordinates_returnsNull() {
		MessageType17Response response = new MessageType17Response();
		// latitude e longitude non impostati → null

		assertNull(response.getGoogleMapsLink());
	}

	// ====================== Getters & Setters ======================

	/**
	 * Input:  tutti i campi di MessageType17Response
	 * Output: stesso valore recuperato con i getter
	 *
	 * Verifica che i getter restituiscano i valori impostati dai setter per
	 * i campi che non hanno logica auto-conversione.
	 */
	@Test
	void testGettersAndSetters_basicFields() {
		MessageType17Response response = new MessageType17Response();

		response.setDeviceId("DEV020");
		response.setDeviceType("TEK822V2");
		response.setTimeToFixSeconds(95);
		response.setHorizontalPrecision(1.2);
		response.setAltitude(42.5);
		response.setGnssPositioningMode(3);
		response.setGroundHeading(180.0);
		response.setSpeedKmh(10.5);
		response.setSpeedKnots(5.67);
		response.setDate("070325");
		response.setNumberOfSatellites(8);

		assertEquals("DEV020", response.getDeviceId());
		assertEquals("TEK822V2", response.getDeviceType());
		assertEquals(95, response.getTimeToFixSeconds());
		assertEquals(1.2, response.getHorizontalPrecision(), 0.001);
		assertEquals(42.5, response.getAltitude(), 0.001);
		assertEquals(3, response.getGnssPositioningMode());
		assertEquals(180.0, response.getGroundHeading(), 0.001);
		assertEquals(10.5, response.getSpeedKmh(), 0.001);
		assertEquals(5.67, response.getSpeedKnots(), 0.001);
		assertEquals("070325", response.getDate());
		assertEquals(8, response.getNumberOfSatellites());
	}
}
