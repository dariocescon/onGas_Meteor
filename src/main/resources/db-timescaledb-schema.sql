-- ============================================================
-- Schema TimescaleDB per oneGasDB
-- ============================================================
-- Prerequisito: PostgreSQL con estensione TimescaleDB installata
-- Eseguire questo script come utente con privilegi CREATE su oneGasDB
--
-- Nota: le hypertable richiedono che la colonna di partizionamento
-- (received_at) faccia parte di ogni vincolo UNIQUE/PK.
-- Per questo le tabelle hypertable usano un id auto-generato
-- SENZA vincolo PRIMARY KEY, e un indice composito (id, received_at).
-- ============================================================

CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================================
-- telemetry_data (hypertable)
-- ============================================================
CREATE TABLE IF NOT EXISTS telemetry_data (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    device_id           VARCHAR(50)         NOT NULL,
    device_type         VARCHAR(50)         NOT NULL,
    raw_message         TEXT,
    decoded_data        TEXT,
    received_at         TIMESTAMP(3)        NOT NULL,
    processed_at        TIMESTAMP(3),
    imei                VARCHAR(20),
    firmware_version    VARCHAR(20),
    battery_voltage     DOUBLE PRECISION,
    battery_percentage  DOUBLE PRECISION,
    signal_strength     INT,
    message_type        VARCHAR(50),
    measurement_count   INT
);

SELECT create_hypertable('telemetry_data', 'received_at',
       chunk_time_interval => INTERVAL '7 days',
       if_not_exists => true);

CREATE UNIQUE INDEX IF NOT EXISTS idx_telemetry_pk        ON telemetry_data (id, received_at);
CREATE INDEX IF NOT EXISTS idx_td_device_id               ON telemetry_data (device_id);
CREATE INDEX IF NOT EXISTS idx_td_device_type             ON telemetry_data (device_type);
CREATE INDEX IF NOT EXISTS idx_td_received_at             ON telemetry_data (received_at DESC);
CREATE INDEX IF NOT EXISTS idx_td_imei                    ON telemetry_data (imei);

-- ============================================================
-- device_commands (tabella relazionale normale, NO hypertable)
-- ============================================================
CREATE TABLE IF NOT EXISTS device_commands (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id           VARCHAR(50)     NOT NULL,
    device_type         VARCHAR(50)     NOT NULL,
    command_type        VARCHAR(50)     NOT NULL,
    command_params      TEXT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP(3)    NOT NULL DEFAULT NOW(),
    sent_at             TIMESTAMP(3),
    delivered_at        TIMESTAMP(3),
    error_message       TEXT,
    retry_count         INT             DEFAULT 0,
    max_retries         INT             DEFAULT 3
);

CREATE INDEX IF NOT EXISTS idx_dc_device_status  ON device_commands (device_id, status);
CREATE INDEX IF NOT EXISTS idx_dc_created_at     ON device_commands (created_at);

-- ============================================================
-- device_settings (hypertable)
-- ============================================================
CREATE TABLE IF NOT EXISTS device_settings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    device_id       VARCHAR(50)     NOT NULL,
    device_type     VARCHAR(50)     NOT NULL,
    raw_message     TEXT,
    settings_json   TEXT,
    received_at     TIMESTAMP       NOT NULL
);

SELECT create_hypertable('device_settings', 'received_at',
       chunk_time_interval => INTERVAL '7 days',
       if_not_exists => true);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ds_pk           ON device_settings (id, received_at);
CREATE INDEX IF NOT EXISTS idx_ds_device_id           ON device_settings (device_id);
CREATE INDEX IF NOT EXISTS idx_ds_received_at         ON device_settings (received_at DESC);

-- ============================================================
-- device_statistics (hypertable)
-- ============================================================
CREATE TABLE IF NOT EXISTS device_statistics (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    device_id               VARCHAR(50)         NOT NULL,
    device_type             VARCHAR(50)         NOT NULL,
    raw_message             TEXT,
    iccid                   VARCHAR(30),
    energy_used             BIGINT,
    min_temperature         INT,
    max_temperature         INT,
    message_count           INT,
    delivery_fail_count     INT,
    total_send_time         BIGINT,
    max_send_time           BIGINT,
    min_send_time           BIGINT,
    rssi_total              BIGINT,
    rssi_valid_count        INT,
    rssi_fail_count         INT,
    average_send_time       DOUBLE PRECISION,
    average_rssi            DOUBLE PRECISION,
    delivery_success_rate   DOUBLE PRECISION,
    received_at             TIMESTAMP           NOT NULL
);

SELECT create_hypertable('device_statistics', 'received_at',
       chunk_time_interval => INTERVAL '7 days',
       if_not_exists => true);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dst_pk          ON device_statistics (id, received_at);
