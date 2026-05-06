# Spec 08 — Subsistema de alarmas pendientes en ServidorCentral

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Mantener un registro local de alarmas pendientes en el ServidorCentral que permita a cmLogistics consultar y resolver alarmas sin depender exclusivamente de la base de datos relacional. Este subsistema actua como puente entre la notificacion de alarmas (Spec 03) y la resolucion por logistica (Spec 09).

**Users:**
- **ServidorCentral** (`Alarma` servant) registra alarmas pendientes al recibirlas de coffeeMach.
- **ControlComLogistica** (servant) expone `consultarAlarmasPendientes()` filtrado por operador.
- **cmLogistics** (cliente) consume las alarmas para resolverlas.

**Requirements:**
1. Cada alarma recibida por el servant `Alarma` se guarda tanto en PostgreSQL (tabla `ALARMA_MAQUINA`, existente) como en un repositorio local serializado (`alarmasPendientes.bd`).
2. Cada alarma pendiente registra: idMaquina, idAlarma, recurso (nombre del enum `RecursoAbastecimiento`), cantidad sugerida, ubicacion y descripcion.
3. `consultarAlarmasPendientes(codigoOperador)` retorna solo alarmas de maquinas asignadas al operador.
4. El formato de transporte es: `"idMaquina|idAlarma|recurso|cantidad|ubicacion|descripcion"`.
5. Al confirmar abastecimiento, la alarma se marca como resuelta en el repositorio local y se cierra en la BD.
6. El repositorio local usa patron Repository + Singleton standalone (no hereda de `Repositorio<K,T>` de coffeeMach ya que es otro modulo Gradle).

**Patron aplicado:** Repository + Singleton, Broker (Servant), Secure Messaging (filtro por operador).

**Edge cases:**
- Alarma duplicada (mismo idMaquina + idAlarma): se sobrescribe en el HashMap.
- Operador sin maquinas asignadas: retorna lista vacia.
- Archivo `alarmasPendientes.bd` corrupto: se imprime error, se arranca con mapa vacio.

**Acceptance criteria:**
- **Given** una alarma de escasez de cafe en maquina 1, **when** `Alarma.recibirNotificacionEscasezIngredientes("Cafe", 1)` se ejecuta, **then** existe una entrada en `AlarmasPendientesRepositorio` con recurso="CAFE" y cantidad=500.
- **Given** operador 1 tiene asignada maquina 1, **when** se llama `consultarAlarmasPendientes(1)`, **then** retorna la alarma de esa maquina en formato pipe-delimited.
- **Given** operador 2 NO tiene asignada maquina 1, **when** se llama `consultarAlarmasPendientes(2)`, **then** retorna lista vacia.
- **Given** alarma resuelta, **when** se llama `cerrarAlarma(1, ALARMA_INGREDIENTE)`, **then** `listarPendientes()` ya no incluye esa alarma.

---

## 2. Plan

**Architecture:**
- `AlarmaPendiente`: POJO serializable con campos descriptivos de la alarma.
- `AlarmasPendientesRepositorio`: Singleton standalone con HashMap, serializado a `alarmasPendientes.bd`.
- `Alarma` (servant existente): se modifica para llamar `registrarPendiente()` en cada notificacion.
- `ControlComLogistica` (servant existente): implementa `consultarAlarmasPendientes()` usando el repositorio + filtro por asignaciones del operador.

**Data model:**
- Archivo: `alarmasPendientes.bd` (HashMap<String, AlarmaPendiente> serializado).
- Clave: `"idMaquina-idAlarma"`.

**Mapeo de notificacion a recurso:**

| Notificacion | idAlarma (en BD) | Recurso | Cantidad |
|---|---|---|---|
| EscasezIngredientes (Agua/Cafe/Azucar) | 1 (INGREDIENTE) | AGUA/CAFE/AZUCAR | 500 |
| EscasezSuministro (Vaso) | 5 (SUMINISTRO) | VASO | 100 |
| InsuficienciaMoneda CIEN | 2 | MONEDA100 | 20 |
| InsuficienciaMoneda DOCIENTOS | 3 | MONEDA200 | 20 |
| InsuficienciaMoneda QUINIENTOS | 4 | MONEDA500 | 20 |
| MalFuncionamiento | 6 | KITREPARACION | 1 |

**Nombres de enum:** Los recursos usan los nombres del enum `RecursoAbastecimiento` en `CoffeMach.ice`: `MONEDA100`, `MONEDA200`, `MONEDA500`, `AGUA`, `CAFE`, `AZUCAR`, `VASO`, `KITREPARACION` (sin guiones bajos).

---

## 3. Tasks

```
Task 1: AlarmaPendiente.java
Depends on: none
What to build: POJO serializable con serialVersionUID, campos descriptivos y toTransportString().
Files: ServidorCentral/src/main/java/modelo/AlarmaPendiente.java

Task 2: AlarmasPendientesRepositorio.java
Depends on: Task 1
What to build: Singleton standalone con HashMap, guardar/cargar a alarmasPendientes.bd,
  metodos guardarAlarma(), cerrarAlarma(), listarPendientes().
Files: ServidorCentral/src/main/java/modelo/AlarmasPendientesRepositorio.java

Task 3: Modificar Alarma.java (servant)
Depends on: Task 2
What to build: Agregar registrarPendiente() y idAlarmaDesdeInsumo() como helpers.
  Modificar cada metodo de notificacion para llamar registrarPendiente().
  Corregir recibirNotificacionAbastesimiento() para cerrar alarma en repositorio.
Files: ServidorCentral/src/main/java/alarma/Alarma.java

Task 4: Implementar consultarAlarmasPendientes en ControlComLogistica
Depends on: Task 2
What to build: Consultar asignaciones del operador, filtrar alarmas pendientes por maquinas asignadas.
Files: ServidorCentral/src/main/java/comunicacion/ControlComLogistica.java
```
