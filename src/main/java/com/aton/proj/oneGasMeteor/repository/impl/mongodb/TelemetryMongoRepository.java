//package com.aton.proj.oneGasMeteor.repository.impl.mongodb;
//
//import com.aton.proj.oneGasMeteor.entity.mongodb.TelemetryDocument;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
///**
// * MongoDB Repository per TelemetryDocument
// */
//@Repository
//@ConditionalOnProperty(name = "database.type", havingValue = "mongodb")
//public interface TelemetryMongoRepository extends MongoRepository<TelemetryDocument, String> {
//    
//    List<TelemetryDocument> findByDeviceIdOrderByReceivedAtDesc(String deviceId);
//    
//    List<TelemetryDocument> findByDeviceIdAndReceivedAtBetween(
//        String deviceId, LocalDateTime from, LocalDateTime to);
//    
//    List<TelemetryDocument> findByImeiOrderByReceivedAtDesc(String imei);
//    
//    List<TelemetryDocument> findByDeviceTypeOrderByReceivedAtDesc(String deviceType);
//    
//    long countByDeviceId(String deviceId);
//    
//    @Query(value = "{'receivedAt': {$lt: ?0}}", delete = true)
//    long deleteByReceivedAtBefore(LocalDateTime threshold);
//}