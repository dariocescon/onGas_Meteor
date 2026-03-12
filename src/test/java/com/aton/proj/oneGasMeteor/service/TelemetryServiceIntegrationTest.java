package com.aton.proj.oneGasMeteor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.model.TelemetryMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryResponse;
import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.aton.proj.oneGasMeteor.repository.impl.sql.TelemetryJpaRepository;
import com.aton.proj.oneGasMeteor.server.TcpSocketServer;
import com.aton.proj.oneGasMeteor.service.impl.BatchInsertService;
import com.aton.proj.oneGasMeteor.utils.ControllerUtils;

/**
 * Test di integrazione per TelemetryService con database H2 in-memory. Verifica
 * il flusso completo di elaborazione: decodifica → accodamento in batch insert
 * → recupero comandi pendenti → codifica comandi di risposta.
 */
@SpringBootTest
@Transactional
class TelemetryServiceIntegrationTest {

	/**
	 * Messaggio reale TEK822V2 con message type 4 (telemetria standard). Stessa
	 * sequenza usata nei test del decoder ({@code TekMessageDecoderTest}).
	 */
	private static final String HEX_MSG_TYPE4 = "180A640188117C0862406075927406047B0078773652FF84002100721E31000161E0860000112233445047B00005200002C84013000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000F594A4E29F30A5029F30A5029F30A5229F30A5029F30A5029F30A4E29F30A5029F30A5029F30A5029F30A5029F30A5429F30A5629F30A5A29F30A5629F30A5229F30A4E29F30A4829F30A4629F30A4229F30A4229F30000000000000000000000000000000000000000000000000000000055F7";

	@Autowired
	private TelemetryService telemetryService;

	@Autowired
	private CommandRepository commandRepository;

	@Autowired
	private BatchInsertService batchInsertService;

	@Autowired
	private TelemetryJpaRepository telemetryJpaRepository;

	@MockitoBean
	private TcpSocketServer tcpSocketServer;

	// ====================== processTelemetry ======================

	@Test
	void testProcessTelemetry_type4_noCommands_returnsOkResponse() {
		byte[] payload = ControllerUtils.hexStringToByteArray(HEX_MSG_TYPE4);
		TelemetryMessage message = new TelemetryMessage(payload, "127.0.0.1");

		TelemetryResponse response = telemetryService.processTelemetry(message);

		assertNotNull(response);
		assertEquals("OK", response.getStatus());
		assertNotNull(response.getDeviceId());
		assertNotNull(response.getDeviceType());
		assertNotNull(response.getReceivedAt());
		assertNotNull(response.getProcessedAt());
		assertTrue(response.getCommands().isEmpty());
	}

	@Test
	void testProcessTelemetry_type4_withPendingCommand_commandIncludedInResponse() {
		byte[] payload = ControllerUtils.hexStringToByteArray(HEX_MSG_TYPE4);

		// Prima chiamata per ottenere il deviceId estratto dal messaggio
		TelemetryResponse firstResponse = telemetryService.processTelemetry(new TelemetryMessage(payload, "127.0.0.1"));
		String deviceId = firstResponse.getDeviceId();
		String deviceType = firstResponse.getDeviceType();

		// Crea un comando pendente per questo device
		DeviceCommand cmd = new DeviceCommand(deviceId, deviceType, "REBOOT");
		commandRepository.save(cmd);

		// Seconda elaborazione: deve trovare il comando pendente
		TelemetryResponse response = telemetryService.processTelemetry(new TelemetryMessage(payload, "127.0.0.1"));

		assertFalse(response.getCommands().isEmpty(), "Il comando pendente deve apparire nella risposta");
		assertEquals("REBOOT", response.getCommands().get(0).getCommandType());
	}

	@Test
	void testProcessTelemetry_type4_extractsDeviceIdAndType() {
		byte[] payload = ControllerUtils.hexStringToByteArray(HEX_MSG_TYPE4);
		TelemetryMessage message = new TelemetryMessage(payload, "127.0.0.1");

		TelemetryResponse response = telemetryService.processTelemetry(message);

		// Il device type deve essere TEK822V2 (come verificato nei decoder test)
		assertEquals("TEK822V2", response.getDeviceType());
		assertNotNull(response.getDeviceId());
		assertFalse(response.getDeviceId().isEmpty());
	}

	@Test
	void testProcessTelemetry_type4_enqueuesForBatchInsert_dataPersisted() {
		byte[] payload = ControllerUtils.hexStringToByteArray(HEX_MSG_TYPE4);
		TelemetryMessage message = new TelemetryMessage(payload, "127.0.0.1");

		TelemetryResponse response = telemetryService.processTelemetry(message);

		// Il flush del batch deve inserire la telemetria nel DB H2
		batchInsertService.flushAll();

		List<?> saved = telemetryJpaRepository.findByDeviceIdOrderByReceivedAtDesc(response.getDeviceId());
		assertFalse(saved.isEmpty(), "La telemetria deve essere persistita dopo il flush");
	}

