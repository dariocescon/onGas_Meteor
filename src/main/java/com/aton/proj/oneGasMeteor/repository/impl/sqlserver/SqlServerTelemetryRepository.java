package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.repository.TelemetryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementazione SQL Server per TelemetryRepository
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public class SqlServerTelemetryRepository implements TelemetryRepository {

	private static final Logger log = LoggerFactory.getLogger(SqlServerTelemetryRepository.class);

	private final TelemetryJpaRepository jpaRepository;
	private final ObjectMapper objectMapper;

	public SqlServerTelemetryRepository(TelemetryJpaRepository jpaRepository, ObjectMapper objectMapper) {
		this.jpaRepository = jpaRepository;
		this.objectMapper = objectMapper;
		log.info(" SqlServerTelemetryRepository initialized");
	}

	@Override
	public TelemetryEntity save(String deviceId, String deviceType, String rawMessage, DecodedMessage decoded) {
		try {
			TelemetryEntity entity = new TelemetryEntity();
			entity.setDeviceId(deviceId);
			entity.setDeviceType(deviceType);
			entity.setRawMessage(rawMessage);
			entity.setReceivedAt(LocalDateTime.now());
			entity.setProcessedAt(LocalDateTime.now());

			// Serializza DecodedMessage in JSON
			String decodedJson = objectMapper.writeValueAsString(decoded);
			entity.setDecodedDataJson(decodedJson);

			// Estrai campi principali per query veloci
			extractMainFields(entity, decoded);

			TelemetryEntity saved = jpaRepository.save(entity);
			log.debug(" Saved telemetry: id={}, deviceId={}", saved.getId(), deviceId);

			return saved;

		} catch (Exception e) {
			log.error(" Failed to save telemetry for device: {}", deviceId, e);
			throw new RuntimeException("Failed to save telemetry", e);
		}
	}

	private void extractMainFields(TelemetryEntity entity, DecodedMessage decoded) {
		if (decoded.getUniqueIdentifier() != null) {
			entity.setImei(decoded.getUniqueIdentifier().getImei());
		}

		if (decoded.getUnitInfo() != null) {
			entity.setFirmwareVersion(decoded.getUnitInfo().getFirmwareRevision());
		}

		if (decoded.getBatteryStatus() != null) {
			String voltageStr = decoded.getBatteryStatus().getBatteryVoltage();
			if (voltageStr != null) {
				try {
					entity.setBatteryVoltage(Double.parseDouble(voltageStr));
				} catch (NumberFormatException ignored) {
				}
			}

			String percentageStr = decoded.getBatteryStatus().getBatteryRemainingPercentage();
			if (percentageStr != null) {
				try {
					entity.setBatteryPercentage(Double.parseDouble(percentageStr));
				} catch (NumberFormatException ignored) {
				}
			}
		}

		if (decoded.getSignalStrength() != null) {
			Integer csq = decoded.getSignalStrength().getCsq();
			Integer rssi = decoded.getSignalStrength().getRssi();
			entity.setSignalStrength(csq != null ? csq : rssi);
		}

		entity.setMessageType(decoded.getMessageType());

		if (decoded.getMeasurementData() != null) {
			entity.setMeasurementCount(decoded.getMeasurementData().size());
		}
	}

	@Override
	public Optional<TelemetryEntity> findById(Long id) {
		return jpaRepository.findById(id);
	}

	@Override
	public List<TelemetryEntity> findByDeviceId(String deviceId) {
		return jpaRepository.findByDeviceIdOrderByReceivedAtDesc(deviceId);
	}

	@Override
	public List<TelemetryEntity> findByDeviceIdAndDateRange(String deviceId, LocalDateTime from, LocalDateTime to) {
		return jpaRepository.findByDeviceIdAndReceivedAtBetween(deviceId, from, to);
	}

	@Override
	public List<TelemetryEntity> findByImei(String imei) {
		return jpaRepository.findByImeiOrderByReceivedAtDesc(imei);
	}

	@Override
	public List<TelemetryEntity> findByDeviceType(String deviceType) {
		return jpaRepository.findByDeviceTypeOrderByReceivedAtDesc(deviceType);
	}

	@Override
	public long countByDeviceId(String deviceId) {
		return jpaRepository.countByDeviceId(deviceId);
	}

	@Override
	@Transactional
	public void deleteOlderThan(LocalDateTime threshold) {
		int deleted = jpaRepository.deleteByReceivedAtBefore(threshold);
		log.info("  Deleted {} old telemetry records before {}", deleted, threshold);
	}
}