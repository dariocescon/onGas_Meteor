package com.aton.proj.oneGasMeteor.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryMessage;
import com.aton.proj.oneGasMeteor.utils.ControllerUtils;

/**
 * Test specifici per il metodo privato {@code decodeMeasurementData} di
 * {@link TekMessageDecoder}.
 *
 * <p>Il metodo viene invocato dal decoder pubblico {@code decode()} solo per
 * i messaggi di tipo 4/8/9 (telemetria con misurazioni ultrasoniche).
 * Lo invochiamo direttamente via reflection per testare in isolamento.
 *
 * <h3>Copertura:</h3>
 * <ul>
 *   <li><b>Logger speed</b> — calcolo loggerSpeedMs per msgType 4/8/9,
 *       inclusa modalita' manual (1 sec/campione)</li>
 *   <li><b>Campi di misura</b> — distance (10 bit), temperature (7 bit + formula),
 *       sonicRssi (nibble basso byte 0), sonicSrc (nibble byte 2 >> 2)</li>
 *   <li><b>Campi nullable</b> — percentageFull, payloadValue, temperatureCode,
 *       auxdata1, auxdata2 devono essere null</li>
 *   <li><b>Blocchi void</b> — se la somma dei 4 byte e' 0 il blocco viene
 *       saltato; l'etichetta "Data i" usa l'indice del loop, non la posizione
 *       nella lista risultante</li>
 *   <li><b>RTC e timestamp</b> — ore estratte dai lower 5 bit di byte[19],
 *       minuti da byte[25]; spaziatura tra misure = loggerSpeedMs</li>
 *   <li><b>Midnight crossing</b> — se baseTimestamp - serverTime > 12 h
 *       il decoder sottrae 24 h (misura del giorno precedente)</li>
 *   <li><b>Troncamento payload</b> — il loop si interrompe quando
 *       j+3 >= payload.length; massimo 28 iterazioni</li>
 * </ul>
 *
 * <h3>Nota importante — serverTimeInMs</h3>
 * <p>{@link TelemetryMessage#getServerTimeInMs()} e' inizializzato a
 * {@code System.currentTimeMillis()} nel costruttore e il campo e' {@code final},
 * quindi non controllabile dal test. I test sul midnight crossing adottano
 * una strategia dinamica: calcolano il risultato atteso in base al valore
 * reale di serverTimeInMs letto dal messaggio appena creato.
 */
public class TekMessageDecoderMeasurementTest {

    // =========================================================================
	// HELPER — REFLECTION
    // =========================================================================

    /**
	 * Invoca il metodo privato
	 * {@code decodeMeasurementData(TelemetryMessage, byte[], DecodedMessage, int)}
	 * di {@link TekMessageDecoder} tramite reflection.
	 *
	 * <p>Il metodo sotto test:
	 * <ol>
	 *   <li>Calcola loggerSpeedMs tramite calculateLoggerSpeed(msgType, payload, decode)</li>
	 *   <li>Estrae rtcHours (byte[19] & 0x1F) e rtcMinutes (byte[25])</li>
	 *   <li>Costruisce baseTimestampMs = serverDate + rtcTime (UTC)</li>
	 *   <li>Applica la correzione midnight crossing se necessario</li>
	 *   <li>Itera fino a 28 blocchi da 4 byte (a partire da byte 26),
	 *       costruendo una MeasurementData per ogni blocco non-void</li>
	 * </ol>
     */
    private void invokeDecode(TelemetryMessage msg, byte[] payload,
                               DecodedMessage decoded, int msgType) {
        TekMessageDecoder decoder = new TekMessageDecoder();
        try {
            var method = TekMessageDecoder.class.getDeclaredMethod(
                    "decodeMeasurementData",
                    TelemetryMessage.class, byte[].class, DecodedMessage.class, int.class);
            method.setAccessible(true);
            method.invoke(decoder, msg, payload, decoded, msgType);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Reflection error: " + e.getMessage());
        }
    }

    // =========================================================================
	// HELPER — PAYLOAD BUILDER
    // =========================================================================

