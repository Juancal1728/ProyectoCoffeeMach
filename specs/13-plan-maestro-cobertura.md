# Spec 13 — Cobertura del Plan Maestro: verificacion de implementacion completa

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Rastrear la cobertura de cada paso del Plan Maestro contra las specs 08-12 y el codigo implementado. Confirmar que el flujo distribuido completo esta implementado end-to-end.

**Requirements:**
Este spec no agrega codigo nuevo. Es un manifiesto de verificacion.

---

## 2. Mapeo Plan Maestro -> Specs -> Estado

### Paso 1: Verificar que no hay cambios en Ice, settings ni build

| Item | Estado | Verificacion |
|---|---|---|
| `CoffeMach.ice` sin cambios | OK | Ya tiene `ServicioBodega`, `ServicioAbastecimiento`, `ServicioComLogistica`, enum `RecursoAbastecimiento` |
| `settings.gradle` sin cambios | OK | Ya incluye los 4 modulos |
| `build.gradle` iceHome configurable | OK | `iceHome` lee de propiedad de proyecto o usa `/opt/homebrew` por defecto |
| Nombres de enum sin guiones bajos | OK | Confirmado: `MONEDA100`, `KITREPARACION`, etc. |

### Paso 2: Modelo de alarmas pendientes en ServidorCentral

| Item | Spec | Estado |
|---|---|---|
| `AlarmaPendiente.java` (POJO serializable) | 08 Task 1 | Implementado |
| `AlarmasPendientesRepositorio.java` (Singleton standalone) | 08 Task 2 | Implementado |
| `guardarAlarma()`, `cerrarAlarma()`, `listarPendientes()` synchronized | 08 Task 2 | Implementado |
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
| `hayExistencias()`, `despachar()`, `recargar()`, `listar()` synchronized | 09 Task 2 | Implementado |
| `cargarInicial()` con 8 recursos y cantidades correctas | 09 Task 2 | Implementado |
| `BodegaService.java` (Servant Ice `ServicioBodega`) | 09 Task 3 | Implementado |
| `BodegaCentral.java` (main + Ice + consola daemon) | 09 Task 4 | Implementado |
| `BodegaCentral.cfg` (puerto 12347) | 09 Task 5 | Implementado |
| Repositorio NO hereda de `Repositorio<K,T>` | 09 | OK - standalone |

### Paso 7: Implementar cmLogistics

| Item | Spec | Estado |
|---|---|---|
| `TecnicoMantenimiento.java` (modelo operador) | 10 Task 1 | Implementado |
| `ZonaGeografica.java` (maquina asignada + fromString) | 10 Task 2 | Implementado |
| `ControlAlarma.java` (servicio de alarmas) | 10 Task 3 | Implementado |
| `PanelAlarmas.java` (JPanel reutilizable en controlAlarma/gui) | 10 Task 4 | Implementado |
| `LogisticaController.java` (orquestador, 3 proxies) | 10 Task 5 | Implementado |
| `InterfazLogistica.java` (JFrame con PanelAlarmas embebido) | 10 Task 6 | Implementado |
| `ControladorLogistica.java` (MVC controller + TecnicoMantenimiento) | 10 Task 7 | Implementado |
| `CmLogistics.java` (main + Ice + EDT launch) | 10 Task 8 | Implementado |
| `CmLogistic.cfg` (3 proxies configurados) | 10 Task 9 | Implementado |
| Flujo: hayExistencias -> despachar -> abastecerRecurso (callback) | 10 Task 3 | Implementado |

### Paso 8: Completar coffeeMach (ControladorMQ)

| Item | Spec | Estado |
|---|---|---|
| `abastecerRecurso()` (nuevo servant callback) | 11 Task 1 | Implementado |
| `aplicarAbastecimiento()` (logica comun) | 11 Task 2 | Implementado |
| `limpiarAlarmasDeRecurso(recurso)` (fix alarm ID mismatch) | 11 Task 3 | Implementado |
| Fix NPE en `cargarRecetaMaquinas()` (ingredientes.addElement) | 11 Task 4 | Implementado |
| `abastecer()` refactorizado a delegar | 11 Task 5 | Implementado |
| Helpers: `recursoDesdeAlarma()`, `cantidadDesdeRecurso()` | 11 Task 5 | Implementado |
| Bug fix: `\|` a `\|\|` en verificarMonedas | 11 Task 6 | Implementado |
| Bug fix: ventas clave unica con timestamp | 11 Task 6 | Implementado |
| Bug fix: `quemarCodMaquina()` archivo inexistente | 11 Task 6 | Implementado |
| `SwingUtilities.invokeLater` para actualizacion de GUI desde hilo Ice | 11 Task 2 | Implementado |
| `synchronized getInstance()` en los 5 repositorios de coffeeMach | 01 Task 4 | Implementado |
| `coffeMach.cfg` actualizado a localhost | 11 | Implementado |

