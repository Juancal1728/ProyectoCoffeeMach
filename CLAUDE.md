# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A distributed coffee-vending-machine system built with **Java 11**, **Gradle 6.6**, **ZeroC Ice 3.7.6** for inter-process communication, and **PostgreSQL** for persistence. All source code lives under `Code-2023-04-14-3/coffeemach/`.

**Critical: Requires Java 11.** Gradle 6.6 is incompatible with JDK 17+. Set `JAVA_HOME` to a JDK 11 installation before building.

## Build & Run

All Gradle commands must be run from `Code-2023-04-14-3/coffeemach/`:

```bash
cd "Code-2023-04-14-3/coffeemach"

# Ensure Java 11
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"

# Build all subprojects
./gradlew clean build

# Build individual JARs
./gradlew :ServidorCentral:jar
./gradlew :coffeeMach:jar
./gradlew :cmLogistics:jar
./gradlew :bodegaCentral:jar
```

**Run order matters** — start components in this sequence:

```bash
# 1. PostgreSQL must already be running on port 5430
# 2. Central server (run from Code-2023-04-14-3/coffeemach/)
java -jar ServidorCentral/build/libs/ServidorCentral.jar
# 3. Warehouse
java -jar bodegaCentral/build/libs/BodegaCentral.jar
# 4. Coffee machine — codMaquina.cafe already exists in Code-2023-04-14-3/coffeemach/
#    Run from Code-2023-04-14-3/coffeemach/ so it finds codMaquina.cafe in the working dir
java -jar coffeeMach/build/libs/coffeeMach.jar
# 5. Logistics client
java -jar cmLogistics/build/libs/cmLogistics.jar
```

**Working directory for `.bd` files:** Each component writes its `.bd` persistence files relative to the JVM working directory at launch. Run all JARs from `Code-2023-04-14-3/coffeemach/` to keep state files co-located. Delete `.bd` files to reset persisted state.

## Database Setup

PostgreSQL must be set up before starting `ServidorCentral`:

```bash
psql -f Code-2023-04-14-3/scripts/postgres/coffeemach-user.sql
psql -d coffeemachine -U cofmachu -f Code-2023-04-14-3/scripts/postgres/coffeemach-ddl.sql
psql -d coffeemachine -U cofmachu -f Code-2023-04-14-3/scripts/postgres/coffeemach-inserts.sql
```

Default connection: `jdbc:postgresql://localhost:5430/coffeemachine`, user `cofmachu`, password `cofmachpwd` (configured in `ServidorCentral/src/main/resources/server.cfg`).

## Architecture

### Four Gradle subprojects

| Subproject | Role | UI |
|---|---|---|
| `ServidorCentral` | Central server — exposes Ice services, owns PostgreSQL | Swing (recipe admin) |
| `coffeeMach` | Coffee machine — Swing GUI, local `.bd` file state | Swing (vending) |
| `cmLogistics` | Logistics operator — coordinates alarm resolution | Console |
| `bodegaCentral` | Warehouse — manages supply inventory | Console |

### Shared contract: `CoffeMach.ice`

All service interfaces are defined in `CoffeMach.ice` at the root of `coffeemach/`. The Gradle Ice Builder plugin compiles this into Java stubs under each subproject's `build/generated-src/servicios/`. **Never edit generated files; edit `CoffeMach.ice`.**

**Enum `RecursoAbastecimiento` values have NO underscores:** `MONEDA100`, `MONEDA200`, `MONEDA500`, `AGUA`, `CAFE`, `AZUCAR`, `VASO`, `KITREPARACION`. All Java code must match these exact names.

### Ice endpoint topology and identities

```
coffeeMach  ──→  ServidorCentral:12345  (Alarmas, Ventas, Recetas)
cmLogistics ──→  ServidorCentral:12345  (logistica)
cmLogistics ──→  coffeeMach:12346       (abastecer / abastecerRecurso callback)
cmLogistics ──→  bodegaCentral:12347    (bodega)
```

| Identity | Component | Port |
|---|---|---|
| `Alarmas` | ServidorCentral | 12345 |
| `Ventas` | ServidorCentral | 12345 |
| `Recetas` | ServidorCentral | 12345 |
| `logistica` | ServidorCentral | 12345 |
| `abastecer` | coffeeMach | 12346 |
| `bodega` | bodegaCentral | 12347 |

Config files: `server.cfg`, `coffeMach.cfg`, `CmLogistic.cfg`, `BodegaCentral.cfg` — all under `src/main/resources/`. Update hostnames when deploying to a real network (default: `localhost`).

