package com.aton.proj.oneGasMeteor.repository.impl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

import jakarta.persistence.EntityManager;

/**
 * Test di integrazione per JpaProcessingMetricsRepository con database H2 in-memory.
 * Verifica la logica di persistenza e cancellazione delle metriche di elaborazione.
 */
@SpringBootTest
@Transactional
class JpaProcessingMetricsRepositoryIntegrationTest {

    @Autowired
    private JpaProcessingMetricsRepository repository;

    @MockitoBean
    private TcpSocketServer tcpSocketServer;

    // ====================== save ======================

    @Test
    void testSave_persistsMetricsWithMinimalData() {
        ProcessingMetricsEntity entity = buildMetrics("metrics-dev-001", "TEK822V2", true);

        ProcessingMetricsEntity saved = repository.save(entity);

        assertNotNull(saved.getId());
        assertEquals("metrics-dev-001", saved.getDeviceId());
        assertEquals("TEK822V2", saved.getDeviceType());
        assertTrue(saved.getSuccess());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    void testSave_persistsMetricsWithTimingData() {
        ProcessingMetricsEntity entity = buildMetrics("metrics-dev-002", "TEK822V2", true);
        entity.setTotalProcessingTimeMs(250L);
        entity.setReadTimeMs(50L);
        entity.setDecodeTimeMs(80L);
        entity.setDbSaveTimeMs(100L);
        entity.setCommandQueryTimeMs(10L);
        entity.setCommandEncodeTimeMs(5L);
        entity.setSendTimeMs(5L);

        ProcessingMetricsEntity saved = repository.save(entity);

        assertEquals(250L, saved.getTotalProcessingTimeMs());
        assertEquals(50L, saved.getReadTimeMs());
        assertEquals(80L, saved.getDecodeTimeMs());
        assertEquals(100L, saved.getDbSaveTimeMs());
        assertEquals(10L, saved.getCommandQueryTimeMs());
        assertEquals(5L, saved.getCommandEncodeTimeMs());
        assertEquals(5L, saved.getSendTimeMs());
    }

    @Test
    void testSave_persistsMetricsWithDeviceHealthSnapshot() {
        ProcessingMetricsEntity entity = buildMetrics("metrics-dev-003", "TEK822V2", true);
        entity.setBatteryVoltage(3.7);
        entity.setBatteryPercentage(85.0);
        entity.setSignalStrength(-70);
        entity.setFirmwareVersion("2.1.0");

        ProcessingMetricsEntity saved = repository.save(entity);

        assertEquals(3.7, saved.getBatteryVoltage(), 0.001);
        assertEquals(85.0, saved.getBatteryPercentage(), 0.001);
        assertEquals(-70, saved.getSignalStrength());
        assertEquals("2.1.0", saved.getFirmwareVersion());
    }

    @Test
    void testSave_persistsMetricsWithMessageInfo() {
        ProcessingMetricsEntity entity = buildMetrics("metrics-dev-004", "TEK822V2", true);
        entity.setMessageType(4);
        entity.setPayloadLengthBytes(256);
        entity.setDeclaredBodyLength(240);
        entity.setMeasurementCount(20);
        entity.setClientAddress("192.168.1.100");

        ProcessingMetricsEntity saved = repository.save(entity);

        assertEquals(4, saved.getMessageType());
        assertEquals(256, saved.getPayloadLengthBytes());
        assertEquals(240, saved.getDeclaredBodyLength());
        assertEquals(20, saved.getMeasurementCount());
        assertEquals("192.168.1.100", saved.getClientAddress());
    }

    @Test
    void testSave_persistsMetricsWithCommandInfo() {
        ProcessingMetricsEntity entity = buildMetrics("metrics-dev-005", "TEK822V2", true);
        entity.setPendingCommandsFound(3);
        entity.setCommandsSent(3);
        entity.setResponseSizeBytes(128);

        ProcessingMetricsEntity saved = repository.save(entity);

        assertEquals(3, saved.getPendingCommandsFound());
        assertEquals(3, saved.getCommandsSent());
        assertEquals(128, saved.getResponseSizeBytes());
    }

    @Test
    void testSave_persistsFailedMetrics_withErrorMessage() {
        ProcessingMetricsEntity entity = buildMetrics("metrics-dev-006", "TEK822V2", false);
        entity.setErrorMessage("Decode error: invalid checksum");
        entity.setCompletedAt(LocalDateTime.now());

        ProcessingMetricsEntity saved = repository.save(entity);

        assertFalse(saved.getSuccess());
        assertEquals("Decode error: invalid checksum", saved.getErrorMessage());
        assertNotNull(saved.getCompletedAt());
    }

    // ====================== deleteOlderThan ======================

    @Test
    void testDeleteOlderThan_deletesExpiredRecords() {
        ProcessingMetricsEntity entity = buildMetrics("delete-metrics-dev", "TEK822V2", true);
        ProcessingMetricsEntity saved = repository.save(entity);

        // Sposta la data nel passato tramite il repository JPA diretto
        ProcessingMetricsJpaRepository jpaRepo = processingMetricsJpaRepository;
        jpaRepo.findById(saved.getId()).ifPresent(e -> {
            e.setReceivedAt(LocalDateTime.now().minusDays(100));
            jpaRepo.save(e);
            jpaRepo.flush();
        });

        repository.deleteOlderThan(LocalDateTime.now().minusDays(90));

        List<ProcessingMetricsEntity> remaining = jpaRepo.findAll().stream()
                .filter(m -> "delete-metrics-dev".equals(m.getDeviceId()))
                .toList();
        assertTrue(remaining.isEmpty(), "Le metriche con più di 90 giorni devono essere eliminate");
    }

    @Test
    void testDeleteOlderThan_keepsRecentRecords() {
        ProcessingMetricsEntity entity = buildMetrics("keep-metrics-dev", "TEK822V2", true);
        repository.save(entity);

        repository.deleteOlderThan(LocalDateTime.now().minusDays(90));

        List<ProcessingMetricsEntity> remaining = processingMetricsJpaRepository.findAll().stream()
                .filter(m -> "keep-metrics-dev".equals(m.getDeviceId()))
                .toList();
        assertFalse(remaining.isEmpty(), "Le metriche recenti non devono essere eliminate");
    }

    @Test
    void testDeleteOlderThan_onlyDeletesOlderThanThreshold() {
        ProcessingMetricsEntity old = buildMetrics("threshold-dev", "TEK822V2", true);
        ProcessingMetricsEntity recent = buildMetrics("threshold-dev", "TEK822V2", true);

        ProcessingMetricsEntity savedOld = repository.save(old);
        ProcessingMetricsEntity savedRecent = repository.save(recent);

        ProcessingMetricsJpaRepository jpaRepo = processingMetricsJpaRepository;
        jpaRepo.findById(savedOld.getId()).ifPresent(e -> {
            e.setReceivedAt(LocalDateTime.now().minusDays(100));
            jpaRepo.save(e);
            jpaRepo.flush();
        });
        // savedRecent rimane con la data attuale

        repository.deleteOlderThan(LocalDateTime.now().minusDays(90));

        // Svuota la first-level cache per forzare una lettura dal DB
        entityManager.clear();

        // Il vecchio deve essere eliminato
        assertFalse(jpaRepo.findById(savedOld.getId()).isPresent(),
                "Il record vecchio deve essere eliminato");
        // Il recente deve essere mantenuto
        assertTrue(jpaRepo.findById(savedRecent.getId()).isPresent(),
                "Il record recente deve essere mantenuto");
    }

    // ====================== Helpers ======================

    @Autowired
    private ProcessingMetricsJpaRepository processingMetricsJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private ProcessingMetricsEntity buildMetrics(String deviceId, String deviceType, boolean success) {
        ProcessingMetricsEntity entity = new ProcessingMetricsEntity();
        entity.setDeviceId(deviceId);
        entity.setDeviceType(deviceType);
        entity.setSuccess(success);
        entity.setReceivedAt(LocalDateTime.now());
        return entity;
    }
}
