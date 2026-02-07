package com.aton.proj.oneGasMeteor.repository.impl.sqlserver;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.entity.CommandEntity;

/**
 * JPA Repository per CommandEntity (SQL Server)
 */
@Repository
@ConditionalOnProperty(name = "database.type", havingValue = "sqlserver", matchIfMissing = true)
public interface CommandJpaRepository extends JpaRepository<CommandEntity, Long> {
    
    List<CommandEntity> findByDeviceIdAndStatusOrderByCreatedAtAsc(
        String deviceId, CommandEntity.CommandStatus status);
    
    List<CommandEntity> findByDeviceTypeAndStatusOrderByCreatedAtAsc(
        String deviceType, CommandEntity.CommandStatus status);
    
    @Modifying
    @Query("DELETE FROM CommandEntity c WHERE c.createdAt < :threshold " +
           "AND (c.status = 'DELIVERED' OR c.status = 'FAILED')")
    int deleteOldCompleted(@Param("threshold") LocalDateTime threshold);
}