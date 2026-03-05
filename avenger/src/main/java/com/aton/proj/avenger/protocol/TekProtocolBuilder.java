package com.aton.proj.avenger.protocol;

import java.time.LocalTime;
import java.util.Random;

import com.aton.proj.avenger.model.SimulatedDevice;

/**
 * Builds binary TEK protocol messages compatible with the oneGasMeteor TCP server.
 *
 * <p>Message structure:
 * <pre>
 * Header (17 bytes):
 *   [0]      Product Type Code
 *   [1]      Hardware Revision  : (byte &amp; 0x07) + "." + ((byte &gt;&gt; 3) &amp; 0x1F)
 *   [2]      Firmware Revision  : (byte &amp; 0x1F) + "." + ((byte &gt;&gt; 5) &amp; 0x07)
 *   [3]      Contact Reason bitmap
 *   [4]      Alarm Status + Last Reset
 *   [5]      Signal Strength (RSSI 0-113 or CSQ 0-31 depending on product type)
 *   [6]      Diagnostic Info (bits 5-6) + Battery Status (bits 4:0)
 *   [7-14]   IMEI BCD packed (8 bytes, leading zero + 15 digits = 16 nibbles)
 *   [15]     Message Type (bits 5:0) + body length high (bits 7:6)
 *   [16]     Body length low
 *
 * Body (msgType 4/8/9, from byte 17):
 *   [17-18]  Message count
 *   [19]     Try attempts (bits 7:5) + RTC hours (bits 4:0)
 *   [20-21]  Energy used / last error code
 *   [22]     Reserved (0x00)
 *   [23]     Logger speed config
 *   [24]     Reserved (0x00)
 *   [25]     RTC minutes
 *   [26+]    4*N measurement bytes
 * </pre>
 *
 * <p>Product types using RSSI signal: 2, 5, 6, 9<br>
 * Product types using CSQ  signal: 7, 8, 10, 11, 23, 24, 25, 26, 27, 28<br>
 * Product types using battery %:   6, 8, 10, 23, 24, 27, 28<br>
 * Product types using battery V:   2, 5, 7, 9, 11, 25, 26
 */
public class TekProtocolBuilder {

    /** HW revision byte: (1 &amp; 0x07) | (1 &lt;&lt; 3) = 0x09 → "1.1" */
    private static final byte HW_REVISION = 0x09;

    /** FW revision byte: (3 &amp; 0x1F) | (1 &lt;&lt; 5) = 0x23 → "3.1" */
    private static final byte FW_REVISION = 0x23;

    /** Contact reason: 0x01 = Scheduled */
    private static final byte CONTACT_REASON_SCHEDULED = 0x01;

    /** Logger speed config: 0x04 → 4 * 15 min = 60 min (1 hour) for msgType 4 */
    private static final byte LOGGER_SPEED_1H = 0x04;

    /** Logger speed config: 0x00 with bit 7 clear → 1 min for msgType 8/9 */
    private static final byte LOGGER_SPEED_1MIN = 0x00;

    private final Random random;

    public TekProtocolBuilder(Random random) {
        this.random = random;
    }

    /**
     * Builds a complete TEK binary message for the given device.
     *
     * @param device        the simulated device
     * @param msgType       message type: 4 (standard), 8 (alarm), 9 (scheduled)
     * @param measurements  number of measurements to include (1-28)
     * @return the binary payload
     */
    public byte[] build(SimulatedDevice device, int msgType, int measurements) {
        byte[] measurementBytes = buildMeasurementBytes(measurements);
        int bodyLength = 9 + measurementBytes.length; // bytes 17-25 = 9 bytes, then measurements
        int totalLength = 17 + bodyLength;

        byte[] payload = new byte[totalLength];

        // ---- Header ----
        payload[0] = (byte) device.getProductTypeCode();
        payload[1] = HW_REVISION;
        payload[2] = FW_REVISION;
        payload[3] = CONTACT_REASON_SCHEDULED;
        payload[4] = 0x00; // no alarm, no last reset flags
        payload[5] = buildSignalStrengthByte(device.getProductTypeCode());
        payload[6] = buildDiagnosticBatteryByte(device.getProductTypeCode());

        encodeImei(device.getImei(), payload, 7);

        // byte[15]: msgType (bits 5:0) + bodyLength high bits (bits 7:6)
        payload[15] = (byte) ((msgType & 0x3F) | (((bodyLength >> 8) & 0x03) << 6));
        payload[16] = (byte) (bodyLength & 0xFF);

        // ---- Body ----
        int msgCount = random.nextInt(1, 500);
        payload[17] = (byte) ((msgCount >> 8) & 0xFF);
        payload[18] = (byte) (msgCount & 0xFF);

        LocalTime now = LocalTime.now();
        int rtcHours = now.getHour();
        int rtcMinutes = now.getMinute();

        // byte[19]: try attempts (bits 7:5) = 3 (max), RTC hours (bits 4:0)
        payload[19] = (byte) ((3 << 5) | (rtcHours & 0x1F));

        // bytes[20-21]: energy used (random realistic value in mAs)
        int energyUsed = random.nextInt(0, 2000);
        payload[20] = (byte) ((energyUsed >> 8) & 0xFF);
        payload[21] = (byte) (energyUsed & 0xFF);

        payload[22] = 0x00; // reserved

        // byte[23]: logger speed
        payload[23] = (msgType == 4) ? LOGGER_SPEED_1H : LOGGER_SPEED_1MIN;

        payload[24] = 0x00; // reserved
        payload[25] = (byte) (rtcMinutes & 0xFF);

        // measurement bytes starting at offset 26
        System.arraycopy(measurementBytes, 0, payload, 26, measurementBytes.length);

        return payload;
    }

