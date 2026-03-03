-- SQL Server DDL per le tabelle dei messaggi di tipo 4/8/9, 6, 16 e 17
-- Da eseguire sul database oneGasDB

-- ============================================================
-- Tabella per la telemetria decodificata (Message Type 4/8/9)
-- ============================================================
CREATE TABLE telemetry_data (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    device_id           NVARCHAR(50)  NOT NULL,
    device_type         NVARCHAR(50)  NOT NULL,
    raw_message         NVARCHAR(MAX) NULL,
    decoded_data        NVARCHAR(MAX) NULL,
    received_at         DATETIME2(3)  NOT NULL,
    processed_at        DATETIME2(3)  NULL,
    imei                NVARCHAR(20)  NULL,
    firmware_version    NVARCHAR(20)  NULL,
    battery_voltage     FLOAT         NULL,
    battery_percentage  FLOAT         NULL,
    signal_strength     INT           NULL,
    message_type        NVARCHAR(50)  NULL,
    measurement_count   INT           NULL
);
CREATE INDEX idx_device_id   ON telemetry_data (device_id);
CREATE INDEX idx_device_type ON telemetry_data (device_type);
CREATE INDEX idx_received_at ON telemetry_data (received_at);
CREATE INDEX idx_imei        ON telemetry_data (imei);

-- ============================================================
-- Tabella per le impostazioni del device (Message Type 6)
-- ============================================================
CREATE TABLE device_settings (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    device_id       NVARCHAR(50)  NOT NULL,
    device_type     NVARCHAR(50)  NOT NULL,
    raw_message     NVARCHAR(MAX) NULL,
    settings_json   NVARCHAR(MAX) NULL,
    received_at     DATETIME2     NOT NULL
);
CREATE INDEX idx_ds_device_id  ON device_settings (device_id);
CREATE INDEX idx_ds_received_at ON device_settings (received_at);

-- Tabella per le statistiche del device (Message Type 16)
CREATE TABLE device_statistics (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    device_id             NVARCHAR(50)  NOT NULL,
    device_type           NVARCHAR(50)  NOT NULL,
    raw_message           NVARCHAR(MAX) NULL,
    iccid                 NVARCHAR(30)  NULL,
    energy_used           BIGINT        NULL,
    min_temperature       INT           NULL,
    max_temperature       INT           NULL,
    message_count         INT           NULL,
    delivery_fail_count   INT           NULL,
    total_send_time       BIGINT        NULL,
    max_send_time         BIGINT        NULL,
    min_send_time         BIGINT        NULL,
    rssi_total            BIGINT        NULL,
    rssi_valid_count      INT           NULL,
    rssi_fail_count       INT           NULL,
    average_send_time     FLOAT         NULL,
    average_rssi          FLOAT         NULL,
    delivery_success_rate FLOAT         NULL,
    received_at           DATETIME2     NOT NULL
);
CREATE INDEX idx_dst_device_id   ON device_statistics (device_id);
CREATE INDEX idx_dst_received_at ON device_statistics (received_at);

-- Tabella per i dati GPS del device (Message Type 17)
CREATE TABLE device_locations (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    device_id             NVARCHAR(50)  NOT NULL,
    device_type           NVARCHAR(50)  NOT NULL,
    raw_message           NVARCHAR(MAX) NULL,
    latitude              FLOAT         NULL,
    longitude             FLOAT         NULL,
    latitude_raw          NVARCHAR(20)  NULL,
    longitude_raw         NVARCHAR(20)  NULL,
    altitude              FLOAT         NULL,
    speed_kmh             FLOAT         NULL,
    speed_knots           FLOAT         NULL,
    ground_heading        FLOAT         NULL,
    horizontal_precision  FLOAT         NULL,
    utc_time              TIME          NULL,
    gps_date              NVARCHAR(10)  NULL,
    number_of_satellites  INT           NULL,
    time_to_fix_seconds   INT           NULL,
    gnss_positioning_mode INT           NULL,
    received_at           DATETIME2     NOT NULL
);
CREATE INDEX idx_dl_device_id   ON device_locations (device_id);
CREATE INDEX idx_dl_received_at ON device_locations (received_at);
