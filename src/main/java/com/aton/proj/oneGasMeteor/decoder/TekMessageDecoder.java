package com.aton.proj.oneGasMeteor.decoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.aton.proj.oneGasMeteor.model.DecodedMessage;
import com.aton.proj.oneGasMeteor.model.TelemetryMessage;


/**
 * Decoder per messaggi TEK
 */
public class TekMessageDecoder {

    public DecodedMessage decode(TelemetryMessage msg) {
        DecodedMessage decode = new DecodedMessage();
        byte[] payload = msg.getPayload();
        
        // DECODING MESSAGE HEADER
        decodeProductType(payload, decode);
        decodeVersions(payload, decode);
        decodeImei(payload, decode);
        decodeMessageType(payload, decode);
        decodeContactReason(payload, decode);
        decodeAlarmStatus(payload, decode);
        decodeLastReset(payload, decode);
        decodeSignalStrength(payload, decode);
        decodeDiagnosticInfo(payload, decode);
        decodeBatteryStatus(payload, decode);
        
        int msgType = payload[15] & 0x3F;
        
        // DECODING MESSAGE 4/8/9
        if (msgType == 4 || msgType == 8 || msgType == 9) {
            decodeDiagnosticData(payload, decode);
            decodeMeasurementData(msg, payload, decode, msgType);
        }
        
        return decode;
    }

    private void decodeProductType(byte[] payload, DecodedMessage decode) {
        String productType = switch (payload[0] & 0xFF) {
            case 2 -> "TEK586";
            case 5 -> "TEK733";
            case 6 -> "TEK643";
            case 7 -> "TEK811";
            case 8 -> "TEK822V1";
            case 9 -> "TEK733A";
            case 10 -> "TEK871";
            case 11 -> "TEK811A";
            case 23 -> "TEK822V1BTN";
            case 24 -> "TEK822V2";
            case 25 -> "TEK900";
            case 26 -> "TEK880";
            case 27 -> "TEK898V2";
            case 28 -> "TEK898V1";
            default -> null;
        };
        decode.getUnitInfo().setProductType(productType);
    }

    private void decodeVersions(byte[] payload, DecodedMessage decode) {
        String hwRevision = (payload[1] & 0x07) + "." + ((payload[1] >> 3) & 0x1F);
        String fwRevision = (payload[2] & 0x1F) + "." + ((payload[2] >> 5) & 0x07);
        
        decode.getUnitInfo().setHardwareRevision(hwRevision);
        decode.getUnitInfo().setFirmwareRevision(fwRevision);
    }

    private void decodeImei(byte[] payload, DecodedMessage decode) {
        StringBuilder imei = new StringBuilder();
        for (int i = 7; i < 15; i++) {
            imei.append((payload[i] >> 4) & 0x0F);
            imei.append(payload[i] & 0x0F);
        }
        // Remove the leading zero
        decode.getUniqueIdentifier().setImei(imei.substring(1));
    }

    private void decodeMessageType(byte[] payload, DecodedMessage decode) {
        int msgType = payload[15] & 0x3F;
        decode.setMessageType("Message Type " + msgType);
    }

    private void decodeContactReason(byte[] payload, DecodedMessage decode) {
        var contactReason = decode.getContactReason();
        
        contactReason.setTspRequested((payload[3] & 0x20) != 0);
        contactReason.setReboot((payload[3] & 0x10) != 0);
        contactReason.setManual((payload[3] & 0x08) != 0);
        contactReason.setServerRequest((payload[3] & 0x04) != 0);
        contactReason.setAlarm((payload[3] & 0x02) != 0);
        contactReason.setScheduled((payload[3] & 0x01) != 0);
        contactReason.setActivation((payload[4] & 0x80) != 0);
        
        decode.getDiagnosticInfo().setActivation(contactReason.getActivation());
    }

