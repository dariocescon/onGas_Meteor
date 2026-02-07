package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;

/**
 * JPA Repository per TelemetryEntity (SQL Server)
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public interface TelemetryJpaRepository extends JpaRepository<TelemetryEntity, Long> {

	List<TelemetryEntity> findByDeviceIdOrderByReceivedAtDesc(String deviceId);

	List<TelemetryEntity> findByDeviceIdAndReceivedAtBetween(String deviceId, LocalDateTime from, LocalDateTime to);

	List<TelemetryEntity> findByImeiOrderByReceivedAtDesc(String imei);

	List<TelemetryEntity> findByDeviceTypeOrderByReceivedAtDesc(String deviceType);

	long countByDeviceId(String deviceId);

	@Modifying
	@Query("DELETE FROM TelemetryEntity t WHERE t.receivedAt < :threshold")
	int deleteByReceivedAtBefore(@Param("threshold") LocalDateTime threshold);
}