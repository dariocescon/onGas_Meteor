package com.aton.proj.oneGasMeteor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.dto.CommandCreateRequest;
import com.aton.proj.oneGasMeteor.dto.CommandResponse;
import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;

/**
 * Test di integrazione per CommandService con database H2 in-memory.
 * Verifica che la logica di business interagisca correttamente con il DB reale.
 */
@SpringBootTest
@Transactional
class CommandServiceIntegrationTest {

    @Autowired
    private CommandService commandService;

    @Autowired
    private CommandRepository commandRepository;

    @MockBean
    private TcpSocketServer tcpSocketServer;

    // ====================== Creazione comandi e persistenza ======================

    @Test
    void testCreateCommand_reboot_persistedInH2() {
        CommandCreateRequest request = new CommandCreateRequest("intg-device-1", "TEK822V2", "REBOOT");

        CommandResponse response = commandService.createCommand(request);

        assertNotNull(response.getId());
        assertEquals("intg-device-1", response.getDeviceId());
        assertEquals("TEK822V2", response.getDeviceType());
        assertEquals("REBOOT", response.getCommandType());
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void testCreateCommand_setInterval_withParams_persistedInH2() {
        CommandCreateRequest request = new CommandCreateRequest("intg-device-2", "TEK822V2", "SET_INTERVAL");
        request.setParameters(Map.of("interval", 300, "samplingPeriod", 60));

        CommandResponse response = commandService.createCommand(request);

        assertNotNull(response.getId());
        assertEquals("SET_INTERVAL", response.getCommandType());
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getParameters());
        assertEquals(300, response.getParameters().get("interval"));
        assertEquals(60, response.getParameters().get("samplingPeriod"));
    }

    @Test
    void testCreateCommand_setAPN_withAllParams_persistedInH2() {
        CommandCreateRequest request = new CommandCreateRequest("intg-device-3", "TEK822V2", "SET_APN");
        request.setParameters(Map.of("apn", "internet.provider.com", "username", "user", "apnPassword", "pass"));

        CommandResponse response = commandService.createCommand(request);

        assertNotNull(response.getId());
        assertEquals("SET_APN", response.getCommandType());
        assertEquals("internet.provider.com", response.getParameters().get("apn"));
    }

    @Test
    void testCreateCommand_setServer_withAllParams_persistedInH2() {
        CommandCreateRequest request = new CommandCreateRequest("intg-device-4", "TEK822V2", "SET_SERVER");
        request.setParameters(Map.of("serverIp", "192.168.1.100", "serverPort", 8080));

        CommandResponse response = commandService.createCommand(request);

        assertNotNull(response.getId());
        assertEquals("SET_SERVER", response.getCommandType());
        assertEquals("192.168.1.100", response.getParameters().get("serverIp"));
    }

    // ====================== Recupero per ID ======================

    @Test
    void testGetCommandById_fromH2() {
        CommandCreateRequest request = new CommandCreateRequest("intg-device-5", "TEK822V2", "REQUEST_STATUS");
        CommandResponse created = commandService.createCommand(request);

        CommandResponse found = commandService.getCommandById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("REQUEST_STATUS", found.getCommandType());
        assertEquals("PENDING", found.getStatus());
        assertEquals("intg-device-5", found.getDeviceId());
    }

    @Test
    void testGetCommandById_notFound_throws() {
        assertThrows(IllegalArgumentException.class, () -> commandService.getCommandById(999999L));
    }

    // ====================== Recupero comandi pendenti ======================

    @Test
    void testGetPendingCommands_multipleCommands_fromH2() {
        commandService.createCommand(new CommandCreateRequest("intg-device-6", "TEK822V2", "REBOOT"));
        commandService.createCommand(new CommandCreateRequest("intg-device-6", "TEK822V2", "REQUEST_STATUS"));
        commandService.createCommand(new CommandCreateRequest("intg-device-6", "TEK822V2", "REQUEST_GPS"));

        List<CommandResponse> pending = commandService.getPendingCommands("intg-device-6");

        assertEquals(3, pending.size());
        assertTrue(pending.stream().allMatch(c -> "PENDING".equals(c.getStatus())));
        assertTrue(pending.stream().allMatch(c -> "intg-device-6".equals(c.getDeviceId())));
    }

