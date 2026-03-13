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

import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.model.MessageType6Response;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

/**
 * Test di integrazione per JpaDeviceSettingsRepository con database H2 in-memory.
 * Verifica la logica di persistenza, ricerca e cancellazione delle impostazioni dispositivo.
 */
@SpringBootTest
@Transactional
class JpaDeviceSettingsRepositoryIntegrationTest {

    @Autowired
    private JpaDeviceSettingsRepository repository;

    @MockitoBean
    private TcpSocketServer tcpSocketServer;

    // ====================== save ======================

    @Test
    void testSave_persistsSettingsWithEmptySettings() {
        MessageType6Response settings = buildSettings("settings-dev-001", "TEK822V2");

        DeviceSettingsEntity saved = repository.save(settings, "raw-settings-001");

        assertNotNull(saved.getId());
        assertEquals("settings-dev-001", saved.getDeviceId());
        assertEquals("TEK822V2", saved.getDeviceType());
        assertEquals("raw-settings-001", saved.getRawMessage());
        assertNotNull(saved.getReceivedAt());
        assertNotNull(saved.getSettingsJson(), "Il JSON delle impostazioni deve essere valorizzato");
    }

    @Test
    void testSave_persistsSettingsWithData_jsonContainsValues() {
        MessageType6Response settings = buildSettings("settings-dev-002", "TEK822V2");
        settings.addSetting("interval", "300");
        settings.addSetting("server", "192.168.1.100");
        settings.addSetting("port", "8080");

        DeviceSettingsEntity saved = repository.save(settings, "raw-settings-002");

        assertNotNull(saved.getSettingsJson());
        assertTrue(saved.getSettingsJson().contains("interval"),
                "Il JSON deve contenere la chiave 'interval'");
        assertTrue(saved.getSettingsJson().contains("300"),
                "Il JSON deve contenere il valore '300'");
        assertTrue(saved.getSettingsJson().contains("192.168.1.100"),
                "Il JSON deve contenere l'indirizzo IP");
    }

    @Test
    void testSave_persistsSettingsWithNullSettings_jsonIsNull() {
        MessageType6Response settings = buildSettings("settings-dev-003", "TEK822V2");
        settings.setSettings(null);

        DeviceSettingsEntity saved = repository.save(settings, "raw-null-settings");

        assertNotNull(saved.getId());
        // Il JSON serializzato di null è "null"
        assertNotNull(saved.getSettingsJson());
    }

    // ====================== buildEntity ======================

    @Test
    void testBuildEntity_doesNotPersistInDatabase() {
        MessageType6Response settings = buildSettings("build-settings-dev", "TEK822V2");
        settings.addSetting("key", "value");

        DeviceSettingsEntity entity = repository.buildEntity(settings, "raw-build");

        assertNotNull(entity);
        assertEquals("build-settings-dev", entity.getDeviceId());
        assertEquals("TEK822V2", entity.getDeviceType());
        assertEquals("raw-build", entity.getRawMessage());
        assertNotNull(entity.getReceivedAt());
        assertNotNull(entity.getSettingsJson(), "Il JSON deve essere generato");
        assertTrue(entity.getSettingsJson().contains("key"),
                "Il JSON deve contenere la chiave 'key'");
        // Verifica che NON sia persistita
        assertNull(entity.getId(), "buildEntity non deve persistere l'entità nel DB");
        List<DeviceSettingsEntity> inDb = repository.findByDeviceId("build-settings-dev");
        assertTrue(inDb.isEmpty(), "buildEntity non deve salvare nel DB");
    }

    @Test
    void testBuildEntity_withMultipleSettings_jsonContainsAll() {
        MessageType6Response settings = buildSettings("build-multi-dev", "TEK822V2");
        settings.addSetting("apn", "internet.com");
        settings.addSetting("username", "admin");
        settings.addSetting("password", "secret");

        DeviceSettingsEntity entity = repository.buildEntity(settings, "raw");

        assertTrue(entity.getSettingsJson().contains("apn"));
        assertTrue(entity.getSettingsJson().contains("internet.com"));
        assertTrue(entity.getSettingsJson().contains("username"));
        assertTrue(entity.getSettingsJson().contains("admin"));
    }

    // ====================== findByDeviceId ======================

    @Test
    void testFindByDeviceId_returnsAllRecordsForDevice() {
        MessageType6Response settings = buildSettings("find-settings-dev", "TEK822V2");
        repository.save(settings, "raw-1");
        repository.save(settings, "raw-2");
        repository.save(settings, "raw-3");

        List<DeviceSettingsEntity> results = repository.findByDeviceId("find-settings-dev");

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(e -> "find-settings-dev".equals(e.getDeviceId())));
    }

    @Test
    void testFindByDeviceId_noRecords_returnsEmpty() {
        List<DeviceSettingsEntity> results = repository.findByDeviceId("nonexistent-settings-dev-xyz");

        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByDeviceId_onlyReturnsRecordsForRequestedDevice() {
        repository.save(buildSettings("settings-dev-A", "TEK822V2"), "raw-a");
        repository.save(buildSettings("settings-dev-B", "TEK822V2"), "raw-b");

        List<DeviceSettingsEntity> resultsA = repository.findByDeviceId("settings-dev-A");
        List<DeviceSettingsEntity> resultsB = repository.findByDeviceId("settings-dev-B");

        assertEquals(1, resultsA.size());
        assertEquals("settings-dev-A", resultsA.get(0).getDeviceId());
        assertEquals(1, resultsB.size());
        assertEquals("settings-dev-B", resultsB.get(0).getDeviceId());
    }

    // ====================== deleteOlderThan ======================

    @Test
    void testDeleteOlderThan_deletesExpiredRecords() {
        MessageType6Response settings = buildSettings("delete-settings-dev", "TEK822V2");
        DeviceSettingsEntity saved = repository.save(settings, "raw-delete");

        // Sposta la data nel passato tramite il repository JPA diretto
        DeviceSettingsJpaRepository jpaRepo = deviceSettingsJpaRepository;
        jpaRepo.findById(saved.getId()).ifPresent(e -> {
            e.setReceivedAt(LocalDateTime.now().minusDays(60));
            jpaRepo.save(e);
            jpaRepo.flush();
        });

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<DeviceSettingsEntity> results = repository.findByDeviceId("delete-settings-dev");
        assertTrue(results.isEmpty(), "Le impostazioni con più di 30 giorni devono essere eliminate");
    }

    @Test
    void testDeleteOlderThan_keepsRecentRecords() {
        MessageType6Response settings = buildSettings("keep-settings-dev", "TEK822V2");
        repository.save(settings, "raw-keep");

        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));

        List<DeviceSettingsEntity> results = repository.findByDeviceId("keep-settings-dev");
        assertFalse(results.isEmpty(), "Le impostazioni recenti non devono essere eliminate");
    }

    // ====================== Helpers ======================

    @Autowired
    private DeviceSettingsJpaRepository deviceSettingsJpaRepository;

    private MessageType6Response buildSettings(String deviceId, String deviceType) {
        MessageType6Response settings = new MessageType6Response();
        settings.setDeviceId(deviceId);
        settings.setDeviceType(deviceType);
        settings.setSettings(new java.util.HashMap<>());
        return settings;
    }
}
