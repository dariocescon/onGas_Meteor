package com.aton.proj.oneGasMeteor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.repository.impl.sql.CommandJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.DeviceLocationJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.DeviceSettingsJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.DeviceStatisticsJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.ProcessingMetricsJpaRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.TelemetryJpaRepository;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

/**
 * Test di integrazione per DataCleanupService con database H2 in-memory.
 * Verifica che la logica di pulizia elimini i dati scaduti e conservi quelli recenti.
 *
 * DataCleanupService è condizionale: attivo solo con cleanup.enabled=true.
 */
@SpringBootTest
@TestPropertySource(properties = "cleanup.enabled=true")
class DataCleanupServiceIntegrationTest {

    @Autowired
    private DataCleanupService dataCleanupService;

    @Autowired
    private TelemetryJpaRepository telemetryRepo;

    @Autowired
    private CommandJpaRepository commandRepo;

    @Autowired
    private DeviceSettingsJpaRepository settingsRepo;

    @Autowired
    private DeviceStatisticsJpaRepository statisticsRepo;

    @Autowired
    private DeviceLocationJpaRepository locationRepo;

    @Autowired
    private ProcessingMetricsJpaRepository metricsRepo;

    @MockBean
    private TcpSocketServer tcpSocketServer;

    // ====================== Cleanup telemetry ======================

    @Test
    void testCleanupOldTelemetry_deletesExpiredRecords() {
        TelemetryEntity old = buildTelemetry("cleanup-old-dev", LocalDateTime.now().minusDays(60));
        TelemetryEntity recent = buildTelemetry("cleanup-recent-dev", LocalDateTime.now().minusDays(5));
        telemetryRepo.save(old);
        telemetryRepo.save(recent);
        telemetryRepo.flush();

        dataCleanupService.cleanupOldTelemetry();

        // I record vecchi (> 30 gg) devono essere eliminati
        assertTrue(telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-old-dev").isEmpty(),
                "La telemetria con 60 giorni deve essere eliminata");
        // I record recenti (< 30 gg) devono essere conservati
        assertNotNull(telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-recent-dev"),
                "La telemetria recente deve essere conservata");
        assertTrue(!telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-recent-dev").isEmpty(),
                "La telemetria recente deve essere conservata");
    }