### Paso 9: Implementar definirProducto en ServidorCentral

| Item | Spec | Estado |
|---|---|---|
| `definirProducto()` en `ProductoReceta.java` | 12 Task 1 | Implementado |
| `buscarIdIngredientePorNombre()` en `ManejadorDatos.java` | 12 Task 2 | Implementado |
| Fix null check en `consultarProductos()` | 12 Task 5 | Implementado |
| Fix `sta.getResultSet()` y `psx3/psx4.executeUpdate()` en `registrarAlarma()` | 12 Task 6 | Implementado |
| Try-with-resources en 4 metodos de `ManejadorDatos` | 12 Task 7 | Implementado |

### Paso 10: Consola de administracion configurable

| Item | Spec | Estado |
|---|---|---|
| `ServerControl` constructor lee property | 12 Task 3 | Implementado |
| `server.cfg` tiene `ConsolaAdministracion.enabled=false` | 12 Task 3 | Implementado |

### Paso 11: Codigo de plantilla del profesor (no eliminar)

| Item | Estado | Nota |
|---|---|---|
| `AlarmaServiceImp.java` (coffeeMach) | Conservado | Plantilla del profesor. `ControladorMQ` usa `AlarmaServicePrx` directamente (Broker) |
| `SuministroServiceImp.java` (coffeeMach) | Conservado | Plantilla del profesor. `ControladorMQ` maneja suministros internamente |
| `AlarmaService.java` (local interface coffeeMach) | Conservado | Plantilla del profesor. No afecta compilacion |
| `SuministroService.java` (local interface coffeeMach) | Conservado | Plantilla del profesor. No afecta compilacion |
| `bodega/Bodega.java`, `mantenimientoExistencias/Inventario.java` | Conservados | Plantilla del profesor en bodegaCentral |

### Configuracion final .cfg

| Archivo | Estado |
|---|---|
| `server.cfg` (ServidorCentral:12345 + BD + ConsolaAdmin=false) | OK |
| `coffeMach.cfg` (localhost:12346 + proxies localhost:12345) | OK |
| `CmLogistic.cfg` (3 proxies: 12345, 12346, 12347) | OK |
| `BodegaCentral.cfg` (localhost:12347) | OK |

---

## 3. Patrones confirmados en el codigo

| Patron | Evidencia |
|---|---|
| **Broker** | Proxies `*Prx`, adapters en `ServidorCentral.java`, `CoffeeMach.java`, `BodegaCentral.java`; `.cfg` files |
| **Callback** | `abastecerRecurso()` como callback de cmLogistics a coffeeMach; confirmacion a servidor via `recibirNotificacionAbastesimiento()` |
| **Secure Messaging** | `inicioSesion()` + filtro en `consultarAlarmasPendientes()` por `ASIGNACION_MAQUINA`; `TecnicoMantenimiento.isAutenticado()` en cmLogistics |
| **Repository + Singleton** | `AlarmasPendientesRepositorio`, `InventarioRepositorio` (standalone); `*Repositorio` en coffeeMach (base class); todos con `synchronized getInstance()` |
| **Controller (MVC)** | `LogisticaController`, `ControlAlarma`, `ControladorLogistica`, `AlarmasManager`, `ServerControl`, `ControladorMQ` |
| **DAO** | `ConexionBD` + `ManejadorDatos` con `PreparedStatement` y try-with-resources |

---

## 4. Estructura de paquetes cmLogistics (implementacion final)

```
cmLogistics/src/main/java/
  CmLogistics.java
  controlAlarma/
    ControlAlarma.java
    gui/
      PanelAlarmas.java
  tecnicoMantenimiento/
    TecnicoMantenimiento.java
  zonaGeografica/
    ZonaGeografica.java
  logistica/
    LogisticaController.java
  interfaz/
    InterfazLogistica.java
    ControladorLogistica.java
```
