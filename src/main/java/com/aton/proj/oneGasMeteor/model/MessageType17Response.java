package com.aton.proj.oneGasMeteor.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response model per Message Type 17 (GPS Location)
 */
public class MessageType17Response {

	private String deviceId;
	private String deviceType;
	private LocalDateTime receivedAt;

	// GPS data
	private Integer timeToFixSeconds;
	private LocalTime utcTime;
	private String latitudeRaw; // Format: ddmm.mmmmN/S
	private String longitudeRaw; // Format: dddmm.mmmmE/W
	private Double latitude; // Decimal degrees
	private Double longitude; // Decimal degrees
	private Double horizontalPrecision;
	private Double altitude;
	private Integer gnssPositioningMode;
	private Double groundHeading;
	private Double speedKmh;
	private Double speedKnots;
	private String date; // Format: ddmmyy
	private Integer numberOfSatellites;

	public MessageType17Response() {
		this.receivedAt = LocalDateTime.now();
	}

	// Convert coordinate from ddmm.mmmm to decimal degrees
	public static double coordinateToDecimal(String coordinate, char direction) {
		if (coordinate == null || coordinate.isEmpty()) {
			return 0.0;
		}

		// Remove direction character if present
		coordinate = coordinate.replaceAll("[NSEW]", "").trim();

		// Parse degrees and minutes
		double degrees;
		double minutes;

		if (direction == 'N' || direction == 'S') {
			// Latitude: ddmm.mmmm
			degrees = Double.parseDouble(coordinate.substring(0, 2));
			minutes = Double.parseDouble(coordinate.substring(2));
		} else {
			// Longitude: dddmm.mmmm
			degrees = Double.parseDouble(coordinate.substring(0, 3));
			minutes = Double.parseDouble(coordinate.substring(3));
		}

		double decimal = degrees + (minutes / 60.0);

		// Apply direction (South and West are negative)
		if (direction == 'S' || direction == 'W') {
			decimal = -decimal;
		}

		return decimal;
	}

	// Getters & Setters
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

	public LocalDateTime getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(LocalDateTime receivedAt) {
		this.receivedAt = receivedAt;
	}

	public Integer getTimeToFixSeconds() {
		return timeToFixSeconds;
	}

	public void setTimeToFixSeconds(Integer timeToFixSeconds) {
		this.timeToFixSeconds = timeToFixSeconds;
	}

	public LocalTime getUtcTime() {
		return utcTime;
	}

	public void setUtcTime(LocalTime utcTime) {
		this.utcTime = utcTime;
	}

	public String getLatitudeRaw() {
		return latitudeRaw;
	}

	public void setLatitudeRaw(String latitudeRaw) {
		this.latitudeRaw = latitudeRaw;
		// Auto-convert to decimal
		if (latitudeRaw != null && latitudeRaw.length() > 0) {
			char direction = latitudeRaw.charAt(latitudeRaw.length() - 1);
			this.latitude = coordinateToDecimal(latitudeRaw, direction);
		}
	}

	public String getLongitudeRaw() {
		return longitudeRaw;
	}

	public void setLongitudeRaw(String longitudeRaw) {
		this.longitudeRaw = longitudeRaw;
		// Auto-convert to decimal
		if (longitudeRaw != null && longitudeRaw.length() > 0) {
			char direction = longitudeRaw.charAt(longitudeRaw.length() - 1);
			this.longitude = coordinateToDecimal(longitudeRaw, direction);
		}
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getHorizontalPrecision() {
		return horizontalPrecision;
	}

	public void setHorizontalPrecision(Double horizontalPrecision) {
		this.horizontalPrecision = horizontalPrecision;
	}

	public Double getAltitude() {
		return altitude;
	}

	public void setAltitude(Double altitude) {
		this.altitude = altitude;
	}

	public Integer getGnssPositioningMode() {
		return gnssPositioningMode;
	}

	public void setGnssPositioningMode(Integer gnssPositioningMode) {
		this.gnssPositioningMode = gnssPositioningMode;
	}

	public Double getGroundHeading() {
		return groundHeading;
	}

	public void setGroundHeading(Double groundHeading) {
		this.groundHeading = groundHeading;
	}

	public Double getSpeedKmh() {
		return speedKmh;
	}

	public void setSpeedKmh(Double speedKmh) {
		this.speedKmh = speedKmh;
	}

	public Double getSpeedKnots() {
		return speedKnots;
	}

	public void setSpeedKnots(Double speedKnots) {
		this.speedKnots = speedKnots;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Integer getNumberOfSatellites() {
		return numberOfSatellites;
	}

	public void setNumberOfSatellites(Integer numberOfSatellites) {
		this.numberOfSatellites = numberOfSatellites;
	}

	@Override
	public String toString() {
		return "MessageType17Response{" + "deviceId='" + deviceId + '\'' + ", latitude=" + latitude + ", longitude="
				+ longitude + ", altitude=" + altitude + ", satellites=" + numberOfSatellites + ", timeToFix="
				+ timeToFixSeconds + "s" + '}';
	}

	/**
	 * Genera link Google Maps
	 */
	public String getGoogleMapsLink() {
		if (latitude != null && longitude != null) {
			return String.format("https://www.google.com/maps?q=%.6f,%.6f", latitude, longitude);
		}
		return null;
	}
}