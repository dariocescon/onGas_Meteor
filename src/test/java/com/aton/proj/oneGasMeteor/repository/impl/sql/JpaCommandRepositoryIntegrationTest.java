package com.aton.proj.oneGasMeteor.repository.impl.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

import jakarta.persistence.EntityManager;

/**
 * Test di integrazione per JpaCommandRepository con database H2 in-memory.
 * Verifica la logica di persistenza, ricerca e aggiornamento dei comandi dispositivo.
 */
@SpringBootTest
@Transactional
class JpaCommandRepositoryIntegrationTest {

    @Autowired
    private JpaCommandRepository repository;

    @MockitoBean
    private TcpSocketServer tcpSocketServer;

    // ====================== save ======================

    @Test
    void testSave_persistsCommandWithMinimalData() {
        DeviceCommand command = new DeviceCommand("cmd-dev-001", "TEK822V2", "REBOOT");

        CommandEntity saved = repository.save(command);

        assertNotNull(saved.getId());
        assertEquals("cmd-dev-001", saved.getDeviceId());
        assertEquals("TEK822V2", saved.getDeviceType());
        assertEquals("REBOOT", saved.getCommandType());
        assertEquals(CommandEntity.CommandStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNull(saved.getCommandParamsJson());
    }

    @Test
    void testSave_persistsCommandWithParameters() {
        DeviceCommand command = new DeviceCommand("cmd-dev-002", "TEK822V2", "SET_INTERVAL");
        command.addParameter("interval", 300);
        command.addParameter("samplingPeriod", 60);

        CommandEntity saved = repository.save(command);

        assertNotNull(saved.getId());
        assertEquals("SET_INTERVAL", saved.getCommandType());
        assertNotNull(saved.getCommandParamsJson(), "I parametri JSON devono essere persistiti");
        assertTrue(saved.getCommandParamsJson().contains("interval"),
                "Il JSON deve contenere il parametro 'interval'");
        assertTrue(saved.getCommandParamsJson().contains("300"),
                "Il JSON deve contenere il valore 300");
    }

    @Test
    void testSave_persistsCommandWithEmptyParameters_noJson() {
        DeviceCommand command = new DeviceCommand("cmd-dev-003", "TEK822V2", "REQUEST_STATUS");
        command.setParameters(Map.of());

        CommandEntity saved = repository.save(command);

        assertNotNull(saved.getId());
        assertNull(saved.getCommandParamsJson(), "Nessun JSON per parametri vuoti");
    }

    @Test
    void testSave_defaultRetryCountIsZero() {
        DeviceCommand command = new DeviceCommand("cmd-dev-004", "TEK822V2", "REBOOT");

        CommandEntity saved = repository.save(command);

        assertEquals(0, saved.getRetryCount());
        assertEquals(3, saved.getMaxRetries());
    }

    // ====================== findById ======================

    @Test
    void testFindById_returnsCorrectCommand() {
        DeviceCommand command = new DeviceCommand("cmd-findbyid", "TEK822V2", "REBOOT");
        CommandEntity saved = repository.save(command);

        Optional<CommandEntity> found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("cmd-findbyid", found.get().getDeviceId());
        assertEquals("REBOOT", found.get().getCommandType());
    }

    @Test
    void testFindById_notFound_returnsEmpty() {
        Optional<CommandEntity> found = repository.findById(999999L);

        assertFalse(found.isPresent());
    }

    // ====================== findPendingCommands ======================

    @Test
    void testFindPendingCommands_returnsPendingCommandsForDevice() {
        repository.save(new DeviceCommand("pending-dev", "TEK822V2", "REBOOT"));
        repository.save(new DeviceCommand("pending-dev", "TEK822V2", "REQUEST_STATUS"));

        List<CommandEntity> pending = repository.findPendingCommands("pending-dev");

        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(c -> CommandEntity.CommandStatus.PENDING.equals(c.getStatus())));
        assertTrue(pending.stream().allMatch(c -> "pending-dev".equals(c.getDeviceId())));
    }

    @Test
    void testFindPendingCommands_noCommands_returnsEmpty() {
        List<CommandEntity> pending = repository.findPendingCommands("nonexistent-device-xyz");

        assertTrue(pending.isEmpty());
    }

