//package com.aton.proj.oneGasMeteor.repository.impl.mongodb;
//
//import com.aton.proj.oneGasMeteor.entity.mongodb.CommandDocument;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
///**
// * MongoDB Repository per CommandDocument
// */
//@Repository
//@ConditionalOnProperty(name = "database.type", havingValue = "mongodb")
//public interface CommandMongoRepository extends MongoRepository<CommandDocument, String> {
//    
//    List<CommandDocument> findByDeviceIdAndStatusOrderByCreatedAtAsc(String deviceId, String status);
//    
//    List<CommandDocument> findByDeviceTypeAndStatusOrderByCreatedAtAsc(String deviceType, String status);
//    
//    @Query(value = "{'createdAt': {$lt: ?0}, 'status': {$in: ['DELIVERED', 'FAILED']}}", delete = true)
//    long deleteOldCompleted(LocalDateTime threshold);
//}