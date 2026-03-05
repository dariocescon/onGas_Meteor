package com.aton.proj.avenger.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.aton.proj.avenger.config.AvengerProperties;
import com.aton.proj.avenger.model.SimulatedDevice;

/**
 * Registers and manages 1000+ simulated IoT devices with unique IMEIs and
 * mixed product type codes as supported by oneGasMeteor.
 *
 * <p>Supported product type codes and names:
 * <ul>
 *   <li>2  → TEK586   (RSSI signal, voltage battery)</li>
 *   <li>5  → TEK733   (RSSI signal, voltage battery)</li>
 *   <li>6  → TEK643   (RSSI signal, percentage battery)</li>
 *   <li>7  → TEK811   (CSQ signal, voltage battery)</li>
 *   <li>8  → TEK822V1 (CSQ signal, percentage battery)</li>
 *   <li>9  → TEK733A  (RSSI signal, voltage battery)</li>
 *   <li>10 → TEK871   (CSQ signal, percentage battery)</li>
 *   <li>11 → TEK811A  (CSQ signal, voltage battery)</li>
 *   <li>23 → TEK822V1BTN (CSQ signal, percentage battery)</li>
 *   <li>24 → TEK822V2   (CSQ signal, percentage battery)</li>
 *   <li>25 → TEK900     (CSQ signal, voltage battery)</li>
 *   <li>26 → TEK880     (CSQ signal, voltage battery)</li>
 *   <li>27 → TEK898V2   (CSQ signal, percentage battery)</li>
 *   <li>28 → TEK898V1   (CSQ signal, percentage battery)</li>
 * </ul>
 */
@Component
public class DeviceRegistry {

    /** IMEI base: 15-digit starting point (incremented per device). */
    private static final long IMEI_BASE = 867000000000001L;

    /** Supported product types: code, name (ordered for round-robin distribution). */
    private static final int[] PRODUCT_TYPE_CODES = {2, 5, 6, 7, 8, 9, 10, 11, 23, 24, 25, 26, 27, 28};
    private static final String[] DEVICE_TYPE_NAMES = {
        "TEK586", "TEK733", "TEK643", "TEK811", "TEK822V1", "TEK733A",
        "TEK871", "TEK811A", "TEK822V1BTN", "TEK822V2", "TEK900", "TEK880",
        "TEK898V2", "TEK898V1"
    };

    private final List<SimulatedDevice> devices;

    public DeviceRegistry(AvengerProperties properties) {
        this.devices = buildDeviceList(properties.getDevices().getTotal());
    }

    /**
     * Returns an unmodifiable view of all registered devices.
     */
    public List<SimulatedDevice> getAllDevices() {
        return Collections.unmodifiableList(devices);
    }

    /**
     * Returns the total number of registered devices.
     */
    public int size() {
        return devices.size();
    }

    private static List<SimulatedDevice> buildDeviceList(int count) {
        List<SimulatedDevice> list = new ArrayList<>(count);
        int typeCount = PRODUCT_TYPE_CODES.length;
        for (int i = 0; i < count; i++) {
            String imei = String.valueOf(IMEI_BASE + i);
            int typeIndex = i % typeCount;
            list.add(new SimulatedDevice(imei, PRODUCT_TYPE_CODES[typeIndex], DEVICE_TYPE_NAMES[typeIndex]));
        }
        return list;
    }
}