    @Test
    void testGetPendingCommands_noCommands_returnsEmpty() {
        List<CommandResponse> pending = commandService.getPendingCommands("device-not-existing-xyz");

        assertTrue(pending.isEmpty());
    }

    @Test
    void testGetPendingCommands_onlyForRequestedDevice() {
        commandService.createCommand(new CommandCreateRequest("intg-device-7a", "TEK822V2", "REBOOT"));
        commandService.createCommand(new CommandCreateRequest("intg-device-7b", "TEK822V2", "REBOOT"));

        List<CommandResponse> pending7a = commandService.getPendingCommands("intg-device-7a");
        List<CommandResponse> pending7b = commandService.getPendingCommands("intg-device-7b");

        assertEquals(1, pending7a.size());
        assertEquals(1, pending7b.size());
        assertEquals("intg-device-7a", pending7a.get(0).getDeviceId());
        assertEquals("intg-device-7b", pending7b.get(0).getDeviceId());
    }

    // ====================== Validazione ======================

    @Test
    void testCreateCommand_invalidCommandType_throws() {
        CommandCreateRequest request = new CommandCreateRequest("device1", "TEK822V2", "INVALID_CMD");
        assertThrows(IllegalArgumentException.class, () -> commandService.createCommand(request));
    }

    @Test
    void testCreateCommand_setInterval_missingInterval_throws() {
        CommandCreateRequest request = new CommandCreateRequest("device1", "TEK822V2", "SET_INTERVAL");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandService.createCommand(request));
        assertTrue(ex.getMessage().contains("interval"));
    }

    @Test
    void testCreateCommand_setListen_missingListenMinutes_throws() {
        CommandCreateRequest request = new CommandCreateRequest("device1", "TEK822V2", "SET_LISTEN");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandService.createCommand(request));
        assertTrue(ex.getMessage().contains("listenMinutes"));
    }

    @Test
    void testCreateCommand_setAlarmThreshold_missingThreshold_throws() {
        CommandCreateRequest request = new CommandCreateRequest("device1", "TEK822V2", "SET_ALARM_THRESHOLD");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandService.createCommand(request));
        assertTrue(ex.getMessage().contains("threshold"));
    }

    @Test
    void testCreateCommand_setAPN_missingApn_throws() {
        CommandCreateRequest request = new CommandCreateRequest("device1", "TEK822V2", "SET_APN");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandService.createCommand(request));
        assertTrue(ex.getMessage().contains("apn"));
    }

    @Test
    void testCreateCommand_setServer_missingServerIp_throws() {
        CommandCreateRequest request = new CommandCreateRequest("device1", "TEK822V2", "SET_SERVER");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandService.createCommand(request));
        assertTrue(ex.getMessage().contains("serverIp"));
    }

    @Test
    void testCreateCommand_setServer_missingServerPort_throws() {
        CommandCreateRequest request = new CommandCreateRequest("device1", "TEK822V2", "SET_SERVER");
        request.setParameters(Map.of("serverIp", "192.168.1.1"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commandService.createCommand(request));
        assertTrue(ex.getMessage().contains("serverPort"));
    }

    // ====================== Tutti i tipi di comando validi ======================

    @Test
    void testCreateCommand_allNoParamCommandTypes_succeed() {
        List<String> noParamCommands = List.of(
                "REBOOT", "REQUEST_STATUS", "SHUTDOWN", "DEACTIVATE",
                "CLOSE_TCP", "REQUEST_GPS", "REQUEST_SETTINGS", "SET_SCHEDULE",
                "RESET_RTC", "REQUEST_BUFFER_DATA", "REQUEST_DIAGNOSTIC_DATA");

        for (String cmdType : noParamCommands) {
            CommandCreateRequest request = new CommandCreateRequest("intg-device-all", "TEK822V2", cmdType);
            CommandResponse response = commandService.createCommand(request);

            assertNotNull(response.getId(), "Command " + cmdType + " should be persisted");
            assertEquals(cmdType, response.getCommandType());
            assertEquals("PENDING", response.getStatus());
        }

        List<CommandResponse> allPending = commandService.getPendingCommands("intg-device-all");
        assertFalse(allPending.isEmpty());
        assertEquals(noParamCommands.size(), allPending.size());
    }
}
