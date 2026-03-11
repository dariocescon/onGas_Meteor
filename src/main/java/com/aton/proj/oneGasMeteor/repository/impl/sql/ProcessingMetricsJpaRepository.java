package com.aton.proj.oneGasMeteor.repository.impl.sql;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnJpaDatabase;
import com.aton.proj.oneGasMeteor.entity.ProcessingMetricsEntity;

/**
 * JPA Repository per ProcessingMetricsEntity
 */
@Repository
@ConditionalOnJpaDatabase
public interface ProcessingMetricsJpaRepository extends JpaRepository<ProcessingMetricsEntity, Long> {

	@Modifying
	@Query("DELETE FROM ProcessingMetricsEntity p WHERE p.receivedAt < :threshold")
	int deleteByReceivedAtBefore(@Param("threshold") LocalDateTime threshold);
}
