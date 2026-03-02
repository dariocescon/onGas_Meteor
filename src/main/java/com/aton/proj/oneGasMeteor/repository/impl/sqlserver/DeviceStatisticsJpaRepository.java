package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public interface DeviceStatisticsJpaRepository extends JpaRepository<DeviceStatisticsEntity, Long> {

    List<DeviceStatisticsEntity> findByDeviceIdOrderByReceivedAtDesc(String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceStatisticsEntity e WHERE e.receivedAt < :threshold")
    int deleteByReceivedAtBefore(@Param("threshold") LocalDateTime threshold);
}
