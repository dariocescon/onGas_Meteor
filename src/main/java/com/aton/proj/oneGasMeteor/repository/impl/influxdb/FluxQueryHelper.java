package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Helper per costruire query Flux per InfluxDB 2.x.
 * Le query Flux usano una sintassi pipe-based funzionale.
 */
public final class FluxQueryHelper {

    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /** Range di default per query senza limiti temporali: ultimi 365 giorni */
    private static final String DEFAULT_RANGE = "-365d";

    private FluxQueryHelper() {
    }

    /**
     * Query per device_id con pivot (restituisce righe con tutti i field come colonne).
     */
    public static String byDeviceId(String bucket, String measurement, String deviceId) {
        return String.format(
                "from(bucket: \"%s\") |> range(start: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r.device_id == \"%s\") " +
                        "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> sort(columns: [\"_time\"], desc: true)",
                bucket, DEFAULT_RANGE, measurement, deviceId);
    }

    /**
     * Query per device_id con range temporale.
     */
    public static String byDeviceIdAndDateRange(String bucket, String measurement,
            String deviceId, LocalDateTime from, LocalDateTime to) {
        String startTime = from.toInstant(ZoneOffset.UTC).toString();
        String stopTime = to.toInstant(ZoneOffset.UTC).toString();
        return String.format(
                "from(bucket: \"%s\") |> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r.device_id == \"%s\") " +
                        "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> sort(columns: [\"_time\"], desc: true)",
                bucket, startTime, stopTime, measurement, deviceId);
    }

    /**
     * Query per IMEI (solo telemetry).
     */
    public static String byImei(String bucket, String measurement, String imei) {
        return String.format(
                "from(bucket: \"%s\") |> range(start: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r._field == \"imei\" and r._value == \"%s\") " +
                        "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> sort(columns: [\"_time\"], desc: true)",
                bucket, DEFAULT_RANGE, measurement, imei);
    }

    /**
     * Query per device_type (tag).
     */
    public static String byDeviceType(String bucket, String measurement, String deviceType) {
        return String.format(
                "from(bucket: \"%s\") |> range(start: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r.device_type == \"%s\") " +
                        "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                        "|> sort(columns: [\"_time\"], desc: true)",
                bucket, DEFAULT_RANGE, measurement, deviceType);
    }

    /**
     * Count per device_id.
     */
    public static String countByDeviceId(String bucket, String measurement, String deviceId) {
        return String.format(
                "from(bucket: \"%s\") |> range(start: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") " +
                        "|> filter(fn: (r) => r.device_id == \"%s\") " +
                        "|> group() " +
                        "|> count()",
                bucket, DEFAULT_RANGE, measurement, deviceId);
    }

    /**
     * Costruisce il predicate per la Delete API di InfluxDB.
     * Formato: _measurement="xxx"
     */
    public static String deletePredicate(String measurement) {
        return String.format("_measurement=\"%s\"", measurement);
    }
}