	@Test
	void testProcessTelemetry_withProcessingContext() {
		byte[] payload = ControllerUtils.hexStringToByteArray(HEX_MSG_TYPE4);
		TelemetryMessage message = new TelemetryMessage(payload, "127.0.0.1");
		com.aton.proj.oneGasMeteor.model.ProcessingContext context = new com.aton.proj.oneGasMeteor.model.ProcessingContext(
				"127.0.0.1");

		TelemetryResponse response = telemetryService.processTelemetry(message, context);

		assertEquals("OK", response.getStatus());
		// Il contesto deve aver registrato device ID e type
		assertNotNull(context.getDeviceId());
		assertNotNull(context.getDeviceType());
		assertEquals("TEK822V2", context.getDeviceType());
	}

	// ====================== markCommandsAsSent ======================

	@Test
	void testMarkCommandsAsSent_updatesCommandStatusToSent() {
		DeviceCommand cmd = new DeviceCommand("mark-sent-device", "TEK822V2", "REBOOT");
		CommandEntity entity = commandRepository.save(cmd);
		assertNotNull(entity.getId());
		assertEquals(CommandEntity.CommandStatus.PENDING, entity.getStatus());

		TelemetryResponse.EncodedCommand encoded = new TelemetryResponse.EncodedCommand(entity.getId(), "REBOOT",
				"encoded-data", "ascii-data");

		telemetryService.markCommandsAsSent(List.of(encoded));

		Optional<CommandEntity> updated = commandRepository.findById(entity.getId());
		assertTrue(updated.isPresent());
		assertEquals(CommandEntity.CommandStatus.SENT, updated.get().getStatus());
		assertNotNull(updated.get().getSentAt());
	}

	@Test
	void testMarkCommandsAsSent_syntheticCommandWithNullId_noException() {
		// I comandi sintetici (es. REBOOT auto-appended) hanno commandId null
		TelemetryResponse.EncodedCommand syntheticCmd = new TelemetryResponse.EncodedCommand(null, "REBOOT", "encoded",
				"ascii");

		// Non deve lanciare eccezioni
		telemetryService.markCommandsAsSent(List.of(syntheticCmd));
	}

	@Test
	void testMarkCommandsAsSent_multipleCommands_allMarkedAsSent() {
		DeviceCommand cmd1 = new DeviceCommand("multi-device", "TEK822V2", "REBOOT");
		DeviceCommand cmd2 = new DeviceCommand("multi-device", "TEK822V2", "REQUEST_STATUS");
		CommandEntity entity1 = commandRepository.save(cmd1);
		CommandEntity entity2 = commandRepository.save(cmd2);

		List<TelemetryResponse.EncodedCommand> encodedList = List.of(
				new TelemetryResponse.EncodedCommand(entity1.getId(), "REBOOT", "enc1", "asc1"),
				new TelemetryResponse.EncodedCommand(entity2.getId(), "REQUEST_STATUS", "enc2", "asc2"));

		telemetryService.markCommandsAsSent(encodedList);

		assertEquals(CommandEntity.CommandStatus.SENT, commandRepository.findById(entity1.getId()).get().getStatus());
		assertEquals(CommandEntity.CommandStatus.SENT, commandRepository.findById(entity2.getId()).get().getStatus());
	}

	// ====================== processTelemetry senza context ======================

	@Test
	void testProcessTelemetry_backwardCompatible_noContextOverload() {
		byte[] payload = ControllerUtils.hexStringToByteArray(HEX_MSG_TYPE4);
		TelemetryMessage message = new TelemetryMessage(payload, "127.0.0.1");

		// Overload senza context
		TelemetryResponse response = telemetryService.processTelemetry(message);

		assertEquals("OK", response.getStatus());
		assertNotNull(response.getDeviceId());
	}

	// ====================== Limite massimo comandi per risposta
	// ======================

	@Test
	void testProcessTelemetry_maxCommandsPerResponse_respected() {
		byte[] payload = ControllerUtils.hexStringToByteArray(HEX_MSG_TYPE4);
		TelemetryMessage firstMsg = new TelemetryMessage(payload, "127.0.0.1");
		TelemetryResponse firstResponse = telemetryService.processTelemetry(firstMsg);
		String deviceId = firstResponse.getDeviceId();
		String deviceType = firstResponse.getDeviceType();

		// Inserisce 15 comandi (> default max di 10)
		for (int i = 0; i < 15; i++) {
			commandRepository.save(new DeviceCommand(deviceId, deviceType, "REBOOT"));
		}

		TelemetryResponse response = telemetryService.processTelemetry(new TelemetryMessage(payload, "127.0.0.1"));

		// Il numero di comandi nella risposta non deve superare il max configurato (10)
		assertTrue(response.getCommands().size() <= 10,
				"La risposta non deve contenere più di 10 comandi (max configurato)");
	}
}