CREATE INDEX IF NOT EXISTS idx_dst_device_id          ON device_statistics (device_id);
CREATE INDEX IF NOT EXISTS idx_dst_received_at        ON device_statistics (received_at DESC);

-- ============================================================
-- device_locations (hypertable)
-- ============================================================
CREATE TABLE IF NOT EXISTS device_locations (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    device_id               VARCHAR(50)         NOT NULL,
    device_type             VARCHAR(50)         NOT NULL,
    raw_message             TEXT,
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    latitude_raw            VARCHAR(20),
    longitude_raw           VARCHAR(20),
    altitude                DOUBLE PRECISION,
    speed_kmh               DOUBLE PRECISION,
    speed_knots             DOUBLE PRECISION,
    ground_heading          DOUBLE PRECISION,
    horizontal_precision    DOUBLE PRECISION,
    utc_time                TIME,
    gps_date                VARCHAR(10),
    number_of_satellites    INT,
    time_to_fix_seconds     INT,
    gnss_positioning_mode   INT,
    received_at             TIMESTAMP           NOT NULL
);

SELECT create_hypertable('device_locations', 'received_at',
       chunk_time_interval => INTERVAL '7 days',
       if_not_exists => true);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dl_pk           ON device_locations (id, received_at);
CREATE INDEX IF NOT EXISTS idx_dl_device_id           ON device_locations (device_id);
CREATE INDEX IF NOT EXISTS idx_dl_received_at         ON device_locations (received_at DESC);

-- ============================================================
-- processing_metrics (hypertable)
-- ============================================================
CREATE TABLE IF NOT EXISTS processing_metrics (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    device_id                   VARCHAR(50),
    device_type                 VARCHAR(50),
    message_type                INT,
    client_address              VARCHAR(100),
    payload_length_bytes        INT,
    declared_body_length        INT,
    measurement_count           INT,
    pending_commands_found      INT,
    commands_sent               INT,
    response_size_bytes         INT,
    total_processing_time_ms    BIGINT,
    read_time_ms                BIGINT,
    decode_time_ms              BIGINT,
    db_save_time_ms             BIGINT,
    command_query_time_ms       BIGINT,
    command_encode_time_ms      BIGINT,
    send_time_ms                BIGINT,
    battery_voltage             DOUBLE PRECISION,
    battery_percentage          DOUBLE PRECISION,
    signal_strength             INT,
    contact_reason              VARCHAR(200),
    firmware_version            VARCHAR(20),
    success                     BOOLEAN         NOT NULL,
    error_message               VARCHAR(500),
    received_at                 TIMESTAMP(3)    NOT NULL,
    completed_at                TIMESTAMP(3)
);

SELECT create_hypertable('processing_metrics', 'received_at',
       chunk_time_interval => INTERVAL '7 days',
       if_not_exists => true);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pm_pk            ON processing_metrics (id, received_at);
CREATE INDEX IF NOT EXISTS idx_pm_device_id            ON processing_metrics (device_id);
CREATE INDEX IF NOT EXISTS idx_pm_received_at          ON processing_metrics (received_at DESC);
CREATE INDEX IF NOT EXISTS idx_pm_message_type         ON processing_metrics (message_type);
CREATE INDEX IF NOT EXISTS idx_pm_success              ON processing_metrics (success);

-- ============================================================
-- Retention policy opzionali (TimescaleDB native)
-- ============================================================
-- Decommentare per abilitare retention policy automatiche gestite
-- direttamente da TimescaleDB (alternativa a DataCleanupService):
--
-- SELECT add_retention_policy('telemetry_data', INTERVAL '30 days', if_not_exists => true);
-- SELECT add_retention_policy('device_settings', INTERVAL '30 days', if_not_exists => true);
-- SELECT add_retention_policy('device_statistics', INTERVAL '30 days', if_not_exists => true);
-- SELECT add_retention_policy('device_locations', INTERVAL '30 days', if_not_exists => true);
-- SELECT add_retention_policy('processing_metrics', INTERVAL '90 days', if_not_exists => true);

-- ============================================================
-- Compression policy opzionali (TimescaleDB)
-- ============================================================
-- Decommentare per abilitare compressione automatica dei chunk
-- piu vecchi di 7 giorni (riduce spazio disco ~90%):
--
-- ALTER TABLE telemetry_data SET (
--     timescaledb.compress,
--     timescaledb.compress_segmentby = 'device_id',
--     timescaledb.compress_orderby = 'received_at DESC'
-- );
-- SELECT add_compression_policy('telemetry_data', INTERVAL '7 days', if_not_exists => true);
--
-- ALTER TABLE processing_metrics SET (
--     timescaledb.compress,
--     timescaledb.compress_segmentby = 'device_id',
--     timescaledb.compress_orderby = 'received_at DESC'
-- );
-- SELECT add_compression_policy('processing_metrics', INTERVAL '7 days', if_not_exists => true);
