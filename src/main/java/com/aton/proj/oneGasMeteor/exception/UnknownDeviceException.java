package com.aton.proj.oneGasMeteor.exception;

public class UnknownDeviceException extends RuntimeException {
    
	private static final long serialVersionUID = -8830293483602792225L;
	
	private final String deviceType;
    
    public UnknownDeviceException(String deviceType) {
        super("Unknown device type: " + deviceType);
        this.deviceType = deviceType;
    }
    
    public UnknownDeviceException(String deviceType, String message) {
        super(message);
        this.deviceType = deviceType;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
}