    /**
	 * Costruisce un payload minimo di 26 byte (senza measurement blocks)
	 * con i byte fondamentali per {@code decodeMeasurementData}.
     *
	 * <p>Layout dei byte rilevanti per decodeMeasurementData:
	 * <pre>
	 *   byte[ 0] = 0x18            prodotto TEK822V2 (non usato direttamente
	 *                               da decodeMeasurementData, ma necessario
	 *                               perche' il payload sia coerente)
	 *   byte[ 3]                   contact reason: bit 3 = manual
	 *                               NOTA: decodeMeasurementData legge lo stato
	 *                               manual da decoded.getContactReason(), NON
	 *                               dal payload. Questo byte viene impostato
	 *                               solo per coerenza strutturale.
	 *   byte[19]                   lower 5 bit = rtcHours (0-23)
	 *                               upper 3 bit = tryAttemptsRemaining (ignorati)
	 *   byte[23]                   loggerSpeed — interpretazione dipende da msgType
	 *   byte[25]                   rtcMinutes (0-59)
	 * </pre>
	 *
	 * @param rtcHours       ore RTC del device (0-23), scritte nei lower 5 bit di byte[19]
	 * @param rtcMinutes     minuti RTC del device (0-59), scritti in byte[25]
	 * @param byte23         byte[23] — controlla la velocita' del logger
	 * @param manualContact  se true imposta bit 3 di byte[3]; per decodeMeasurementData
	 *                       occorre ANCHE chiamare decoded.getContactReason().setManual(TRUE)
     */
    private byte[] buildPayload(int rtcHours, int rtcMinutes,
                                byte byte23, boolean manualContact) {
        byte[] payload = new byte[26];
		payload[0]  = 0x18;                                // TEK822V2 (product type = 24)
		payload[3]  = manualContact ? (byte) 0x08 : 0x00;  // bit 3 = manual (coerenza)
		payload[19] = (byte) (rtcHours & 0x1F);            // rtcHours nei lower 5 bit
		payload[23] = byte23;                              // loggerSpeed byte
		payload[25] = (byte) (rtcMinutes & 0xFF);          // rtcMinutes
        return payload;
    }

    /**
	 * Costruisce un payload con header (26 byte) + measurement blocks accodati.
	 *
	 * <p>Ogni blocco occupa 4 byte a partire dall'offset 26. Il decoder itera
	 * fino a 28 blocchi o fino a quando {@code j+3 >= payload.length}.
     *
	 * @param measurementBlocks ciascun elemento e' un {@code byte[4]} che rappresenta
	 *                          una singola misura (sonicRssi, temp, sonicSrc+distHi, distLo)
     */
    private byte[] buildPayload(int rtcHours, int rtcMinutes,
                                byte byte23, boolean manualContact,
                                byte[]... measurementBlocks) {
        byte[] header = buildPayload(rtcHours, rtcMinutes, byte23, manualContact);
        byte[] payload = new byte[26 + measurementBlocks.length * 4];
        System.arraycopy(header, 0, payload, 0, 26);
        for (int i = 0; i < measurementBlocks.length; i++) {
            int base = 26 + i * 4;
            System.arraycopy(measurementBlocks[i], 0, payload, base, 4);
        }
        return payload;
    }

	/**
	 * Crea un blocco di misura da 4 byte.
	 *
	 * <pre>
	 * Layout del blocco (da PDF sezione 2.2.2):
	 *   b0         [3:0] = sonicRssi (0-15), [7:4] = non usato qui
	 *   b1         [6:0] = temperatura raw (formula: value/2.0 - 30.0 °C)
	 *              [7]   = ignorato dalla formula
	 *   b2         [1:0] = distance high (2 bit)
	 *              [5:2] = sonicSrc (4 bit)
	 *              [7:6] = non usato
	 *   b3         [7:0] = distance low  (8 bit)
	 *
	 *   distance (cm) = (b2 & 0x03) << 8 | b3    — range 0-1023
	 * </pre>
	 */
    private byte[] block(int b0, int b1, int b2, int b3) {
        return new byte[]{ (byte) b0, (byte) b1, (byte) b2, (byte) b3 };
    }

	/**
	 * Blocco void: tutti zero. Il decoder lo salta perche'
	 * {@code filter = b0 + b1 + b2 + b3 == 0}.
	 */
    private static final byte[] VOID_BLOCK = { 0, 0, 0, 0 };

    // =========================================================================
	// 1. LOGGER SPEED
	// =========================================================================
	// calculateLoggerSpeed(msgType, payload, decode) restituisce millisecondi.
	// Il valore viene diviso per 60_000 e salvato come loggerSpeedMinutes.
	//
	// msgType 8:
	//   manual=true                → 1 sec   (1_000 ms)
	//   manual=false, bit7=0      → 1 min   (60_000 ms)
	//   manual=false, bit7=1      → 15 min  (900_000 ms)
	//
	// msgType 9:
	//   bit7=0                    → 1 min   (60_000 ms)
	//   bit7=1                    → 15 min  (900_000 ms)
	//
	// msgType 4:
	//   lower7 == 0 && bit7=0    → 1 min   (60_000 ms)
	//   lower7 == 0 && bit7=1    → 15 min  (900_000 ms)
	//   lower7 > 0               → lower7 × 15 min
    // =========================================================================