    @Test
    void testCleanupOldTelemetry_noExpiredData_noOp() {
        TelemetryEntity recent = buildTelemetry("cleanup-noop-dev", LocalDateTime.now().minusDays(5));
        telemetryRepo.save(recent);
        telemetryRepo.flush();

        assertDoesNotThrow(() -> dataCleanupService.cleanupOldTelemetry());

        assertTrue(!telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-noop-dev").isEmpty());
    }

    // ====================== Cleanup commands ======================

    @Test
    void testCleanupOldCommands_deletesOldCompletedCommands() {
        CommandEntity oldDelivered = buildCommand("cleanup-cmd-dev", CommandEntity.CommandStatus.DELIVERED,
                LocalDateTime.now().minusDays(30));
        CommandEntity oldFailed = buildCommand("cleanup-cmd-dev", CommandEntity.CommandStatus.FAILED,
                LocalDateTime.now().minusDays(30));
        CommandEntity recentPending = buildCommand("cleanup-cmd-dev", CommandEntity.CommandStatus.PENDING,
                LocalDateTime.now().minusDays(1));
        commandRepo.save(oldDelivered);
        commandRepo.save(oldFailed);
        commandRepo.save(recentPending);
        commandRepo.flush();

        dataCleanupService.cleanupOldCommands();

        // I comandi DELIVERED/FAILED più vecchi devono essere eliminati (retention 7 gg)
        assertTrue(
                commandRepo.findByDeviceIdAndStatusOrderByCreatedAtAsc(
                        "cleanup-cmd-dev", CommandEntity.CommandStatus.DELIVERED).isEmpty(),
                "I comandi DELIVERED vecchi devono essere eliminati");
        assertTrue(
                commandRepo.findByDeviceIdAndStatusOrderByCreatedAtAsc(
                        "cleanup-cmd-dev", CommandEntity.CommandStatus.FAILED).isEmpty(),
                "I comandi FAILED vecchi devono essere eliminati");
        // I comandi PENDING recenti devono essere conservati
        assertNotNull(commandRepo.findByDeviceIdAndStatusOrderByCreatedAtAsc(
                "cleanup-cmd-dev", CommandEntity.CommandStatus.PENDING));
        assertTrue(!commandRepo.findByDeviceIdAndStatusOrderByCreatedAtAsc(
                "cleanup-cmd-dev", CommandEntity.CommandStatus.PENDING).isEmpty(),
                "I comandi PENDING recenti devono essere conservati");
    }

    @Test
    void testCleanupOldCommands_recentCommands_notDeleted() {
        CommandEntity recentDelivered = buildCommand("cleanup-recent-cmd", CommandEntity.CommandStatus.DELIVERED,
                LocalDateTime.now().minusDays(1));
        commandRepo.save(recentDelivered);
        commandRepo.flush();

        dataCleanupService.cleanupOldCommands();

        // I comandi recenti non devono essere eliminati (retention 7 gg)
        assertTrue(!commandRepo.findByDeviceIdAndStatusOrderByCreatedAtAsc(
                "cleanup-recent-cmd", CommandEntity.CommandStatus.DELIVERED).isEmpty(),
                "I comandi recenti non devono essere eliminati");
    }

    // ====================== Cleanup device data ======================

    @Test
    void testCleanupOldDeviceData_deletesExpiredSettings() {
        DeviceSettingsEntity old = buildSettings("cleanup-settings-dev", LocalDateTime.now().minusDays(60));
        DeviceSettingsEntity recent = buildSettings("cleanup-settings-recent", LocalDateTime.now().minusDays(5));
        settingsRepo.save(old);
        settingsRepo.save(recent);
        settingsRepo.flush();

        dataCleanupService.cleanupOldDeviceData();

        assertTrue(settingsRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-settings-dev").isEmpty(),
                "Le impostazioni vecchie devono essere eliminate");
        assertTrue(!settingsRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-settings-recent").isEmpty(),
                "Le impostazioni recenti devono essere conservate");
    }

    @Test
    void testCleanupOldDeviceData_deletesExpiredStatistics() {
        DeviceStatisticsEntity old = buildStatistics("cleanup-stats-dev", LocalDateTime.now().minusDays(60));
        DeviceStatisticsEntity recent = buildStatistics("cleanup-stats-recent", LocalDateTime.now().minusDays(5));
        statisticsRepo.save(old);
        statisticsRepo.save(recent);
        statisticsRepo.flush();

        dataCleanupService.cleanupOldDeviceData();

        assertTrue(statisticsRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-stats-dev").isEmpty(),
                "Le statistiche vecchie devono essere eliminate");
        assertTrue(!statisticsRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-stats-recent").isEmpty(),
                "Le statistiche recenti devono essere conservate");
    }

    @Test
    void testCleanupOldDeviceData_deletesExpiredLocations() {
        DeviceLocationEntity old = buildLocation("cleanup-loc-dev", LocalDateTime.now().minusDays(60));
        DeviceLocationEntity recent = buildLocation("cleanup-loc-recent", LocalDateTime.now().minusDays(5));
        locationRepo.save(old);
        locationRepo.save(recent);
        locationRepo.flush();

        dataCleanupService.cleanupOldDeviceData();

        assertTrue(locationRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-loc-dev").isEmpty(),
                "Le posizioni vecchie devono essere eliminate");
        assertTrue(!locationRepo.findByDeviceIdOrderByReceivedAtDesc("cleanup-loc-recent").isEmpty(),
                "Le posizioni recenti devono essere conservate");
    }

    // ====================== Cleanup processing metrics ======================

    @Test
    void testCleanupOldProcessingMetrics_deletesExpiredMetrics() {
        ProcessingMetricsEntity old = buildMetrics("cleanup-metrics-dev", LocalDateTime.now().minusDays(100));
        ProcessingMetricsEntity recent = buildMetrics("cleanup-metrics-recent", LocalDateTime.now().minusDays(30));
        metricsRepo.save(old);
        metricsRepo.save(recent);
        metricsRepo.flush();

        dataCleanupService.cleanupOldProcessingMetrics();

        // Metrics con 100 giorni devono essere eliminate (retention 90 gg)
        assertTrue(metricsRepo.findAll().stream()
                .noneMatch(m -> "cleanup-metrics-dev".equals(m.getDeviceId())),
                "Le metriche più vecchie di 90 giorni devono essere eliminate");
        // Metrics recenti (30 gg) devono essere conservate
        assertTrue(metricsRepo.findAll().stream()
                .anyMatch(m -> "cleanup-metrics-recent".equals(m.getDeviceId())),
                "Le metriche recenti devono essere conservate");
    }

    // ====================== Manual cleanup ======================

    @Test
    void testManualCleanup_returnsSuccessReport() {
        DataCleanupService.CleanupReport report = dataCleanupService.manualCleanup();

        assertNotNull(report);
        assertTrue(report.isSuccess(), "Il cleanup manuale deve avere successo");
        assertNotNull(report.getStartTime());
        assertNotNull(report.getEndTime());
        assertTrue(!report.getEndTime().isBefore(report.getStartTime()),
                "L'end time deve essere >= start time");
    }

    @Test
    void testManualCleanup_removesExpiredData_acrossAllTypes() {
        // Inserisce dati scaduti per tutti i tipi
        telemetryRepo.save(buildTelemetry("manual-cleanup-dev", LocalDateTime.now().minusDays(60)));
        CommandEntity delivered = buildCommand("manual-cleanup-dev", CommandEntity.CommandStatus.DELIVERED,
                LocalDateTime.now().minusDays(30));
        commandRepo.save(delivered);
        settingsRepo.save(buildSettings("manual-cleanup-dev", LocalDateTime.now().minusDays(60)));
        statisticsRepo.save(buildStatistics("manual-cleanup-dev", LocalDateTime.now().minusDays(60)));
        locationRepo.save(buildLocation("manual-cleanup-dev", LocalDateTime.now().minusDays(60)));
        metricsRepo.save(buildMetrics("manual-cleanup-dev", LocalDateTime.now().minusDays(100)));

        telemetryRepo.flush();
        commandRepo.flush();
        settingsRepo.flush();
        statisticsRepo.flush();
        locationRepo.flush();
        metricsRepo.flush();

        DataCleanupService.CleanupReport report = dataCleanupService.manualCleanup();

        assertTrue(report.isSuccess());
        assertTrue(telemetryRepo.findByDeviceIdOrderByReceivedAtDesc("manual-cleanup-dev").isEmpty());
        assertTrue(settingsRepo.findByDeviceIdOrderByReceivedAtDesc("manual-cleanup-dev").isEmpty());
        assertTrue(statisticsRepo.findByDeviceIdOrderByReceivedAtDesc("manual-cleanup-dev").isEmpty());
        assertTrue(locationRepo.findByDeviceIdOrderByReceivedAtDesc("manual-cleanup-dev").isEmpty());
    }

    // ====================== Helpers ======================

    private TelemetryEntity buildTelemetry(String deviceId, LocalDateTime receivedAt) {
        TelemetryEntity e = new TelemetryEntity();
        e.setDeviceId(deviceId);
        e.setDeviceType("TEK822V2");
        e.setRawMessage("test-raw");
        e.setReceivedAt(receivedAt);
        return e;
    }

    private CommandEntity buildCommand(String deviceId, CommandEntity.CommandStatus status,
            LocalDateTime createdAt) {
        CommandEntity e = new CommandEntity(deviceId, "TEK822V2", "REBOOT");
        e.setStatus(status);
        e.setCreatedAt(createdAt);
        return e;
    }

    private DeviceSettingsEntity buildSettings(String deviceId, LocalDateTime receivedAt) {
        DeviceSettingsEntity e = new DeviceSettingsEntity();
        e.setDeviceId(deviceId);
        e.setDeviceType("TEK822V2");
        e.setRawMessage("test-raw");
        e.setSettingsJson("{}");
        e.setReceivedAt(receivedAt);
        return e;
    }

    private DeviceStatisticsEntity buildStatistics(String deviceId, LocalDateTime receivedAt) {
        DeviceStatisticsEntity e = new DeviceStatisticsEntity();
        e.setDeviceId(deviceId);
        e.setDeviceType("TEK822V2");
        e.setRawMessage("test-raw");
        e.setReceivedAt(receivedAt);
        return e;
    }

    private DeviceLocationEntity buildLocation(String deviceId, LocalDateTime receivedAt) {
        DeviceLocationEntity e = new DeviceLocationEntity();
        e.setDeviceId(deviceId);
        e.setDeviceType("TEK822V2");
        e.setRawMessage("test-raw");
        e.setReceivedAt(receivedAt);
        return e;
    }

    private ProcessingMetricsEntity buildMetrics(String deviceId, LocalDateTime receivedAt) {
        ProcessingMetricsEntity e = new ProcessingMetricsEntity();
        e.setDeviceId(deviceId);
        e.setDeviceType("TEK822V2");
        e.setSuccess(true);
        e.setReceivedAt(receivedAt);
        return e;
    }
}