    private void decodeAlarmStatus(byte[] payload, DecodedMessage decode) {
        var alarmStatus = decode.getAlarmStatus();
        
        alarmStatus.setDynamic2((payload[3] & 0x80) != 0);
        alarmStatus.setDynamic1((payload[3] & 0x40) != 0);
        alarmStatus.setBund((payload[4] & 0x08) != 0);
        alarmStatus.setStatic3((payload[4] & 0x04) != 0);
        alarmStatus.setStatic2((payload[4] & 0x02) != 0);
        alarmStatus.setStatic1((payload[4] & 0x01) != 0);
    }

    private void decodeLastReset(byte[] payload, DecodedMessage decode) {
        decode.getLastReset().setBrownout((payload[4] & 0x40) != 0);
        decode.getLastReset().setWatchdog((payload[4] & 0x20) != 0);
    }

    private void decodeSignalStrength(byte[] payload, DecodedMessage decode) {
        int productType = payload[0] & 0xFF;
        var signalStrength = decode.getSignalStrength();
        
        switch (productType) {
            case 2, 5, 6, 9 -> // TEK586, TEK733, TEK643, TEK733A
                signalStrength.setRssi(payload[5] & 0xFF);
            case 7, 8, 10, 11, 23, 24, 25, 26, 27, 28 -> // TEK811, TEK822V1, etc.
                signalStrength.setCsq(payload[5] & 0xFF);
        }
    }

    private void decodeDiagnosticInfo(byte[] payload, DecodedMessage decode) {
        decode.getDiagnosticInfo().setRtcSet((payload[6] & 0x20) != 0);
    }

    private void decodeBatteryStatus(byte[] payload, DecodedMessage decode) {
    	
        int productType = payload[0] & 0xFF;
        var batteryStatus = decode.getBatteryStatus();
        
        switch (productType) {
            case 6, 8, 10, 23, 24, 27, 28 -> { // Battery percentage
                double percentage = ((payload[6] & 0x1F) * 100.0) / 31.0;
                batteryStatus.setBatteryRemainingPercentage(String.format("%.1f", percentage));
            }
            case 2, 5, 9, 7, 11, 25, 26 -> { // Battery voltage
                double voltage = ((payload[6] & 0x1F) + 30.0) / 10.0;
                batteryStatus.setBatteryVoltage(String.format("%.1f", voltage));
            }
        }
    }

    private void decodeDiagnosticData(byte[] payload, DecodedMessage decode) {
        int productType = payload[0] & 0xFF;
        var diagnosticInfo = decode.getDiagnosticInfo();
        
        switch (productType) {
            case 7, 10, 11, 25 -> // TEK811, TEK871, TEK811A, TEK900
                diagnosticInfo.setEnergyUsedLastContactMaSeconds(
                    ((payload[20] & 0xFF) << 8) | (payload[21] & 0xFF));
            
            case 8, 23, 24, 27, 28 -> { // TEK822V1, TEK822V1BTN, TEK822V2, TEK898V2, TEK898V1
                double fwVersion = Double.parseDouble(decode.getUnitInfo().getFirmwareRevision());
                if (fwVersion > 3.0) {
                    diagnosticInfo.setEnergyUsedLastContactMaSeconds(
                        ((payload[20] & 0xFF) << 8) | (payload[21] & 0xFF));
                } else {
                    diagnosticInfo.setLastErrorCode(
                        ((payload[20] & 0xFF) << 8) | (payload[21] & 0xFF));
                }
            }
            
            case 2, 5, 9, 26 -> // TEK586, TEK733, TEK733A, TEK880
                diagnosticInfo.setLastErrorCode(
                    ((payload[20] & 0xFF) << 8) | (payload[21] & 0xFF));
            
            default ->
                diagnosticInfo.setEnergyUsedLastContactMaSeconds(
                    ((payload[20] & 0xFF) << 8) | (payload[21] & 0xFF));
        }
        
        diagnosticInfo.setMessageCount(((payload[17] & 0xFF) << 8) | (payload[18] & 0xFF));
        diagnosticInfo.setTryAttemptsRemaining((payload[19] & 0xE0) >> 5);
    }

