package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FluxQueryHelper.
 * Verifica che le query Flux generate siano sintatticamente corrette e contengano
 * i parametri attesi.
 */
class FluxQueryHelperTest {

    private static final String BUCKET = "oneGasDB";
    private static final String MEASUREMENT = "telemetry";
    private static final String DEVICE_ID = "862406075927406";
    private static final String DEVICE_TYPE = "TEK822V2";
    private static final String IMEI = "862406075927406";

    @Test
    void byDeviceId_containsRequiredElements() {
        String query = FluxQueryHelper.byDeviceId(BUCKET, MEASUREMENT, DEVICE_ID);

        assertNotNull(query);
        assertTrue(query.contains("from(bucket: \"" + BUCKET + "\")"));
        assertTrue(query.contains("_measurement == \"" + MEASUREMENT + "\""));
        assertTrue(query.contains("device_id == \"" + DEVICE_ID + "\""));
        assertTrue(query.contains("|> pivot("));
        assertTrue(query.contains("|> sort("));
        assertTrue(query.contains("range(start:"));
    }

    @Test
    void byDeviceIdAndDateRange_containsStartAndStop() {
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 6, 30, 23, 59, 59);

        String query = FluxQueryHelper.byDeviceIdAndDateRange(BUCKET, MEASUREMENT, DEVICE_ID, from, to);

        assertNotNull(query);
        assertTrue(query.contains("from(bucket: \"" + BUCKET + "\")"));
        assertTrue(query.contains("_measurement == \"" + MEASUREMENT + "\""));
        assertTrue(query.contains("device_id == \"" + DEVICE_ID + "\""));
        assertTrue(query.contains("range(start:"));
        assertTrue(query.contains("stop:"));
        // Verify both timestamps appear
        assertTrue(query.contains("2024-01-01"));
        assertTrue(query.contains("2024-06-30"));
    }

    @Test
    void byImei_containsImeiFilter() {
        String query = FluxQueryHelper.byImei(BUCKET, MEASUREMENT, IMEI);

        assertNotNull(query);
        assertTrue(query.contains("from(bucket: \"" + BUCKET + "\")"));
        assertTrue(query.contains("_measurement == \"" + MEASUREMENT + "\""));
        assertTrue(query.contains("r._field == \"imei\""));
        assertTrue(query.contains("r._value == \"" + IMEI + "\""));
        assertTrue(query.contains("|> pivot("));
    }

    @Test
    void byDeviceType_containsDeviceTypeTag() {
        String query = FluxQueryHelper.byDeviceType(BUCKET, MEASUREMENT, DEVICE_TYPE);

        assertNotNull(query);
        assertTrue(query.contains("from(bucket: \"" + BUCKET + "\")"));
        assertTrue(query.contains("_measurement == \"" + MEASUREMENT + "\""));
        assertTrue(query.contains("device_type == \"" + DEVICE_TYPE + "\""));
        assertTrue(query.contains("|> pivot("));
    }

    @Test
    void countByDeviceId_containsCountAndGroup() {
        String query = FluxQueryHelper.countByDeviceId(BUCKET, MEASUREMENT, DEVICE_ID);

        assertNotNull(query);
        assertTrue(query.contains("from(bucket: \"" + BUCKET + "\")"));
        assertTrue(query.contains("_measurement == \"" + MEASUREMENT + "\""));
        assertTrue(query.contains("device_id == \"" + DEVICE_ID + "\""));
        assertTrue(query.contains("|> group()"));
        assertTrue(query.contains("|> count()"));
    }

    @Test
    void deletePredicate_formatsCorrectly() {
        String predicate = FluxQueryHelper.deletePredicate(MEASUREMENT);

        assertNotNull(predicate);
        assertEquals("_measurement=\"" + MEASUREMENT + "\"", predicate);
    }

    @Test
    void deletePredicate_worksForAllMeasurements() {
        assertEquals("_measurement=\"telemetry\"",
                FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_TELEMETRY));
        assertEquals("_measurement=\"device_settings\"",
                FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_SETTINGS));
        assertEquals("_measurement=\"device_statistics\"",
                FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_STATISTICS));
        assertEquals("_measurement=\"device_locations\"",
                FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_LOCATIONS));
        assertEquals("_measurement=\"processing_metrics\"",
                FluxQueryHelper.deletePredicate(InfluxDBPointMapper.MEASUREMENT_METRICS));
    }

    @Test
    void byDeviceId_usesDefaultRange() {
        String query = FluxQueryHelper.byDeviceId(BUCKET, MEASUREMENT, DEVICE_ID);
        // Default range should be used (not empty)
        assertTrue(query.contains("range(start: -"));
    }

    @Test
    void queries_useDoubleQuotesForStringValues() {
        String query = FluxQueryHelper.byDeviceId(BUCKET, MEASUREMENT, DEVICE_ID);
        // Flux queries must use double quotes for string literals
        assertTrue(query.contains("\"" + BUCKET + "\""));
        assertTrue(query.contains("\"" + MEASUREMENT + "\""));
        assertTrue(query.contains("\"" + DEVICE_ID + "\""));
    }
}
