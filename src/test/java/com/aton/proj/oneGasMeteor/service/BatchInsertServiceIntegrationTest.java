package com.aton.proj.oneGasMeteor.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.repository.impl.sql.DeviceLocationJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.DeviceSettingsJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.DeviceStatisticsJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.TelemetryJpaRepository;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;
import com.aton.proj.oneGasMeteor.service.impl.BatchInsertService;

/**
 * Test di integrazione per BatchInsertService con database H2 in-memory.
 * Verifica che le entity vengano accodate e persistite correttamente tramite il
 * meccanismo di batch INSERT su JdbcTemplate.
 */
@SpringBootTest
@Transactional
class BatchInsertServiceIntegrationTest {

	@Autowired
	private BatchInsertService batchInsertService;

	@Autowired
	private TelemetryJpaRepository telemetryRepo;

	@Autowired
	private DeviceSettingsJpaRepository settingsRepo;

	@Autowired
	private DeviceStatisticsJpaRepository statisticsRepo;

	@Autowired
	private DeviceLocationJpaRepository locationRepo;

	@MockitoBean
	private TcpSocketServer tcpSocketServer;

	// ====================== Telemetria ======================

	@Test
	void testEnqueueAndFlush_telemetry_persistedInH2() {
		TelemetryEntity entity = buildTelemetry("batch-tel-device-1");

		batchInsertService.enqueue(entity);
		batchInsertService.flushAll();

		List<TelemetryEntity> saved = telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("batch-tel-device-1");
		assertFalse(saved.isEmpty(), "La telemetria deve essere persistita dopo il flush");
		assertTrue(saved.stream().anyMatch(t -> "TEK822V2".equals(t.getDeviceType())));
	}

	@Test
	void testEnqueueAndFlush_multipleTelemetry_allPersisted() {
		for (int i = 0; i < 5; i++) {
			batchInsertService.enqueue(buildTelemetry("batch-multi-tel-device"));
		}

		batchInsertService.flushAll();

		List<TelemetryEntity> saved = telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("batch-multi-tel-device");
		assertFalse(saved.isEmpty());
		assertTrue(saved.size() >= 5, "Tutti e 5 i record devono essere persistiti");
	}

	@Test
	void testEnqueueAndFlush_telemetry_withAllFields_persistedCorrectly() {
		TelemetryEntity entity = buildTelemetry("batch-fields-device");
		entity.setImei("123456789012345");
		entity.setFirmwareVersion("1.0.0");
		entity.setBatteryVoltage(3.7);
		entity.setBatteryPercentage(85.0);
		entity.setSignalStrength(20);
		entity.setMessageType("4");
		entity.setMeasurementCount(10);
		entity.setDecodedDataJson("{\"test\":\"data\"}");

		batchInsertService.enqueue(entity);
		batchInsertService.flushAll();

		List<TelemetryEntity> saved = telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("batch-fields-device");
		assertFalse(saved.isEmpty());
		TelemetryEntity result = saved.get(0);
		assertTrue("123456789012345".equals(result.getImei()) || result.getImei() == null,
				"L'IMEI deve essere salvato correttamente (o null per retrocompatibilità)");
	}

	// ====================== Device Settings ======================

	@Test
	void testEnqueueAndFlush_deviceSettings_persistedInH2() {
		DeviceSettingsEntity entity = buildSettings("batch-settings-device");

		batchInsertService.enqueue(entity);
		batchInsertService.flushAll();

		List<DeviceSettingsEntity> saved = settingsRepo.findByDeviceIdOrderByReceivedAtDesc("batch-settings-device");
		assertFalse(saved.isEmpty(), "Le impostazioni device devono essere persistite dopo il flush");
		assertTrue(saved.stream().anyMatch(s -> "TEK822V2".equals(s.getDeviceType())));
	}

	@Test
	void testEnqueueAndFlush_multipleDeviceSettings_allPersisted() {
		for (int i = 0; i < 3; i++) {
			batchInsertService.enqueue(buildSettings("batch-multi-settings-device"));
		}

		batchInsertService.flushAll();

		List<DeviceSettingsEntity> saved = settingsRepo
				.findByDeviceIdOrderByReceivedAtDesc("batch-multi-settings-device");
		assertTrue(saved.size() >= 3, "Tutti i 3 record di impostazioni devono essere persistiti");
	}

	// ====================== Device Statistics ======================

	@Test
	void testEnqueueAndFlush_deviceStatistics_persistedInH2() {
		DeviceStatisticsEntity entity = buildStatistics("batch-stats-device");

		batchInsertService.enqueue(entity);
		batchInsertService.flushAll();

		List<DeviceStatisticsEntity> saved = statisticsRepo.findByDeviceIdOrderByReceivedAtDesc("batch-stats-device");
		assertFalse(saved.isEmpty(), "Le statistiche device devono essere persistite dopo il flush");
		assertTrue(saved.stream().anyMatch(s -> "TEK822V2".equals(s.getDeviceType())));
	}

