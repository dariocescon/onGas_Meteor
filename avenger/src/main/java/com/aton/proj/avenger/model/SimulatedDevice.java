package com.aton.proj.avenger.model;

/**
 * Represents a simulated IoT device with its identity and type information.
 */
public class SimulatedDevice {

    private final String imei;
    private final int productTypeCode;
    private final String deviceTypeName;

    public SimulatedDevice(String imei, int productTypeCode, String deviceTypeName) {
        this.imei = imei;
        this.productTypeCode = productTypeCode;
        this.deviceTypeName = deviceTypeName;
    }

    public String getImei() {
        return imei;
    }

    public int getProductTypeCode() {
        return productTypeCode;
    }

    public String getDeviceTypeName() {
        return deviceTypeName;
    }

    @Override
    public String toString() {
        return "SimulatedDevice{imei='" + imei + "', productType=" + productTypeCode + " (" + deviceTypeName + ")}";
    }
}
