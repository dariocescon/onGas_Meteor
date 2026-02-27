package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.CommandEntity;
import com.aton.proj.oneGasMeteor.model.DeviceCommand;
import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementazione SQL Server per CommandRepository
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public class SqlServerCommandRepository implements CommandRepository {

	private static final Logger log = LoggerFactory.getLogger(SqlServerCommandRepository.class);

	private final CommandJpaRepository jpaRepository;
	private final ObjectMapper objectMapper;

	public SqlServerCommandRepository(CommandJpaRepository jpaRepository, ObjectMapper objectMapper) {
		this.jpaRepository = jpaRepository;
		this.objectMapper = objectMapper;
		log.info("  SqlServerCommandRepository initialized");
	}

	@Override
	public CommandEntity save(DeviceCommand command) {
		try {
			CommandEntity entity = new CommandEntity(command.getDeviceId(), command.getDeviceType(),
					command.getCommandType());

			// Serializza parametri in JSON
			if (command.getParameters() != null && !command.getParameters().isEmpty()) {
				String paramsJson = objectMapper.writeValueAsString(command.getParameters());
				System.err.println(paramsJson);
				entity.setCommandParamsJson(paramsJson);
			}

			CommandEntity saved = jpaRepository.save(entity);
			log.debug("  Saved command: id={}, type={}, deviceId={}", saved.getId(), saved.getCommandType(),
					saved.getDeviceId());

			return saved;

		} catch (Exception e) {
			log.error("  Failed to save command for device: {}", command.getDeviceId(), e);
			throw new RuntimeException("Failed to save command", e);
		}
	}

	@Override
	public Optional<CommandEntity> findById(Long id) {
		return jpaRepository.findById(id);
	}

	@Override
	public List<CommandEntity> findPendingCommands(String deviceId) {
		return jpaRepository.findByDeviceIdAndStatusOrderByCreatedAtAsc(deviceId, CommandEntity.CommandStatus.PENDING);
	}

	@Override
	public List<CommandEntity> findPendingCommandsByDeviceType(String deviceType) {
		return jpaRepository.findByDeviceTypeAndStatusOrderByCreatedAtAsc(deviceType,
				CommandEntity.CommandStatus.PENDING);
	}

	@Override
	@Transactional
	public void updateStatus(Long commandId, CommandEntity.CommandStatus status) {
		jpaRepository.findById(commandId).ifPresent(entity -> {
			entity.setStatus(status);
			jpaRepository.save(entity);
			log.debug(" Updated command {} status to {}", commandId, status);
		});
	}

	@Override
	@Transactional
	public void markAsSent(Long commandId) {
		jpaRepository.findById(commandId).ifPresent(entity -> {
			entity.setStatus(CommandEntity.CommandStatus.SENT);
			entity.setSentAt(LocalDateTime.now());
			jpaRepository.save(entity);
			log.debug(" Marked command {} as SENT", commandId);
		});
	}

	@Override
	@Transactional
	public void markAsDelivered(Long commandId) {
		jpaRepository.findById(commandId).ifPresent(entity -> {
			entity.setStatus(CommandEntity.CommandStatus.DELIVERED);
			entity.setDeliveredAt(LocalDateTime.now());
			jpaRepository.save(entity);
			log.debug(" Marked command {} as DELIVERED", commandId);
		});
	}

	@Override
	@Transactional
	public void markAsFailed(Long commandId, String errorMessage) {
		jpaRepository.findById(commandId).ifPresent(entity -> {
			entity.setStatus(CommandEntity.CommandStatus.FAILED);
			entity.setErrorMessage(errorMessage);
			jpaRepository.save(entity);
			log.warn(" Marked command {} as FAILED: {}", commandId, errorMessage);
		});
	}

	@Override
	@Transactional
	public void incrementRetryCount(Long commandId) {
		jpaRepository.findById(commandId).ifPresent(entity -> {
			entity.setRetryCount(entity.getRetryCount() + 1);
			jpaRepository.save(entity);
			log.debug(" Incremented retry count for command {}: {}/{}", commandId, entity.getRetryCount(),
					entity.getMaxRetries());
		});
	}

	@Override
	@Transactional
	public void deleteOldCompletedCommands(int daysOld) {
		LocalDateTime threshold = LocalDateTime.now().minusDays(daysOld);
		int deleted = jpaRepository.deleteOldCompleted(threshold);
		log.info("  Deleted {} old completed commands before {}", deleted, threshold);
	}

}