    private void decodeMeasurementData(TelemetryMessage msg, byte[] payload, 
                                       DecodedMessage decode, int msgType) {
        long loggerSpeedMs = calculateLoggerSpeed(msgType, payload, decode);
        decode.getUnitSetup().setLoggerSpeedMinutes((int) (loggerSpeedMs / 60000));
        
        long serverTimeInMs = msg.getServerTimeInMs();
        
        if (loggerSpeedMs > (1 * 60 * 1000)) {
            // Logs on quarters of an hour
            serverTimeInMs = (serverTimeInMs / (15 * 60 * 1000)) * (15 * 60 * 1000);
        } else {
            // Logs every minute
            serverTimeInMs = (serverTimeInMs / (1 * 60 * 1000)) * (1 * 60 * 1000);
        }
        
        List<DecodedMessage.MeasurementData> measurements = new ArrayList<>();
        
        for (int i = 0; i < 28; i++) {
            int j = (i * 4) + 26;
            
            if (j + 3 >= payload.length) {
                break;
            }
            
            int filter = (payload[j] & 0xFF) + (payload[j + 1] & 0xFF) + 
                        (payload[j + 2] & 0xFF) + (payload[j + 3] & 0xFF);
            
            if (filter == 0) {
                continue; // Ignore void readings
            }
            
            double temperatureC = ((payload[j + 1] >> 1) & 0x7F) - 30.0;
            
            var measurement = new DecodedMessage.MeasurementData();
            measurement.setMeasurementNum("Data " + i);
            
            int distance = ((payload[j + 2] & 0x03) << 8) | (payload[j + 3] & 0xFF);
            measurement.setMeasuredValue(distance);
            measurement.setDistanceCm(distance);
            measurement.setPercentageFull(null);
            measurement.setPayloadValue(null);
            measurement.setSonicSrc((payload[j + 2] >> 2) & 0x0F);
            measurement.setSonicRssi(payload[j] & 0x0F);
            measurement.setTemperatureC(temperatureC);
            measurement.setTemperatureF(String.format("%.2f", (temperatureC * 9 / 5) + 32));
            measurement.setTemperatureCode(null);
            measurement.setAuxdata1(null);
            measurement.setAuxdata2(null);
            
            long timestamp = serverTimeInMs - (loggerSpeedMs * i);
            measurement.setTimestamp(Instant.ofEpochMilli(timestamp).toString());
            
            measurements.add(measurement);
        }
        
        decode.setMeasurementData(measurements);
    }

    private long calculateLoggerSpeed(int msgType, byte[] payload, DecodedMessage decode) {
        long loggerSpeedMs;
        
        if (msgType == 8) {
            if (Boolean.TRUE.equals(decode.getContactReason().getManual())) {
                // NOTE: in this case the logger runs at 1sec
                loggerSpeedMs = 1 * 1000;
            } else if ((payload[23] >> 7) == 0) {
                loggerSpeedMs = 1 * 60 * 1000;
            } else {
                loggerSpeedMs = 15 * 60 * 1000;
            }
        } else if (msgType == 9) {
            if ((payload[23] >> 7) == 0) {
                loggerSpeedMs = 1 * 60 * 1000;
            } else {
                loggerSpeedMs = 15 * 60 * 1000;
            }
        } else if (msgType == 4) {
            loggerSpeedMs = payload[23] & 0x7F;
            if (loggerSpeedMs == 0) {
                if ((payload[23] >> 7) == 0) {
                    loggerSpeedMs = 1 * 60 * 1000;
                } else {
                    loggerSpeedMs = 15 * 60 * 1000;
                }
            } else {
                loggerSpeedMs = loggerSpeedMs * 15 * 60 * 1000;
            }
        } else {
            loggerSpeedMs = 15 * 60 * 1000; // Default
        }
        
        return loggerSpeedMs;
    }
}