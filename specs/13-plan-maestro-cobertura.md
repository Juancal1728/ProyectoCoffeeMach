# Spec 13 — Cobertura del Plan Maestro: verificacion de implementacion completa

> Estado: En verificacion.

---

## 1. Spec

**Purpose:** Rastrear la cobertura de cada paso del Plan Maestro contra las specs 08-12 y el codigo implementado. Identificar brechas y confirmar que el flujo distribuido completo funciona end-to-end.

**Requirements:**
Este spec no agrega codigo nuevo. Es un manifiesto de verificacion.

---

## 2. Mapeo Plan Maestro -> Specs -> Estado

### Paso 1: Verificar que no hay cambios en Ice, settings ni build

| Item | Estado | Verificacion |
|---|---|---|
| `CoffeMach.ice` sin cambios | OK | Ya tiene `ServicioBodega`, `ServicioAbastecimiento`, `ServicioComLogistica`, enum `RecursoAbastecimiento` |
| `settings.gradle` sin cambios | OK | Ya incluye los 4 modulos |
| `build.gradle` sin cambios | OK | Ya tiene `ice:3.7.6` y `postgresql:42.3.1` |
| Nombres de enum sin guiones bajos | OK | Confirmado: `MONEDA100`, `KITREPARACION`, etc. |

### Paso 2: Modelo de alarmas pendientes en ServidorCentral

| Item | Spec | Estado |
|---|---|---|
| `AlarmaPendiente.java` (POJO serializable) | 08 Task 1 | Implementado |
| `AlarmasPendientesRepositorio.java` (Singleton standalone) | 08 Task 2 | Implementado |
| Repositorio NO hereda de `Repositorio<K,T>` de coffeeMach | 08 | OK - standalone |

### Paso 3: Modificar Alarma.java en ServidorCentral

| Item | Spec | Estado |
|---|---|---|
| `registrarPendiente()` helper | 08 Task 3 | Implementado |
| `idAlarmaDesdeInsumo()` helper | 08 Task 3 | Implementado |
| `recibirNotificacionEscasezIngredientes()` -> registra pendiente | 08 Task 3 | Implementado |
| `recibirNotificacionInsuficienciaMoneda()` -> registra pendiente | 08 Task 3 | Implementado |
| `recibirNotificacionEscasezSuministro()` -> registra pendiente | 08 Task 3 | Implementado |
| `recibirNotificacionMalFuncionamiento()` -> registra pendiente | 08 Task 3 | Implementado |
| `recibirNotificacionAbastesimiento()` -> cierra alarma en repo + BD | 08 Task 3 | Implementado |

### Paso 4: Completar ControlComLogistica

| Item | Spec | Estado |
|---|---|---|
| `consultarAlarmasPendientes(codigoOperador)` | 08 Task 4 | Implementado |
| Filtro por maquinas asignadas (Secure Messaging) | 08 Task 4 | Implementado |
| Usa `control.listaAsignaciones()` + `AlarmasPendientesRepositorio` | 08 Task 4 | Implementado |

### Paso 5: Corregir orden en ServidorCentral.java

| Item | Spec | Estado |
|---|---|---|
| `adapter.activate()` ANTES de `controladorRecetas.run()` | 12 Task 4 | Implementado |

### Paso 6: Implementar bodegaCentral

| Item | Spec | Estado |
|---|---|---|
| `ItemInventario.java` (POJO serializable) | 09 Task 1 | Implementado |
| `InventarioRepositorio.java` (Singleton standalone) | 09 Task 2 | Implementado |
| `cargarInicial()` con 8 recursos y cantidades correctas | 09 Task 2 | Implementado |
| `BodegaService.java` (Servant Ice `ServicioBodega`) | 09 Task 3 | Implementado |
| `BodegaCentral.java` (main + Ice + consola daemon) | 09 Task 4 | Implementado |
| `BodegaCentral.cfg` (puerto 12347) | 09 Task 5 | Implementado |
| Repositorio NO hereda de `Repositorio<K,T>` | 09 | OK - standalone |

### Paso 7: Implementar cmLogistics

| Item | Spec | Estado |
|---|---|---|
| `LogisticaController.java` (3 proxies, coordina resolucion) | 10 Task 1 | Implementado |
| `CmLogistics.java` (main + 3 proxies Ice + consola) | 10 Task 2 | Implementado |
| `CmLogistic.cfg` (3 proxies configurados) | 10 Task 3 | Implementado |
| Flujo: hayExistencias -> despachar -> abastecerRecurso (callback) | 10 Task 1 | Implementado |

