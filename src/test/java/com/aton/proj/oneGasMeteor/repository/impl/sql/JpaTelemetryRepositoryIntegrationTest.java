package com.aton.proj.oneGasMeteor.repository.impl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

/**
 * Test di integrazione per JpaTelemetryRepository con database H2 in-memory.
 * Verifica la logica di persistenza, ricerca e cancellazione della telemetria.
 */
@SpringBootTest
@Transactional
class JpaTelemetryRepositoryIntegrationTest {

    @Autowired
    private JpaTelemetryRepository repository;

    @MockitoBean
    private TcpSocketServer tcpSocketServer;

    // ====================== save ======================

    @Test
    void testSave_persistsTelemetryWithMinimalData() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");

        TelemetryEntity saved = repository.save("dev-001", "TEK822V2", "raw-msg-001", decoded);

        assertNotNull(saved.getId());
        assertEquals("dev-001", saved.getDeviceId());
        assertEquals("TEK822V2", saved.getDeviceType());
        assertEquals("raw-msg-001", saved.getRawMessage());
        assertNotNull(saved.getReceivedAt());
        assertNotNull(saved.getProcessedAt());
        assertEquals("4", saved.getMessageType());
    }

    @Test
    void testSave_persistsTelemetryWithImei() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        DecodedMessage.UniqueIdentifier uid = new DecodedMessage.UniqueIdentifier();
        uid.setImei("123456789012345");
        decoded.setUniqueIdentifier(uid);

        TelemetryEntity saved = repository.save("dev-002", "TEK822V2", "raw-msg-002", decoded);

        assertNotNull(saved.getId());
        assertEquals("123456789012345", saved.getImei());
    }

    @Test
    void testSave_persistsTelemetryWithFirmwareVersion() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        DecodedMessage.UnitInfo unitInfo = new DecodedMessage.UnitInfo();
        unitInfo.setFirmwareRevision("2.1.0");
        decoded.setUnitInfo(unitInfo);

        TelemetryEntity saved = repository.save("dev-003", "TEK822V2", "raw-msg-003", decoded);

        assertEquals("2.1.0", saved.getFirmwareVersion());
    }

    @Test
    void testSave_persistsTelemetryWithBatteryData() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        DecodedMessage.BatteryStatus battery = new DecodedMessage.BatteryStatus();
        battery.setBatteryVoltage("3.7");
        battery.setBatteryRemainingPercentage("85.0");
        decoded.setBatteryStatus(battery);

        TelemetryEntity saved = repository.save("dev-004", "TEK822V2", "raw-msg-004", decoded);

        assertEquals(3.7, saved.getBatteryVoltage(), 0.001);
        assertEquals(85.0, saved.getBatteryPercentage(), 0.001);
    }

    @Test
    void testSave_persistsTelemetryWithSignalStrength_csqPreferred() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        DecodedMessage.SignalStrength signal = new DecodedMessage.SignalStrength();
        signal.setCsq(20);
        signal.setRssi(-70);
        decoded.setSignalStrength(signal);

        TelemetryEntity saved = repository.save("dev-005", "TEK822V2", "raw-msg-005", decoded);

        assertEquals(20, saved.getSignalStrength());
    }

    @Test
    void testSave_persistsTelemetryWithSignalStrength_rssiWhenNoCsq() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        DecodedMessage.SignalStrength signal = new DecodedMessage.SignalStrength();
        signal.setRssi(-70);
        decoded.setSignalStrength(signal);

        TelemetryEntity saved = repository.save("dev-006", "TEK822V2", "raw-msg-006", decoded);

        assertEquals(-70, saved.getSignalStrength());
    }

    @Test
    void testSave_persistsTelemetryWithMeasurementCount() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        List<DecodedMessage.MeasurementData> measurements = List.of(
                new DecodedMessage.MeasurementData(),
                new DecodedMessage.MeasurementData(),
                new DecodedMessage.MeasurementData());
        decoded.setMeasurementData(measurements);

        TelemetryEntity saved = repository.save("dev-007", "TEK822V2", "raw-msg-007", decoded);

        assertEquals(3, saved.getMeasurementCount());
    }

    // ====================== findById ======================

    @Test
    void testFindById_returnsCorrectEntity() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        TelemetryEntity saved = repository.save("dev-findbyid", "TEK822V2", "raw-findbyid", decoded);

        Optional<TelemetryEntity> found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("dev-findbyid", found.get().getDeviceId());
    }

    @Test
    void testFindById_notFound_returnsEmpty() {
        Optional<TelemetryEntity> found = repository.findById(999999L);

        assertFalse(found.isPresent());
    }

    // ====================== findByDeviceId ======================

    @Test
    void testFindByDeviceId_returnsAllRecordsForDevice() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        repository.save("dev-multi", "TEK822V2", "raw-1", decoded);
        repository.save("dev-multi", "TEK822V2", "raw-2", decoded);
        repository.save("dev-multi", "TEK822V2", "raw-3", decoded);

        List<TelemetryEntity> results = repository.findByDeviceId("dev-multi");

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(e -> "dev-multi".equals(e.getDeviceId())));
    }

    @Test
    void testFindByDeviceId_noRecords_returnsEmpty() {
        List<TelemetryEntity> results = repository.findByDeviceId("dev-nonexistent-xyz");

        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByDeviceId_onlyReturnsRecordsForRequestedDevice() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        repository.save("dev-A", "TEK822V2", "raw-a", decoded);
        repository.save("dev-B", "TEK822V2", "raw-b", decoded);

        List<TelemetryEntity> resultsA = repository.findByDeviceId("dev-A");
        List<TelemetryEntity> resultsB = repository.findByDeviceId("dev-B");

        assertEquals(1, resultsA.size());
        assertEquals("dev-A", resultsA.get(0).getDeviceId());
        assertEquals(1, resultsB.size());
        assertEquals("dev-B", resultsB.get(0).getDeviceId());
    }

    // ====================== findByDeviceIdAndDateRange ======================

    @Test
    void testFindByDeviceIdAndDateRange_returnsRecordsWithinRange() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        TelemetryEntity saved = repository.save("dev-range", "TEK822V2", "raw-range", decoded);

        LocalDateTime from = LocalDateTime.now().minusMinutes(5);
        LocalDateTime to = LocalDateTime.now().plusMinutes(5);

        List<TelemetryEntity> results = repository.findByDeviceIdAndDateRange("dev-range", from, to);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getId().equals(saved.getId())));
    }

    @Test
    void testFindByDeviceIdAndDateRange_noRecordsInRange_returnsEmpty() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        repository.save("dev-range-empty", "TEK822V2", "raw-range-empty", decoded);

        // Intervallo nel futuro: nessun record deve essere incluso
        LocalDateTime from = LocalDateTime.now().plusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(2);

        List<TelemetryEntity> results = repository.findByDeviceIdAndDateRange("dev-range-empty", from, to);

        assertTrue(results.isEmpty());
    }

    // ====================== findByImei ======================

    @Test
    void testFindByImei_returnsRecordsWithMatchingImei() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        DecodedMessage.UniqueIdentifier uid = new DecodedMessage.UniqueIdentifier();
        uid.setImei("999888777666555");
        decoded.setUniqueIdentifier(uid);
        repository.save("dev-imei", "TEK822V2", "raw-imei", decoded);

        List<TelemetryEntity> results = repository.findByImei("999888777666555");

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(e -> "999888777666555".equals(e.getImei())));
    }

    @Test
    void testFindByImei_noMatch_returnsEmpty() {
        List<TelemetryEntity> results = repository.findByImei("000000000000000");

        assertTrue(results.isEmpty());
    }

    // ====================== findByDeviceType ======================

    @Test
    void testFindByDeviceType_returnsRecordsMatchingDeviceType() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        repository.save("dev-type-1", "TEK822V2", "raw-type-1", decoded);
        repository.save("dev-type-2", "TEK822V2", "raw-type-2", decoded);

        List<TelemetryEntity> results = repository.findByDeviceType("TEK822V2");

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(e -> "TEK822V2".equals(e.getDeviceType())));
    }

    @Test
    void testFindByDeviceType_noMatch_returnsEmpty() {
        List<TelemetryEntity> results = repository.findByDeviceType("UNKNOWN_DEVICE_TYPE_XYZ");

        assertTrue(results.isEmpty());
    }

    // ====================== countByDeviceId ======================

    @Test
    void testCountByDeviceId_returnsCorrectCount() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        repository.save("dev-count", "TEK822V2", "raw-c1", decoded);
        repository.save("dev-count", "TEK822V2", "raw-c2", decoded);

        long count = repository.countByDeviceId("dev-count");

        assertEquals(2L, count);
    }

    @Test
    void testCountByDeviceId_noRecords_returnsZero() {
        long count = repository.countByDeviceId("dev-nonexistent-count");

        assertEquals(0L, count);
    }

    // ====================== deleteOlderThan ======================

    @Test
    void testDeleteOlderThan_deletesExpiredRecords() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        TelemetryEntity saved = repository.save("dev-delete", "TEK822V2", "raw-delete", decoded);

        // Modifica la data a un valore nel passato tramite il JPA repository diretto
        // Usa il TelemetryJpaRepository per impostare una data vecchia
        TelemetryJpaRepository jpaRepo = getJpaRepository();
        jpaRepo.findById(saved.getId()).ifPresent(e -> {
            e.setReceivedAt(LocalDateTime.now().minusDays(60));
            jpaRepo.save(e);
            jpaRepo.flush();
        });

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<TelemetryEntity> results = repository.findByDeviceId("dev-delete");
        assertTrue(results.isEmpty(), "I record con più di 30 giorni devono essere eliminati");
    }

    @Test
    void testDeleteOlderThan_keepsRecentRecords() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");
        repository.save("dev-keep", "TEK822V2", "raw-keep", decoded);

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<TelemetryEntity> results = repository.findByDeviceId("dev-keep");
        assertFalse(results.isEmpty(), "I record recenti non devono essere eliminati");
    }

    // ====================== buildEntity ======================

    @Test
    void testBuildEntity_doesNotPersist() {
        DecodedMessage decoded = new DecodedMessage();
        decoded.setMessageType("4");

        TelemetryEntity entity = repository.buildEntity("dev-build", "TEK822V2", "raw-build", decoded);

        assertNotNull(entity);
        assertNotNull(entity.getDeviceId());
        assertEquals("dev-build", entity.getDeviceId());
        // Non è stato salvato, quindi non ha un ID
        assertNotNull(entity.getDecodedDataJson());
        assertEquals("4", entity.getMessageType());
        // Verifica che NON sia persistito
        List<TelemetryEntity> inDb = repository.findByDeviceId("dev-build");
        assertTrue(inDb.isEmpty(), "buildEntity non deve persistere l'entità nel DB");
    }

    // ====================== Helpers ======================

    @Autowired
    private TelemetryJpaRepository telemetryJpaRepository;

    private TelemetryJpaRepository getJpaRepository() {
        return telemetryJpaRepository;
    }
}
