# onGas_Meteor

**Spring Boot application that receives telemetry data from IoT gas monitoring devices (Tekelek TEK822 family) via TCP and sends commands back to them.**

## Overview

onGas_Meteor is a backend service built with **Spring Boot 3.5** and **Java 21** that:

1. Listens for incoming binary telemetry messages from IoT gas devices via a **raw TCP socket server**
2. Decodes the binary protocol (product type, IMEI, battery, signal, measurements, GPS, settings, statistics)
3. Persists decoded data to a **SQL Server** database
4. Retrieves pending commands for the device and encodes them back into the device protocol
5. Sends the encoded commands as a TCP response

## Architecture

The application follows a **layered architecture** with **Strategy Pattern** for device extensibility:

```
Device  ──TCP──▶  TcpSocketServer  ──▶  TcpConnectionHandler
                                              │
                                              ▼
                                      TelemetryService
                                       │           │
                              DecoderFactory    EncoderFactory
                              (Strategy)        (Strategy)
                                │                   │
                         Tek822Decoder        Tek822Encoder
                         UnknownDecoder       NoOpEncoder
                                │                   │
                                ▼                   ▼
                           Repository Layer (JPA / SQL Server)
```

### Layers

| Layer | Components | Role |
|-------|-----------|------|
| **Server** | `TcpSocketServer` | Accepts TCP connections on configurable port (default 8091), uses Java 21 virtual threads |
| **Handler** | `TcpConnectionHandler` | Reads raw bytes, creates `TelemetryMessage`, invokes service, sends response |
| **Controller** | `MeteorController`, `CommandController`, `CleanupAdminController` | REST endpoints for status, command management, and data cleanup |
| **Service** | `TelemetryService`, `CommandService`, `DataCleanupService` | Business logic: decode → save → encode response |
| **Decoder** | `DecoderFactory` → `Tek822Decoder`, `UnknownDeviceDecoder` | Strategy pattern: binary payload → `DecodedMessage` |
| **Encoder** | `EncoderFactory` → `Tek822Encoder`, `NoOpEncoder` | Strategy pattern: `DeviceCommand` → ASCII/HEX encoded response |
| **Repository** | `TelemetryRepository`, `CommandRepository`, `DeviceSettingsRepository`, `DeviceStatisticsRepository`, `DeviceLocationRepository` | Database abstraction with SQL Server (JPA) and optional MongoDB implementations |
| **Entity** | `TelemetryEntity`, `CommandEntity`, `DeviceSettingsEntity`, `DeviceStatisticsEntity`, `DeviceLocationEntity` | JPA entities mapped to SQL Server tables |

## Supported Devices

The Tek822Decoder supports 14 Tekelek product types:

| Code | Device |
|------|--------|
| 2 | TEK586 |
| 5 | TEK733 |
| 6 | TEK643 |
| 7 | TEK811 |
| 8 | TEK822V1 |
| 9 | TEK733A |
| 10 | TEK871 |
| 11 | TEK811A |
| 23 | TEK822V1BTN |
| 24 | TEK822V2 |
| 25 | TEK900 |
| 26 | TEK880 |
| 27 | TEK898V2 |
| 28 | TEK898V1 |

## Message Types

| Type | Description | Data Stored |
|------|-------------|-------------|
| 4, 8, 9 | Standard telemetry | Measurements (distance, temperature), battery, signal, diagnostics → `TELEMETRY_DATA` |
| 6 | Device settings | Key-value settings (S0, S1, S2, …) → `DEVICE_SETTINGS` |
| 16 | ICCID & statistics | Energy usage, temperature range, message counts, RSSI → `DEVICE_STATISTICS` |
| 17 | GPS location | Latitude, longitude, altitude, speed, satellites → `DEVICE_LOCATIONS` |

## Requesting Data from Devices (Command → Response Mapping)

Messages 6, 16 and 17 are **not sent spontaneously** by the device — they are **responses to specific commands** sent by the server. The table below shows which command to send via `POST /api/commands` to trigger each response message type:

| Response Message Type | Command to Send | Register | Description |
|-----------------------|----------------|----------|-------------|
| **6** — Settings | `REQUEST_SETTINGS` | R1=02 / R1=04 / R1=08 | Device replies with its current settings (S0–S11, S12–S18, or S19+) |
| **16** — Statistics | `REQUEST_STATUS` | R6=02 | Device replies with ICCID, energy usage, temperature range, RSSI, message counts |
| **17** — GPS | `REQUEST_GPS` | R7=XX | Device acquires a GPS fix (XX = timeout in hex seconds) and replies with coordinates |

### REST API Examples

#### Request Settings (Message Type 6)

```bash
curl -X POST http://localhost:8081/api/commands \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "862406075927406",
    "deviceType": "TEK822V2",
    "commandType": "REQUEST_SETTINGS"
  }'
```

