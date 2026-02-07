//package com.aton.proj.oneGasMeteor.repository.impl.mongodb;
//
//import com.aton.proj.oneGasMeteor.entity.TelemetryEntity;
//import com.aton.proj.oneGasMeteor.entity.mongodb.TelemetryDocument;
//import com.aton.proj.oneGasMeteor.model.DecodedMessage;
//import com.aton.proj.oneGasMeteor.repository.TelemetryRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * Implementazione MongoDB per TelemetryRepository
// */
//@Repository
//@ConditionalOnProperty(name = "database.type", havingValue = "mongodb")
//public class MongoTelemetryRepository implements TelemetryRepository {
//    
//    private static final Logger log = LoggerFactory.getLogger(MongoTelemetryRepository.class);
//    
//    private final TelemetryMongoRepository mongoRepository;
//    private final ObjectMapper objectMapper;
//    
//    public MongoTelemetryRepository(TelemetryMongoRepository mongoRepository, ObjectMapper objectMapper) {
//        this.mongoRepository = mongoRepository;
//        this.objectMapper = objectMapper;
//        log.info("✅ MongoTelemetryRepository initialized");
//    }
//    
//    @Override
//    public TelemetryEntity save(String deviceId, String deviceType, String rawMessage, DecodedMessage decoded) {
//        try {
//            TelemetryDocument doc = new TelemetryDocument();
//            doc.setDeviceId(deviceId);
//            doc.setDeviceType(deviceType);
//            doc.setRawMessage(rawMessage);
//            doc.setReceivedAt(LocalDateTime.now());
//            doc.setProcessedAt(LocalDateTime.now());
//            
//            // Converti DecodedMessage in Map per salvare come documento nativo MongoDB
//            @SuppressWarnings("unchecked")
//            Map<String, Object> decodedMap = objectMapper.convertValue(decoded, Map.class);
//            doc.setDecodedData(decodedMap);
//            
//            // Estrai campi principali
//            extractMainFields(doc, decoded);
//            
//            TelemetryDocument saved = mongoRepository.save(doc);
//            log.debug("💾 Saved telemetry to MongoDB: id={}, deviceId={}", saved.getId(), deviceId);
//            
//            // Converti in TelemetryEntity per mantenere compatibilità con l'interface
//            return toTelemetryEntity(saved);
//            
//        } catch (Exception e) {
//            log.error("❌ Failed to save telemetry to MongoDB for device: {}", deviceId, e);
//            throw new RuntimeException("Failed to save telemetry", e);
//        }
//    }
//    
//    private void extractMainFields(TelemetryDocument doc, DecodedMessage decoded) {
//        if (decoded.getUniqueIdentifier() != null) {
//            doc.setImei(decoded.getUniqueIdentifier().getImei());
//        }
//        
//        if (decoded.getUnitInfo() != null) {
//            doc.setFirmwareVersion(decoded.getUnitInfo().getFirmwareRevision());
//        }
//        
//        if (decoded.getBatteryStatus() != null) {
//            String voltageStr = decoded.getBatteryStatus().getBatteryVoltage();
//            if (voltageStr != null) {
//                try {
//                    doc.setBatteryVoltage(Double.parseDouble(voltageStr));
//                } catch (NumberFormatException ignored) {}
//            }
//            
//            String percentageStr = decoded.getBatteryStatus().getBatteryRemainingPercentage();
//            if (percentageStr != null) {
//                try {
//                    doc.setBatteryPercentage(Double.parseDouble(percentageStr));
//                } catch (NumberFormatException ignored) {}
//            }
//        }
//        
//        if (decoded.getSignalStrength() != null) {
//            Integer csq = decoded.getSignalStrength().getCsq();
//            Integer rssi = decoded.getSignalStrength().getRssi();
//            doc.setSignalStrength(csq != null ? csq : rssi);
//        }
//        
//        doc.setMessageType(decoded.getMessageType());
//        
//        if (decoded.getMeasurementData() != null) {
//            doc.setMeasurementCount(decoded.getMeasurementData().size());
//        }
//    }
//    
//    private TelemetryEntity toTelemetryEntity(TelemetryDocument doc) {
//        TelemetryEntity entity = new TelemetryEntity();
//        // MongoDB usa String ID, SQL Server usa Long
//        // Per compatibilità, convertiamo
//        try {
//            entity.setId(Long.parseLong(doc.getId()));
//        } catch (NumberFormatException e) {
//            // Se l'ID MongoDB non è numerico, usa hashCode
//            entity.setId((long) doc.getId().hashCode());
//        }
//        entity.setDeviceId(doc.getDeviceId());
//        entity.setDeviceType(doc.getDeviceType());
//        entity.setRawMessage(doc.getRawMessage());
//        entity.setReceivedAt(doc.getReceivedAt());
//        entity.setProcessedAt(doc.getProcessedAt());
//        entity.setImei(doc.getImei());
//        entity.setFirmwareVersion(doc.getFirmwareVersion());
//        entity.setBatteryVoltage(doc.getBatteryVoltage());
//        entity.setBatteryPercentage(doc.getBatteryPercentage());
//        entity.setSignalStrength(doc.getSignalStrength());
//        entity.setMessageType(doc.getMessageType());
//        entity.setMeasurementCount(doc.getMeasurementCount());
//        
//        return entity;
//    }
//    
//    @Override
//    public Optional<TelemetryEntity> findById(Long id) {
//        // MongoDB usa String ID
//        return mongoRepository.findById(id.toString())
//            .map(this::toTelemetryEntity);
//    }
//    
//    @Override
//    public List<TelemetryEntity> findByDeviceId(String deviceId) {
//        return mongoRepository.findByDeviceIdOrderByReceivedAtDesc(deviceId).stream()
//            .map(this::toTelemetryEntity)
//            .toList();
//    }
//    
//    @Override
//    public List<TelemetryEntity> findByDeviceIdAndDateRange(String deviceId, LocalDateTime from, LocalDateTime to) {
//        return mongoRepository.findByDeviceIdAndReceivedAtBetween(deviceId, from, to).stream()
//            .map(this::toTelemetryEntity)
//            .toList();
//    }
//    
//    @Override
//    public List<TelemetryEntity> findByImei(String imei) {
//        return mongoRepository.findByImeiOrderByReceivedAtDesc(imei).stream()
//            .map(this::toTelemetryEntity)
//            .toList();
//    }
//    
//    @Override
//    public List<TelemetryEntity> findByDeviceType(String deviceType) {
//        return mongoRepository.findByDeviceTypeOrderByReceivedAtDesc(deviceType).stream()
//            .map(this::toTelemetryEntity)
//            .toList();
//    }
//    
//    @Override
//    public long countByDeviceId(String deviceId) {
//        return mongoRepository.countByDeviceId(deviceId);
//    }
//    
//    @Override
//    public void deleteOlderThan(LocalDateTime threshold) {
//        long deleted = mongoRepository.deleteByReceivedAtBefore(threshold);
//        log.info("🗑️  Deleted {} old telemetry documents before {}", deleted, threshold);
//    }
//}