    @Test
    void testLoggerSpeedMinutes_msgType8_nonManual_bit7on_is15min() {
		// byte[23] = 0x80 → bit7=1
		// msgType=8, manual=null (non impostato, Boolean.TRUE.equals(null)=false)
		// → ramo: bit7=1 → loggerSpeedMs = 15 × 60 × 1000 = 900_000
		// → loggerSpeedMinutes = 900_000 / 60_000 = 15
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(15, decoded.getUnitSetup().getLoggerSpeedMinutes());
    }

    @Test
    void testLoggerSpeedMinutes_msgType8_manual_is0min() {
		// msgType=8, manual=true → loggerSpeedMs = 1_000 (1 secondo)
		// → loggerSpeedMinutes = 1_000 / 60_000 = 0 (integer division)
		//
		// NOTA: decodeMeasurementData legge lo stato manual dal DecodedMessage
		// (decoded.getContactReason().getManual()), NON dal payload.
		// Il flag manualContact=true nel builder e' solo per coerenza strutturale.
        byte[] payload = buildPayload(12, 0, (byte) 0x80, true,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();
		decoded.getContactReason().setManual(Boolean.TRUE); // ← questo e' il flag effettivo

        invokeDecode(msg, payload, decoded, 8);

		assertEquals(0, decoded.getUnitSetup().getLoggerSpeedMinutes());
    }

    @Test
    void testLoggerSpeedMinutes_msgType4_value4_is60min() {
		// byte[23] = 0x04 → lower 7 bit = 4 (non-zero)
		// → loggerSpeedMs = 4 × 15 × 60 × 1000 = 3_600_000 ms = 60 min
		// → loggerSpeedMinutes = 3_600_000 / 60_000 = 60
        byte[] payload = buildPayload(12, 0, (byte) 0x04, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 4);

        assertEquals(60, decoded.getUnitSetup().getLoggerSpeedMinutes());
    }

    @Test
    void testLoggerSpeedMinutes_msgType9_bit7off_is1min() {
		// byte[23] = 0x00 → bit7=0
		// msgType=9 → ramo: bit7=0 → loggerSpeedMs = 1 × 60 × 1000 = 60_000
		// → loggerSpeedMinutes = 1
        byte[] payload = buildPayload(12, 0, (byte) 0x00, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 9);

        assertEquals(1, decoded.getUnitSetup().getLoggerSpeedMinutes());
    }

    // =========================================================================
	// 2. DECODIFICA DISTANZA
	// =========================================================================
	// Formula (da PDF sezione 2.2.2):
	//   distance = ((byte[j+2] & 0x03) << 8) | (byte[j+3] & 0xFF)
	//
	// I 2 bit bassi di byte[j+2] (high) + gli 8 bit di byte[j+3] (low)
	// formano un valore a 10 bit → range 0-1023 cm
    // =========================================================================

    @Test
    void testDistanceDecoding_typicalValue() {
		// byte[j+2]=0x2B: 0x2B & 0x03 = 3     → high 2 bit
		// byte[j+3]=0xFE: 0xFE & 0xFF = 254   → low 8 bit
		// distance = (3 << 8) | 254 = 768 + 254 = 1022
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x2B, 0xFE));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
        assertEquals(1022, m.getDistanceCm());
		assertEquals(1022, m.getMeasuredValue()); // measuredValue = distance
    }

    @Test
    void testDistanceDecoding_maxValue() {
		// byte[j+2]=0xFF: 0xFF & 0x03 = 3   → high = 3
		// byte[j+3]=0xFF: 0xFF & 0xFF = 255 → low  = 255
		// distance = (3 << 8) | 255 = 768 + 255 = 1023 (valore massimo a 10 bit)
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x64, 0xFF, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(1023, decoded.getMeasurementData().get(0).getDistanceCm());
    }

    @Test
    void testDistanceDecoding_zero() {
		// byte[j+2]=0x28: 0x28 & 0x03 = 0   → high = 0
		// byte[j+3]=0x00: 0x00 & 0xFF = 0   → low  = 0
		// distance = 0
		// NB: il blocco NON e' void perche' filter = 0x0A + 0x6A + 0x28 + 0x00 = 162 ≠ 0
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x28, 0x00));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(0, decoded.getMeasurementData().get(0).getDistanceCm());
    }

	// =========================================================================
	// 3. DECODIFICA TEMPERATURA
	// =========================================================================
	// Formula (da PDF sezione 2.2.2):
	//   temperatureC = (byte[j+1] & 0x7F) / 2.0 - 30.0
	//   temperatureF = (temperatureC × 9/5) + 32    (formato "%.2f")
	//
	// Il bit 7 di byte[j+1] viene mascherato (& 0x7F) e non influisce
	// sul calcolo della temperatura. Range: -30.0 a +33.5 °C
	// =========================================================================

    @Test
    void testTemperatureDecoding_23gradi() {
		// byte[j+1] = 0x6A = 106
		// (106 & 0x7F) = 106   → 106 / 2.0 - 30.0 = 53.0 - 30.0 = 23.0 °C
		// Fahrenheit: (23.0 × 9/5) + 32 = 41.4 + 32 = 73.4 → "73.40"
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
        assertEquals(23.0, m.getTemperatureC(), 0.01);
        assertEquals("73.40", m.getTemperatureF());
    }

    @Test
    void testTemperatureDecoding_bit7ignorato() {
		// byte[j+1] = 0xEA = 1110 1010
		// 0xEA & 0x7F = 0x6A = 106 (bit 7 mascherato)
		// → identico a 0x6A → 23.0 °C
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0xEA, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(23.0, decoded.getMeasurementData().get(0).getTemperatureC(), 0.01);
    }

    @Test
    void testTemperatureDecoding_zeroGradi() {
		// byte[j+1] = 0x3C = 60
		// (60 & 0x7F) = 60   → 60 / 2.0 - 30.0 = 30.0 - 30.0 = 0.0 °C
		// Fahrenheit: (0.0 × 9/5) + 32 = 32.0 → "32.00"
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x3C, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
        assertEquals(0.0, m.getTemperatureC(), 0.01);
        assertEquals("32.00", m.getTemperatureF());
    }

	// =========================================================================
	// 4. DECODIFICA CAMPI SONICI
	// =========================================================================
	// sonicRssi = byte[j] & 0x0F       → nibble basso del primo byte del blocco
	// sonicSrc  = (byte[j+2] >> 2) & 0x0F  → bit [5:2] del terzo byte del blocco
	// =========================================================================

    @Test
    void testSonicFields() {
		// byte[j]   = 0xAB = 1010 1011 → & 0x0F = 0x0B = 11 → sonicRssi = 11
		// byte[j+2] = 0x2C = 0010 1100 → >> 2 = 0000 1011 = 0x0B
		//                               → & 0x0F = 11      → sonicSrc  = 11
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0xAB, 0x6A, 0x2C, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
		assertEquals(11, m.getSonicRssi());
		assertEquals(11, m.getSonicSrc());
    }

	// =========================================================================
	// 5. CAMPI NULLABLE
	// =========================================================================
	// Il decoder imposta esplicitamente a null i campi non calcolati:
	// percentageFull, payloadValue, temperatureCode, auxdata1, auxdata2
	// Questi campi sono riservati per eventuali estensioni future.
	// =========================================================================

    @Test
    void testCampiNullabiliSonoNull() {
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
        assertNull(m.getPercentageFull());
        assertNull(m.getPayloadValue());
        assertNull(m.getTemperatureCode());
        assertNull(m.getAuxdata1());
        assertNull(m.getAuxdata2());
    }

    // =========================================================================
	// 6. BLOCCHI VOID
	// =========================================================================
	// Un blocco e' considerato void quando:
	//   filter = byte[j] + byte[j+1] + byte[j+2] + byte[j+3] == 0
	//
	// I blocchi void vengono saltati (continue nel loop) ma l'indice i
	// continua a incrementare. Di conseguenza:
	//   - measurementNum = "Data " + i  usa l'indice del loop, non la posizione
	//     nella lista risultante
	//   - il timestamp di ogni misura e' calcolato come:
	//       baseTimestamp - (loggerSpeedMs × i)
	//     quindi i "buchi" nei void creano gap temporali corrispondenti
    // =========================================================================

    @Test
    void testVoidReadings_sonoSaltati() {
		// Sequenza: valido, void, void, valido, valido
		//   i=0 → filter=373 ≠ 0 → aggiunto ("Data 0")
		//   i=1 → filter=0       → saltato
		//   i=2 → filter=0       → saltato
		//   i=3 → filter=373 ≠ 0 → aggiunto ("Data 3")
		//   i=4 → filter=373 ≠ 0 → aggiunto ("Data 4")
		// Risultato: 3 misurazioni nella lista
		byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
				block(0x0A, 0x6A, 0x02, 0xFF),  // i=0 — valido
				VOID_BLOCK,                       // i=1 — void → saltato
				VOID_BLOCK,                       // i=2 — void → saltato
				block(0x0A, 0x6A, 0x02, 0xFF),  // i=3 — valido
				block(0x0A, 0x6A, 0x02, 0xFF));  // i=4 — valido
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(3, decoded.getMeasurementData().size());
    }

    @Test
    void testVoidReadings_measurementNumPreservesLoopIndex() {
		// I void saltano l'iterazione ma NON resettano l'indice.
		// Sequenza: void(i=0), valido(i=1), void(i=2), valido(i=3)
		// La lista contiene 2 elementi con label "Data 1" e "Data 3"
		// (non "Data 0" e "Data 1"!)
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                VOID_BLOCK,                       // i=0 — saltato
                block(0x0A, 0x6A, 0x02, 0xFF),  // i=1 → "Data 1"
                VOID_BLOCK,                       // i=2 — saltato
                block(0x0A, 0x6A, 0x02, 0xFF));  // i=3 → "Data 3"
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var measurements = decoded.getMeasurementData();
        assertEquals(2, measurements.size());
        assertEquals("Data 1", measurements.get(0).getMeasurementNum());
        assertEquals("Data 3", measurements.get(1).getMeasurementNum());
    }

    // =========================================================================
	// 7. ESTRAZIONE RTC
	// =========================================================================
	// rtcHours   = byte[19] & 0x1F   → lower 5 bit (range 0-23)
	//   I 3 bit alti di byte[19] contengono tryAttemptsRemaining,
	//   ma il mask & 0x1F li ignora.
	// rtcMinutes = byte[25] & 0xFF   → intero byte (range 0-59)
    // =========================================================================

    @Test
    void testRtcExtraction_lower5BitsDiByte19() {
		// byte[19] = 0xB7 = 1011 0111
		//   upper 3 bit: 101 = 5 (tryAttemptsRemaining — ignorati)
		//   lower 5 bit: 10111 = 0x17 = 23 → rtcHours = 23
		// byte[25] = 0x3B = 59 → rtcMinutes = 59
		//
		// Verifica: il timestamp della misura deve avere ora=23, minuto=59
        byte[] payload = buildPayload(0, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
		payload[19] = (byte) 0xB7; // sovrascrittura per forzare upper 3 bit non-zero
        payload[25] = (byte) 0x3B;

        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        Instant ts = Instant.parse(decoded.getMeasurementData().get(0).getTimestamp());
        LocalTime t = ts.atZone(ZoneOffset.UTC).toLocalTime();
        assertEquals(23, t.getHour(),   "rtcHours deve essere i lower 5 bit di byte[19]");
        assertEquals(59, t.getMinute(), "rtcMinutes deve essere byte[25]");
    }

	// =========================================================================
	// 8. SPAZIATURA TIMESTAMP
	// =========================================================================
	// Il timestamp della misura i-esima e':
	//   timestamp[i] = baseTimestampMs - (loggerSpeedMs × i)
	//
	// Quindi la differenza tra due misure consecutive (i, i+1) e':
	//   timestamp[i] - timestamp[i+1] = loggerSpeedMs
	//
	// NOTA: le misure sono ordinate dal piu' recente (i=0) al piu' vecchio.
	// =========================================================================

    @Test
    void testTimestampSpacing_15min() {
		// msgType=8, bit7=1, non-manual → loggerSpeedMs = 900_000 ms = 15 min
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData();
        assertEquals(3, m.size());
        for (int i = 1; i < m.size(); i++) {
            Instant prev = Instant.parse(m.get(i - 1).getTimestamp());
            Instant curr = Instant.parse(m.get(i).getTimestamp());
            long diffMin = (prev.toEpochMilli() - curr.toEpochMilli()) / 60_000;
            assertEquals(15, diffMin,
                    "Spaziatura attesa 15 min tra Data " + (i - 1) + " e Data " + i);
        }
    }

    @Test
    void testTimestampSpacing_1min() {
		// msgType=9, bit7=0 → loggerSpeedMs = 60_000 ms = 1 min
        byte[] payload = buildPayload(12, 0, (byte) 0x00, false,
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 9);

        var m = decoded.getMeasurementData();
        for (int i = 1; i < m.size(); i++) {
            Instant prev = Instant.parse(m.get(i - 1).getTimestamp());
            Instant curr = Instant.parse(m.get(i).getTimestamp());
            long diffMin = (prev.toEpochMilli() - curr.toEpochMilli()) / 60_000;
            assertEquals(1, diffMin, "Spaziatura attesa 1 min tra consecutive");
        }
    }

    @Test
    void testTimestampSpacing_60min() {
		// msgType=4, byte[23]=0x04 → lower7=4 (non-zero)
		// → loggerSpeedMs = 4 × 15 × 60 × 1000 = 3_600_000 ms = 60 min
        byte[] payload = buildPayload(12, 0, (byte) 0x04, false,
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 4);

        var m = decoded.getMeasurementData();
        for (int i = 1; i < m.size(); i++) {
            Instant prev = Instant.parse(m.get(i - 1).getTimestamp());
            Instant curr = Instant.parse(m.get(i).getTimestamp());
            long diffMin = (prev.toEpochMilli() - curr.toEpochMilli()) / 60_000;
            assertEquals(60, diffMin, "Spaziatura attesa 60 min tra consecutive");
        }
    }

    @Test
    void testTimestampSpacing_manual_1secondoTraMisurazioni() {
		// msgType=8, manual=true → loggerSpeedMs = 1_000 ms
		// Differenza tra consecutive = 1_000 ms = 1 secondo
        byte[] payload = buildPayload(12, 0, (byte) 0x80, true,
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();
		decoded.getContactReason().setManual(Boolean.TRUE); // flag effettivo per il decoder

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData();
        assertEquals(3, m.size());
        for (int i = 1; i < m.size(); i++) {
            Instant prev = Instant.parse(m.get(i - 1).getTimestamp());
            Instant curr = Instant.parse(m.get(i).getTimestamp());
            long diffMs = prev.toEpochMilli() - curr.toEpochMilli();
			assertEquals(1000, diffMs, "Spaziatura attesa 1 secondo in modalita' manual");
        }
    }

    // =========================================================================
	// 9. MIDNIGHT CROSSING
	// =========================================================================
	// Il decoder ricostruisce il timestamp come:
	//   baseTimestampMs = serverDate (UTC) + LocalTime(rtcHours, rtcMinutes)
	//
	// Se baseTimestampMs - serverTimeMs > 12 ore, significa che l'orologio
	// RTC del device indica un'ora del GIORNO PRECEDENTE (es: il device
	// ha misurato ieri alle 23:50 e il server riceve oggi alle 00:10).
	// In questo caso il decoder sottrae 24 ore a baseTimestampMs.
	//
	// NOTA: serverTimeInMs = System.currentTimeMillis() (campo final di
	// TelemetryMessage, non sovrascrivibile). I test usano una strategia
	// dinamica per verificare la correttezza.
    // =========================================================================

    @Test
    void testMidnightCrossing_noCorrectionNeeded_rtcAMezzanotte() {
		// RTC = 00:00 → baseTimestampMs = serverDate alle 00:00 UTC
		// Poiche' serverTimeMs >= serverDate 00:00 (per definizione),
		// la differenza baseTimestampMs - serverTimeMs <= 0
		// → condizione > 12h MAI soddisfatta → nessuna sottrazione
		//
		// Verifica: data del timestamp = data del server, ora = 00:00
        final int rtcHours = 0, rtcMinutes = 0;
        byte[] payload = buildPayload(rtcHours, rtcMinutes, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        long serverTimeMs = msg.getServerTimeInMs();
		LocalDate serverDate = Instant.ofEpochMilli(serverTimeMs)
				.atZone(ZoneOffset.UTC).toLocalDate();

        DecodedMessage decoded = new DecodedMessage();
        invokeDecode(msg, payload, decoded, 8);

        Instant ts = Instant.parse(decoded.getMeasurementData().get(0).getTimestamp());
        LocalDate measDate = ts.atZone(ZoneOffset.UTC).toLocalDate();
        LocalTime measTime = ts.atZone(ZoneOffset.UTC).toLocalTime();

        assertEquals(serverDate, measDate,
				"RTC=00:00 → diff <= 0 → nessun crossing → stessa data del server");
        assertEquals(0, measTime.getHour());
        assertEquals(0, measTime.getMinute());
    }

    @Test
    void testMidnightCrossing_verificaDinamica() {
		// RTC = 23:59.
		// Il test calcola dinamicamente se il crossing dovrebbe attivarsi
		// in base al serverTimeMs effettivo, poi verifica che il decoder
		// produca la data corretta (ieri o oggi).
		//
		// Questo test passa SEMPRE, indipendentemente dall'ora di esecuzione:
		//   - Se UTC < 12:00 → crossing attivo → data = ieri
		//   - Se UTC >= 12:00 → crossing non attivo → data = oggi
        final int rtcHours = 23, rtcMinutes = 59;
        byte[] payload = buildPayload(rtcHours, rtcMinutes, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        long serverTimeMs = msg.getServerTimeInMs();

		// Replica della stessa formula usata dal decoder
		LocalDate serverDate = Instant.ofEpochMilli(serverTimeMs)
				.atZone(ZoneOffset.UTC).toLocalDate();
        long baseTimestampMs = serverDate.atTime(rtcHours, rtcMinutes)
                .atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        boolean crossingExpected = (baseTimestampMs - serverTimeMs) > 12L * 3600 * 1000;

        DecodedMessage decoded = new DecodedMessage();
        invokeDecode(msg, payload, decoded, 8);

        Instant ts = Instant.parse(decoded.getMeasurementData().get(0).getTimestamp());
        LocalDate measDate = ts.atZone(ZoneOffset.UTC).toLocalDate();
        LocalTime measTime = ts.atZone(ZoneOffset.UTC).toLocalTime();

        LocalDate expectedDate = crossingExpected ? serverDate.minusDays(1) : serverDate;
        assertEquals(expectedDate, measDate,
                "crossing=" + crossingExpected + ": data attesa=" + expectedDate);
        // L'ora RTC deve essere preservata in entrambi i rami
        assertEquals(rtcHours,   measTime.getHour());
        assertEquals(rtcMinutes, measTime.getMinute());
    }

    @Test
    void testMidnightCrossing_correctionApplied_whenServerIsEarlyUTC() {
		// Verifica ESPLICITAMENTE che il crossing sottragga 1 giorno.
		//
		// Prerequisito: il server deve girare prima delle ~12:00 UTC,
		// altrimenti con RTC=23:59 la differenza e' < 12h e il crossing
		// non si attiva. Se il prerequisito non e' soddisfatto, il test
		// viene saltato con assumeTrue (non fallisce).
		//
		// Strategia:
		//   1. Creiamo un probe per leggere serverTimeMs
		//   2. Calcoliamo se la condizione > 12h sarebbe soddisfatta
		//   3. Se no → assumeTrue salta il test
		//   4. Se si → eseguiamo e verifichiamo data = serverDate - 1
        final int rtcHours = 23, rtcMinutes = 59;

        byte[] probePayload = buildPayload(rtcHours, rtcMinutes, (byte) 0x80, false);
        long serverTimeMs = new TelemetryMessage(probePayload, "probe").getServerTimeInMs();
		LocalDate serverDate = Instant.ofEpochMilli(serverTimeMs)
				.atZone(ZoneOffset.UTC).toLocalDate();
        long baseTimestampMs = serverDate.atTime(rtcHours, rtcMinutes)
                .atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        assumeTrue(
                baseTimestampMs - serverTimeMs > 12L * 3600 * 1000,
                "Test saltato: ora UTC >= 12, con RTC=23:59 il crossing non si attiva");

		// --- Test effettivo ---
        byte[] payload = buildPayload(rtcHours, rtcMinutes, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        Instant ts = Instant.parse(decoded.getMeasurementData().get(0).getTimestamp());
        LocalDate measDate = ts.atZone(ZoneOffset.UTC).toLocalDate();
        LocalTime measTime = ts.atZone(ZoneOffset.UTC).toLocalTime();

        assertEquals(serverDate.minusDays(1), measDate,
                "Midnight crossing attivo: timestamp deve essere sul giorno precedente");
        assertEquals(rtcHours,   measTime.getHour());
        assertEquals(rtcMinutes, measTime.getMinute());
    }

    // =========================================================================
	// 10. TRONCAMENTO PAYLOAD E LIMITE 28 BLOCCHI
	// =========================================================================
	// Il loop: for (int i = 0; i < 28; i++)
	//   j = i * 4 + 26
	//   if (j + 3 >= payload.length) break;
	//
	// - Il loop itera al massimo 28 volte (indice 0-27)
	// - Si interrompe prima se il payload e' troppo corto per contenere
	//   il blocco corrente (j + 3 >= payload.length)
    // =========================================================================

    @Test
    void testPayloadTruncation_soloUnBloccoLeggibile() {
		// Payload = 26 (header) + 4 (1 blocco) = 30 byte
		//   i=0: j=26, j+3=29 < 30 → blocco letto OK
        // i=1: j=30, j+3=33 >= 30 → break
		// Risultato: esattamente 1 misura
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        assertEquals(30, payload.length, "Precondizione: payload deve essere 30 byte");

        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(1, decoded.getMeasurementData().size(),
				"Con 1 solo blocco nel payload, deve essere decodificata 1 misura");
    }

    @Test
    void testMaxMeasurements_28BlocchiPieni() {
		// 28 blocchi validi → loop itera 28 volte → 28 misurazioni
		// (anche se il payload contenesse piu' dati, il loop si ferma a i=27)
        byte[][] blocks = new byte[28][];
        for (int i = 0; i < 28; i++) {
            blocks[i] = block(0x0A, 0x6A, 0x02, 0xFF);
        }
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false, blocks);

        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(28, decoded.getMeasurementData().size(),
				"Il loop e' limitato a 28 iterazioni: massimo 28 misurazioni");
    }

    // =========================================================================
	// 11. INTEGRAZIONE — PAYLOAD REALE
	// =========================================================================
	// Payload reale catturato da un dispositivo TEK822V2 in produzione.
	// Message type 8 (alarm), 10 misurazioni valide + 18 blocchi void.
	//
	// Header (17 byte):
	//   byte[0]=0x18 → TEK822V2    byte[15]=0x08 → msgType=8
	//   byte[5]=0x18 → CSQ=24      byte[16]=0x7B → declaredLength=123
	//   byte[19]=0x47 → rtcHours = 0x47 & 0x1F = 7
	//   byte[25]=0x00 → rtcMinutes = 0
	//
	// Diagnostic (9 byte):
	//   byte[23]=0x81 → bit7=1 → 15 min (non-manual)
	//
	// Blocchi misura (10 validi, 18 void, 2 byte trailing checksum):
	//   Data 0: 0A 6A 2B FE → dist=1022, temp=23.0, rssi=10, src=10
	//   Data 1: 0A 6A 28 00 → dist=0,    temp=23.0
	//   Data 2: 0A 6A 2B FE → dist=1022
	//   Data 3-8: 0A 6A 28 43 → dist=67,  temp=23.0
	//   Data 9: 0A 6A 28 00 → dist=0,    temp=23.0
    // =========================================================================

    @Test
    void testFullDecode_msgType8_payloadReale() {
		String hex = "180203428918360864431047987054"   // header byte 0-13
		           + "087B"                              // byte 14-16: msgType=8, len=123
		           + "0931470008FF810F00"                // diagnostic byte 17-25
		           + "0A6A2BFE"                          // Data 0: dist=1022
		           + "0A6A2800"                          // Data 1: dist=0
		           + "0A6A2BFE"                          // Data 2: dist=1022
		           + "0A6A2843"                          // Data 3: dist=67
		           + "0A6A2843"                          // Data 4
		           + "0A6A2843"                          // Data 5
		           + "0A6A2843"                          // Data 6
		           + "0A6A2843"                          // Data 7
		           + "0A6A2843"                          // Data 8
		           + "0A6A2800"                          // Data 9: dist=0
		           // 18 blocchi void (72 byte = 144 hex char)
		           + "000000000000000000000000000000000000000000000000" // void 10-15
		           + "000000000000000000000000000000000000000000000000" // void 16-21
		           + "000000000000000000000000000000000000000000000000" // void 22-27
		           + "294A";                              // checksum (2 byte)
        byte[] payload = ControllerUtils.hexStringToByteArray(hex);
        TelemetryMessage msg = new TelemetryMessage(payload, hex, LocalDateTime.now(), "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

		// --- Logger speed: byte[23]=0x81, bit7=1, non-manual → 15 min ---
        assertEquals(15, decoded.getUnitSetup().getLoggerSpeedMinutes());

		// --- 10 misurazioni valide (18 void saltati) ---
        var measurements = decoded.getMeasurementData();
        assertNotNull(measurements);
        assertEquals(10, measurements.size());

		// --- Data 0: blocco 0A 6A 2B FE ---
        var m0 = measurements.get(0);
        assertEquals("Data 0",   m0.getMeasurementNum());
		assertEquals(1022,     m0.getDistanceCm());  // (0x2B & 0x03)<<8 | 0xFE = 1022
		assertEquals(23.0,     m0.getTemperatureC(), 0.01);  // (0x6A & 0x7F)/2 - 30 = 23.0
        assertEquals("73.40",    m0.getTemperatureF());
		assertEquals(10,       m0.getSonicRssi());   // 0x0A & 0x0F = 10
		assertEquals(10,       m0.getSonicSrc());    // (0x2B >> 2) & 0x0F = 10

		// --- Spaziatura 15 min tra tutte le misurazioni consecutive ---
        for (int i = 1; i < measurements.size(); i++) {
            Instant prev = Instant.parse(measurements.get(i - 1).getTimestamp());
            Instant curr = Instant.parse(measurements.get(i).getTimestamp());
            long diffMin = (prev.toEpochMilli() - curr.toEpochMilli()) / 60_000;
            assertEquals(15, diffMin,
                    "Spaziatura 15 min attesa tra Data " + (i - 1) + " e Data " + i);
        }
    }
}
