package com.aton.proj.oneGasMeteor.repository.impl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType16Response;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

/**
 * Test di integrazione per JpaDeviceStatisticsRepository con database H2 in-memory.
 * Verifica la logica di persistenza, ricerca e cancellazione delle statistiche dispositivo.
 */
@SpringBootTest
@Transactional
class JpaDeviceStatisticsRepositoryIntegrationTest {

    @Autowired
    private JpaDeviceStatisticsRepository repository;

    @MockitoBean
    private TcpSocketServer tcpSocketServer;

    // ====================== save ======================

    @Test
    void testSave_persistsStatisticsWithMinimalData() {
        MessageType16Response stats = buildStats("stats-dev-001", "TEK822V2");

        DeviceStatisticsEntity saved = repository.save(stats, "raw-stats-001");

        assertNotNull(saved.getId());
        assertEquals("stats-dev-001", saved.getDeviceId());
        assertEquals("TEK822V2", saved.getDeviceType());
        assertEquals("raw-stats-001", saved.getRawMessage());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    void testSave_persistsStatisticsWithFullData() {
        MessageType16Response stats = buildStats("stats-dev-002", "TEK822V2");
        stats.setIccid("89390100001234567890");
        stats.setEnergyUsed(1500L);
        stats.setMinTemperature(-5);
        stats.setMaxTemperature(45);
        stats.setMessageCount(100);
        stats.setDeliveryFailCount(5);
        stats.setTotalSendTime(50000L);
        stats.setMaxSendTime(800L);
        stats.setMinSendTime(100L);
        stats.setRssiTotal(-7000L);
        stats.setRssiValidCount(95);
        stats.setRssiFailCount(5);
        stats.setAverageSendTime(500.0);
        stats.setAverageRssi(-73.7);
        stats.setDeliverySuccessRate(95.0);

        DeviceStatisticsEntity saved = repository.save(stats, "raw-full-stats");

        assertEquals("89390100001234567890", saved.getIccid());
        assertEquals(1500L, saved.getEnergyUsed());
        assertEquals(-5, saved.getMinTemperature());
        assertEquals(45, saved.getMaxTemperature());
        assertEquals(100, saved.getMessageCount());
        assertEquals(5, saved.getDeliveryFailCount());
        assertEquals(50000L, saved.getTotalSendTime());
        assertEquals(800L, saved.getMaxSendTime());
        assertEquals(100L, saved.getMinSendTime());
        assertEquals(-7000L, saved.getRssiTotal());
        assertEquals(95, saved.getRssiValidCount());
        assertEquals(5, saved.getRssiFailCount());
        assertEquals(500.0, saved.getAverageSendTime(), 0.001);
        assertEquals(-73.7, saved.getAverageRssi(), 0.001);
        assertEquals(95.0, saved.getDeliverySuccessRate(), 0.001);
    }

    // ====================== buildEntity ======================

    @Test
    void testBuildEntity_doesNotPersistInDatabase() {
        MessageType16Response stats = buildStats("build-entity-dev", "TEK822V2");

        DeviceStatisticsEntity entity = repository.buildEntity(stats, "raw-build");

        assertNotNull(entity);
        assertEquals("build-entity-dev", entity.getDeviceId());
        assertEquals("TEK822V2", entity.getDeviceType());
        assertEquals("raw-build", entity.getRawMessage());
        assertNotNull(entity.getReceivedAt());
        // Verifica che NON sia persistita (nessun ID assegnato da JPA)
        assertNull(entity.getId(), "buildEntity non deve persistere l'entità nel DB");
        // Verifica che dopo buildEntity non ci siano record nel DB
        List<DeviceStatisticsEntity> inDb = repository.findByDeviceId("build-entity-dev");
        assertTrue(inDb.isEmpty(), "buildEntity non deve salvare nel DB");
    }

    @Test
    void testBuildEntity_copiesAllFieldsFromModel() {
        MessageType16Response stats = buildStats("build-full-dev", "TEK822V2");
        stats.setIccid("12345678901234567890");
        stats.setEnergyUsed(2000L);
        stats.setMessageCount(50);

        DeviceStatisticsEntity entity = repository.buildEntity(stats, "raw");

        assertEquals("12345678901234567890", entity.getIccid());
        assertEquals(2000L, entity.getEnergyUsed());
        assertEquals(50, entity.getMessageCount());
    }

    // ====================== findByDeviceId ======================

    @Test
    void testFindByDeviceId_returnsAllRecordsForDevice() {
        MessageType16Response stats = buildStats("find-dev", "TEK822V2");
        repository.save(stats, "raw-1");
        repository.save(stats, "raw-2");
        repository.save(stats, "raw-3");

        List<DeviceStatisticsEntity> results = repository.findByDeviceId("find-dev");

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(e -> "find-dev".equals(e.getDeviceId())));
    }

    @Test
    void testFindByDeviceId_noRecords_returnsEmpty() {
        List<DeviceStatisticsEntity> results = repository.findByDeviceId("nonexistent-stats-dev-xyz");

        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByDeviceId_onlyReturnsRecordsForRequestedDevice() {
        repository.save(buildStats("stats-A", "TEK822V2"), "raw-a");
        repository.save(buildStats("stats-B", "TEK822V2"), "raw-b");

        List<DeviceStatisticsEntity> resultsA = repository.findByDeviceId("stats-A");
        List<DeviceStatisticsEntity> resultsB = repository.findByDeviceId("stats-B");

        assertEquals(1, resultsA.size());
        assertEquals("stats-A", resultsA.get(0).getDeviceId());
        assertEquals(1, resultsB.size());
        assertEquals("stats-B", resultsB.get(0).getDeviceId());
    }

    // ====================== deleteOlderThan ======================

    @Test
    void testDeleteOlderThan_deletesExpiredRecords() {
        MessageType16Response stats = buildStats("delete-stats-dev", "TEK822V2");
        DeviceStatisticsEntity saved = repository.save(stats, "raw-delete");

        // Sposta la data nel passato tramite il repository JPA diretto
        DeviceStatisticsJpaRepository jpaRepo = deviceStatisticsJpaRepository;
        jpaRepo.findById(saved.getId()).ifPresent(e -> {
            e.setReceivedAt(LocalDateTime.now().minusDays(60));
            jpaRepo.save(e);
            jpaRepo.flush();
        });

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<DeviceStatisticsEntity> results = repository.findByDeviceId("delete-stats-dev");
        assertTrue(results.isEmpty(), "Le statistiche con più di 30 giorni devono essere eliminate");
    }

    @Test
    void testDeleteOlderThan_keepsRecentRecords() {
        MessageType16Response stats = buildStats("keep-stats-dev", "TEK822V2");
        repository.save(stats, "raw-keep");

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<DeviceStatisticsEntity> results = repository.findByDeviceId("keep-stats-dev");
        assertFalse(results.isEmpty(), "Le statistiche recenti non devono essere eliminate");
    }

    // ====================== Helpers ======================

    @Autowired
    private DeviceStatisticsJpaRepository deviceStatisticsJpaRepository;

    private MessageType16Response buildStats(String deviceId, String deviceType) {
        MessageType16Response stats = new MessageType16Response();
        stats.setDeviceId(deviceId);
        stats.setDeviceType(deviceType);
        return stats;
    }
}
