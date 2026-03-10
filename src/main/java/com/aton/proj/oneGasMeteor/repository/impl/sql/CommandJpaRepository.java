package com.aton.proj.oneGasMeteor.repository.impl.sql;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aton.proj.oneGasMeteor.config.condition.ConditionalOnSqlCommands;
import com.aton.proj.oneGasMeteor.entity.CommandEntity;

/**
 * JPA Repository per CommandEntity (SQL Server / PostgreSQL).
 * Attivo per tutti i database.type che usano SQL per i comandi
 * (sqlserver, timescaledb, influxdb).
 */
@Repository
@ConditionalOnSqlCommands
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