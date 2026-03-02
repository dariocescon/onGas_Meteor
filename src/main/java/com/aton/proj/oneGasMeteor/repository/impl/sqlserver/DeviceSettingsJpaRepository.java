package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository per DeviceSettingsEntity (SQL Server)
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public interface DeviceSettingsJpaRepository extends JpaRepository<DeviceSettingsEntity, Long> {

    List<DeviceSettingsEntity> findByDeviceIdOrderByReceivedAtDesc(String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceSettingsEntity e WHERE e.receivedAt < :threshold")
    int deleteByReceivedAtBefore(@Param("threshold") LocalDateTime threshold);
}
