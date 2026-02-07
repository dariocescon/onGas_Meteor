package com.aton.proj.oneGasMeteor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Struttura dati decodificata
 */
public class DecodedMessage {
	private UnitInfo unitInfo = new UnitInfo();
	private UniqueIdentifier uniqueIdentifier = new UniqueIdentifier();
	private String messageType;
	private ContactReason contactReason = new ContactReason();
	private AlarmStatus alarmStatus = new AlarmStatus();
	private LastReset lastReset = new LastReset();
	private SignalStrength signalStrength = new SignalStrength();
	private DiagnosticInfo diagnosticInfo = new DiagnosticInfo();
	private BatteryStatus batteryStatus = new BatteryStatus();
	private UnitSetup unitSetup = new UnitSetup();
	private List<MeasurementData> measurementData = new ArrayList<>();

	// Getters e Setters
	public UnitInfo getUnitInfo() {
		return unitInfo;
	}

	public void setUnitInfo(UnitInfo unitInfo) {
		this.unitInfo = unitInfo;
	}

	public UniqueIdentifier getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public void setUniqueIdentifier(UniqueIdentifier uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public ContactReason getContactReason() {
		return contactReason;
	}

	public void setContactReason(ContactReason contactReason) {
		this.contactReason = contactReason;
	}

	public AlarmStatus getAlarmStatus() {
		return alarmStatus;
	}

	public void setAlarmStatus(AlarmStatus alarmStatus) {
		this.alarmStatus = alarmStatus;
	}

	public LastReset getLastReset() {
		return lastReset;
	}

	public void setLastReset(LastReset lastReset) {
		this.lastReset = lastReset;
	}

	public SignalStrength getSignalStrength() {
		return signalStrength;
	}

	public void setSignalStrength(SignalStrength signalStrength) {
		this.signalStrength = signalStrength;
	}

	public DiagnosticInfo getDiagnosticInfo() {
		return diagnosticInfo;
	}

	public void setDiagnosticInfo(DiagnosticInfo diagnosticInfo) {
		this.diagnosticInfo = diagnosticInfo;
	}

	public BatteryStatus getBatteryStatus() {
		return batteryStatus;
	}

	public void setBatteryStatus(BatteryStatus batteryStatus) {
		this.batteryStatus = batteryStatus;
	}

	public UnitSetup getUnitSetup() {
		return unitSetup;
	}

	public void setUnitSetup(UnitSetup unitSetup) {
		this.unitSetup = unitSetup;
	}

	public List<MeasurementData> getMeasurementData() {
		return measurementData;
	}

	public void setMeasurementData(List<MeasurementData> measurementData) {
		this.measurementData = measurementData;
	}

	// Classi interne
	public static class UnitInfo {
		private String productType;
		private String hardwareRevision;
		private String firmwareRevision;

		public String getProductType() {
			return productType;
		}

		public void setProductType(String productType) {
			this.productType = productType;
		}

		public String getHardwareRevision() {
			return hardwareRevision;
		}

		public void setHardwareRevision(String hardwareRevision) {
			this.hardwareRevision = hardwareRevision;
		}

		public String getFirmwareRevision() {
			return firmwareRevision;
		}

		public void setFirmwareRevision(String firmwareRevision) {
			this.firmwareRevision = firmwareRevision;
		}
	}

	public static class UniqueIdentifier {
		private String imei;

		public String getImei() {
			return imei;
		}

		public void setImei(String imei) {
			this.imei = imei;
		}
	}

	public static class ContactReason {
		private Boolean tspRequested;
		private Boolean reboot;
		private Boolean manual;
		private Boolean serverRequest;
		private Boolean alarm;
		private Boolean scheduled;
		private Boolean activation;

		public Boolean getTspRequested() {
			return tspRequested;
		}

		public void setTspRequested(Boolean tspRequested) {
			this.tspRequested = tspRequested;
		}

		public Boolean getReboot() {
			return reboot;
		}

		public void setReboot(Boolean reboot) {
			this.reboot = reboot;
		}

		public Boolean getManual() {
			return manual;
		}

		public void setManual(Boolean manual) {
			this.manual = manual;
		}

		public Boolean getServerRequest() {
			return serverRequest;
		}

		public void setServerRequest(Boolean serverRequest) {
			this.serverRequest = serverRequest;
		}

		public Boolean getAlarm() {
			return alarm;
		}

		public void setAlarm(Boolean alarm) {
			this.alarm = alarm;
		}

		public Boolean getScheduled() {
			return scheduled;
		}

		public void setScheduled(Boolean scheduled) {
			this.scheduled = scheduled;
		}

		public Boolean getActivation() {
			return activation;
		}

		public void setActivation(Boolean activation) {
			this.activation = activation;
		}
	}

	public static class AlarmStatus {
		private Boolean dynamic2;
		private Boolean dynamic1;
		private Boolean bund;
		private Boolean static3;
		private Boolean static2;
		private Boolean static1;

		public Boolean getDynamic2() {
			return dynamic2;
		}

		public void setDynamic2(Boolean dynamic2) {
			this.dynamic2 = dynamic2;
		}

		public Boolean getDynamic1() {
			return dynamic1;
		}

		public void setDynamic1(Boolean dynamic1) {
			this.dynamic1 = dynamic1;
		}

		public Boolean getBund() {
			return bund;
		}

		public void setBund(Boolean bund) {
			this.bund = bund;
		}

		public Boolean getStatic3() {
			return static3;
		}

		public void setStatic3(Boolean static3) {
			this.static3 = static3;
		}

		public Boolean getStatic2() {
			return static2;
		}

		public void setStatic2(Boolean static2) {
			this.static2 = static2;
		}

		public Boolean getStatic1() {
			return static1;
		}

		public void setStatic1(Boolean static1) {
			this.static1 = static1;
		}
	}

	public static class LastReset {
		private Boolean brownout;
		private Boolean watchdog;

		public Boolean getBrownout() {
			return brownout;
		}

		public void setBrownout(Boolean brownout) {
			this.brownout = brownout;
		}

		public Boolean getWatchdog() {
			return watchdog;
		}

		public void setWatchdog(Boolean watchdog) {
			this.watchdog = watchdog;
		}
	}

	public static class SignalStrength {
		private Integer rssi;
		private Integer csq;

		public Integer getRssi() {
			return rssi;
		}

		public void setRssi(Integer rssi) {
			this.rssi = rssi;
		}

		public Integer getCsq() {
			return csq;
		}

		public void setCsq(Integer csq) {
			this.csq = csq;
		}
	}

	public static class DiagnosticInfo {
		private Boolean rtcSet;
		private Boolean activation;
		private Integer energyUsedLastContactMaSeconds;
		private Integer lastErrorCode;
		private Integer messageCount;
		private Integer tryAttemptsRemaining;

		public Boolean getRtcSet() {
			return rtcSet;
		}

		public void setRtcSet(Boolean rtcSet) {
			this.rtcSet = rtcSet;
		}

		public Boolean getActivation() {
			return activation;
		}

		public void setActivation(Boolean activation) {
			this.activation = activation;
		}

		public Integer getEnergyUsedLastContactMaSeconds() {
			return energyUsedLastContactMaSeconds;
		}

		public void setEnergyUsedLastContactMaSeconds(Integer energyUsedLastContactMaSeconds) {
			this.energyUsedLastContactMaSeconds = energyUsedLastContactMaSeconds;
		}

		public Integer getLastErrorCode() {
			return lastErrorCode;
		}

		public void setLastErrorCode(Integer lastErrorCode) {
			this.lastErrorCode = lastErrorCode;
		}

		public Integer getMessageCount() {
			return messageCount;
		}

		public void setMessageCount(Integer messageCount) {
			this.messageCount = messageCount;
		}

		public Integer getTryAttemptsRemaining() {
			return tryAttemptsRemaining;
		}

		public void setTryAttemptsRemaining(Integer tryAttemptsRemaining) {
			this.tryAttemptsRemaining = tryAttemptsRemaining;
		}
	}

	public static class BatteryStatus {
		private String batteryVoltage;
		private String batteryRemainingPercentage;

		public String getBatteryVoltage() {
			return batteryVoltage;
		}

		public void setBatteryVoltage(String batteryVoltage) {
			this.batteryVoltage = batteryVoltage;
		}

		public String getBatteryRemainingPercentage() {
			return batteryRemainingPercentage;
		}

		public void setBatteryRemainingPercentage(String batteryRemainingPercentage) {
			this.batteryRemainingPercentage = batteryRemainingPercentage;
		}
	}

	public static class UnitSetup {
		private Integer loggerSpeedMinutes;

		public Integer getLoggerSpeedMinutes() {
			return loggerSpeedMinutes;
		}

		public void setLoggerSpeedMinutes(Integer loggerSpeedMinutes) {
			this.loggerSpeedMinutes = loggerSpeedMinutes;
		}
	}

	public static class MeasurementData {
		private String measurementNum;
		private Integer measuredValue;
		private Integer distanceCm;
		private String percentageFull;
		private String payloadValue;
		private Integer sonicSrc;
		private Integer sonicRssi;
		private Double temperatureC;
		private String temperatureF;
		private String temperatureCode;
		private String auxdata1;
		private String auxdata2;
		private String timestamp;

		public String getMeasurementNum() {
			return measurementNum;
		}

		public void setMeasurementNum(String measurementNum) {
			this.measurementNum = measurementNum;
		}

		public Integer getMeasuredValue() {
			return measuredValue;
		}

		public void setMeasuredValue(Integer measuredValue) {
			this.measuredValue = measuredValue;
		}

		public Integer getDistanceCm() {
			return distanceCm;
		}

		public void setDistanceCm(Integer distanceCm) {
			this.distanceCm = distanceCm;
		}

		public String getPercentageFull() {
			return percentageFull;
		}

		public void setPercentageFull(String percentageFull) {
			this.percentageFull = percentageFull;
		}

		public String getPayloadValue() {
			return payloadValue;
		}

		public void setPayloadValue(String payloadValue) {
			this.payloadValue = payloadValue;
		}

		public Integer getSonicSrc() {
			return sonicSrc;
		}

		public void setSonicSrc(Integer sonicSrc) {
			this.sonicSrc = sonicSrc;
		}

		public Integer getSonicRssi() {
			return sonicRssi;
		}

		public void setSonicRssi(Integer sonicRssi) {
			this.sonicRssi = sonicRssi;
		}

		public Double getTemperatureC() {
			return temperatureC;
		}

		public void setTemperatureC(Double temperatureC) {
			this.temperatureC = temperatureC;
		}

		public String getTemperatureF() {
			return temperatureF;
		}

		public void setTemperatureF(String temperatureF) {
			this.temperatureF = temperatureF;
		}

		public String getTemperatureCode() {
			return temperatureCode;
		}

		public void setTemperatureCode(String temperatureCode) {
			this.temperatureCode = temperatureCode;
		}

		public String getAuxdata1() {
			return auxdata1;
		}

		public void setAuxdata1(String auxdata1) {
			this.auxdata1 = auxdata1;
		}

		public String getAuxdata2() {
			return auxdata2;
		}

		public void setAuxdata2(String auxdata2) {
			this.auxdata2 = auxdata2;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}
	}
}