Optional parameter `startFrom` selects the settings range:
- `"S0"` (default) → `R1=02` — returns settings from S0
- `"S12"` → `R1=04` — returns settings from S12
- `"S19"` → `R1=08` — returns settings from S19

```json
{
  "deviceId": "862406075927406",
  "deviceType": "TEK822V2",
  "commandType": "REQUEST_SETTINGS",
  "parameters": { "startFrom": "S12" }
}
```

#### Request Statistics (Message Type 16)

```bash
curl -X POST http://localhost:8081/api/commands \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "862406075927406",
    "deviceType": "TEK822V2",
    "commandType": "REQUEST_STATUS"
  }'
```

No parameters required.

#### Request GPS (Message Type 17)

```bash
curl -X POST http://localhost:8081/api/commands \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "862406075927406",
    "deviceType": "TEK822V2",
    "commandType": "REQUEST_GPS"
  }'
```

Optional parameter `timeout` (default: 60 seconds):

```json
{
  "deviceId": "862406075927406",
  "deviceType": "TEK822V2",
  "commandType": "REQUEST_GPS",
  "parameters": { "timeout": 120 }
}
```

> **Flow:** The command is saved as `PENDING` in the database. When the device next connects via TCP, Meteor sends the encoded command in the TCP response. The device processes the command and, on its next connection, sends back the corresponding message type (6, 16, or 17), which Meteor decodes and persists.

## Supported Commands (Encoder)

The Tek822Encoder supports 17 command types that can be sent back to devices:

| Command | Register | Description |
|---------|----------|-------------|
| `SET_INTERVAL` | S0 | Logger configuration (sampling period and speed) |
| `SET_LISTEN` | S1 | Listen window configuration |
| `SET_SCHEDULE` | S2 | Upload schedule (days, time, frequency) |
| `REBOOT` | R3=ACTIVE | Reboot / activate device (auto-appended when S-commands are present) |
| `REQUEST_STATUS` | R6=02 | Request statistics (triggers Message Type 16) |
| `SET_ALARM_THRESHOLD` | S4/S5/S6 | Static alarm configuration |
| `SHUTDOWN` | R1=80 | Shutdown modem and sleep |
| `SET_RTC` | R2 | Set real-time clock |
| `DEACTIVATE` | R4=DEACT | Deactivate scheduled uploads |
| `CLOSE_TCP` | R6=03 | Close TCP connection |
| `REQUEST_GPS` | R7 | Request GPS fix (triggers Message Type 17) |
| `REQUEST_SETTINGS` | R1=02/04/08 | Request settings (triggers Message Type 6) |
| `RESET_RTC` | R1=10 | Force RTC re-sync |
| `REQUEST_BUFFER_DATA` | R1=20 | Request buffered data |
| `REQUEST_DIAGNOSTIC_DATA` | R6=01 | Request diagnostic data |
| `SET_APN` | S12/S13/S14 | Set APN, username, password |
| `SET_SERVER` | S15/S16 | Set server IP and port |

## Database

The application uses **SQL Server** with the following tables:

- **`TELEMETRY_DATA`** — Raw + decoded telemetry (IMEI, battery, signal, measurements)
- **`DEVICE_COMMANDS`** — Pending/sent commands with status tracking and retry logic
- **`DEVICE_SETTINGS`** — Device configuration snapshots (Message Type 6)
- **`DEVICE_STATISTICS`** — Device operational statistics (Message Type 16)
- **`DEVICE_LOCATIONS`** — GPS location history (Message Type 17)

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8081 | HTTP REST API port |
| `tcp.server.port` | 8091 | TCP socket server port for device communication |
| `spring.datasource.url` | `jdbc:sqlserver://localhost:1433;databaseName=oneGasDB` | SQL Server connection |
| `command.max.per.response` | 10 | Max commands per device response |
| `cleanup.enabled` | false | Enable automatic data cleanup |
| `cleanup.cron` | `0 0 2 * * *` | Cleanup schedule (default: 2 AM daily) |

## Build & Run

**Prerequisites:** Java 21, Maven, SQL Server

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test
```

Environment variables:
- `SQL_DB_USERNAME` — SQL Server username
- `SQL_DB_PASSWORD` — SQL Server password
- `ONE_GAS_METEOR_SERVER_PORT` — Override HTTP port (default: 8081)
- `ONE_GAS_METEOR_TCP_SERVER_PORT` — Override TCP port (default: 8091)

## Extensibility

To add support for a new device family:

1. Create a new class implementing `DeviceDecoder` interface (with `@Component` and `@Order`)
2. Create a new class implementing `DeviceEncoder` interface (with `@Component` and `@Order`)
3. The `DecoderFactory` and `EncoderFactory` will auto-discover them via Spring dependency injection
