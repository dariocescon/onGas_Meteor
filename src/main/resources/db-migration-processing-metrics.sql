-- ============================================
-- Processing Metrics Table
-- Metriche di performance per ogni elaborazione TCP
-- ============================================

CREATE TABLE processing_metrics (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    device_id               NVARCHAR(50)    NULL,
    device_type             NVARCHAR(50)    NULL,
    message_type            INT             NULL,
    client_address          NVARCHAR(100)   NULL,

    -- Payload info
    payload_length_bytes    INT             NULL,
    declared_body_length    INT             NULL,
    measurement_count       INT             NULL,

    -- Command info
    pending_commands_found  INT             NULL,
    commands_sent           INT             NULL,
    response_size_bytes     INT             NULL,

    -- Timing (millisecondi)
    total_processing_time_ms BIGINT         NULL,
    read_time_ms            BIGINT          NULL,
    decode_time_ms          BIGINT          NULL,
    db_save_time_ms         BIGINT          NULL,
    command_query_time_ms   BIGINT          NULL,
    command_encode_time_ms  BIGINT          NULL,
    send_time_ms            BIGINT          NULL,

    -- Device health snapshot
    battery_voltage         FLOAT           NULL,
    battery_percentage      FLOAT           NULL,
    signal_strength         INT             NULL,
    contact_reason          NVARCHAR(200)   NULL,
    firmware_version        NVARCHAR(20)    NULL,

    -- Result
    success                 BIT             NOT NULL,
    error_message           NVARCHAR(500)   NULL,

    -- Timestamps
    received_at             DATETIME2(3)    NOT NULL,
    completed_at            DATETIME2(3)    NULL
);

-- Indici per query analitiche
CREATE INDEX idx_pm_device_id    ON processing_metrics (device_id);
CREATE INDEX idx_pm_received_at  ON processing_metrics (received_at);
CREATE INDEX idx_pm_message_type ON processing_metrics (message_type);
CREATE INDEX idx_pm_success      ON processing_metrics (success);