    @Test
    void testFindPendingCommands_onlyPendingReturned_sentExcluded() {
        CommandEntity savedCmd = repository.save(new DeviceCommand("status-dev", "TEK822V2", "REBOOT"));
        repository.markAsSent(savedCmd.getId());

        List<CommandEntity> pending = repository.findPendingCommands("status-dev");

        assertTrue(pending.isEmpty(), "I comandi SENT non devono essere inclusi nei pending");
    }

    @Test
    void testFindPendingCommands_orderedByCreatedAtAscending() {
        CommandEntity cmd1 = repository.save(new DeviceCommand("ordered-dev", "TEK822V2", "REBOOT"));
        CommandEntity cmd2 = repository.save(new DeviceCommand("ordered-dev", "TEK822V2", "REQUEST_STATUS"));

        List<CommandEntity> pending = repository.findPendingCommands("ordered-dev");

        assertEquals(2, pending.size());
        // Il primo inserito deve venire prima (ordine ascendente per createdAt)
        assertTrue(pending.get(0).getId() <= pending.get(1).getId(),
                "I comandi devono essere ordinati per data di creazione ascendente");
    }

    // ====================== findPendingCommandsByDeviceType ======================

    @Test
    void testFindPendingCommandsByDeviceType_returnsPendingForType() {
        repository.save(new DeviceCommand("type-dev-1", "TEK822V2", "REBOOT"));
        repository.save(new DeviceCommand("type-dev-2", "TEK822V2", "REQUEST_STATUS"));

        List<CommandEntity> pending = repository.findPendingCommandsByDeviceType("TEK822V2");

        assertFalse(pending.isEmpty());
        assertTrue(pending.stream().allMatch(c -> "TEK822V2".equals(c.getDeviceType())));
        assertTrue(pending.stream().allMatch(c -> CommandEntity.CommandStatus.PENDING.equals(c.getStatus())));
    }

    @Test
    void testFindPendingCommandsByDeviceType_noMatch_returnsEmpty() {
        List<CommandEntity> pending = repository.findPendingCommandsByDeviceType("UNKNOWN_TYPE_XYZ");

        assertTrue(pending.isEmpty());
    }

    // ====================== updateStatus ======================

    @Test
    void testUpdateStatus_changesToDelivered() {
        CommandEntity saved = repository.save(new DeviceCommand("update-dev", "TEK822V2", "REBOOT"));

        repository.updateStatus(saved.getId(), CommandEntity.CommandStatus.DELIVERED);

        Optional<CommandEntity> updated = repository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(CommandEntity.CommandStatus.DELIVERED, updated.get().getStatus());
    }

    @Test
    void testUpdateStatus_changesToFailed() {
        CommandEntity saved = repository.save(new DeviceCommand("update-failed-dev", "TEK822V2", "REBOOT"));

        repository.updateStatus(saved.getId(), CommandEntity.CommandStatus.FAILED);

        Optional<CommandEntity> updated = repository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(CommandEntity.CommandStatus.FAILED, updated.get().getStatus());
    }

    @Test
    void testUpdateStatus_nonExistentId_noException() {
        // Non deve lanciare eccezioni per un ID inesistente
        repository.updateStatus(999999L, CommandEntity.CommandStatus.DELIVERED);
    }

    // ====================== markAsSent ======================

    @Test
    void testMarkAsSent_setsStatusToSentAndSentAt() {
        CommandEntity saved = repository.save(new DeviceCommand("sent-dev", "TEK822V2", "REBOOT"));

        repository.markAsSent(saved.getId());

        Optional<CommandEntity> updated = repository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(CommandEntity.CommandStatus.SENT, updated.get().getStatus());
        assertNotNull(updated.get().getSentAt(), "sentAt deve essere valorizzato dopo markAsSent");
    }

    @Test
    void testMarkAsSent_nonExistentId_noException() {
        repository.markAsSent(999999L);
    }

    // ====================== markAsDelivered ======================

    @Test
    void testMarkAsDelivered_setsStatusToDeliveredAndDeliveredAt() {
        CommandEntity saved = repository.save(new DeviceCommand("delivered-dev", "TEK822V2", "REBOOT"));

        repository.markAsDelivered(saved.getId());

        Optional<CommandEntity> updated = repository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(CommandEntity.CommandStatus.DELIVERED, updated.get().getStatus());
        assertNotNull(updated.get().getDeliveredAt(), "deliveredAt deve essere valorizzato dopo markAsDelivered");
    }

