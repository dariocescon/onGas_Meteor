package com.aton.proj.oneGasMeteor.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnInfluxDatabase;
import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
import com.aton.proj.oneGasMeteor.repository.impl.influxdb.InfluxDBPointMapper;
import com.aton.proj.oneGasMeteor.service.BatchWriteService;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;

import jakarta.annotation.PostConstruct;

/**
 * Service per batch INSERT su InfluxDB. Stessa architettura di
 * BatchInsertService (code concorrenti + flush periodico) ma usa
 * WriteApiBlocking.writePoints() al posto di JdbcTemplate.batchUpdate().
 */
@Service
@ConditionalOnInfluxDatabase
public class InfluxDBBatchInsertService implements BatchWriteService {

	private static final Logger log = LoggerFactory.getLogger(InfluxDBBatchInsertService.class);

	private final WriteApiBlocking writeApi;

	@Value("${batch.insert.size:100}")
	private int batchSize;

	@Value("${batch.insert.interval-ms:2000}")
	private long batchInsertIntervalMs;

	private final ConcurrentLinkedQueue<TelemetryEntity> telemetryQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<DeviceSettingsEntity> settingsQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<DeviceStatisticsEntity> statisticsQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<DeviceLocationEntity> locationQueue = new ConcurrentLinkedQueue<>();

	private final ReentrantLock flushLock = new ReentrantLock();

	public InfluxDBBatchInsertService(WriteApiBlocking writeApi) {
		this.writeApi = writeApi;
		log.info("InfluxDBBatchInsertService initialized");
	}

	@PostConstruct
	void validate() {
		if (batchSize == -1) {
			log.info(
					"InfluxDBBatchInsertService: batch mode DISABLED (batch.insert.size=-1), writing directly to InfluxDB");
		} else {
			log.info("InfluxDBBatchInsertService: batch mode ENABLED (batch.insert.size={})", batchSize);
		}
	}

	@Override
	public void enqueue(TelemetryEntity entity) {
		if (batchSize == -1) {
			writeDirectly(InfluxDBPointMapper.toPoint(entity), "telemetry");
		} else {
			telemetryQueue.add(entity);
		}
	}

	@Override
	public void enqueue(DeviceSettingsEntity entity) {
		if (batchSize == -1) {
			writeDirectly(InfluxDBPointMapper.toPoint(entity), "device_settings");
		} else {
			settingsQueue.add(entity);
		}
	}

	@Override
	public void enqueue(DeviceStatisticsEntity entity) {
		if (batchSize == -1) {
			writeDirectly(InfluxDBPointMapper.toPoint(entity), "device_statistics");
		} else {
			statisticsQueue.add(entity);
		}
	}

	@Override
	public void enqueue(DeviceLocationEntity entity) {
		if (batchSize == -1) {
			writeDirectly(InfluxDBPointMapper.toPoint(entity), "device_locations");
		} else {
			locationQueue.add(entity);
		}
	}

	@Scheduled(fixedDelayString = "${batch.insert.interval-ms:2000}")
	public void flushAll() {
		if (batchSize == -1) {
			return;
		}
		if (telemetryQueue.isEmpty() && settingsQueue.isEmpty() && statisticsQueue.isEmpty()
				&& locationQueue.isEmpty()) {
			log.trace("All queues empty, skipping flush cycle");
			return;
		}

		if (!flushLock.tryLock()) {
			log.warn("Flush already in progress, skipping this cycle");
			return;
		}

		Instant start = Instant.now();
		int[] counts = new int[4];
		int maxBatchesPerCycle = 10;

		try {
			counts[0] += flushQueue(telemetryQueue, this::flushTelemetry, maxBatchesPerCycle);
			counts[1] += flushQueue(settingsQueue, this::flushSettings, maxBatchesPerCycle);
			counts[2] += flushQueue(statisticsQueue, this::flushStatistics, maxBatchesPerCycle);
			counts[3] += flushQueue(locationQueue, this::flushLocations, maxBatchesPerCycle);
		} finally {
			flushLock.unlock();
			long ms = Duration.between(start, Instant.now()).toMillis();
			log.info(
					"InfluxDB batch flush completed in {} ms — inserted: telemetry={}, settings={}, statistics={}, locations={}",
					ms, counts[0], counts[1], counts[2], counts[3]);
		}
	}

