package com.aton.proj.oneGasMeteor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aton.proj.oneGasMeteor.dto.CommandCreateRequest;
import com.aton.proj.oneGasMeteor.dto.CommandResponse;
import com.aton.proj.oneGasMeteor.encoder.EncoderFactory;
import com.aton.proj.oneGasMeteor.encoder.impl.Tek822Encoder;
import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

	@Mock
	private CommandRepository commandRepository;

	@Mock
	private EncoderFactory encoderFactory;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Tek822Encoder tek822Encoder = new Tek822Encoder();
	private CommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new CommandService(commandRepository, encoderFactory, objectMapper);
	}

	// ====================== Validazione deviceType ======================

	@Test
	void testCreateCommand_invalidDeviceType() {
		when(encoderFactory.getEncoder("UNKNOWN")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "UNKNOWN", "REBOOT");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("Unsupported deviceType"));
	}

	@Test
	void testCreateCommand_nullDeviceType() {
		when(encoderFactory.getEncoder(null)).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", null, "REBOOT");

		assertThrows(IllegalArgumentException.class, () -> commandService.createCommand(request));
	}

	// ====================== Validazione commandType ======================

	@Test
	void testCreateCommand_invalidCommandType() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "INVALID_CMD");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("Unknown commandType"));
	}

	// ====================== Validazione parametri obbligatori ======================

	@Test
	void testCreateCommand_setInterval_missingInterval() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_INTERVAL");
		// Nessun parametro → manca "interval"

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("interval"));
	}

	@Test
	void testCreateCommand_setListen_missingListenMinutes() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_LISTEN");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("listenMinutes"));
	}

	@Test
	void testCreateCommand_setAlarmThreshold_missingThreshold() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_ALARM_THRESHOLD");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("threshold"));
	}

	@Test
	void testCreateCommand_setRTC_missingDatetime() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_RTC");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("datetime"));
	}

	@Test
	void testCreateCommand_setAPN_missingApn() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_APN");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("apn"));
	}

	@Test
	void testCreateCommand_setServer_missingServerIp() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_SERVER");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("serverIp"));
	}

	@Test
	void testCreateCommand_setServer_missingServerPort() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_SERVER");
		request.setParameters(Map.of("serverIp", "192.168.1.1"));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> commandService.createCommand(request));
		assertTrue(ex.getMessage().contains("serverPort"));
	}

	// ====================== Creazione riuscita ======================

	@Test
	void testCreateCommand_reboot_noParams() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandEntity savedEntity = new CommandEntity("dev1", "TEK822V2", "REBOOT");
		savedEntity.setId(1L);
		when(commandRepository.save(any(DeviceCommand.class))).thenReturn(savedEntity);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "REBOOT");
		CommandResponse response = commandService.createCommand(request);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("dev1", response.getDeviceId());
		assertEquals("TEK822V2", response.getDeviceType());
		assertEquals("REBOOT", response.getCommandType());
		assertEquals("PENDING", response.getStatus());
	}

	@Test
	void testCreateCommand_setInterval_withParams() throws Exception {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		Map<String, Object> allParams = Map.of("interval", 1, "samplingPeriod", 2);

		CommandEntity savedEntity = new CommandEntity("dev1", "TEK822V2", "SET_INTERVAL");
		savedEntity.setId(2L);
		savedEntity.setCommandParamsJson(objectMapper.writeValueAsString(allParams));
		when(commandRepository.save(any(DeviceCommand.class))).thenReturn(savedEntity);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_INTERVAL");
		request.setParameters(allParams);
		CommandResponse response = commandService.createCommand(request);

		assertNotNull(response);
		assertEquals(2L, response.getId());
		assertEquals("SET_INTERVAL", response.getCommandType());
		assertEquals("1", response.getParameters().get("interval").toString());
		assertEquals("2", response.getParameters().get("samplingPeriod").toString());
		assertEquals("PENDING", response.getStatus());
	}

	@Test
	void testCreateCommand_requestStatus_noParams() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandEntity savedEntity = new CommandEntity("dev1", "TEK822V2", "REQUEST_STATUS");
		savedEntity.setId(3L);
		when(commandRepository.save(any(DeviceCommand.class))).thenReturn(savedEntity);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "REQUEST_STATUS");
		CommandResponse response = commandService.createCommand(request);

		assertEquals("REQUEST_STATUS", response.getCommandType());
		assertEquals("PENDING", response.getStatus());
	}

	@Test
	void testCreateCommand_setAPN_withAllParams() throws Exception {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		Map<String, Object> allParams = Map.of("apn", "internet", "username", "user", "apnPassword", "pass");
		
		CommandEntity savedEntity = new CommandEntity("dev1", "TEK822V2", "SET_APN");
		savedEntity.setId(4L);
		savedEntity.setCommandParamsJson(objectMapper.writeValueAsString(allParams));
		when(commandRepository.save(any(DeviceCommand.class))).thenReturn(savedEntity);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SET_APN");
		request.setParameters(Map.of("apn", "internet", "username", "user", "apnPassword", "pass"));
		CommandResponse response = commandService.createCommand(request);

		assertEquals("SET_APN", response.getCommandType());
		assertNotNull(response.getParameters());
		assertEquals("internet", response.getParameters().get("apn"));
		assertEquals("user", response.getParameters().get("username").toString());
		assertEquals("pass", response.getParameters().get("apnPassword").toString());
	}

	// ====================== Comandi senza parametri obbligatori ======================

	@Test
	void testCreateCommand_shutdown_noParams() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandEntity savedEntity = new CommandEntity("dev1", "TEK822V2", "SHUTDOWN");
		savedEntity.setId(5L);
		when(commandRepository.save(any(DeviceCommand.class))).thenReturn(savedEntity);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "SHUTDOWN");
		CommandResponse response = commandService.createCommand(request);

		assertEquals("SHUTDOWN", response.getCommandType());
	}

	@Test
	void testCreateCommand_requestGPS_noParams() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		CommandEntity savedEntity = new CommandEntity("dev1", "TEK822V2", "REQUEST_GPS");
		savedEntity.setId(6L);
		when(commandRepository.save(any(DeviceCommand.class))).thenReturn(savedEntity);

		CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", "REQUEST_GPS");
		CommandResponse response = commandService.createCommand(request);

		assertEquals("REQUEST_GPS", response.getCommandType());
	}

	// ====================== GET commands ======================

	@Test
	void testGetCommandById_found() {
		CommandEntity entity = new CommandEntity("dev1", "TEK822V2", "REBOOT");
		entity.setId(10L);
		when(commandRepository.findById(10L)).thenReturn(Optional.of(entity));

		CommandResponse response = commandService.getCommandById(10L);

		assertEquals(10L, response.getId());
		assertEquals("REBOOT", response.getCommandType());
	}

	@Test
	void testGetCommandById_notFound() {
		when(commandRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> commandService.getCommandById(999L));
	}

	@Test
	void testGetPendingCommands_empty() {
		when(commandRepository.findPendingCommands("dev1")).thenReturn(List.of());

		List<CommandResponse> result = commandService.getPendingCommands("dev1");

		assertTrue(result.isEmpty());
	}

	@Test
	void testGetPendingCommands_withResults() {
		CommandEntity cmd1 = new CommandEntity("dev1", "TEK822V2", "SET_INTERVAL");
		cmd1.setId(1L);
		CommandEntity cmd2 = new CommandEntity("dev1", "TEK822V2", "REBOOT");
		cmd2.setId(2L);

		when(commandRepository.findPendingCommands("dev1")).thenReturn(List.of(cmd1, cmd2));

		List<CommandResponse> result = commandService.getPendingCommands("dev1");

		assertEquals(2, result.size());
		assertEquals("SET_INTERVAL", result.get(0).getCommandType());
		assertEquals("REBOOT", result.get(1).getCommandType());
	}

	// ====================== Tutti i commandType validi passano validazione ======================

	@Test
	void testAllValidCommandTypes_passValidation() {
		when(encoderFactory.getEncoder("TEK822V2")).thenReturn(tek822Encoder);

		// Comandi senza parametri obbligatori
		List<String> noParamCommands = List.of("REBOOT", "REQUEST_STATUS", "SHUTDOWN",
				"DEACTIVATE", "CLOSE_TCP", "REQUEST_GPS", "REQUEST_SETTINGS", "SET_SCHEDULE");

		for (String cmdType : noParamCommands) {
			CommandEntity savedEntity = new CommandEntity("dev1", "TEK822V2", cmdType);
			savedEntity.setId(100L);
			when(commandRepository.save(any(DeviceCommand.class))).thenReturn(savedEntity);

			CommandCreateRequest request = new CommandCreateRequest("dev1", "TEK822V2", cmdType);
			CommandResponse response = commandService.createCommand(request);

			assertEquals(cmdType, response.getCommandType(),
					"Command type " + cmdType + " should pass validation without parameters");
		}
	}
}