    /**
     * Encodes a 15-digit IMEI string into 8 bytes BCD packed with a leading zero.
     * "0" + 15 digits = 16 nibbles = 8 bytes.
     */
    private void encodeImei(String imei, byte[] payload, int offset) {
        // Pad IMEI to exactly 15 digits
        String digits = String.format("%015d", Long.parseLong(imei));
        // Prepend leading zero → 16 nibbles
        String nibbles = "0" + digits;
        for (int i = 0; i < 8; i++) {
            int highNibble = Character.digit(nibbles.charAt(i * 2), 16);
            int lowNibble  = Character.digit(nibbles.charAt(i * 2 + 1), 16);
            payload[offset + i] = (byte) ((highNibble << 4) | lowNibble);
        }
    }

    /**
     * Generates the signal strength byte.
     * Product types 2, 5, 6, 9 use RSSI (0-113).
     * All others use CSQ (0-31).
     */
    private byte buildSignalStrengthByte(int productTypeCode) {
        return switch (productTypeCode) {
            case 2, 5, 6, 9 -> (byte) random.nextInt(0, 114); // RSSI 0-113
            default          -> (byte) random.nextInt(0, 32);  // CSQ 0-31
        };
    }

    /**
     * Generates the diagnostic info + battery status byte.
     * Bits 6:5 = diagnostic flags (RTC set, LTE active).
     * Bits 4:0 = battery value (0-31).
     * Product types 6, 8, 10, 23, 24, 27, 28 use percentage (0-31 → 0-100%).
     * All others use voltage (0-31 → 3.0-6.1V).
     */
    private byte buildDiagnosticBatteryByte(int productTypeCode) {
        int batteryValue = random.nextInt(10, 32); // realistic non-zero battery
        int diagnosticBits = 0x60; // bits 5 (RTC set) + 6 (LTE active) both set
        return (byte) (diagnosticBits | (batteryValue & 0x1F));
    }

    /**
     * Builds measurement data bytes (4 bytes per measurement).
     * Each measurement:
     *   byte[0]: Sonic RSSI (bits 3:0), upper nibble unused (0)
     *   byte[1]: Temperature encoded as (tempC + 30.0) * 2.0 in bits 6:0
     *   byte[2]: Sonic Source (bits 5:2) + Distance high 2 bits (bits 1:0)
     *   byte[3]: Distance low byte
     */
    private byte[] buildMeasurementBytes(int count) {
        byte[] data = new byte[count * 4];
        for (int i = 0; i < count; i++) {
            int base = i * 4;

            int sonicRssi  = random.nextInt(0, 16);   // 0-15
            double tempC   = 5.0 + random.nextDouble() * 30.0; // 5-35°C
            int tempEncoded = (int) ((tempC + 30.0) * 2.0);    // maps to bits 6:0
            int distanceCm = 10 + random.nextInt(0, 491);       // 10-500 cm
            int sonicSrc   = random.nextInt(0, 16);   // 0-15 (4 bits)

            data[base]     = (byte) (sonicRssi & 0x0F);
            data[base + 1] = (byte) (tempEncoded & 0x7F);
            data[base + 2] = (byte) ((sonicSrc << 2) | ((distanceCm >> 8) & 0x03));
            data[base + 3] = (byte) (distanceCm & 0xFF);
        }
        return data;
    }
}