### Alarm resolution flow (Callback pattern)

This is the core distributed workflow:
1. `coffeeMach` detects low resource → calls `AlarmaServicePrx` on server
2. `ServidorCentral` records alarm in PostgreSQL + `AlarmasPendientesRepositorio` (`.bd` file)
3. `cmLogistics` operator logs in, queries pending alarms (filtered by assigned machines)
4. `cmLogistics` requests dispatch from `bodegaCentral` via `ServicioBodegaPrx`
5. `bodegaCentral` decrements inventory
6. `cmLogistics` calls `abastecerRecurso()` callback on `coffeeMach` via `ServicioAbastecimientoPrx`
7. `coffeeMach` applies restock locally, confirms to server
8. `ServidorCentral` closes the alarm

### Local state persistence (Repository + Singleton)

**coffeeMach** uses `Repositorio<K,T>` base class (`interfaces/Repositorio.java`) — serializes `HashMap` to `.bd` files via Java `ObjectOutputStream`. Files: `alarmas.bd`, `ingredientes.bd`, `monedas.bd`, `recetas.bd`, `ventas.bd`. Delete to reset state.

**bodegaCentral** uses standalone `InventarioRepositorio` → `inventarioBodega.bd`. **ServidorCentral** uses standalone `AlarmasPendientesRepositorio` → `alarmasPendientes.bd`. These are standalone singletons — they **cannot** extend `Repositorio<K,T>` because it lives in the `coffeeMach` module and Gradle subprojects compile independently.

**Serialization compatibility:** Changing field names, types, or package names of domain classes breaks deserialization of existing `.bd` files. Delete `.bd` files after such changes.

### coffeeMach specifics

- Machine ID read from `codMaquina.cafe` in the JVM working directory (plain text integer; file already present in `Code-2023-04-14-3/coffeemach/`)
- `ControladorMQ.java` is the central controller: Swing event wiring, Ice servant for `ServicioAbastecimiento`, alarm threshold monitoring
- `import com.zeroc.Ice.*` conflicts with `java.lang.Exception` — always use `java.lang.Exception` explicitly in catch blocks when wildcard-importing Ice

### ServidorCentral specifics

- `ConexionBD` reads JDBC credentials from Ice properties in `server.cfg`
- `ManejadorDatos` contains all SQL via `PreparedStatement`
- `adapter.activate()` must come BEFORE `controladorRecetas.run()` to avoid blocking Ice services
- `ConsolaAdministracion` launch controlled by `ConsolaAdministracion.enabled` property in `server.cfg`

### Alarm ID mapping (hardcoded in `ControladorMQ` and `ManejadorDatos.validarAlarma()`)

| ID | Meaning | ID | Meaning |
|---|---|---|---|
| 1 | Maintenance | 8 | Water low |
| 2 | 100-coin low | 9 | Coffee low |
| 3 | 100-coin critical | 10 | Sugar low |
| 4 | 200-coin low | 11 | Cup low |
| 5 | 200-coin critical | 12 | Water critical |
| 6 | 500-coin low | 13 | Coffee critical |
| 7 | 500-coin critical | 14 | Sugar critical |
| | | 15 | Cup critical |

### Wire formats

- `RecetaService.consultarProductos()` returns: `"recipeId-recipeName-price#ingredientId-ingredientName-unit-...-qty#..."`
- `VentaService.registrarVenta()` receives a `StringArr` where each element is `"recipeId#price"` (one entry per item sold)
- `AlarmaPendiente.toTransportString()` returns: `"idMaquina|idAlarma|recurso|cantidad|ubicacion|descripcion"` (pipe-delimited)

### Design patterns in use

| Pattern | Where |
|---|---|
| **Broker** | ZeroC Ice proxies (`*Prx`), adapters, `.cfg` config |
| **Callback** | Alarm resolution: machine→server→logistics→machine→server |
| **Secure Messaging** | `inicioSesion()` + filter by `ASIGNACION_MAQUINA` |
| **Repository + Singleton** | `*Repositorio` classes with `.bd` serialization |
| **Controller/Manager** | `LogisticaController`, `AlarmasManager`, `ServerControl` |
| **DAO** | `ConexionBD` + `ManejadorDatos` for PostgreSQL |

## Specs

Feature specifications live in `specs/` at the project root (outside `Code-2023-04-14-3/`). Index at `specs/00-index.md`. Specs 01–07 document pre-existing code; specs 08–12 track Second Part implementation; specs 13–14 cover verification and build/integration testing.

## No Tests

There are no automated tests in this project.