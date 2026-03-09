package com.aton.proj.oneGasMeteor.repository.impl.influxdb;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.influxdb.client.write.Point;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InfluxDBPointMapper.
 * Verifica che le entity JPA vengano convertite correttamente in InfluxDB Points.
 */
class InfluxDBPointMapperTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

    @Test
    void toPoint_telemetry_setsCorrectMeasurementAndTags() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.setDeviceId("862406075927406");
        entity.setDeviceType("TEK822V2");
        entity.setMessageType("4");
        entity.setReceivedAt(NOW);
        entity.setRawMessage("AABBCC");
        entity.setImei("862406075927406");
        entity.setFirmwareVersion("1.3");
        entity.setBatteryVoltage(3.8);
        entity.setBatteryPercentage(75.0);
        entity.setSignalStrength(20);
        entity.setMeasurementCount(3);

        Point point = InfluxDBPointMapper.toPoint(entity);

        assertNotNull(point);
        String lineProtocol = point.toLineProtocol();
        assertNotNull(lineProtocol);
        assertTrue(lineProtocol.startsWith("telemetry,"));
        assertTrue(lineProtocol.contains("device_id=862406075927406"));
        assertTrue(lineProtocol.contains("device_type=TEK822V2"));
        assertTrue(lineProtocol.contains("message_type=4"));
        assertTrue(lineProtocol.contains("battery_voltage=3.8"));
        assertTrue(lineProtocol.contains("signal_strength=20i"));
        assertTrue(lineProtocol.contains("measurement_count=3i"));
    }

    @Test
    void toPoint_telemetry_handlesNullOptionalFields() {
        TelemetryEntity entity = new TelemetryEntity();
        entity.setDeviceId("test-device");
        entity.setDeviceType("TEK822V1");
        entity.setMessageType("8");
        entity.setReceivedAt(NOW);
        entity.setRawMessage("AABB"); // at least one field required for a valid Point
        // batteryVoltage, batteryPercentage, signalStrength, measurementCount are null

        Point point = InfluxDBPointMapper.toPoint(entity);

        assertNotNull(point);
        String lineProtocol = point.toLineProtocol();
        assertNotNull(lineProtocol);
        assertTrue(lineProtocol.startsWith("telemetry,"));
        // Null fields should not appear in line protocol
        assertFalse(lineProtocol.contains("battery_voltage"));
        assertFalse(lineProtocol.contains("battery_percentage"));
        assertFalse(lineProtocol.contains("signal_strength"));
    }

    @Test
    void toPoint_deviceSettings_setsCorrectMeasurement() {
        DeviceSettingsEntity entity = new DeviceSettingsEntity();
        entity.setDeviceId("862406075927406");
        entity.setDeviceType("TEK822V2");
        entity.setRawMessage("CCDDEE");
        entity.setSettingsJson("{\"S0\":\"4\"}");
        entity.setReceivedAt(NOW);

        Point point = InfluxDBPointMapper.toPoint(entity);

        assertNotNull(point);
        String lineProtocol = point.toLineProtocol();
        assertNotNull(lineProtocol);
        assertTrue(lineProtocol.startsWith("device_settings,"));
        assertTrue(lineProtocol.contains("device_id=862406075927406"));
        assertTrue(lineProtocol.contains("settings_json"));
    }

    @Test
    void toPoint_deviceStatistics_setsCorrectMeasurement() {
        DeviceStatisticsEntity entity = new DeviceStatisticsEntity();
        entity.setDeviceId("862406075927406");
        entity.setDeviceType("TEK822V2");
        entity.setRawMessage("FFEEDD");
        entity.setIccid("89390100001234567890");
        entity.setEnergyUsed(12345L);
        entity.setMinTemperature(-10);
        entity.setMaxTemperature(40);
        entity.setMessageCount(100);
        entity.setDeliveryFailCount(2);
        entity.setAverageSendTime(250.5);
        entity.setAverageRssi(-70.0);
        entity.setDeliverySuccessRate(98.0);
        entity.setReceivedAt(NOW);

        Point point = InfluxDBPointMapper.toPoint(entity);

        assertNotNull(point);
        String lineProtocol = point.toLineProtocol();
        assertNotNull(lineProtocol);
        assertTrue(lineProtocol.startsWith("device_statistics,"));
        assertTrue(lineProtocol.contains("energy_used=12345i"));
        assertTrue(lineProtocol.contains("message_count=100i"));
        assertTrue(lineProtocol.contains("delivery_success_rate=98.0"));
    }

    @Test
    void toPoint_deviceLocations_setsCorrectMeasurement() {
        DeviceLocationEntity entity = new DeviceLocationEntity();
        entity.setDeviceId("862406075927406");
        entity.setDeviceType("TEK822V2");
        entity.setRawMessage("AABBCCDD");
        entity.setLatitude(45.4654);
        entity.setLongitude(9.1859);
        entity.setAltitude(122.0);
        entity.setSpeedKmh(0.0);
        entity.setSpeedKnots(0.0);
        entity.setNumberOfSatellites(8);
        entity.setUtcTime(LocalTime.of(10, 30, 0));
        entity.setReceivedAt(NOW);

        Point point = InfluxDBPointMapper.toPoint(entity);

        assertNotNull(point);
        String lineProtocol = point.toLineProtocol();
        assertNotNull(lineProtocol);
        assertTrue(lineProtocol.startsWith("device_locations,"));
        assertTrue(lineProtocol.contains("latitude=45.4654"));
        assertTrue(lineProtocol.contains("longitude=9.1859"));
        assertTrue(lineProtocol.contains("number_of_satellites=8i"));
    }

    @Test
    void toPoint_processingMetrics_setsCorrectMeasurementAndSuccessTag() {
        ProcessingMetricsEntity entity = new ProcessingMetricsEntity();
        entity.setDeviceId("862406075927406");
        entity.setDeviceType("TEK822V2");
        entity.setSuccess(true);
        entity.setMessageType(4);
        entity.setTotalProcessingTimeMs(42L);
        entity.setDecodeTimeMs(5L);
        entity.setDbSaveTimeMs(15L);
        entity.setCommandQueryTimeMs(10L);
        entity.setCommandEncodeTimeMs(2L);
        entity.setSendTimeMs(8L);
        entity.setReceivedAt(NOW);

        Point point = InfluxDBPointMapper.toPoint(entity);

        assertNotNull(point);
        String lineProtocol = point.toLineProtocol();
        assertNotNull(lineProtocol);
        assertTrue(lineProtocol.startsWith("processing_metrics,"));
        assertTrue(lineProtocol.contains("success=true"));
        assertTrue(lineProtocol.contains("total_processing_time_ms=42i"));
        assertTrue(lineProtocol.contains("decode_time_ms=5i"));
    }

    @Test
    void toPoint_processingMetrics_handlesNullDeviceId() {
        ProcessingMetricsEntity entity = new ProcessingMetricsEntity();
        entity.setDeviceId(null);
        entity.setDeviceType(null);
        entity.setSuccess(false);
        entity.setTotalProcessingTimeMs(10L); // at least one field required for a valid Point
        entity.setReceivedAt(NOW);

        Point point = InfluxDBPointMapper.toPoint(entity);

        assertNotNull(point);
        String lineProtocol = point.toLineProtocol();
        assertNotNull(lineProtocol);
        // Null device_id and device_type should default to "unknown"
        assertTrue(lineProtocol.contains("device_id=unknown"));
        assertTrue(lineProtocol.contains("device_type=unknown"));
        assertTrue(lineProtocol.contains("success=false"));
    }

    @Test
    void measurementConstants_haveExpectedValues() {
        assertEquals("telemetry", InfluxDBPointMapper.MEASUREMENT_TELEMETRY);
        assertEquals("device_settings", InfluxDBPointMapper.MEASUREMENT_SETTINGS);
        assertEquals("device_statistics", InfluxDBPointMapper.MEASUREMENT_STATISTICS);
        assertEquals("device_locations", InfluxDBPointMapper.MEASUREMENT_LOCATIONS);
        assertEquals("processing_metrics", InfluxDBPointMapper.MEASUREMENT_METRICS);
    }
}
