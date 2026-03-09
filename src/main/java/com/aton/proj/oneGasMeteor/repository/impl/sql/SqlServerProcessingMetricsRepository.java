package com.aton.proj.oneGasMeteor.repository.impl.sql;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;
import com.aton.proj.oneGasMeteor.repository.ProcessingMetricsRepository;

/**
 * Implementazione SQL Server per le metriche di performance.
 * Attivo solo quando metrics.enabled=true (default: true).
 * La dipendenza da ProcessingMetricsJpaRepository implicitamente richiede database.type=sqlserver.
 */
@Repository
@ConditionalOnProperty(name = "metrics.enabled", havingValue = "true", matchIfMissing = true)
public class SqlServerProcessingMetricsRepository implements ProcessingMetricsRepository {

	private static final Logger log = LoggerFactory.getLogger(SqlServerProcessingMetricsRepository.class);

	private final ProcessingMetricsJpaRepository jpaRepository;

	public SqlServerProcessingMetricsRepository(ProcessingMetricsJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
		log.info(" SqlServerProcessingMetricsRepository initialized");
	}

	@Override
	public ProcessingMetricsEntity save(ProcessingMetricsEntity entity) {
		try {
			ProcessingMetricsEntity saved = jpaRepository.save(entity);
			log.debug(" Saved processing metrics: id={}, deviceId={}, totalMs={}",
					saved.getId(), saved.getDeviceId(), saved.getTotalProcessingTimeMs());
			return saved;
		} catch (Exception e) {
			log.error(" Failed to save processing metrics for device: {}", entity.getDeviceId(), e);
			throw new RuntimeException("Failed to save processing metrics", e);
		}
	}

	@Override
	@Transactional
	public void deleteOlderThan(LocalDateTime threshold) {
		int deleted = jpaRepository.deleteByReceivedAtBefore(threshold);
		log.info("  Deleted {} old processing metrics records before {}", deleted, threshold);
	}
}
