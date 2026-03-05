package com.aton.proj.oneGasMeteor.repository;

import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;

import java.time.LocalDateTime;

/**
 * Repository astratto per le metriche di performance.
 * Le implementazioni concrete gestiscono SQL Server o MongoDB.
 */
public interface ProcessingMetricsRepository {

	/**
	 * Salva le metriche di una singola elaborazione
	 */
	ProcessingMetricsEntity save(ProcessingMetricsEntity entity);

	/**
	 * Elimina metriche piu' vecchie della soglia (per cleanup)
	 */
	void deleteOlderThan(LocalDateTime threshold);
}
