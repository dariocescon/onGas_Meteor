package com.aton.proj.oneGasMeteor.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Rappresenta un comando da inviare al device
 */
public class DeviceCommand {
    
    private Long id;
    private String deviceId;
    private String deviceType;
    private String commandType;
    private Map<String, Object> parameters = new HashMap<>();
    
    public DeviceCommand() {
    }
    
    public DeviceCommand(String deviceId, String deviceType, String commandType) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.commandType = commandType;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public String getCommandType() {
        return commandType;
    }
    
    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
    
    @Override
    public String toString() {
        return "DeviceCommand{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", commandType='" + commandType + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}