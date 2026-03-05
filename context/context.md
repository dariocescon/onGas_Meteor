# Contesto del Progetto onGas_Meteor

> Documento di riferimento unico per architettura, componenti, flussi, database, API e configurazione di **onGas_Meteor**.  
> Generato da: lettura completa di `docs/` e del codice sorgente.

---

## Indice

1. [Panoramica del Progetto](#1-panoramica-del-progetto)
2. [Architettura a Strati](#2-architettura-a-strati)
3. [Flusso Principale](#3-flusso-principale)
4. [Componenti e Package](#4-componenti-e-package)
5. [Protocollo Binario TEK](#5-protocollo-binario-tek)
6. [Encoder e Comandi](#6-encoder-e-comandi)
7. [Database](#7-database)
8. [API REST](#8-api-rest)
9. [Configurazione](#9-configurazione)
10. [Dispositivi Supportati](#10-dispositivi-supportati)
11. [Pattern di Estensibilità](#11-pattern-di-estensibilità)
12. [Test](#12-test)
13. [Build & Run](#13-build--run)
14. [Riferimenti alla Documentazione](#14-riferimenti-alla-documentazione)

---

## 1. Panoramica del Progetto

| Campo           | Valore                                      |
|-----------------|---------------------------------------------|
| **Nome**        | onGas_Meteor                                |
| **Descrizione** | Server TCP + REST che riceve messaggi binari da dispositivi IoT per il monitoraggio gas (famiglia Tekelek TEK822), li decodifica, li persiste nel DB e restituisce comandi di risposta codificati |
| **GroupId Maven** | `com.aton.proj`                           |
| **ArtifactId**  | `oneGasMeteor`                              |
| **Framework**   | Spring Boot **3.5.10**                      |
| **Linguaggio**  | Java **21** (con Virtual Threads)           |
| **Database**    | SQL Server (attivo) — MongoDB (opzionale)   |
| **Build tool**  | Maven (wrapper incluso)                     |
| **Porta HTTP**  | `8081` (configurabile)                      |
| **Porta TCP**   | `8091` (configurabile)                      |

### Obiettivo funzionale

I dispositivi IoT (es. TEK822, TEK586) inviano messaggi binari via TCP al server sulla porta `8091`.  
Il server:
1. decodifica il payload binario secondo il protocollo TEK;
2. persiste la telemetria nel database;
3. recupera eventuali comandi pendenti per quel device;
4. codifica i comandi in formato ASCII→HEX;
5. restituisce la risposta TCP contenente i comandi concatenati con virgola.

---

## 2. Architettura a Strati

```
┌─────────────────────────────────────────────────────────────────┐
│                        TRANSPORT LAYER                          │
│   TcpSocketServer (:8091)   │   MeteorController / CommandController (:8081)  │
│   TcpConnectionHandler      │   CleanupAdminController                        │
└──────────────────┬──────────────────────────────────────────────┘
                   │ TelemetryMessage
┌──────────────────▼──────────────────────────────────────────────┐
│                        SERVICE LAYER                            │
│        TelemetryService          │   CommandService             │
│        DataCleanupService                                       │
└──────────────────┬──────────────────────────────────────────────┘
           ┌───────┴───────┐
           │               │
┌──────────▼──────┐ ┌──────▼──────────────────────────────────────┐
│ DECODER LAYER   │ │              ENCODER LAYER                   │
│ (INBOUND)       │ │              (OUTBOUND)                      │
│                 │ │                                              │
│ DecoderFactory  │ │ EncoderFactory                               │
│  └ Tek822Decoder│ │  └ Tek822Encoder                             │
│  └ UnknownDevice│ │  └ NoOpEncoder                               │
└──────────┬──────┘ └──────┬───────────────────────────────────────┘
           │               │
┌──────────▼───────────────▼──────────────────────────────────────┐
│                     REPOSITORY LAYER                            │
│  TelemetryRepository  │  CommandRepository  │  DeviceSettings*  │
│  ┌─ SqlServer ─────┐  │  ┌─ SqlServer ──┐  │  DeviceStatistics*│
│  │ JpaRepository   │  │  │ JpaRepository│  │  DeviceLocation*  │
│  └─────────────────┘  │  └──────────────┘  │                   │
│  ┌─ MongoDB (opt) ─┐  │  ┌─ MongoDB ────┐  │                   │
│  └─────────────────┘  │  └──────────────┘  │                   │
└──────────┬─────────────────────────────────────────────────────-┘
           │
┌──────────▼──────────────────────────────────────────────────────┐
│                        DATABASE LAYER                           │
│  SQL Server (oneGasDB)              MongoDB (opzionale)         │
│  - TELEMETRY_DATA                   - TelemetryDocument         │
│  - DEVICE_COMMANDS                  - CommandDocument           │
│  - DEVICE_SETTINGS                                              │
│  - DEVICE_STATISTICS                                            │
│  - DEVICE_LOCATIONS                                             │
└─────────────────────────────────────────────────────────────────┘
```

### Descrizione dei layer

| Layer | Responsabilità |
|-------|---------------|
| **Transport** | Accetta connessioni TCP (Java 21 virtual threads) con Semaphore per limitare le connessioni concorrenti e backlog configurabile sul `ServerSocket`; espone richieste HTTP REST |
| **Service** | Orchestrazione del flusso: decodifica → persistenza → encoding comandi |
| **Decoder** | Trasforma il payload binario in oggetti Java (`DecodedMessage`) |
| **Encoder** | Trasforma i `DeviceCommand` in stringhe ASCII/HEX da inviare al device |
| **Repository** | Astrazione del database; implementazioni per SQL Server e MongoDB |
| **Database** | Persistenza dati di telemetria, comandi, settings, statistiche e GPS |

---

## 3. Flusso Principale

```
Dispositivo IoT
    │
    │  Connessione TCP → porta 8091
    │  Payload binario (17-byte header + body)
    ▼
TcpSocketServer
    │  Semaphore.acquire() → limita connessioni concorrenti (max 10000)
    │  Accetta connessione, crea virtual thread
    ▼
TcpConnectionHandler (o TcpConnectionHandlerReadExactly)
    │  1. Legge bytes raw dal socket (buffer 4096 o header+body esatti)
    │  2. Crea TelemetryMessage { payload, hexData, receivedAt, sourceAddress }
    ▼
TelemetryService.processTelemetry(TelemetryMessage)
    │
    │  3. DecoderFactory.getDecoder(payload)
    │     └─ Scorre decoder ordinati per @Order
    │     └─ Tek822Decoder.canDecode(payload) → true se product type noto
    │     └─ Fallback: UnknownDeviceDecoder
    │
    │  4. decoder.decode(message)
    │     └─ TekMessageDecoder.decode()
    │        ├─ decodeProductType()  → byte[0]
    │        ├─ decodeVersions()     → byte[1], byte[2]
    │        ├─ decodeImei()         → byte[7..14]
    │        ├─ decodeMessageType()  → byte[15] & 0x3F
    │        ├─ decodeContactReason()→ byte[3]
    │        ├─ decodeAlarmStatus()  → byte[4]
    │        ├─ decodeLastReset()    → byte[4]
    │        ├─ decodeSignalStrength()→ byte[5]
    │        ├─ decodeDiagnosticInfo()→ byte[6]
    │        ├─ decodeBatteryStatus()→ byte[6]
    │        └─ (se msgType 4/8/9) decodeDiagnosticData() + decodeMeasurementData()
    │
    │  5. Switch su messageType:
    │     ├─ 4/8/9 → TelemetryRepository.save()    → TELEMETRY_DATA
    │     ├─ 6    → MessageTypeParser.parseType6()  → DEVICE_SETTINGS
    │     ├─ 16   → MessageTypeParser.parseType16() → DEVICE_STATISTICS
    │     └─ 17   → MessageTypeParser.parseType17() → DEVICE_LOCATIONS
    │
    │  6. CommandRepository.findPendingCommands(deviceId)
    │     └─ Recupera comandi con status=PENDING per quell'IMEI
    │
    │  7. (se comandi presenti) EncoderFactory.getEncoder(deviceType)
    │     └─ Tek822Encoder.encode(deviceCommands)
    │        ├─ ensureRebootIfNeeded() → auto-append R3=ACTIVE per S-commands
    │        ├─ Per ogni comando: encodeXxx() → ASCII string
    │        └─ asciiToHex() → HEX string
    │
    │  8. Crea TelemetryResponse { deviceId, deviceType, commands[] }
    ▼
TcpConnectionHandler
    │  9.  Concatena comandi HEX con virgola (ControllerUtils.concatenateCommands)
    │  10. Invia risposta TCP al device
    │  11. TelemetryService.markCommandsAsSent() → status=SENT nel DB
    ▼
Dispositivo IoT riceve risposta
```

---

## 4. Componenti e Package

### `server/`

#### `TcpSocketServer`
- **Tipo**: `@Component`, `CommandLineRunner`
- **Funzione**: Avvia un `ServerSocket` sulla porta configurata (`tcp.server.port`, default `8091`) con backlog configurabile (`tcp.server.backlog`, default `1024`). Usa un `Semaphore` (`connectionLimiter`) inizializzato a `tcp.server.max-connections` (default `10000`) per limitare le connessioni concorrenti. Per ogni connessione in entrata acquisisce un permesso dal semaforo, crea un virtual thread (Java 21 `Executors.newVirtualThreadPerTaskExecutor()`) e delega a `TcpConnectionHandlerReadExactly`; il permesso viene rilasciato nel blocco `finally` del thread. Quando i permessi disponibili scendono sotto il 10 % viene loggato un warning.
- **Lifecycle**: `@PreDestroy` chiude il `ServerSocket`, esegue `executorService.shutdown()` con attesa di 5 s (poi `shutdownNow()`) e logga lo stato finale del semaforo.

---

### `handler/`

#### `TcpConnectionHandler`
- **Tipo**: `@Component`
- **Funzione**: Legge dati raw dal socket in un buffer da 4096 byte, costruisce un `TelemetryMessage`, invoca `TelemetryService.processTelemetry()`, concatena i comandi codificati e li invia come risposta TCP, poi chiama `markCommandsAsSent()`.

#### `TcpConnectionHandlerReadExactly`
- **Tipo**: `@Component`
- **Funzione**: Variante del handler sopra che legge esattamente **17 byte di header** e poi `declaredLength` byte di body (lunghezza dichiarata nei byte 15-16 del payload). Stessa logica di risposta.

---

### `controller/`

#### `MeteorController`
- **Tipo**: `@RestController`
- **Endpoint**:
  - `GET /status` — stato dell'applicazione
  - `GET /process` — trigger manuale di elaborazione
  - `GET /health` — health check

#### `CommandController`
- **Tipo**: `@RestController @RequestMapping("/api/commands")`
- **Endpoint**:
  - `POST /api/commands` — crea un nuovo comando
  - `GET /api/commands/{id}` — dettaglio comando
  - `GET /api/commands/device/{deviceId}` — comandi pendenti per device
- **Funzione**: Gestisce la creazione e il recupero dei comandi da inviare ai device. Include exception handlers per validazione.

#### `CleanupAdminController`
- **Tipo**: `@RestController @RequestMapping("/admin/cleanup")`, `@ConditionalOnProperty(cleanup.enabled=true)`
- **Endpoint**:
  - `POST /admin/cleanup/trigger` — trigger manuale cleanup
  - `POST /admin/cleanup/telemetry` — pulizia solo telemetria
  - `POST /admin/cleanup/commands` — pulizia solo comandi
  - `GET /admin/cleanup/status` — stato del cleanup

---

### `service/`

#### `TelemetryService`
- **Tipo**: `@Service`
- **Funzione**: Orchestratore principale. Riceve `TelemetryMessage`, seleziona il decoder, decodifica, salva nel DB in base al messageType, recupera comandi pendenti, codifica con l'encoder, ritorna `TelemetryResponse`.
- **Iniezione**: `DecoderFactory`, `EncoderFactory`, `TelemetryRepository`, `CommandRepository`, `MessageTypeParser`, `ObjectMapper`, `DeviceSettingsRepository`, `DeviceStatisticsRepository`, `DeviceLocationRepository`.
- **Property**: `command.max.per.response=10`

#### `CommandService`
- **Tipo**: `@Service`
- **Funzione**: Valida e crea comandi. Controlla `deviceType` (deve essere nei tipi abilitati), `commandType` (deve essere uno dei 17 validi) e parametri obbligatori per tipo.
- **Mappa**: `REQUIRED_PARAMS` definisce i parametri obbligatori per ogni tipo di comando.

#### `DataCleanupService`
- **Tipo**: `@Service`, `@ConditionalOnProperty(cleanup.enabled=true)`
- **Funzione**: Scheduled cleanup di telemetria (retention 30 gg), comandi (retention 7 gg) e dati device. Cron: `0 0 2 * * *` (ogni notte alle 2:00).

---

### `decoder/`

#### `DeviceDecoder` (interface)
```java
boolean canDecode(byte[] payload);
DecodedMessage decode(TelemetryMessage message);
List<String> getSupportedDeviceTypes();
String getDecoderName();
```

#### `DecoderFactory`
- **Tipo**: `@Component`
- **Funzione**: Itera la lista di decoder iniettati (ordinati per `@Order`), chiama `canDecode()` sul payload e ritorna il primo decoder compatibile. Se nessuno è compatibile, usa `UnknownDeviceDecoder` come fallback.

#### `TekMessageDecoder`
- **Funzione**: Classe di supporto (non Spring component) con la logica di decodifica binaria del protocollo TEK. Usata da `Tek822Decoder`. Decoding completo di header + body.

#### `MessageTypeParser`
- **Tipo**: `@Component`
- **Funzione**: Parsifica i message type specializzati:
  - `parseMessageType6()` → settings (risposta a REQUEST_SETTINGS)
  - `parseMessageType16()` → ICCID + statistiche (risposta a REQUEST_STATUS)
  - `parseMessageType17()` → dati GPS (risposta a REQUEST_GPS)

#### `impl/Tek822Decoder`
- **Tipo**: `@Component @Order(1)`
- **Funzione**: Decoder primario per la famiglia TEK822. Supporta 14 product type codes (vedi sezione 10). Usa `TekMessageDecoder` per la decodifica.

#### `impl/UnknownDeviceDecoder`
- **Tipo**: `@Component @Order(999)`
- **Funzione**: Decoder fallback. Ritorna un `DecodedMessage` generico senza decodifica specifica.

---

### `encoder/`

#### `DeviceEncoder` (interface)
```java
boolean canEncode(String deviceType);
List<TelemetryResponse.EncodedCommand> encode(List<DeviceCommand> commands);
List<String> getSupportedDeviceTypes();
String getEncoderName();
```

#### `EncoderFactory`
- **Tipo**: `@Component`
- **Funzione**: Seleziona l'encoder per `deviceType`. Fallback: `NoOpEncoder`.

#### `impl/Tek822Encoder`
- **Tipo**: `@Component @Order(1)`
- **Funzione**: Encoder per tutti i device TEK (tutti e 14 i tipi). Implementa 17 command types. Auto-append `R3=ACTIVE` (REBOOT) se ci sono S-commands. Codifica ASCII→HEX.
- **Password default**: `TEK822`

#### `impl/NoOpEncoder`
- **Tipo**: `@Component @Order(999)`
- **Funzione**: Encoder fallback che non codifica nulla. Ritorna lista vuota.

---

### `model/`

| Classe | Descrizione |
|--------|-------------|
| `TelemetryMessage` | Immutabile. Contiene `payload` (byte[]), `hexData`, `receivedAt`, `sourceAddress`, `serverTimeInMs` |
| `DecodedMessage` | Risultato decodifica. Inner classes: `UnitInfo`, `UniqueIdentifier`, `ContactReason`, `AlarmStatus`, `LastReset`, `SignalStrength`, `DiagnosticInfo`, `BatteryStatus`, `UnitSetup`, `List<MeasurementData>` |
| `DeviceCommand` | Comando da inviare. Campi: `id`, `deviceId`, `deviceType`, `commandType`, `parameters` (Map), `encodedCommandASCII`, `encodedCommandHEX` |
| `TelemetryResponse` | Risposta del servizio. Contiene `deviceId`, `deviceType`, `commands` (List), `receivedAt`, `processedAt` |
| `MessageType6Response` | Risultato parse settings (tipo 6) |
| `MessageType16Response` | Risultato parse statistiche + ICCID (tipo 16) |
| `MessageType17Response` | Risultato parse GPS (tipo 17). Include `getGoogleMapsLink()` |

---

### `entity/`

| Classe | Tabella DB |
|--------|-----------|
| `TelemetryEntity` | `TELEMETRY_DATA` |
| `CommandEntity` | `DEVICE_COMMANDS` — inner enum `CommandStatus` (PENDING, SENT, DELIVERED, FAILED, EXPIRED) |
| `DeviceSettingsEntity` | `DEVICE_SETTINGS` |
| `DeviceStatisticsEntity` | `DEVICE_STATISTICS` |
| `DeviceLocationEntity` | `DEVICE_LOCATIONS` |
| `mongodb/TelemetryDocument` | MongoDB (commentato, uso futuro) |
| `mongodb/CommandDocument` | MongoDB (commentato, uso futuro) |

---

### `repository/`

#### Interfaces
- `TelemetryRepository`
- `CommandRepository` — include `findPendingCommands(deviceId)` e `markAsSent(id)`
- `DeviceSettingsRepository`
- `DeviceStatisticsRepository`
- `DeviceLocationRepository`

#### SQL Server (`@ConditionalOnProperty(database.type=sqlserver)`)
- `SqlServerTelemetryRepository` — delega a `TelemetryJpaRepository`
- `SqlServerCommandRepository` — delega a `CommandJpaRepository`
- `SqlServerDeviceSettingsRepository` — delega a `DeviceSettingsJpaRepository`
- `SqlServerDeviceStatisticsRepository` — delega a `DeviceStatisticsJpaRepository`
- `SqlServerDeviceLocationRepository` — delega a `DeviceLocationJpaRepository`
- JPA interfaces corrispondenti estendono `JpaRepository`

#### MongoDB (opzionale)
- `MongoTelemetryRepository` — delega a `TelemetryMongoRepository`
- `MongoCommandRepository` — delega a `CommandMongoRepository`

---

### `dto/`

| Classe | Uso |
|--------|-----|
| `CommandCreateRequest` | Body della POST `/api/commands` |
| `CommandResponse` | Risposta della GET `/api/commands/{id}` |

---

### `config/`

| Classe | Funzione |
|--------|---------|
| `SchedulerConfig` | `@EnableScheduling` per il cleanup schedulato |
| `tcp/TcpServerProperties` | Binding `@ConfigurationProperties(prefix = "tcp.server")` con: `port` (default 8091), `timeout` (default 10000 ms), `maxConnections` (default 10000), `backlog` (default 1024) |

---

### `utils/`

#### `ControllerUtils`
- `concatenateCommands(List<String> hexCommands)` → stringa comandi separati da virgola
- `hexToAscii(String hex)` → converte HEX in ASCII
- `hexStringToByteArray(String hex)` → converte HEX in `byte[]`

---

### `exception/`

| Classe | Uso |
|--------|-----|
| `DecodingException` | Errore durante la decodifica del payload |
| `EncodingException` | Errore durante l'encoding di un comando |
| `UnknownDeviceException` | Device type non riconosciuto |

---

## 5. Protocollo Binario TEK

### Struttura del messaggio

Ogni messaggio è composto da un **header fisso di 17 byte** seguito da un **body** di lunghezza variabile dichiarata in byte 15-16.

```
Offset  Len  Campo
──────  ───  ──────────────────────────────────────────────────
[0]      1   Product Type Code (vedi tabella sezione 10)
[1]      1   Hardware Revision: (byte & 0x07) + "." + ((byte >> 3) & 0x1F)
[2]      1   Firmware Revision: (byte & 0x1F) + "." + ((byte >> 5) & 0x07)
[3]      1   Contact Reason (bitmap, vedi sotto)
[4]      1   Alarm Status + Last Reset (bitmap, vedi sotto)
[5]      1   Signal Strength (RSSI o CSQ in base al product type)
[6]      1   Diagnostic Info (bits) + Battery Status (bits 4:0)
[7-14]   8   IMEI (BCD, 15 cifre, skip leading zero)
[15]     1   Message Type (bits 5:0) + body length high (bits 7:6)
[16]     1   Body length low (body length = ((byte[15]>>6)&0x03)*256 + (byte[16]&0xFF))
──────────── BODY (da byte [17] in poi) ─────────────────────
[17-18]  2   Message count (diagnosticData, msgType 4/8/9)
[19]     1   Try attempts remaining (bits 7:5) + RTC hours (bits 4:0)
[20-21]  2   Energy used last contact (mAs) o last error code (dipende da FW)
[22]     1   (riservato)
[23]     1   Logger speed config (msgType 4: S0 value; msgType 8/9: bit 7 = sampling period)
[24]     1   (riservato)
[25]     1   RTC minutes
[26+]       Measurement data (fino a 28 misurazioni, 4 byte ciascuna)
```

### Contact Reason — byte[3] (bitmap)

| Bit | Maschera | Campo         |
|-----|----------|---------------|
| 0   | 0x01     | Scheduled     |
| 1   | 0x02     | Alarm         |
| 2   | 0x04     | Server Request|
| 3   | 0x08     | Manual        |
| 4   | 0x10     | Reboot        |
| 5   | 0x20     | TSP Requested |
| 6   | 0x40     | Dynamic1      |
| 7   | 0x80     | Dynamic2      |

### Alarm Status — byte[4] (bitmap)

| Bit | Maschera | Campo    |
|-----|----------|----------|
| 0   | 0x01     | Static1  |
| 1   | 0x02     | Static2  |
| 2   | 0x04     | Static3  |
| 3   | 0x08     | Bund     |
| 7   | 0x80     | Active   |

### Last Reset — byte[4] (bitmap)

| Bit | Maschera | Campo    |
|-----|----------|----------|
| 5   | 0x20     | Watchdog |
| 6   | 0x40     | Brownout |

### Signal Strength — byte[5]

| Product Types | Campo |
|---------------|-------|
| TEK586, TEK733, TEK643, TEK733A (codes 2,5,6,9) | RSSI |
| TEK811, TEK822V1, TEK871, TEK811A, TEK822V1BTN, TEK822V2, TEK900, TEK880, TEK898V2, TEK898V1 (codes 7,8,10,11,23,24,25,26,27,28) | CSQ |

### Battery Status — byte[6] (bits 4:0)

| Product Types | Formula |
|---------------|---------|
| TEK643, TEK822V1, TEK871, TEK822V1BTN, TEK822V2, TEK898V2, TEK898V1 (codes 6,8,10,23,24,27,28) | `percentage = (value * 100.0) / 31.0` |
| TEK586, TEK733, TEK733A, TEK811, TEK811A, TEK900, TEK880 (codes 2,5,9,7,11,25,26) | `voltage = (value + 30.0) / 10.0` V |

### Diagnostic Info — byte[6]

| Bit | Maschera | Campo      |
|-----|----------|------------|
| 5   | 0x20     | RTC Set    |
| 6   | 0x40     | LTE Active |

### Decodifica IMEI

```
byte[7..14] contiene l'IMEI in formato BCD packed (8 byte = 16 nibble = 15 cifre + 1 nibble leading zero)
Algoritmo:
  for i in 7..14:
    append (payload[i] >> 4) & 0x0F
    append  payload[i] & 0x0F
  result = joined_string.substring(1)   // rimuovi lo zero iniziale
```

### Decodifica Message Type

```
msgType = payload[15] & 0x3F   // 6 bit meno significativi
```

### Tipi di messaggio

| Tipo | Descrizione | Tabella DB |
|------|-------------|-----------|
| 4    | Telemetria standard (Logger Speed configurabile) | TELEMETRY_DATA |
| 8    | Telemetria alarm (buffer 10 misurazioni a 1 sec o 15 min) | TELEMETRY_DATA |
| 9    | Telemetria scheduled (1 min o 15 min) | TELEMETRY_DATA |
| 6    | Settings del device (risposta a REQUEST_SETTINGS) | DEVICE_SETTINGS |
| 16   | ICCID + Statistiche (risposta a REQUEST_STATUS) | DEVICE_STATISTICS |
| 17   | Dati GPS (risposta a REQUEST_GPS) | DEVICE_LOCATIONS |

### Measurement Data (msgType 4/8/9)

Ogni misurazione occupa **4 byte** a partire da byte[26], per un massimo di 28 misurazioni:

```
Offset (relativo all'inizio misurazione)  Campo
[0]  bits 3:0  → Sonic RSSI
[1]  bits 6:0  → Temperatura: (value / 2.0) - 30.0 °C
[2]  bits 5:2  → Sonic Source (4 bit)
[2]  bits 1:0  → Distance high byte (2 bit MSB)
[3]  bits 7:0  → Distance low byte
```

- `distanceCm = ((byte[j+2] & 0x03) << 8) | (byte[j+3] & 0xFF)`
- `temperatureF = (temperatureC * 9/5) + 32`
- Le misurazioni con tutti e 4 i byte a zero vengono ignorate.

### Calcolo Logger Speed (msgType 4/8/9)

| msgType | Condizione | Logger Speed |
|---------|-----------|--------------|
| 8 | ContactReason.manual == true | 1 secondo |
| 8 | byte[23] & 0x80 == 0 | 1 minuto |
| 8 | byte[23] & 0x80 != 0 | 15 minuti |
| 9 | byte[23] & 0x80 == 0 | 1 minuto |
| 9 | byte[23] & 0x80 != 0 | 15 minuti |
| 4 | byte[23] & 0x7F != 0 | `(byte[23] & 0x7F) * 15 * 60 * 1000` ms |
| 4 | byte[23] & 0x7F == 0, bit7 == 0 | 1 minuto |
| 4 | byte[23] & 0x7F == 0, bit7 != 0 | 15 minuti |

### Gestione attraversamento mezzanotte

L'ora/minuto nel payload provengono dal RTC del device, la data dal server.  
Se `baseTimestamp - serverTime > 12h` → il device ha misurato il giorno precedente → si sottrae 1 giorno al timestamp base.

---

## 6. Encoder e Comandi

### Descrizione

Il `Tek822Encoder` codifica i comandi in stringhe ASCII nel formato `<PASSWORD>,<REGISTRO>=<VALORE>` e poi le converte in HEX (ogni carattere ASCII → 2 cifre hex).

**Password default**: `TEK822`

**Auto-append REBOOT**: Se la lista comandi contiene almeno un S-command (registro S), l'encoder aggiunge automaticamente un comando `R3=ACTIVE` in coda se non già presente. Questo è necessario perché le modifiche ai registri S diventano effettive solo dopo un reboot.

**Concatenazione**: Più comandi vengono concatenati con virgola: `CMD1_HEX,CMD2_HEX,...`

### 17 Command Types

| Command Type | Registro | Formato ASCII | Note |
|---|---|---|---|
| `SET_INTERVAL` | S0 | `TEK822,S0=XX` | `XX = (128 * samplingPeriod) + (interval * 4)` hex |
| `SET_LISTEN` | S1 | `TEK822,S1=XX` | `XX = listenMinutes / 5` |
| `SET_SCHEDULE` | S2 | `TEK822,S2=7F2000` | Default: tutti i giorni, ore 8:00 |
| `REBOOT` | R3 | `TEK822,R3=ACTIVE` | Auto-appended per S-commands |
| `REQUEST_STATUS` | R6 | `TEK822,R6=02` | Risposta: Msg#16 (ICCID + stats) |
| `SET_ALARM_THRESHOLD` | S4/S5/S6 | `TEK822,S4=XXXX` | `XXXX = threshold + hysteresis*(2^10) + enabled*(2^14) + polarity*(2^15)` |
| `SHUTDOWN` | R1 | `TEK822,R1=80` | Spegne modem |
| `SET_RTC` | R2 | `TEK822,R2=yy/MM/dd:HH/mm/ss` | Imposta orologio |
| `DEACTIVATE` | R4 | `TEK822,R4=DEACT` | Disattiva upload schedulati |
| `CLOSE_TCP` | R6 | `TEK822,R6=03` | Chiude connessione TCP |
| `REQUEST_GPS` | R7 | `TEK822,R7=3C` | Risposta: Msg#17. Default timeout 60s (0x3C) |
| `REQUEST_SETTINGS` | R1 | `TEK822,R1=02/04/08` | Risposta: Msg#6. 02=da S0, 04=da S12, 08=da S19 |
| `RESET_RTC` | R1 | `TEK822,R1=10` | Forza risincronizzazione clock |
| `REQUEST_BUFFER_DATA` | R1 | `TEK822,R1=20` | Invia buffer dati (Msg#8, 10 misurazioni) |
| `REQUEST_DIAGNOSTIC_DATA` | R6 | `TEK822,R6=01` | Dati diagnostici (segnale, batteria, temp.) |
| `SET_APN` | S12/S13/S14 | `TEK822,S12=apn,S13=user,S14=pass` | S-command → auto-REBOOT |
| `SET_SERVER` | S15/S16 | `TEK822,S15=ip,S16=port` | S-command → auto-REBOOT |

### S-Commands (richiedono REBOOT)

I seguenti command types modificano registri S e richiedono `R3=ACTIVE` per applicare le modifiche:

```
SET_INTERVAL, SET_LISTEN, SET_SCHEDULE, SET_ALARM_THRESHOLD, SET_APN, SET_SERVER
```

### Conversione ASCII → HEX

```java
byte[] bytes = ascii.getBytes(StandardCharsets.US_ASCII);
StringBuilder hex = new StringBuilder();
for (byte b : bytes) {
    hex.append(String.format("%02X", b));
}
```

**Esempio**: `TEK822,R6=02` → `54454B3832322C52363D3032`

---

## 7. Database

### Panoramica

Il database SQL Server (`oneGasDB`) contiene 5 tabelle principali. Lo schema è gestito con `ddl-auto=validate` (Hibernate non modifica lo schema, si aspetta che esista già).

### Relazioni

```
TELEMETRY_DATA ||--o{ DEVICE_COMMANDS : "device_id"
```

### Tabella: TELEMETRY_DATA

| Colonna | Tipo SQL Server | Note |
|---------|----------------|------|
| `id` | `bigint` PK | Auto-increment |
| `device_id` | `nvarchar` | IMEI del device |
| `device_type` | `nvarchar` | Es. "TEK822V1" |
| `raw_message` | `nvarchar` | Hex string del payload grezzo |
| `decoded_data` | `nvarchar` | JSON del DecodedMessage |
| `received_at` | `datetime2` | Timestamp ricezione |
| `processed_at` | `datetime2` | Timestamp elaborazione |
| `imei` | `nvarchar` | IMEI (ridondante con device_id) |
| `firmware_version` | `nvarchar` | Es. "1.3" |
| `battery_voltage` | `float` | Volt (se applicabile) |
| `battery_percentage` | `float` | % (se applicabile) |
| `signal_strength` | `int` | RSSI o CSQ |
| `message_type` | `nvarchar` | Es. "4", "8", "9" |
| `measurement_count` | `int` | Numero misurazioni nel messaggio |

**Indici**:
- `idx_device_id` su `device_id`
- `idx_device_type` su `device_type`
- `idx_received_at` su `received_at`
- `idx_imei` su `imei`

### Tabella: DEVICE_COMMANDS

| Colonna | Tipo SQL Server | Note |
|---------|----------------|------|
| `id` | `bigint` PK | Auto-increment |
| `device_id` | `nvarchar` FK | Riferimento a TELEMETRY_DATA.device_id |
| `device_type` | `nvarchar` | |
| `command_type` | `nvarchar` | Uno dei 17 command types |
| `command_params` | `nvarchar` | JSON con parametri |
| `status` | `nvarchar` | PENDING / SENT / DELIVERED / FAILED / EXPIRED |
| `created_at` | `datetime2` | |
| `sent_at` | `datetime2` | Timestamp invio al device |
| `delivered_at` | `datetime2` | Timestamp conferma delivery |
| `error_message` | `nvarchar` | Messaggio di errore (se FAILED) |
| `retry_count` | `int` | Tentativi effettuati |
| `max_retries` | `int` | Tentativi massimi |

**Indici**:
- `idx_device_status` su `(device_id, status)`
- `idx_created_at` su `created_at`

**Enum CommandStatus**:
```
PENDING → SENT → DELIVERED
               ↘ FAILED
PENDING → EXPIRED
```

### Tabella: DEVICE_SETTINGS

| Colonna | Tipo SQL Server | Note |
|---------|----------------|------|
| `id` | `bigint` PK | |
| `device_id` | `nvarchar` | |
| `device_type` | `nvarchar` | |
| `raw_message` | `nvarchar` | Hex payload |
| `settings_json` | `nvarchar` | JSON dei settings S0..S19 |
| `received_at` | `datetime2` | |

**Indici**: `idx_ds_device_id`, `idx_ds_received_at`

### Tabella: DEVICE_STATISTICS

| Colonna | Tipo SQL Server | Note |
|---------|----------------|------|
| `id` | `bigint` PK | |
| `device_id` | `nvarchar` | |
| `device_type` | `nvarchar` | |
| `raw_message` | `nvarchar` | |
| `iccid` | `nvarchar` | SIM card identifier |
| `energy_used` | `bigint` | mAs totale |
| `min_temperature` | `int` | |
| `max_temperature` | `int` | |
| `message_count` | `int` | |
| `delivery_fail_count` | `int` | |
| `total_send_time` | `bigint` | ms |
| `max_send_time` | `bigint` | ms |
| `min_send_time` | `bigint` | ms |
| `rssi_total` | `bigint` | |
| `rssi_valid_count` | `int` | |
| `rssi_fail_count` | `int` | |
| `average_send_time` | `float` | ms |
| `average_rssi` | `float` | |
| `delivery_success_rate` | `float` | % |
| `received_at` | `datetime2` | |

**Indici**: `idx_dst_device_id`, `idx_dst_received_at`

### Tabella: DEVICE_LOCATIONS

| Colonna | Tipo SQL Server | Note |
|---------|----------------|------|
| `id` | `bigint` PK | |
| `device_id` | `nvarchar` | |
| `device_type` | `nvarchar` | |
| `raw_message` | `nvarchar` | |
| `latitude` | `float` | Decimale |
| `longitude` | `float` | Decimale |
| `latitude_raw` | `nvarchar` | Formato NMEA |
| `longitude_raw` | `nvarchar` | Formato NMEA |
| `altitude` | `float` | Metri |
| `speed_kmh` | `float` | |
| `speed_knots` | `float` | |
| `ground_heading` | `float` | Gradi |
| `horizontal_precision` | `float` | HDOP |
| `utc_time` | `time` | |
| `gps_date` | `nvarchar` | |
| `number_of_satellites` | `int` | |
| `time_to_fix_seconds` | `int` | |
| `gnss_positioning_mode` | `int` | |
| `received_at` | `datetime2` | |

**Indici**: `idx_dl_device_id`, `idx_dl_received_at`

---

## 8. API REST

### `GET /status`

Ritorna lo stato dell'applicazione.

**Response 200**:
```json
{
  "status": "UP",
  "application": "oneGas_Meteor"
}
```

---

### `GET /health`

Health check dell'applicazione.

**Response 200**:
```json
{
  "status": "healthy"
}
```

---

### `GET /process`

Trigger manuale di elaborazione (utile per test).

---

### `POST /api/commands`

Crea un nuovo comando da inviare al device.

**Request Body**:
```json
{
  "deviceId": "123456789012345",
  "deviceType": "TEK822V1",
  "commandType": "SET_INTERVAL",
  "parameters": {
    "interval": 4.0,
    "samplingPeriod": 1
  }
}
```

**Response 200**:
```json
{
  "id": 42,
  "deviceId": "123456789012345",
  "deviceType": "TEK822V1",
  "commandType": "SET_INTERVAL",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Response 400** (validazione fallita):
```json
{
  "error": "Invalid command type: INVALID_CMD",
  "validTypes": ["SET_INTERVAL", "SET_LISTEN", "..."]
}
```

---

### `GET /api/commands/{id}`

Dettaglio di un comando specifico.

**Path param**: `id` — ID del comando

**Response 200**:
```json
{
  "id": 42,
  "deviceId": "123456789012345",
  "deviceType": "TEK822V1",
  "commandType": "SET_INTERVAL",
  "status": "SENT",
  "createdAt": "2024-01-15T10:30:00",
  "sentAt": "2024-01-15T10:35:00"
}
```

---

### `GET /api/commands/device/{deviceId}`

Lista comandi pendenti per un device.

**Path param**: `deviceId` — IMEI del device

**Response 200**:
```json
[
  {
    "id": 42,
    "commandType": "SET_INTERVAL",
    "status": "PENDING"
  }
]
```

---

### `POST /admin/cleanup/trigger`

*(Solo se `cleanup.enabled=true`)*  
Trigger manuale del cleanup completo.

**Response 200**:
```json
{
  "message": "Cleanup triggered successfully"
}
```

---

### `POST /admin/cleanup/telemetry`

*(Solo se `cleanup.enabled=true`)*  
Pulizia solo della telemetria (rispetta `cleanup.telemetry.retention.days`).

---

### `POST /admin/cleanup/commands`

*(Solo se `cleanup.enabled=true`)*  
Pulizia solo dei comandi (rispetta `cleanup.commands.retention.days`).

---

### `GET /admin/cleanup/status`

*(Solo se `cleanup.enabled=true`)*  
Stato del servizio cleanup.

**Response 200**:
```json
{
  "enabled": true,
  "lastRun": "2024-01-15T02:00:00",
  "telemetryRetentionDays": 30,
  "commandsRetentionDays": 7
}
```

---

## 9. Configurazione

### `application.properties` — tutti i parametri

| Property | Default / Valore | Variabile d'ambiente | Descrizione |
|----------|-----------------|---------------------|-------------|
| `spring.application.name` | `oneGas_Meteor` | — | Nome applicazione |
| `server.port` | `8081` | `ONE_GAS_METEOR_SERVER_PORT` | Porta HTTP REST |
| `tcp.server.port` | `8091` | `ONE_GAS_METEOR_TCP_SERVER_PORT` | Porta server TCP |
| `tcp.server.max-connections` | `10000` | — | Numero massimo di connessioni TCP concorrenti (Semaphore permits) |
| `tcp.server.backlog` | `1024` | — | Dimensione coda connessioni in attesa del `ServerSocket` |
| `device.enabled.types` | `TEK822V1,TEK822V2,TEK586` | — | Device types abilitati (comma-separated) |
| `database.type` | `sqlserver` | — | Tipo DB: `sqlserver` o `mongodb` |
| `spring.datasource.url` | `jdbc:sqlserver://localhost:1433;databaseName=oneGasDB;encrypt=false;trustServerCertificate=true` | — | URL SQL Server |
| `spring.datasource.username` | *(obbligatorio)* | `SQL_DB_USERNAME` | Username SQL Server |
| `spring.datasource.password` | *(obbligatorio)* | `SQL_DB_PASSWORD` | Password SQL Server |
| `spring.datasource.driver-class-name` | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | — | Driver JDBC |
| `spring.jpa.hibernate.ddl-auto` | `validate` | — | Hibernate DDL: validate (non modifica lo schema) |
| `spring.jpa.show-sql` | `true` | — | Log SQL queries |
| `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.SQLServerDialect` | — | Dialetto Hibernate |
| `spring.datasource.hikari.maximum-pool-size` | `100` | — | Numero massimo di connessioni nel pool HikariCP |
| `spring.datasource.hikari.minimum-idle` | `10` | — | Connessioni minime mantenute idle nel pool HikariCP |
| `spring.datasource.hikari.connection-timeout` | `10000` | — | Timeout (ms) per ottenere una connessione dal pool HikariCP |
| `command.max.per.response` | `10` | — | Max comandi per risposta TCP |
| `telemetry.processing.timeout` | `5000` | — | Timeout elaborazione messaggio (ms) |
| `cleanup.enabled` | `false` | — | Abilita cleanup automatico |
| `cleanup.cron` | `0 0 2 * * *` | — | Cron del cleanup (ogni notte alle 2:00) |
| `cleanup.telemetry.retention.days` | `30` | — | Giorni di retention telemetria |
| `cleanup.commands.retention.days` | `7` | — | Giorni di retention comandi |
| `logging.level.com.aton.proj.oneGasMeteor` | `DEBUG` | — | Log level applicazione |
| `logging.level.org.springframework.web` | `INFO` | — | Log level Spring Web |
| `logging.config` | `classpath:log4j2.properties` | — | Config Log4j2 |

### Configurazione MongoDB (opzionale — commentata nel file)

```properties
# database.type=mongodb
# spring.data.mongodb.uri=mongodb://admin:password@localhost:27017/oneGasDB?authSource=admin
# spring.data.mongodb.database=oneGasDB
# spring.autoconfigure.exclude=\
#   org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
#   org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

### Variabili d'ambiente richieste

| Variabile | Tipo | Note |
|-----------|------|------|
| `SQL_DB_USERNAME` | Obbligatoria | Username per SQL Server |
| `SQL_DB_PASSWORD` | Obbligatoria | Password per SQL Server |
| `ONE_GAS_METEOR_SERVER_PORT` | Opzionale | Default 8081 |
| `ONE_GAS_METEOR_TCP_SERVER_PORT` | Opzionale | Default 8091 |

---

## 10. Dispositivi Supportati

La famiglia Tekelek TEK822 e dispositivi compatibili. Il product type è codificato in **byte[0]** del payload binario.

| Codice (byte[0]) | Device Type | Signal | Battery |
|:-:|---|---|---|
| 2 | TEK586 | RSSI | Voltage |
| 5 | TEK733 | RSSI | Voltage |
| 6 | TEK643 | RSSI | Percentage |
| 7 | TEK811 | CSQ | Voltage |
| 8 | TEK822V1 | CSQ | Percentage |
| 9 | TEK733A | RSSI | Voltage |
| 10 | TEK871 | CSQ | Percentage |
| 11 | TEK811A | CSQ | Voltage |
| 23 | TEK822V1BTN | CSQ | Percentage |
| 24 | TEK822V2 | CSQ | Percentage |
| 25 | TEK900 | CSQ | Voltage |
| 26 | TEK880 | CSQ | Voltage |
| 27 | TEK898V2 | CSQ | Percentage |
| 28 | TEK898V1 | CSQ | Percentage |

> **Nota**: I device types abilitati per l'invio di comandi sono configurati in `device.enabled.types` (default: `TEK822V1,TEK822V2,TEK586`). Tutti i 14 tipi sono supportati per la decodifica dei messaggi in entrata.

---

## 11. Pattern di Estensibilità

Il progetto usa il **Strategy Pattern** per decoder ed encoder, con Spring `@Order` per la priorità.

### Come aggiungere un nuovo Decoder (nuovo device type)

1. **Crea la classe decoder**:

```java
@Component
@Order(2)   // priorità dopo Tek822Decoder (@Order(1))
public class MioNuovoDecoder implements DeviceDecoder {

    @Override
    public boolean canDecode(byte[] payload) {
        // Controlla il product type nel byte[0]
        int productType = payload[0] & 0xFF;
        return productType == 99;   // es: nuovo codice 99
    }

    @Override
    public DecodedMessage decode(TelemetryMessage message) {
        DecodedMessage decoded = new DecodedMessage();
        // ... logica di decodifica ...
        return decoded;
    }

    @Override
    public List<String> getSupportedDeviceTypes() {
        return List.of("NUOVOTIPO");
    }

    @Override
    public String getDecoderName() {
        return "MioNuovoDecoder";
    }
}
```

2. **Spring lo rileva automaticamente** grazie a `@Component`. Il `DecoderFactory` lo include nell'elenco dei decoder ordinati.

### Come aggiungere un nuovo Encoder (nuovo device type)

1. **Crea la classe encoder**:

```java
@Component
@Order(2)   // priorità dopo Tek822Encoder (@Order(1))
public class MioNuovoEncoder implements DeviceEncoder {

    @Override
    public boolean canEncode(String deviceType) {
        return "NUOVOTIPO".equals(deviceType);
    }

    @Override
    public List<TelemetryResponse.EncodedCommand> encode(List<DeviceCommand> commands) {
        List<TelemetryResponse.EncodedCommand> result = new ArrayList<>();
        for (DeviceCommand command : commands) {
            // ... logica di encoding ...
            TelemetryResponse.EncodedCommand encoded = new TelemetryResponse.EncodedCommand();
            encoded.setCommandId(command.getId());
            encoded.setCommandType(command.getCommandType());
            encoded.setEncodedData(/* hex string */);
            encoded.setAsciiData(/* ascii string */);
            result.add(encoded);
        }
        return result;
    }

    @Override
    public List<String> getSupportedDeviceTypes() {
        return List.of("NUOVOTIPO");
    }

    @Override
    public String getEncoderName() {
        return "MioNuovoEncoder";
    }
}
```

### Come aggiungere un nuovo Command Type all'encoder esistente

1. Aggiungere la costante in `Tek822Encoder`:
   ```java
   public static final String CMD_NUOVO = "NUOVO_TIPO";
   ```
2. Aggiungere il case nello `switch` di `encodeCommand()`:
   ```java
   case CMD_NUOVO -> encodeNuovo(password, command);
   ```
3. Implementare il metodo `encodeNuovo()`.
4. Se è un S-command (registro S), aggiungere la costante in `S_COMMAND_TYPES`.
5. Aggiornare `REQUIRED_PARAMS` in `CommandService` con i parametri obbligatori.

### Come aggiungere supporto per un nuovo database

1. Creare l'implementazione del repository:
   ```java
   @Component
   @ConditionalOnProperty(name = "database.type", havingValue = "nuovodb")
   public class NuovoDbTelemetryRepository implements TelemetryRepository {
       // ...
   }
   ```
2. Aggiungere la configurazione in `application.properties`:
   ```properties
   database.type=nuovodb
   ```

---

## 12. Test

La suite di test è situata in `src/test/java/com/aton/proj/oneGasMeteor/`.

### Test esistenti

#### `decoder/DecoderFactoryTest`
- Verifica che `DecoderFactory` selezioni il decoder corretto in base al payload
- Testa la selezione di `Tek822Decoder` per payload con product type noti
- Testa il fallback a `UnknownDeviceDecoder` per product type sconosciuti

#### `decoder/TekMessageDecoderTest`
- Test decodifica del **product type** (byte[0]) per tutti i 14 tipi supportati
- Test decodifica delle **versioni** hardware e firmware (byte[1], byte[2])
- Test decodifica dell'**IMEI** (byte[7..14], BCD packed)
- Test decodifica **signal strength** (RSSI vs CSQ per diversi product types)
- Test decodifica **battery status** (voltage vs percentage per diversi product types)

#### `decoder/TekMessageDecoderMeasurementTest`
- Test decodifica delle **misurazioni** (msgType 4/8/9)
- Test calcolo timestamp con gestione **midnight crossing** (attraversamento mezzanotte)
- Test calcolo logger speed per i diversi msgType (4, 8, 9)
- Test filtraggio misurazioni void (tutti 4 byte a zero)

#### `encoder/impl/Tek822EncoderTest`
- Test `canEncode()` per tutti i 14 device types supportati
- Test encoding dei singoli command types (SET_INTERVAL, REBOOT, REQUEST_STATUS, ecc.)
- Test **auto-append REBOOT** per S-commands
- Test conversione `asciiToHex()`
- Test password default `TEK822`

#### `service/CommandServiceTest`
- Test validazione **deviceType** (deve essere in `device.enabled.types`)
- Test validazione **commandType** (deve essere uno dei 17 validi)
- Test validazione **parametri obbligatori** per ogni command type
- Test rifiuto di parametri mancanti

#### `OneGasMeteorApplicationTests`
- Test di avvio del contesto Spring (smoke test)

### Eseguire i test

```bash
./mvnw test
./mvnw test -pl . -Dtest=Tek822EncoderTest
./mvnw test -pl . -Dtest=TekMessageDecoderTest,TekMessageDecoderMeasurementTest
```

---

## 13. Build & Run

### Prerequisiti

| Requisito | Versione minima |
|-----------|----------------|
| Java | 21 |
| Maven | incluso (wrapper `./mvnw`) |
| SQL Server | 2017+ (o Azure SQL) |

### Compilazione

```bash
./mvnw clean package
```

### Esecuzione

```bash
# Con variabili d'ambiente
export SQL_DB_USERNAME=myuser
export SQL_DB_PASSWORD=mypassword
./mvnw spring-boot:run

# Oppure con jar
java -jar target/oneGasMeteor-*.jar \
  -DSQL_DB_USERNAME=myuser \
  -DSQL_DB_PASSWORD=mypassword
```

### Override delle porte

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="\
    -DONE_GAS_METEOR_SERVER_PORT=9081 \
    -DONE_GAS_METEOR_TCP_SERVER_PORT=9091"
```

### Test

```bash
# Tutti i test
./mvnw test

# Test specifico
./mvnw test -Dtest=Tek822EncoderTest

# Skip test
./mvnw clean package -DskipTests
```

### Build Docker (esempio)

```dockerfile
FROM eclipse-temurin:21-jre
COPY target/oneGasMeteor-*.jar app.jar
EXPOSE 8081 8091
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t ongas-meteor .
docker run -p 8081:8081 -p 8091:8091 \
  -e SQL_DB_USERNAME=sa \
  -e SQL_DB_PASSWORD=YourPassword123 \
  ongas-meteor
```

---

## 14. Riferimenti alla Documentazione

Tutti i file di documentazione si trovano nella directory [`docs/`](../docs/) alla radice del progetto.

### Documenti testuali (Mermaid)

| File | Contenuto |
|------|-----------|
| [`docs/Database schema.txt`](../docs/Database%20schema.txt) | Diagramma ER Mermaid con le 5 tabelle del DB e le loro relazioni e indici |
| [`docs/Diagramma Architettura a strati Meteor.txt`](../docs/Diagramma%20Architettura%20a%20strati%20Meteor.txt) | Diagramma Mermaid `graph TB` con tutti i layer dell'applicazione |
| [`docs/Diagramma Pattern di estensibilità Meteor.txt`](../docs/Diagramma%20Pattern%20di%20estensibilità%20Meteor.txt) | Diagramma classi UML Mermaid con le interfacce Decoder/Encoder e le loro implementazioni |
| [`docs/Diagramma Solo Meteor.txt`](../docs/Diagramma%20Solo%20Meteor.txt) | Diagramma semplificato `Device → Meteor → Database` |

### Immagini SVG

| File | Contenuto |
|------|-----------|
| [`docs/Database schema.svg`](../docs/Database%20schema.svg) | Versione SVG del diagramma ER |
| [`docs/Diagramma Architettura a strati Meteor.svg`](../docs/Diagramma%20Architettura%20a%20strati%20Meteor.svg) | Versione SVG dell'architettura a strati |
| [`docs/Diagramma Pattern di estensibilità Meteor.svg`](../docs/Diagramma%20Pattern%20di%20estensibilità%20Meteor.svg) | Versione SVG del pattern di estensibilità |
| [`docs/Diagramma Solo Meteor.svg`](../docs/Diagramma%20Solo%20Meteor.svg) | Versione SVG del diagramma semplificato |

### Manuali e specifiche tecniche (binari)

| File | Contenuto |
|------|-----------|
| [`docs/9-5988-07 TEK 822 Logger NB-IoT_CAT-M1 User Manual.pdf`](../docs/9-5988-07%20TEK%20822%20Logger%20NB-IoT_CAT-M1%20User%20Manual.pdf) | Manuale utente Tekelek TEK822 NB-IoT/CAT-M1. Contiene: struttura pacchetto binario (sezione 2.2), registri di configurazione S0..S19 (sezione 3.20), comandi R1..R7 (sezione 3.21), protocollo di comunicazione |
| [`docs/CF-5018-20 cellular configuration and command v1.21.xlsm`](../docs/CF-5018-20%20cellular%20configuration%20and%20command%20v1.21.xlsm) | Excel fornitore con configurazione cellulare e comandi. Sheet "822 CC" con valori di configurazione (es. S2=7F2000), Sheet "Request Commands" con R1=10 (Reset RTC), R1=20 (Buffer Data) |

---

*Fine documento — onGas_Meteor context v1.1*
