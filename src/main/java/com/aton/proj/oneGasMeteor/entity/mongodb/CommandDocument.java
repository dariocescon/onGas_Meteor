//package com.aton.proj.oneGasMeteor.entity.mongodb;
//
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.CompoundIndex;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
///**
// * MongoDB Document per comandi device
// */
//@Document(collection = "device_commands")
//@CompoundIndex(name = "device_status_idx", def = "{'deviceId': 1, 'status': 1}")
//public class CommandDocument {
//    
//    @Id
//    private String id;
//    
//    @Indexed
//    private String deviceId;
//    
//    private String deviceType;
//    private String commandType;
//    
//    // Salviamo direttamente come Map invece di JSON string
//    private Map<String, Object> commandParams;
//    
//    @Indexed
//    private String status = "PENDING"; // PENDING, SENT, DELIVERED, FAILED, EXPIRED
//    
//    @Indexed
//    private LocalDateTime createdAt;
//    
//    private LocalDateTime sentAt;
//    private LocalDateTime deliveredAt;
//    private String errorMessage;
//    private Integer retryCount = 0;
//    private Integer maxRetries = 3;
//    
//    // Constructors
//    public CommandDocument() {
//        this.createdAt = LocalDateTime.now();
//    }
//    
//    public CommandDocument(String deviceId, String deviceType, String commandType) {
//        this();
//        this.deviceId = deviceId;
//        this.deviceType = deviceType;
//        this.commandType = commandType;
//    }
//    
//    // Getters & Setters
//    public String getId() {
//        return id;
//    }
//    
//    public void setId(String id) {
//        this.id = id;
//    }
//    
//    public String getDeviceId() {
//        return deviceId;
//    }
//    
//    public void setDeviceId(String deviceId) {
//        this.deviceId = deviceId;
//    }
//    
//    public String getDeviceType() {
//        return deviceType;
//    }
//    
//    public void setDeviceType(String deviceType) {
//        this.deviceType = deviceType;
//    }
//    
//    public String getCommandType() {
//        return commandType;
//    }
//    
//    public void setCommandType(String commandType) {
//        this.commandType = commandType;
//    }
//    
//    public Map<String, Object> getCommandParams() {
//        return commandParams;
//    }
//    
//    public void setCommandParams(Map<String, Object> commandParams) {
//        this.commandParams = commandParams;
//    }
//    
//    public String getStatus() {
//        return status;
//    }
//    
//    public void setStatus(String status) {
//        this.status = status;
//    }
//    
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//    
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//    
//    public LocalDateTime getSentAt() {
//        return sentAt;
//    }
//    
//    public void setSentAt(LocalDateTime sentAt) {
//        this.sentAt = sentAt;
//    }
//    
//    public LocalDateTime getDeliveredAt() {
//        return deliveredAt;
//    }
//    
//    public void setDeliveredAt(LocalDateTime deliveredAt) {
//        this.deliveredAt = deliveredAt;
//    }
//    
//    public String getErrorMessage() {
//        return errorMessage;
//    }
//    
//    public void setErrorMessage(String errorMessage) {
//        this.errorMessage = errorMessage;
//    }
//    
//    public Integer getRetryCount() {
//        return retryCount;
//    }
//    
//    public void setRetryCount(Integer retryCount) {
//        this.retryCount = retryCount;
//    }
//    
//    public Integer getMaxRetries() {
//        return maxRetries;
//    }
//    
//    public void setMaxRetries(Integer maxRetries) {
//        this.maxRetries = maxRetries;
//    }
//    
//    @Override
//    public String toString() {
//        return "CommandDocument{" +
//                "id='" + id + '\'' +
//                ", deviceId='" + deviceId + '\'' +
//                ", commandType='" + commandType + '\'' +
//                ", status='" + status + '\'' +
//                ", createdAt=" + createdAt +
//                '}';
//    }
//}