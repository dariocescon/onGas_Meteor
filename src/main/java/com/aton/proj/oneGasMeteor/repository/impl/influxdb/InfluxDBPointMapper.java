package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import java.time.ZoneOffset;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

/**
 * Utility per convertire entity JPA in InfluxDB Point.
 * Tags = campi indicizzati a bassa cardinalità (per filtri/group by).
 * Fields = valori numerici e stringhe ad alta cardinalità.
 */
public final class InfluxDBPointMapper {

    public static final String MEASUREMENT_TELEMETRY = "telemetry";
    public static final String MEASUREMENT_SETTINGS = "device_settings";
    public static final String MEASUREMENT_STATISTICS = "device_statistics";
    public static final String MEASUREMENT_LOCATIONS = "device_locations";
    public static final String MEASUREMENT_METRICS = "processing_metrics";

    private InfluxDBPointMapper() {
    }

    public static Point toPoint(TelemetryEntity e) {
        Point p = Point.measurement(MEASUREMENT_TELEMETRY)
                .addTag("device_id", e.getDeviceId())
                .addTag("device_type", e.getDeviceType())
                .addTag("message_type", e.getMessageType())
                .time(e.getReceivedAt().toInstant(ZoneOffset.UTC), WritePrecision.MS);

        addStringField(p, "raw_message", e.getRawMessage());
        addStringField(p, "decoded_data", e.getDecodedDataJson());
        addStringField(p, "imei", e.getImei());
        addStringField(p, "firmware_version", e.getFirmwareVersion());
        addDoubleField(p, "battery_voltage", e.getBatteryVoltage());
        addDoubleField(p, "battery_percentage", e.getBatteryPercentage());
        addIntField(p, "signal_strength", e.getSignalStrength());
        addIntField(p, "measurement_count", e.getMeasurementCount());

        return p;
    }

    public static Point toPoint(DeviceSettingsEntity e) {
        Point p = Point.measurement(MEASUREMENT_SETTINGS)
                .addTag("device_id", e.getDeviceId())
                .addTag("device_type", e.getDeviceType())
                .time(e.getReceivedAt().toInstant(ZoneOffset.UTC), WritePrecision.MS);

        addStringField(p, "raw_message", e.getRawMessage());
        addStringField(p, "settings_json", e.getSettingsJson());

        return p;
    }

    public static Point toPoint(DeviceStatisticsEntity e) {
        Point p = Point.measurement(MEASUREMENT_STATISTICS)
                .addTag("device_id", e.getDeviceId())
                .addTag("device_type", e.getDeviceType())
                .time(e.getReceivedAt().toInstant(ZoneOffset.UTC), WritePrecision.MS);

        addStringField(p, "raw_message", e.getRawMessage());
        addStringField(p, "iccid", e.getIccid());
        addLongField(p, "energy_used", e.getEnergyUsed());
        addIntField(p, "min_temperature", e.getMinTemperature());
        addIntField(p, "max_temperature", e.getMaxTemperature());
        addIntField(p, "message_count", e.getMessageCount());
        addIntField(p, "delivery_fail_count", e.getDeliveryFailCount());
        addLongField(p, "total_send_time", e.getTotalSendTime());
        addLongField(p, "max_send_time", e.getMaxSendTime());
        addLongField(p, "min_send_time", e.getMinSendTime());
        addLongField(p, "rssi_total", e.getRssiTotal());
        addIntField(p, "rssi_valid_count", e.getRssiValidCount());
        addIntField(p, "rssi_fail_count", e.getRssiFailCount());
        addDoubleField(p, "average_send_time", e.getAverageSendTime());
        addDoubleField(p, "average_rssi", e.getAverageRssi());
        addDoubleField(p, "delivery_success_rate", e.getDeliverySuccessRate());

        return p;
    }

