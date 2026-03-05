# Avenger

**Avenger** is a standalone Spring Boot 3.x stress-testing tool that simulates IoT device telemetry towards the **oneGasMeteor** TCP server.

## Overview

Avenger registers 1 000+ simulated devices with unique 15-digit IMEIs and mixed product types. Every minute it selects a random batch of 200-400 devices, builds a binary TEK-protocol message for each one, and fires independent TCP connections using **Java 21 Virtual Threads**.

## Requirements

- Java 21+
- Maven 3.9+ (or use the included `./mvnw`)
- A running oneGasMeteor TCP server (default: `localhost:8091`)

## Quick Start

```bash
# From the avenger/ directory
./mvnw spring-boot:run
```

Or build and run the fat jar:

```bash
./mvnw package
java -jar target/avenger-0.0.1-SNAPSHOT.jar
```

Override the target server at runtime:

```bash
java -jar target/avenger-0.0.1-SNAPSHOT.jar \
     --avenger.target.host=192.168.1.100 \
     --avenger.target.port=8091
```

## Configuration

All settings can be overridden in `src/main/resources/application.properties` or via command-line arguments.

| Property | Default | Description |
|---|---|---|
| `avenger.target.host` | `localhost` | Target TCP server hostname |
| `avenger.target.port` | `8091` | Target TCP server port |
| `avenger.devices.total` | `1000` | Total simulated devices in registry |
| `avenger.devices.per-batch` | `300` | (informational) typical devices per batch |
| `avenger.batch.interval-ms` | `60000` | Interval between batches in ms |
| `avenger.tcp.timeout-seconds` | `60` | TCP connect/read timeout in seconds |
| `avenger.measurements.min` | `3` | Minimum measurements per message |
| `avenger.measurements.max` | `10` | Maximum measurements per message |

## Supported Device Types

| Product Type Code | Device Name | Signal | Battery |
|---|---|---|---|
| 2  | TEK586       | RSSI (0-113) | Voltage |
| 5  | TEK733       | RSSI (0-113) | Voltage |
| 6  | TEK643       | RSSI (0-113) | Percentage |
| 7  | TEK811       | CSQ  (0-31)  | Voltage |
| 8  | TEK822V1     | CSQ  (0-31)  | Percentage |
| 9  | TEK733A      | RSSI (0-113) | Voltage |
| 10 | TEK871       | CSQ  (0-31)  | Percentage |
| 11 | TEK811A      | CSQ  (0-31)  | Voltage |
| 23 | TEK822V1BTN  | CSQ  (0-31)  | Percentage |
| 24 | TEK822V2     | CSQ  (0-31)  | Percentage |
| 25 | TEK900       | CSQ  (0-31)  | Voltage |
| 26 | TEK880       | CSQ  (0-31)  | Voltage |
| 27 | TEK898V2     | CSQ  (0-31)  | Percentage |
| 28 | TEK898V1     | CSQ  (0-31)  | Percentage |

## Project Structure

```
avenger/
├── pom.xml
├── mvnw / mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
├── src/main/java/com/aton/proj/avenger/
│   ├── AvengerApplication.java          Spring Boot entry point
│   ├── config/
│   │   └── AvengerProperties.java       @ConfigurationProperties binding
│   ├── model/
│   │   └── SimulatedDevice.java         Device POJO (IMEI, productType, name)
│   ├── registry/
│   │   └── DeviceRegistry.java          Generates and holds 1000+ devices
│   ├── protocol/
│   │   └── TekProtocolBuilder.java      Builds binary TEK messages
│   ├── client/
│   │   └── TcpDeviceClient.java         TCP send/receive per device
│   └── scheduler/
│       └── StressTestScheduler.java     @Scheduled batch runner
└── src/main/resources/
    └── application.properties
```

## Notes

- Avenger is **completely independent** from the oneGasMeteor codebase.
- Server responses are read and logged but not interpreted.
- Virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) are used for maximum concurrency with minimal resource overhead.
