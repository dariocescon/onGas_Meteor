package com.aton.proj.oneGasMeteor.service;

import com.aton.proj.oneGasMeteor.entity.DeviceLocationEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceSettingsEntity;
import com.aton.proj.oneGasMeteor.entity.DeviceStatisticsEntity;
import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;

/**
 * Interfaccia per il batch write di entity time-series.
 * Implementata da BatchInsertService (SQL/JPA) e InfluxDBBatchInsertService (InfluxDB).
 */
public interface BatchWriteService {

    void enqueue(TelemetryEntity entity);

    void enqueue(DeviceSettingsEntity entity);

    void enqueue(DeviceStatisticsEntity entity);

    void enqueue(DeviceLocationEntity entity);
}
