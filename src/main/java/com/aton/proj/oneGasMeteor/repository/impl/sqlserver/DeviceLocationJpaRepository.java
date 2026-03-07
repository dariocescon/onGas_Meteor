package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.config.ConditionalOnJpaDatabase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository per DeviceLocationEntity (SQL Server)
 */
@Repository
@ConditionalOnJpaDatabase
public interface DeviceLocationJpaRepository extends JpaRepository<DeviceLocationEntity, Long> {

    List<DeviceLocationEntity> findByDeviceIdOrderByReceivedAtDesc(String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceLocationEntity e WHERE e.receivedAt < :threshold")
    int deleteByReceivedAtBefore(@Param("threshold") LocalDateTime threshold);
}
