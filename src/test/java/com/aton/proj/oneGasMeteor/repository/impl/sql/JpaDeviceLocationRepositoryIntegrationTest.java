package com.aton.proj.oneGasMeteor.repository.impl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.model.MessageType17Response;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

/**
 * Test di integrazione per JpaDeviceLocationRepository con database H2 in-memory.
 * Verifica la logica di persistenza, ricerca e cancellazione delle posizioni GPS dispositivo.
 */
@SpringBootTest
@Transactional
class JpaDeviceLocationRepositoryIntegrationTest {

    @Autowired
    private JpaDeviceLocationRepository repository;

    @MockitoBean
    private TcpSocketServer tcpSocketServer;

    // ====================== save ======================

    @Test
    void testSave_persistsLocationWithMinimalData() {
        MessageType17Response gps = buildGps("loc-dev-001", "TEK822V2");

        DeviceLocationEntity saved = repository.save(gps, "raw-loc-001");

        assertNotNull(saved.getId());
        assertEquals("loc-dev-001", saved.getDeviceId());
        assertEquals("TEK822V2", saved.getDeviceType());
        assertEquals("raw-loc-001", saved.getRawMessage());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    void testSave_persistsLocationWithFullGpsData() {
        MessageType17Response gps = buildGps("loc-dev-002", "TEK822V2");
        gps.setLatitude(45.4654219);
        gps.setLongitude(9.1859243);
        gps.setAltitude(122.5);
        gps.setSpeedKmh(0.0);
        gps.setSpeedKnots(0.0);
        gps.setGroundHeading(270.0);
        gps.setHorizontalPrecision(1.2);
        gps.setUtcTime(LocalTime.of(12, 30, 0));
        gps.setDate("120325");
        gps.setNumberOfSatellites(8);
        gps.setTimeToFixSeconds(15);
        gps.setGnssPositioningMode(2);

        DeviceLocationEntity saved = repository.save(gps, "raw-full-loc");

        assertEquals(45.4654219, saved.getLatitude(), 0.0000001);
        assertEquals(9.1859243, saved.getLongitude(), 0.0000001);
        assertEquals(122.5, saved.getAltitude(), 0.001);
        assertEquals(0.0, saved.getSpeedKmh(), 0.001);
        assertEquals(0.0, saved.getSpeedKnots(), 0.001);
        assertEquals(270.0, saved.getGroundHeading(), 0.001);
        assertEquals(1.2, saved.getHorizontalPrecision(), 0.001);
        assertEquals(LocalTime.of(12, 30, 0), saved.getUtcTime());
        assertEquals("120325", saved.getDate());
        assertEquals(8, saved.getNumberOfSatellites());
        assertEquals(15, saved.getTimeToFixSeconds());
        assertEquals(2, saved.getGnssPositioningMode());
    }

    @Test
    void testSave_persistsLocationWithRawCoordinates() {
        MessageType17Response gps = buildGps("loc-dev-003", "TEK822V2");
        gps.setLatitudeRaw("4527.9253N");
        gps.setLongitudeRaw("00911.1555E");

        DeviceLocationEntity saved = repository.save(gps, "raw-coord");

        assertEquals("4527.9253N", saved.getLatitudeRaw());
        assertEquals("00911.1555E", saved.getLongitudeRaw());
    }

    // ====================== buildEntity ======================

    @Test
    void testBuildEntity_doesNotPersistInDatabase() {
        MessageType17Response gps = buildGps("build-loc-dev", "TEK822V2");
        gps.setLatitude(45.0);
        gps.setLongitude(9.0);

        DeviceLocationEntity entity = repository.buildEntity(gps, "raw-build");

        assertNotNull(entity);
        assertEquals("build-loc-dev", entity.getDeviceId());
        assertEquals("TEK822V2", entity.getDeviceType());
        assertEquals("raw-build", entity.getRawMessage());
        assertNotNull(entity.getReceivedAt());
        assertEquals(45.0, entity.getLatitude(), 0.001);
        assertEquals(9.0, entity.getLongitude(), 0.001);
        // Verifica che NON sia persistita
        assertNull(entity.getId(), "buildEntity non deve persistere l'entità nel DB");
        List<DeviceLocationEntity> inDb = repository.findByDeviceId("build-loc-dev");
        assertTrue(inDb.isEmpty(), "buildEntity non deve salvare nel DB");
    }

    @Test
    void testBuildEntity_copiesAllGpsFields() {
        MessageType17Response gps = buildGps("build-full-loc-dev", "TEK822V2");
        gps.setNumberOfSatellites(10);
        gps.setTimeToFixSeconds(5);
        gps.setGnssPositioningMode(1);
        gps.setAltitude(200.0);
        gps.setSpeedKmh(60.5);
        gps.setDate("120325");

        DeviceLocationEntity entity = repository.buildEntity(gps, "raw");

        assertEquals(10, entity.getNumberOfSatellites());
        assertEquals(5, entity.getTimeToFixSeconds());
        assertEquals(1, entity.getGnssPositioningMode());
        assertEquals(200.0, entity.getAltitude(), 0.001);
        assertEquals(60.5, entity.getSpeedKmh(), 0.001);
        assertEquals("120325", entity.getDate());
    }

    // ====================== findByDeviceId ======================

    @Test
    void testFindByDeviceId_returnsAllRecordsForDevice() {
        MessageType17Response gps = buildGps("find-loc-dev", "TEK822V2");
        repository.save(gps, "raw-1");
        repository.save(gps, "raw-2");
        repository.save(gps, "raw-3");

        List<DeviceLocationEntity> results = repository.findByDeviceId("find-loc-dev");

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(e -> "find-loc-dev".equals(e.getDeviceId())));
    }