    @Test
    void testMarkAsDelivered_nonExistentId_noException() {
        repository.markAsDelivered(999999L);
    }

    // ====================== markAsFailed ======================

    @Test
    void testMarkAsFailed_setsStatusToFailedAndErrorMessage() {
        CommandEntity saved = repository.save(new DeviceCommand("failed-dev", "TEK822V2", "REBOOT"));

        repository.markAsFailed(saved.getId(), "Timeout during send");

        Optional<CommandEntity> updated = repository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(CommandEntity.CommandStatus.FAILED, updated.get().getStatus());
        assertEquals("Timeout during send", updated.get().getErrorMessage());
    }

    @Test
    void testMarkAsFailed_nonExistentId_noException() {
        repository.markAsFailed(999999L, "Error message");
    }

    // ====================== incrementRetryCount ======================

    @Test
    void testIncrementRetryCount_incrementsFromZeroToOne() {
        CommandEntity saved = repository.save(new DeviceCommand("retry-dev", "TEK822V2", "REBOOT"));
        assertEquals(0, saved.getRetryCount());

        repository.incrementRetryCount(saved.getId());

        Optional<CommandEntity> updated = repository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getRetryCount());
    }

    @Test
    void testIncrementRetryCount_incrementsMultipleTimes() {
        CommandEntity saved = repository.save(new DeviceCommand("retry-multi-dev", "TEK822V2", "REBOOT"));

        repository.incrementRetryCount(saved.getId());
        repository.incrementRetryCount(saved.getId());
        repository.incrementRetryCount(saved.getId());

        Optional<CommandEntity> updated = repository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(3, updated.get().getRetryCount());
    }

    @Test
    void testIncrementRetryCount_nonExistentId_noException() {
        repository.incrementRetryCount(999999L);
    }

    // ====================== deleteOldCompletedCommands ======================

    @Test
    void testDeleteOldCompletedCommands_deletesOldDeliveredAndFailed() {
        CommandEntity delivered = repository.save(new DeviceCommand("del-dev", "TEK822V2", "REBOOT"));
        CommandEntity failed = repository.save(new DeviceCommand("del-dev", "TEK822V2", "REQUEST_STATUS"));

        // Aggiorna direttamente le date e stati nel DB
        CommandJpaRepository jpaRepo = commandJpaRepository;
        jpaRepo.findById(delivered.getId()).ifPresent(e -> {
            e.setStatus(CommandEntity.CommandStatus.DELIVERED);
            e.setCreatedAt(LocalDateTime.now().minusDays(30));
            jpaRepo.save(e);
            jpaRepo.flush();
        });
        jpaRepo.findById(failed.getId()).ifPresent(e -> {
            e.setStatus(CommandEntity.CommandStatus.FAILED);
            e.setCreatedAt(LocalDateTime.now().minusDays(30));
            jpaRepo.save(e);
            jpaRepo.flush();
        });

        repository.deleteOldCompletedCommands(7);

        // Svuota la first-level cache per forzare una lettura dal DB
        entityManager.clear();

        assertFalse(repository.findById(delivered.getId()).isPresent(),
                "Il comando DELIVERED vecchio deve essere eliminato");
        assertFalse(repository.findById(failed.getId()).isPresent(),
                "Il comando FAILED vecchio deve essere eliminato");
    }

    @Test
    void testDeleteOldCompletedCommands_keepsPendingCommands() {
        CommandEntity pending = repository.save(new DeviceCommand("keep-dev", "TEK822V2", "REBOOT"));

        repository.deleteOldCompletedCommands(0);

        assertTrue(repository.findById(pending.getId()).isPresent(),
                "I comandi PENDING non devono essere eliminati");
    }

    @Test
    void testDeleteOldCompletedCommands_keepsRecentDeliveredCommands() {
        CommandEntity delivered = repository.save(new DeviceCommand("recent-del-dev", "TEK822V2", "REBOOT"));
        repository.markAsDelivered(delivered.getId());

        // Elimina comandi con più di 7 giorni: il comando appena creato deve rimanere
        repository.deleteOldCompletedCommands(7);

        assertTrue(repository.findById(delivered.getId()).isPresent(),
                "I comandi DELIVERED recenti non devono essere eliminati");
    }

    // ====================== Helpers ======================

    @Autowired
    private CommandJpaRepository commandJpaRepository;

    @Autowired
    private EntityManager entityManager;
}
