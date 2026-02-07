//package com.aton.proj.oneGasMeteor.repository.impl.mongodb;
//
//import com.aton.proj.oneGasMeteor.entity.CommandEntity;
//import com.aton.proj.oneGasMeteor.entity.mongodb.CommandDocument;
//import com.aton.proj.oneGasMeteor.model.DeviceCommand;
//import com.aton.proj.oneGasMeteor.repository.CommandRepository;
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
// * Implementazione MongoDB per CommandRepository
// */
//@Repository
//@ConditionalOnProperty(name = "database.type", havingValue = "mongodb")
//public class MongoCommandRepository implements CommandRepository {
//    
//    private static final Logger log = LoggerFactory.getLogger(MongoCommandRepository.class);
//    
//    private final CommandMongoRepository mongoRepository;
//    private final ObjectMapper objectMapper;
//    
//    public MongoCommandRepository(CommandMongoRepository mongoRepository, ObjectMapper objectMapper) {
//        this.mongoRepository = mongoRepository;
//        this.objectMapper = objectMapper;
//        log.info("✅ MongoCommandRepository initialized");
//    }
//    
//    @Override
//    public CommandEntity save(DeviceCommand command) {
//        try {
//            CommandDocument doc = new CommandDocument(
//                command.getDeviceId(),
//                command.getDeviceType(),
//                command.getCommandType()
//            );
//            
//            // Salva parametri direttamente come Map
//            if (command.getParameters() != null && !command.getParameters().isEmpty()) {
//                doc.setCommandParams(command.getParameters());
//            }
//            
//            CommandDocument saved = mongoRepository.save(doc);
//            log.debug("💾 Saved command to MongoDB: id={}, type={}, deviceId={}", 
//                saved.getId(), saved.getCommandType(), saved.getDeviceId());
//            
//            return toCommandEntity(saved);
//            
//        } catch (Exception e) {
//            log.error("❌ Failed to save command to MongoDB for device: {}", command.getDeviceId(), e);
//            throw new RuntimeException("Failed to save command", e);
//        }
//    }
//    
//    private CommandEntity toCommandEntity(CommandDocument doc) {
//        CommandEntity entity = new CommandEntity(
//            doc.getDeviceId(),
//            doc.getDeviceType(),
//            doc.getCommandType()
//        );
//        
//        try {
//            entity.setId(Long.parseLong(doc.getId()));
//        } catch (NumberFormatException e) {
//            entity.setId((long) doc.getId().hashCode());
//        }
//        
//        entity.setStatus(CommandEntity.CommandStatus.valueOf(doc.getStatus()));
//        entity.setCreatedAt(doc.getCreatedAt());
//        entity.setSentAt(doc.getSentAt());
//        entity.setDeliveredAt(doc.getDeliveredAt());
//        entity.setErrorMessage(doc.getErrorMessage());
//        entity.setRetryCount(doc.getRetryCount());
//        entity.setMaxRetries(doc.getMaxRetries());
//        
//        return entity;
//    }
//    
//    @Override
//    public Optional<CommandEntity> findById(Long id) {
//        return mongoRepository.findById(id.toString())
//            .map(this::toCommandEntity);
//    }
//    
//    @Override
//    public List<CommandEntity> findPendingCommands(String deviceId) {
//        return mongoRepository.findByDeviceIdAndStatusOrderByCreatedAtAsc(deviceId, "PENDING").stream()
//            .map(this::toCommandEntity)
//            .toList();
//    }
//    
//    @Override
//    public List<CommandEntity> findPendingCommandsByDeviceType(String deviceType) {
//        return mongoRepository.findByDeviceTypeAndStatusOrderByCreatedAtAsc(deviceType, "PENDING").stream()
//            .map(this::toCommandEntity)
//            .toList();
//    }
//    
//    @Override
//    public void updateStatus(Long commandId, CommandEntity.CommandStatus status) {
//        mongoRepository.findById(commandId.toString()).ifPresent(doc -> {
//            doc.setStatus(status.name());
//            mongoRepository.save(doc);
//            log.debug("🔄 Updated command {} status to {}", commandId, status);
//        });
//    }
//    
//    @Override
//    public void markAsSent(Long commandId) {
//        mongoRepository.findById(commandId.toString()).ifPresent(doc -> {
//            doc.setStatus("SENT");
//            doc.setSentAt(LocalDateTime.now());
//            mongoRepository.save(doc);
//            log.debug("📤 Marked command {} as SENT", commandId);
//        });
//    }
//    
//    @Override
//    public void markAsDelivered(Long commandId) {
//        mongoRepository.findById(commandId.toString()).ifPresent(doc -> {
//            doc.setStatus("DELIVERED");
//            doc.setDeliveredAt(LocalDateTime.now());
//            mongoRepository.save(doc);
//            log.debug("✅ Marked command {} as DELIVERED", commandId);
//        });
//    }
//    
//    @Override
//    public void markAsFailed(Long commandId, String errorMessage) {
//        mongoRepository.findById(commandId.toString()).ifPresent(doc -> {
//            doc.setStatus("FAILED");
//            doc.setErrorMessage(errorMessage);
//            mongoRepository.save(doc);
//            log.warn("❌ Marked command {} as FAILED: {}", commandId, errorMessage);
//        });
//    }
//    
//    @Override
//    public void incrementRetryCount(Long commandId) {
//        mongoRepository.findById(commandId.toString()).ifPresent(doc -> {
//            doc.setRetryCount(doc.getRetryCount() + 1);
//            mongoRepository.save(doc);
//            log.debug("🔄 Incremented retry count for command {}: {}/{}", 
//                commandId, doc.getRetryCount(), doc.getMaxRetries());
//        });
//    }
//    
//    @Override
//    public void deleteOldCompletedCommands(int daysOld) {
//        LocalDateTime threshold = LocalDateTime.now().minusDays(daysOld);
//        long deleted = mongoRepository.deleteOldCompleted(threshold);
//        log.info("🗑️  Deleted {} old completed commands before {}", deleted, threshold);
//    }
//}