    @Test
    void testFindByDeviceId_noRecords_returnsEmpty() {
        List<DeviceLocationEntity> results = repository.findByDeviceId("nonexistent-loc-dev-xyz");

        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByDeviceId_onlyReturnsRecordsForRequestedDevice() {
        repository.save(buildGps("loc-dev-A", "TEK822V2"), "raw-a");
        repository.save(buildGps("loc-dev-B", "TEK822V2"), "raw-b");

        List<DeviceLocationEntity> resultsA = repository.findByDeviceId("loc-dev-A");
        List<DeviceLocationEntity> resultsB = repository.findByDeviceId("loc-dev-B");

        assertEquals(1, resultsA.size());
        assertEquals("loc-dev-A", resultsA.get(0).getDeviceId());
        assertEquals(1, resultsB.size());
        assertEquals("loc-dev-B", resultsB.get(0).getDeviceId());
    }

    // ====================== deleteOlderThan ======================

    @Test
    void testDeleteOlderThan_deletesExpiredRecords() {
        MessageType17Response gps = buildGps("delete-loc-dev", "TEK822V2");
        DeviceLocationEntity saved = repository.save(gps, "raw-delete");

        // Sposta la data nel passato tramite il repository JPA diretto
        DeviceLocationJpaRepository jpaRepo = deviceLocationJpaRepository;
        jpaRepo.findById(saved.getId()).ifPresent(e -> {
            e.setReceivedAt(LocalDateTime.now().minusDays(60));
            jpaRepo.save(e);
            jpaRepo.flush();
        });

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<DeviceLocationEntity> results = repository.findByDeviceId("delete-loc-dev");
        assertTrue(results.isEmpty(), "Le posizioni con più di 30 giorni devono essere eliminate");
    }

    @Test
    void testDeleteOlderThan_keepsRecentRecords() {
        MessageType17Response gps = buildGps("keep-loc-dev", "TEK822V2");
        repository.save(gps, "raw-keep");

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<DeviceLocationEntity> results = repository.findByDeviceId("keep-loc-dev");
        assertFalse(results.isEmpty(), "Le posizioni recenti non devono essere eliminate");
    }

    // ====================== Helpers ======================

    @Autowired
    private DeviceLocationJpaRepository deviceLocationJpaRepository;

    private MessageType17Response buildGps(String deviceId, String deviceType) {
        MessageType17Response gps = new MessageType17Response();
        gps.setDeviceId(deviceId);
        gps.setDeviceType(deviceType);
        return gps;
    }
}
