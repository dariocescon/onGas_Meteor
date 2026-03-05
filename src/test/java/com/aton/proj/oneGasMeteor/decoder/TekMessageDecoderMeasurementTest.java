package com.aton.proj.oneGasMeteor.decoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

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
 * Test della logica di decodeMeasurementData (TekMessageDecoder).
 *
 * Copre:
 *   - calcolo loggerSpeed per msgType 4/8/9 (incluso caso manual)
 *   - decodifica distance, temperature, sonicRssi, sonicSrc
 *   - campi nullable (percentageFull, payloadValue, ecc.)
 *   - skip dei blocchi void (tutti zero)
 *   - etichetta "Data i" basata sull'indice di loop
 *   - estrazione RTC da byte[19] (lower 5 bit) e byte[25]
 *   - spaziatura timestamp per velocità di 1 sec / 1 min / 15 min / 60 min
 *   - gestione attraversamento mezzanotte (midnight crossing)
 *   - troncamento payload (break quando il blocco non sta)
 *   - massimo 28 blocchi
 */
public class TekMessageDecoderMeasurementTest {

    // =========================================================================
    // REFLECTION HELPER
    // =========================================================================

    /**
     * Invoca il metodo privato decodeMeasurementData tramite reflection.
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
    // PAYLOAD BUILDER
    // =========================================================================

    /**
     * Costruisce un payload minimo (senza measurement blocks) con i byte
     * fondamentali per decodeMeasurementData.
     *
     * Layout rilevante:
     *   byte[ 0] = 0x18 → TEK822V2
     *   byte[ 2] = 0x44 → FW 4.2 (> 3.0, ramo energyUsed per TEK822V2)
     *   byte[ 3] = 0x08 se manualContact, altrimenti 0x00
     *   byte[19] = rtcHours & 0x1F  (lower 5 bit; upper 3 = tryAttempts, lascati a 0)
     *   byte[23] = byte23           (loggerSpeed)
     *   byte[25] = rtcMinutes
     */
    private byte[] buildPayload(int rtcHours, int rtcMinutes,
                                byte byte23, boolean manualContact) {
        byte[] payload = new byte[26];
        payload[0]  = 0x18;                                // TEK822V2
        payload[2]  = 0x44;                                // FW 4.2
        payload[3]  = manualContact ? (byte) 0x08 : 0x00; // bit3 = manual
        payload[19] = (byte) (rtcHours & 0x1F);
        payload[23] = byte23;
        payload[25] = (byte) (rtcMinutes & 0xFF);
        return payload;
    }

    /**
     * Costruisce un payload con i measurement blocks allegati.
     *
     * @param measurementBlocks array di blocchi da 4 byte ciascuno
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

    /** Helper: crea un blocco da 4 byte. */
    private byte[] block(int b0, int b1, int b2, int b3) {
        return new byte[]{ (byte) b0, (byte) b1, (byte) b2, (byte) b3 };
    }

    /** Blocco void: tutti zero → viene saltato dalla decodifica. */
    private static final byte[] VOID_BLOCK = { 0, 0, 0, 0 };

    // =========================================================================
    // LOGGER SPEED
    // =========================================================================

    @Test
    void testLoggerSpeedMinutes_msgType8_nonManual_bit7on_is15min() {
        // bit7=1, non-manual → loggerSpeedMs = 15 min → loggerSpeedMinutes = 15
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(15, decoded.getUnitSetup().getLoggerSpeedMinutes());
    }