    public static Point toPoint(DeviceLocationEntity e) {
        Point p = Point.measurement(MEASUREMENT_LOCATIONS)
                .addTag("device_id", e.getDeviceId())
                .addTag("device_type", e.getDeviceType())
                .time(e.getReceivedAt().toInstant(ZoneOffset.UTC), WritePrecision.MS);

        addStringField(p, "raw_message", e.getRawMessage());
        addDoubleField(p, "latitude", e.getLatitude());
        addDoubleField(p, "longitude", e.getLongitude());
        addStringField(p, "latitude_raw", e.getLatitudeRaw());
        addStringField(p, "longitude_raw", e.getLongitudeRaw());
        addDoubleField(p, "altitude", e.getAltitude());
        addDoubleField(p, "speed_kmh", e.getSpeedKmh());
        addDoubleField(p, "speed_knots", e.getSpeedKnots());
        addDoubleField(p, "ground_heading", e.getGroundHeading());
        addDoubleField(p, "horizontal_precision", e.getHorizontalPrecision());
        if (e.getUtcTime() != null) {
            p.addField("utc_time", e.getUtcTime().toString());
        }
        addStringField(p, "gps_date", e.getDate());
        addIntField(p, "number_of_satellites", e.getNumberOfSatellites());
        addIntField(p, "time_to_fix_seconds", e.getTimeToFixSeconds());
        addIntField(p, "gnss_positioning_mode", e.getGnssPositioningMode());

        return p;
    }

    public static Point toPoint(ProcessingMetricsEntity e) {
        Point p = Point.measurement(MEASUREMENT_METRICS)
                .addTag("device_id", e.getDeviceId() != null ? e.getDeviceId() : "unknown")
                .addTag("device_type", e.getDeviceType() != null ? e.getDeviceType() : "unknown")
                .addTag("success", e.getSuccess() != null ? e.getSuccess().toString() : "false")
                .time(e.getReceivedAt().toInstant(ZoneOffset.UTC), WritePrecision.MS);

        addIntField(p, "message_type", e.getMessageType());
        addStringField(p, "client_address", e.getClientAddress());
        addIntField(p, "payload_length_bytes", e.getPayloadLengthBytes());
        addIntField(p, "declared_body_length", e.getDeclaredBodyLength());
        addIntField(p, "measurement_count", e.getMeasurementCount());
        addIntField(p, "pending_commands_found", e.getPendingCommandsFound());
        addIntField(p, "commands_sent", e.getCommandsSent());
        addIntField(p, "response_size_bytes", e.getResponseSizeBytes());
        addLongField(p, "total_processing_time_ms", e.getTotalProcessingTimeMs());
        addLongField(p, "read_time_ms", e.getReadTimeMs());
        addLongField(p, "decode_time_ms", e.getDecodeTimeMs());
        addLongField(p, "db_save_time_ms", e.getDbSaveTimeMs());
        addLongField(p, "command_query_time_ms", e.getCommandQueryTimeMs());
        addLongField(p, "command_encode_time_ms", e.getCommandEncodeTimeMs());
        addLongField(p, "send_time_ms", e.getSendTimeMs());
        addDoubleField(p, "battery_voltage", e.getBatteryVoltage());
        addDoubleField(p, "battery_percentage", e.getBatteryPercentage());
        addIntField(p, "signal_strength", e.getSignalStrength());
        addStringField(p, "contact_reason", e.getContactReason());
        addStringField(p, "firmware_version", e.getFirmwareVersion());
        addStringField(p, "error_message", e.getErrorMessage());

        return p;
    }

    // -- Null-safe field helpers --

    private static void addStringField(Point p, String key, String value) {
        if (value != null) {
            p.addField(key, value);
        }
    }

    private static void addDoubleField(Point p, String key, Double value) {
        if (value != null) {
            p.addField(key, value);
        }
    }

    private static void addIntField(Point p, String key, Integer value) {
        if (value != null) {
            p.addField(key, (long) value);
        }
    }

    private static void addLongField(Point p, String key, Long value) {
        if (value != null) {
            p.addField(key, value);
        }
    }
}
