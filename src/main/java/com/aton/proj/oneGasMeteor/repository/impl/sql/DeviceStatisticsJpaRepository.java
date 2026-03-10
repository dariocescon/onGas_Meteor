package com.aton.proj.oneGasMeteor.repository.impl.sql;

import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnJpaDatabase;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository per DeviceStatisticsEntity (SQL Server)
 */
@Repository
@ConditionalOnJpaDatabase
public interface DeviceStatisticsJpaRepository extends JpaRepository<DeviceStatisticsEntity, Long> {

    List<DeviceStatisticsEntity> findByDeviceIdOrderByReceivedAtDesc(String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceStatisticsEntity e WHERE e.receivedAt < :threshold")
    int deleteByReceivedAtBefore(@Param("threshold") LocalDateTime threshold);
}