    @Test
    void testLoggerSpeedMinutes_msgType8_manual_is0min() {
        // manual=true → loggerSpeedMs = 1000 ms → 1000/60000 = 0 minuti interi
        byte[] payload = buildPayload(12, 0, (byte) 0x80, true,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();
        decoded.getContactReason().setManual(Boolean.TRUE);

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(0, decoded.getUnitSetup().getLoggerSpeedMinutes()); // 1000ms / 60000 = 0
    }

    @Test
    void testLoggerSpeedMinutes_msgType4_value4_is60min() {
        // byte[23] lower7bits=4 → 4 × 15 min = 60 min
        byte[] payload = buildPayload(12, 0, (byte) 0x04, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 4);

        assertEquals(60, decoded.getUnitSetup().getLoggerSpeedMinutes());
    }

    @Test
    void testLoggerSpeedMinutes_msgType9_bit7off_is1min() {
        // msgType=9, bit7=0 → loggerSpeedMs = 1 min
        byte[] payload = buildPayload(12, 0, (byte) 0x00, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 9);

        assertEquals(1, decoded.getUnitSetup().getLoggerSpeedMinutes());
    }

    // =========================================================================
    // DECODIFICA CAMPI DI MISURA
    // =========================================================================

    @Test
    void testDistanceDecoding_typicalValue() {
        // byte2=0x2B (0x2B & 0x03 = 3), byte3=0xFE → distance = (3<<8)|254 = 1022
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x2B, 0xFE));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
        assertEquals(1022, m.getDistanceCm());
        assertEquals(1022, m.getMeasuredValue());
    }

    @Test
    void testDistanceDecoding_maxValue() {
        // byte2=0xFF (0xFF & 0x03 = 3), byte3=0xFF → (3<<8)|255 = 1023 (massimo)
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x64, 0xFF, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(1023, decoded.getMeasurementData().get(0).getDistanceCm());
    }

    @Test
    void testDistanceDecoding_zero() {
        // byte2=0x28 (0x28 & 0x03 = 0), byte3=0x00 → 0 cm
        // filter ≠ 0 perché byte0 e byte1 sono non-zero
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x28, 0x00));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(0, decoded.getMeasurementData().get(0).getDistanceCm());
    }

    @Test
    void testTemperatureDecoding_23gradi() {
        // byte1=0x6A = 106 → (106 & 0x7F = 106) / 2.0 - 30.0 = 23.0°C
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
        // byte1=0xEA = 1110 1010 → & 0x7F = 0x6A = 106 → stessa decodifica di 0x6A
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0xEA, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(23.0, decoded.getMeasurementData().get(0).getTemperatureC(), 0.01);
    }

    @Test
    void testTemperatureDecoding_zeroGradi() {
        // byte1=0x3C = 60 → (60 & 0x7F = 60) / 2.0 - 30.0 = 0.0°C
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x3C, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
        assertEquals(0.0, m.getTemperatureC(), 0.01);
        assertEquals("32.00", m.getTemperatureF());
    }

    @Test
    void testSonicFields() {
        // byte0=0xAB → sonicRssi = 0xAB & 0x0F = 0x0B = 11
        // byte2=0x2C → sonicSrc = (0x2C >> 2) & 0x0F = 0x0B = 11
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0xAB, 0x6A, 0x2C, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData().get(0);
        assertEquals(11, m.getSonicRssi()); // 0x0B
        assertEquals(11, m.getSonicSrc());  // 0x0B
    }

    @Test
    void testCampiNullabiliSonoNull() {
        // percentageFull, payloadValue, temperatureCode, auxdata1, auxdata2 → null
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
    // BLOCCHI VOID
    // =========================================================================

    @Test
    void testVoidReadings_sonoSaltati() {
        // 5 blocchi: valido, void, void, valido, valido → 3 misurazioni
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF),  // Data 0 — valido
                VOID_BLOCK,                       // Data 1 — void → saltato
                VOID_BLOCK,                       // Data 2 — void → saltato
                block(0x0A, 0x6A, 0x02, 0xFF),  // Data 3 — valido
                block(0x0A, 0x6A, 0x02, 0xFF));  // Data 4 — valido
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(3, decoded.getMeasurementData().size());
    }

    @Test
    void testVoidReadings_measurementNumPreservesLoopIndex() {
        // I blocchi void saltano le iterazioni: "Data i" usa l'indice i del loop,
        // NON la posizione nella lista risultante.
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
    // ESTRAZIONE RTC E TIMESTAMP
    // =========================================================================

    @Test
    void testRtcExtraction_lower5BitsDiByte19() {
        // byte[19] = 0xB7 = 1011 0111 → upper3=5 (tryAttempts), lower5=0x17=23 → rtcHours=23
        // byte[25] = 0x3B = 59 → rtcMinutes=59
        byte[] payload = buildPayload(0, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        payload[19] = (byte) 0xB7; // sovrascrittura manuale per testare il masking
        payload[25] = (byte) 0x3B;

        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        Instant ts = Instant.parse(decoded.getMeasurementData().get(0).getTimestamp());
        LocalTime t = ts.atZone(ZoneOffset.UTC).toLocalTime();
        assertEquals(23, t.getHour(),   "rtcHours deve essere i lower 5 bit di byte[19]");
        assertEquals(59, t.getMinute(), "rtcMinutes deve essere byte[25]");
    }

    @Test
    void testTimestampSpacing_15min() {
        // msgType=8, bit7=1, non-manual → 15 min tra misurazioni consecutive
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
        // msgType=9, bit7=0 → 1 min tra misurazioni consecutive
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
        // msgType=4, byte[23]=0x04 → 4 × 15 min = 60 min tra consecutive
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
        // manual=true → loggerSpeedMs = 1000 ms → diff tra consecutive = 1000 ms
        byte[] payload = buildPayload(12, 0, (byte) 0x80, true,
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF),
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();
        decoded.getContactReason().setManual(Boolean.TRUE);

        invokeDecode(msg, payload, decoded, 8);

        var m = decoded.getMeasurementData();
        assertEquals(3, m.size());
        for (int i = 1; i < m.size(); i++) {
            Instant prev = Instant.parse(m.get(i - 1).getTimestamp());
            Instant curr = Instant.parse(m.get(i).getTimestamp());
            long diffMs = prev.toEpochMilli() - curr.toEpochMilli();
            assertEquals(1000, diffMs, "Spaziatura attesa 1 secondo in modalità manual");
        }
    }

    // =========================================================================
    // MIDNIGHT CROSSING
    // =========================================================================

    @Test
    void testMidnightCrossing_noCorrectionNeeded_rtcAMezzanotte() {
        // RTC = 00:00 → baseTimestampMs = serverDate 00:00
        // diff ≤ 0 per qualsiasi ora del server → nessuna sottrazione
        // Risultato: timestamp sulla stessa data del server, ora 00:00
        final int rtcHours = 0, rtcMinutes = 0;
        byte[] payload = buildPayload(rtcHours, rtcMinutes, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        long serverTimeMs = msg.getServerTimeInMs();
        LocalDate serverDate = Instant.ofEpochMilli(serverTimeMs).atZone(ZoneOffset.UTC).toLocalDate();

        DecodedMessage decoded = new DecodedMessage();
        invokeDecode(msg, payload, decoded, 8);

        Instant ts = Instant.parse(decoded.getMeasurementData().get(0).getTimestamp());
        LocalDate measDate = ts.atZone(ZoneOffset.UTC).toLocalDate();
        LocalTime measTime = ts.atZone(ZoneOffset.UTC).toLocalTime();

        assertEquals(serverDate, measDate,
                "RTC=00:00 → diff ≤ 0 → nessun crossing → data uguale al server");
        assertEquals(0, measTime.getHour());
        assertEquals(0, measTime.getMinute());
    }

    @Test
    void testMidnightCrossing_verificaDinamica() {
        // RTC = 23:59. Il crossing avviene solo se serverTime < 11:59 UTC.
        // Il test calcola dinamicamente il risultato atteso e verifica
        // che il decoder produca la data corretta (ieri o oggi) e l'ora RTC esatta.
        final int rtcHours = 23, rtcMinutes = 59;
        byte[] payload = buildPayload(rtcHours, rtcMinutes, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        long serverTimeMs = msg.getServerTimeInMs();

        // Calcolo identico a quello interno al decoder
        LocalDate serverDate = Instant.ofEpochMilli(serverTimeMs).atZone(ZoneOffset.UTC).toLocalDate();
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
        // Verifica esplicitamente che il crossing sottragga 1 giorno.
        // Il test viene saltato (assumeTrue) se l'ora UTC del server è ≥ 12:00,
        // poiché in quel caso RTC=23:59 non supera la soglia di 12h.
        final int rtcHours = 23, rtcMinutes = 59;

        // Probe per leggere il serverTimeMs prima di costruire il payload reale
        byte[] probePayload = buildPayload(rtcHours, rtcMinutes, (byte) 0x80, false);
        long serverTimeMs = new TelemetryMessage(probePayload, "probe").getServerTimeInMs();
        LocalDate serverDate = Instant.ofEpochMilli(serverTimeMs).atZone(ZoneOffset.UTC).toLocalDate();
        long baseTimestampMs = serverDate.atTime(rtcHours, rtcMinutes)
                .atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        assumeTrue(
                baseTimestampMs - serverTimeMs > 12L * 3600 * 1000,
                "Test saltato: ora UTC >= 12, con RTC=23:59 il crossing non si attiva");

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
    // TRONCAMENTO PAYLOAD E LIMITE 28 BLOCCHI
    // =========================================================================

    @Test
    void testPayloadTruncation_soloUnBloccoLeggibile() {
        // Payload = 30 byte (header 26 + 1 blocco da 4).
        // i=0: j=26, j+3=29 < 30 → OK (letto)
        // i=1: j=30, j+3=33 >= 30 → break
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false,
                block(0x0A, 0x6A, 0x02, 0xFF));
        assertEquals(30, payload.length, "Precondizione: payload deve essere 30 byte");

        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(1, decoded.getMeasurementData().size(),
                "Con payload di 30 byte deve essere decodificato esattamente 1 blocco");
    }

    @Test
    void testMaxMeasurements_28BlocchiPieni() {
        // Il loop itera al massimo 28 volte → 28 misurazioni
        byte[][] blocks = new byte[28][];
        for (int i = 0; i < 28; i++) {
            blocks[i] = block(0x0A, 0x6A, 0x02, 0xFF);
        }
        byte[] payload = buildPayload(12, 0, (byte) 0x80, false, blocks);

        TelemetryMessage msg = new TelemetryMessage(payload, "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        assertEquals(28, decoded.getMeasurementData().size(),
                "Devono essere decodificati al massimo 28 blocchi");
    }

    // =========================================================================
    // INTEGRAZIONE (payload reale)
    // =========================================================================

    @Test
    void testFullDecode_msgType8_payloadReale() {
        // Payload reale da dispositivo TEK822V2 — msgType=8, 10 misurazioni valide
        String hex = "180203428918360864431047987054087B0931470008FF810F00"
                   + "0A6A2BFE0A6A28000A6A2BFE0A6A28430A6A28430A6A28430A6A28430A6A28430A6A28430A6A2800"
                   + "00000000000000000000000000000000000000000000000000000000"
                   + "00000000000000000000000000000000000000000000000000000000294A";
        byte[] payload = ControllerUtils.hexStringToByteArray(hex);
        TelemetryMessage msg = new TelemetryMessage(payload, hex, LocalDateTime.now(), "test");
        DecodedMessage decoded = new DecodedMessage();

        invokeDecode(msg, payload, decoded, 8);

        // Logger speed
        assertEquals(15, decoded.getUnitSetup().getLoggerSpeedMinutes());

        // Conteggio misurazioni valide (le restanti 18 sono blocchi zero → saltate)
        var measurements = decoded.getMeasurementData();
        assertNotNull(measurements);
        assertEquals(10, measurements.size());

        // Prima misura: 0A 6A 2B FE
        var m0 = measurements.get(0);
        assertEquals("Data 0",   m0.getMeasurementNum());
        assertEquals(1022,       m0.getDistanceCm());
        assertEquals(23.0,       m0.getTemperatureC(), 0.01);
        assertEquals("73.40",    m0.getTemperatureF());
        assertEquals(10,         m0.getSonicRssi()); // 0x0A & 0x0F
        assertEquals(10,         m0.getSonicSrc());  // (0x2B>>2) & 0x0F

        // Spaziatura 15 min tra tutte le misurazioni
        for (int i = 1; i < measurements.size(); i++) {
            Instant prev = Instant.parse(measurements.get(i - 1).getTimestamp());
            Instant curr = Instant.parse(measurements.get(i).getTimestamp());
            long diffMin = (prev.toEpochMilli() - curr.toEpochMilli()) / 60_000;
            assertEquals(15, diffMin,
                    "Spaziatura 15 min attesa tra Data " + (i - 1) + " e Data " + i);
        }
    }
}