	private int flushQueue(Queue<?> queue, Supplier<Integer> flushMethod, int maxBatches) {
		int total = 0;
		int processed = 0;
		while (!queue.isEmpty() && processed < maxBatches) {
			total += flushMethod.get();
			processed++;
		}
		return total;
	}

	// -------------------------------------------------------------------------
	// Flush methods — conversione Entity → Point e scrittura batch su InfluxDB
	// -------------------------------------------------------------------------

	private int flushTelemetry() {
		List<TelemetryEntity> batch = drain(telemetryQueue);
		if (batch.isEmpty())
			return 0;

		log.debug("Flushing {} telemetry records to InfluxDB", batch.size());
		try {
			List<Point> points = batch.stream().map(InfluxDBPointMapper::toPoint).toList();
			writeApi.writePoints(points);
			log.info("InfluxDB batch write telemetry: {} records persisted", batch.size());
			return batch.size();
		} catch (Exception ex) {
			log.error("InfluxDB batch write failed for telemetry ({} records), re-enqueuing: {}", batch.size(),
					ex.getMessage(), ex);
			telemetryQueue.addAll(batch);
			return 0;
		}
	}

	private int flushSettings() {
		List<DeviceSettingsEntity> batch = drain(settingsQueue);
		if (batch.isEmpty())
			return 0;

		log.debug("Flushing {} device_settings records to InfluxDB", batch.size());
		try {
			List<Point> points = batch.stream().map(InfluxDBPointMapper::toPoint).toList();
			writeApi.writePoints(points);
			log.info("InfluxDB batch write device_settings: {} records persisted", batch.size());
			return batch.size();
		} catch (Exception ex) {
			log.error("InfluxDB batch write failed for device_settings ({} records), re-enqueuing: {}", batch.size(),
					ex.getMessage(), ex);
			settingsQueue.addAll(batch);
			return 0;
		}
	}

	private int flushStatistics() {
		List<DeviceStatisticsEntity> batch = drain(statisticsQueue);
		if (batch.isEmpty())
			return 0;

		log.debug("Flushing {} device_statistics records to InfluxDB", batch.size());
		try {
			List<Point> points = batch.stream().map(InfluxDBPointMapper::toPoint).toList();
			writeApi.writePoints(points);
			log.info("InfluxDB batch write device_statistics: {} records persisted", batch.size());
			return batch.size();
		} catch (Exception ex) {
			log.error("InfluxDB batch write failed for device_statistics ({} records), re-enqueuing: {}", batch.size(),
					ex.getMessage(), ex);
			statisticsQueue.addAll(batch);
			return 0;
		}
	}

	private int flushLocations() {
		List<DeviceLocationEntity> batch = drain(locationQueue);
		if (batch.isEmpty())
			return 0;

		log.debug("Flushing {} device_locations records to InfluxDB", batch.size());
		try {
			List<Point> points = batch.stream().map(InfluxDBPointMapper::toPoint).toList();
			writeApi.writePoints(points);
			log.info("InfluxDB batch write device_locations: {} records persisted", batch.size());
			return batch.size();
		} catch (Exception ex) {
			log.error("InfluxDB batch write failed for device_locations ({} records), re-enqueuing: {}", batch.size(),
					ex.getMessage(), ex);
			locationQueue.addAll(batch);
			return 0;
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private void writeDirectly(Point point, String entityType) {
		try {
			writeApi.writePoint(point);
			log.debug("Direct write {}: record persisted", entityType);
		} catch (Exception ex) {
			log.error("Direct write failed for {}: {}", entityType, ex.getMessage(), ex);
		}
	}

	private <T> List<T> drain(ConcurrentLinkedQueue<T> queue) {
		List<T> list = new ArrayList<>(batchSize);
		T item;
		int count = 0;
		while (count < batchSize && (item = queue.poll()) != null) {
			list.add(item);
			count++;
		}
		return list;
	}
}
