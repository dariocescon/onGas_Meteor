//package com.aton.proj.oneGasMeteor.entity.mongodb;
//
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
///**
// * MongoDB Document per dati di telemetria
// */
//@Document(collection = "telemetry_data")
//public class TelemetryDocument {
//    
//    @Id
//    private String id;
//    
//    @Indexed
//    private String deviceId;
//    
//    @Indexed
//    private String deviceType;
//    
//    private String rawMessage;
//    
//    // Salviamo direttamente come Map invece di JSON string
//    private Map<String, Object> decodedData;
//    
//    @Indexed
//    private LocalDateTime receivedAt;
//    
//    private LocalDateTime processedAt;
//    
//    // Campi estratti per query veloci
//    @Indexed
//    private String imei;
//    
//    private String firmwareVersion;
//    private Double batteryVoltage;
//    private Double batteryPercentage;
//    private Integer signalStrength;
//    private String messageType;
//    private Integer measurementCount;
//    
//    // Constructors
//    public TelemetryDocument() {
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
//    public String getRawMessage() {
//        return rawMessage;
//    }
//    
//    public void setRawMessage(String rawMessage) {
//        this.rawMessage = rawMessage;
//    }
//    
//    public Map<String, Object> getDecodedData() {
//        return decodedData;
//    }
//    
//    public void setDecodedData(Map<String, Object> decodedData) {
//        this.decodedData = decodedData;
//    }
//    
//    public LocalDateTime getReceivedAt() {
//        return receivedAt;
//    }
//    
//    public void setReceivedAt(LocalDateTime receivedAt) {
//        this.receivedAt = receivedAt;
//    }
//    
//    public LocalDateTime getProcessedAt() {
//        return processedAt;
//    }
//    
//    public void setProcessedAt(LocalDateTime processedAt) {
//        this.processedAt = processedAt;
//    }
//    
//    public String getImei() {
//        return imei;
//    }
//    
//    public void setImei(String imei) {
//        this.imei = imei;
//    }
//    
//    public String getFirmwareVersion() {
//        return firmwareVersion;
//    }
//    
//    public void setFirmwareVersion(String firmwareVersion) {
//        this.firmwareVersion = firmwareVersion;
//    }
//    
//    public Double getBatteryVoltage() {
//        return batteryVoltage;
//    }
//    
//    public void setBatteryVoltage(Double batteryVoltage) {
//        this.batteryVoltage = batteryVoltage;
//    }
//    
//    public Double getBatteryPercentage() {
//        return batteryPercentage;
//    }
//    
//    public void setBatteryPercentage(Double batteryPercentage) {
//        this.batteryPercentage = batteryPercentage;
//    }
//    
//    public Integer getSignalStrength() {
//        return signalStrength;
//    }
//    
//    public void setSignalStrength(Integer signalStrength) {
//        this.signalStrength = signalStrength;
//    }
//    
//    public String getMessageType() {
//        return messageType;
//    }
//    
//    public void setMessageType(String messageType) {
//        this.messageType = messageType;
//    }
//    
//    public Integer getMeasurementCount() {
//        return measurementCount;
//    }
//    
//    public void setMeasurementCount(Integer measurementCount) {
//        this.measurementCount = measurementCount;
//    }
//    
//    @Override
//    public String toString() {
//        return "TelemetryDocument{" +
//                "id='" + id + '\'' +
//                ", deviceId='" + deviceId + '\'' +
//                ", deviceType='" + deviceType + '\'' +
//                ", imei='" + imei + '\'' +
//                ", receivedAt=" + receivedAt +
//                '}';
//    }
//}