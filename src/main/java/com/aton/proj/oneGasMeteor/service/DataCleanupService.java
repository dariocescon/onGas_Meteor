package com.aton.proj.oneGasMeteor.service;

import com.aton.proj.oneGasMeteor.repository.CommandRepository;
import com.aton.proj.oneGasMeteor.repository.DeviceLocationRepository;
import com.aton.proj.oneGasMeteor.repository.DeviceSettingsRepository;
import com.aton.proj.oneGasMeteor.repository.DeviceStatisticsRepository;
import com.aton.proj.oneGasMeteor.repository.ProcessingMetricsRepository;
import com.aton.proj.oneGasMeteor.repository.TelemetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service per il cleanup automatico dei dati vecchi
 */
@Service
@ConditionalOnProperty(name = "cleanup.enabled", havingValue = "true", matchIfMissing = false)
public class DataCleanupService {

	private static final Logger log = LoggerFactory.getLogger(DataCleanupService.class);

	private final TelemetryRepository telemetryRepository;
	private final CommandRepository commandRepository;
	private final DeviceSettingsRepository deviceSettingsRepository;
	private final DeviceStatisticsRepository deviceStatisticsRepository;
	private final DeviceLocationRepository deviceLocationRepository;
	private final ProcessingMetricsRepository processingMetricsRepository;

	@Value("${cleanup.telemetry.retention.days:30}")
	private int telemetryRetentionDays;

	@Value("${cleanup.commands.retention.days:7}")
	private int commandsRetentionDays;

	@Value("${cleanup.metrics.retention.days:90}")
	private int metricsRetentionDays;

	public DataCleanupService(TelemetryRepository telemetryRepository, CommandRepository commandRepository,
			DeviceSettingsRepository deviceSettingsRepository,
			DeviceStatisticsRepository deviceStatisticsRepository,
			DeviceLocationRepository deviceLocationRepository,
			@Nullable ProcessingMetricsRepository processingMetricsRepository) {
		this.telemetryRepository = telemetryRepository;
		this.commandRepository = commandRepository;
		this.deviceSettingsRepository = deviceSettingsRepository;
		this.deviceStatisticsRepository = deviceStatisticsRepository;
		this.deviceLocationRepository = deviceLocationRepository;
		this.processingMetricsRepository = processingMetricsRepository;

		log.info(" DataCleanupService initialized");
		log.info("   Telemetry retention: {} days", telemetryRetentionDays);
		log.info("   Commands retention: {} days", commandsRetentionDays);
		log.info("   Metrics retention: {} days", metricsRetentionDays);
	}

	/**
	 * Cleanup automatico eseguito ogni notte alle 2:00 AM Cron format: "secondo
	 * minuto ora giorno mese giorno_settimana"
	 */
	@Scheduled(cron = "${cleanup.cron:0 0 2 * * *}")
	public void scheduledCleanup() {
		log.info(" Starting scheduled data cleanup...");

		try {
			long startTime = System.currentTimeMillis();

			// Cleanup telemetry
			cleanupOldTelemetry();

			// Cleanup commands
			cleanupOldCommands();

			// Cleanup device settings, statistics and locations
			cleanupOldDeviceData();

			// Cleanup processing metrics
			cleanupOldProcessingMetrics();

			long duration = System.currentTimeMillis() - startTime;
			log.info(" Cleanup completed successfully in {} ms", duration);

		} catch (Exception e) {
			log.error(" Error during scheduled cleanup", e);
		}
	}

	/**
	 * Elimina telemetry più vecchia di N giorni
	 */
	public void cleanupOldTelemetry() {
		try {
			LocalDateTime threshold = LocalDateTime.now().minusDays(telemetryRetentionDays);

			log.info("  Cleaning telemetry older than {} (retention: {} days)", threshold, telemetryRetentionDays);

			telemetryRepository.deleteOlderThan(threshold);

		} catch (Exception e) {
			log.error(" Error cleaning old telemetry", e);
		}
	}

	/**
	 * Elimina comandi completati più vecchi di N giorni
	 */
	public void cleanupOldCommands() {
		try {
			log.info(" Cleaning completed commands older than {} days", commandsRetentionDays);

			commandRepository.deleteOldCompletedCommands(commandsRetentionDays);

		} catch (Exception e) {
			log.error(" Error cleaning old commands", e);
		}
	}

	/**
	 * Elimina device settings, statistics e locations più vecchi di N giorni
	 */
	public void cleanupOldDeviceData() {
		try {
			LocalDateTime threshold = LocalDateTime.now().minusDays(telemetryRetentionDays);

			log.info(" Cleaning device data older than {} (retention: {} days)", threshold, telemetryRetentionDays);

			deviceSettingsRepository.deleteOlderThan(threshold);
			deviceStatisticsRepository.deleteOlderThan(threshold);
			deviceLocationRepository.deleteOlderThan(threshold);

		} catch (Exception e) {
			log.error(" Error cleaning old device data", e);
		}
	}

	/**
	 * Elimina metriche di performance piu' vecchie di N giorni
	 */
	public void cleanupOldProcessingMetrics() {
		if (processingMetricsRepository == null) {
			log.debug("Processing metrics repository not available, skipping cleanup");
			return;
		}
		try {
			LocalDateTime threshold = LocalDateTime.now().minusDays(metricsRetentionDays);

			log.info(" Cleaning processing metrics older than {} (retention: {} days)", threshold, metricsRetentionDays);

			processingMetricsRepository.deleteOlderThan(threshold);

		} catch (Exception e) {
			log.error(" Error cleaning old processing metrics", e);
		}
	}

	/**
	 * Cleanup manuale (può essere chiamato da un endpoint admin)
	 */
	public CleanupReport manualCleanup() {
		log.info(" Manual cleanup triggered");

		CleanupReport report = new CleanupReport();
		report.setStartTime(LocalDateTime.now());

		try {
			cleanupOldTelemetry();
			cleanupOldCommands();
			cleanupOldDeviceData();
			cleanupOldProcessingMetrics();

			report.setSuccess(true);
			report.setEndTime(LocalDateTime.now());

		} catch (Exception e) {
			report.setSuccess(false);
			report.setErrorMessage(e.getMessage());
			report.setEndTime(LocalDateTime.now());
		}

		return report;
	}

	/**
	 * Report del cleanup
	 */
	public static class CleanupReport {
		private LocalDateTime startTime;
		private LocalDateTime endTime;
		private boolean success;
		private String errorMessage;

		public LocalDateTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public long getDurationMs() {
			if (startTime != null && endTime != null) {
				return java.time.Duration.between(startTime, endTime).toMillis();
			}
			return 0;
		}
	}
}