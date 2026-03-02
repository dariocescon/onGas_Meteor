package com.aton.proj.oneGasMeteor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity per salvare i dati GPS del device (Message Type 17)
 */
@Entity
@Table(name = "device_locations", indexes = {
        @Index(name = "idx_dl_device_id", columnList = "device_id"),
        @Index(name = "idx_dl_received_at", columnList = "received_at")
})
public class DeviceLocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(name = "raw_message", columnDefinition = "NVARCHAR(MAX)")
    private String rawMessage;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "latitude_raw", length = 20)
    private String latitudeRaw;

    @Column(name = "longitude_raw", length = 20)
    private String longitudeRaw;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "speed_knots")
    private Double speedKnots;

    @Column(name = "ground_heading")
    private Double groundHeading;

    @Column(name = "horizontal_precision")
    private Double horizontalPrecision;

    @Column(name = "utc_time")
    private LocalTime utcTime;

    @Column(name = "gps_date", length = 10)
    private String date;

    @Column(name = "number_of_satellites")
    private Integer numberOfSatellites;

    @Column(name = "time_to_fix_seconds")
    private Integer timeToFixSeconds;

    @Column(name = "gnss_positioning_mode")
    private Integer gnssPositioningMode;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public DeviceLocationEntity() {
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

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
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

    public String getLatitudeRaw() {
        return latitudeRaw;
    }

    public void setLatitudeRaw(String latitudeRaw) {
        this.latitudeRaw = latitudeRaw;
    }

    public String getLongitudeRaw() {
        return longitudeRaw;
    }

    public void setLongitudeRaw(String longitudeRaw) {
        this.longitudeRaw = longitudeRaw;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
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

    public Double getGroundHeading() {
        return groundHeading;
    }

    public void setGroundHeading(Double groundHeading) {
        this.groundHeading = groundHeading;
    }

    public Double getHorizontalPrecision() {
        return horizontalPrecision;
    }

    public void setHorizontalPrecision(Double horizontalPrecision) {
        this.horizontalPrecision = horizontalPrecision;
    }

    public LocalTime getUtcTime() {
        return utcTime;
    }

    public void setUtcTime(LocalTime utcTime) {
        this.utcTime = utcTime;
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

    public Integer getTimeToFixSeconds() {
        return timeToFixSeconds;
    }

    public void setTimeToFixSeconds(Integer timeToFixSeconds) {
        this.timeToFixSeconds = timeToFixSeconds;
    }

    public Integer getGnssPositioningMode() {
        return gnssPositioningMode;
    }

    public void setGnssPositioningMode(Integer gnssPositioningMode) {
        this.gnssPositioningMode = gnssPositioningMode;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    @Override
    public String toString() {
        return "DeviceLocationEntity{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