### Paso 8: Completar coffeeMach (ControladorMQ)

| Item | Spec | Estado |
|---|---|---|
| `abastecerRecurso()` (nuevo servant callback) | 11 Task 1 | Implementado |
| `aplicarAbastecimiento()` (logica comun) | 11 Task 2 | Implementado |
| `abastecer()` refactorizado a delegar | 11 Task 3 | Implementado |
| Helpers: `recursoDesdeAlarma()`, `cantidadDesdeRecurso()` | 11 Task 3 | Implementado |
| Bug fix: `\|` a `\|\|` en verificarMonedas | 11 Task 4 | Implementado |
| Bug fix: ventas clave unica con timestamp | 11 Task 4 | Implementado |
| Bug fix: `quemarCodMaquina()` archivo inexistente | 11 Task 4 | Implementado |
| Bug fix: `arrancarMaquina()` maneja codMaquina == -1 | 11 Task 4 | Implementado |
| `coffeMach.cfg` actualizado a localhost | 11 | Implementado |

### Paso 9: Implementar definirProducto en ServidorCentral

| Item | Spec | Estado |
|---|---|---|
| `definirProducto()` en `ProductoReceta.java` | 12 Task 1 | Implementado |
| `buscarIdIngredientePorNombre()` en `ManejadorDatos.java` | 12 Task 2 | Implementado |

### Paso 10: Consola de administracion configurable

| Item | Spec | Estado |
|---|---|---|
| `ServerControl` constructor lee property | 12 Task 3 | Implementado |
| `server.cfg` tiene `ConsolaAdministracion.enabled=false` | 12 Task 3 | Implementado |

### Paso 11: Interfaces locales no usadas

| Item | Estado | Nota |
|---|---|---|
| `AlarmaServiceImp.java` (coffeeMach) | Codigo muerto | Tiene `UnsupportedOperationException`. Decision: no implementar — `ControladorMQ` usa `AlarmaServicePrx` directamente (Broker) |
| `SuministroServiceImp.java` (coffeeMach) | Codigo muerto | Tiene `UnsupportedOperationException`. Decision: no implementar — `ControladorMQ` maneja suministros internamente |
| `AlarmaService.java` (local interface coffeeMach) | Codigo muerto | No es la interfaz Ice — es una interfaz local nunca consumida |
| `SuministroService.java` (local interface coffeeMach) | Codigo muerto | Nunca consumida |

### Configuracion final .cfg

| Archivo | Estado |
|---|---|
| `server.cfg` (ServidorCentral:12345 + BD + ConsolaAdmin) | OK |
| `coffeMach.cfg` (localhost:12346 + proxies localhost:12345) | OK |
| `CmLogistic.cfg` (3 proxies: 12345, 12346, 12347) | OK |
| `BodegaCentral.cfg` (localhost:12347) | OK |

---

## 3. Patrones confirmados en el codigo

| Patron | Evidencia |
|---|---|
| **Broker** | Proxies `*Prx`, adapters en `ServidorCentral.java`, `CoffeeMach.java`, `BodegaCentral.java`; `.cfg` files |
| **Callback** | `abastecerRecurso()` como callback de cmLogistics a coffeeMach; confirmacion a servidor |
| **Secure Messaging** | `inicioSesion()` + filtro en `consultarAlarmasPendientes()` por `ASIGNACION_MAQUINA` |
| **Repository + Singleton** | `AlarmasPendientesRepositorio`, `InventarioRepositorio` (standalone); `*Repositorio` en coffeeMach (base class) |
| **Controller/Manager** | `LogisticaController`, `AlarmasManager`, `ServerControl`, `ControladorMQ` |
| **DAO** | `ConexionBD` + `ManejadorDatos` con `PreparedStatement` |

---

## 4. Items pendientes (NO codigo - solo verificacion)

| # | Item | Bloqueador |
|---|---|---|
| 1 | **Build con Java 11** | Requiere JDK 11 instalado. Gradle 6.6 no soporta JDK 17+ |
| 2 | **Prueba end-to-end** | Requiere PostgreSQL corriendo en puerto 5430 |
| 3 | **com.zeroc.Ice.* vs java.lang.Exception** | Fijado en `BodegaCentral.java` con `java.lang.Exception` explicito |
| 4 | **Stubs existentes en bodegaCentral** | `Bodega.java`, `Inventario.java`, `Interfaz.java` no molestan pero son dead code |