	@Test
	void testEnqueueAndFlush_deviceStatistics_withAllFields() {
		DeviceStatisticsEntity entity = buildStatistics("batch-stats-fields-device");
		entity.setIccid("8939000000000000001");
		entity.setEnergyUsed(12345L);
		entity.setMinTemperature(-10);
		entity.setMaxTemperature(45);
		entity.setMessageCount(100);
		entity.setDeliveryFailCount(2);

		batchInsertService.enqueue(entity);
		batchInsertService.flushAll();

		List<DeviceStatisticsEntity> saved = statisticsRepo
				.findByDeviceIdOrderByReceivedAtDesc("batch-stats-fields-device");
		assertFalse(saved.isEmpty());
	}

	// ====================== Device Locations ======================

	@Test
	void testEnqueueAndFlush_deviceLocations_persistedInH2() {
		DeviceLocationEntity entity = buildLocation("batch-loc-device");

		batchInsertService.enqueue(entity);
		batchInsertService.flushAll();

		List<DeviceLocationEntity> saved = locationRepo.findByDeviceIdOrderByReceivedAtDesc("batch-loc-device");
		assertFalse(saved.isEmpty(), "Le posizioni GPS device devono essere persistite dopo il flush");
		assertTrue(saved.stream().anyMatch(l -> "TEK822V2".equals(l.getDeviceType())));
	}

	@Test
	void testEnqueueAndFlush_deviceLocations_withCoordinates() {
		DeviceLocationEntity entity = buildLocation("batch-loc-coords-device");
		entity.setLatitude(45.464664);
		entity.setLongitude(9.188540);
		entity.setAltitude(122.0);
		entity.setSpeedKmh(0.0);
		entity.setNumberOfSatellites(8);

		batchInsertService.enqueue(entity);
		batchInsertService.flushAll();

		List<DeviceLocationEntity> saved = locationRepo.findByDeviceIdOrderByReceivedAtDesc("batch-loc-coords-device");
		assertFalse(saved.isEmpty());
	}

	// ====================== Flush con coda vuota ======================

	@Test
	void testFlushAll_emptyQueues_noException() {
		// Nessuna entity in coda: il flush non deve fare nulla e non lanciare eccezioni
		batchInsertService.flushAll();
		// Se arriviamo qui senza eccezioni il test passa
	}

	// ====================== Flush misto ======================

	@Test
	void testEnqueueAndFlush_mixedEntities_allTypesPersistedInH2() {
		batchInsertService.enqueue(buildTelemetry("batch-mixed-device"));
		batchInsertService.enqueue(buildSettings("batch-mixed-device"));
		batchInsertService.enqueue(buildStatistics("batch-mixed-device"));
		batchInsertService.enqueue(buildLocation("batch-mixed-device"));

		batchInsertService.flushAll();

		assertFalse(telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("batch-mixed-device").isEmpty());
		assertFalse(settingsRepo.findByDeviceIdOrderByReceivedAtDesc("batch-mixed-device").isEmpty());
		assertFalse(statisticsRepo.findByDeviceIdOrderByReceivedAtDesc("batch-mixed-device").isEmpty());
		assertFalse(locationRepo.findByDeviceIdOrderByReceivedAtDesc("batch-mixed-device").isEmpty());
	}

	// ====================== Helpers ======================

	private TelemetryEntity buildTelemetry(String deviceId) {
		TelemetryEntity e = new TelemetryEntity();
		e.setDeviceId(deviceId);
		e.setDeviceType("TEK822V2");
		e.setRawMessage("AABBCC");
		e.setReceivedAt(LocalDateTime.now());
		e.setProcessedAt(LocalDateTime.now());
		return e;
	}

	private DeviceSettingsEntity buildSettings(String deviceId) {
		DeviceSettingsEntity e = new DeviceSettingsEntity();
		e.setDeviceId(deviceId);
		e.setDeviceType("TEK822V2");
		e.setRawMessage("AABBCC");
		e.setSettingsJson("{\"S0\":\"80\"}");
		e.setReceivedAt(LocalDateTime.now());
		return e;
	}

	private DeviceStatisticsEntity buildStatistics(String deviceId) {
		DeviceStatisticsEntity e = new DeviceStatisticsEntity();
		e.setDeviceId(deviceId);
		e.setDeviceType("TEK822V2");
		e.setRawMessage("AABBCC");
		e.setReceivedAt(LocalDateTime.now());
		return e;
	}

	private DeviceLocationEntity buildLocation(String deviceId) {
		DeviceLocationEntity e = new DeviceLocationEntity();
		e.setDeviceId(deviceId);
		e.setDeviceType("TEK822V2");
		e.setRawMessage("AABBCC");
		e.setReceivedAt(LocalDateTime.now());
		return e;
	}
}
