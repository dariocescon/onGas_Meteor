package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;

/**
 * JPA Repository per ProcessingMetricsEntity (SQL Server)
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public interface ProcessingMetricsJpaRepository extends JpaRepository<ProcessingMetricsEntity, Long> {

	@Modifying
	@Query("DELETE FROM ProcessingMetricsEntity p WHERE p.receivedAt < :threshold")
	int deleteByReceivedAtBefore(@Param("threshold") LocalDateTime threshold);
}
