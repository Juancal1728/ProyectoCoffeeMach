# Spec 07 — Servicio de logística para operadores

> Estado: Implementado completamente. `consultarAlarmasPendientes()` implementado en Spec 08. Cliente cmLogistics con GUI Swing implementado en Spec 10. `existeOperador()` y `darOperador()` convertidos a try-with-resources en Spec 12.

---

## 1. Spec

**Purpose:** Exponer al operador de logística la información necesaria para gestionar sus máquinas asignadas: autenticación, lista de asignaciones y lista de máquinas con alarmas activas.

**Users:**
- **Operador de logística** se autentica y consulta sus máquinas asignadas y sus alarmas pendientes.
- **`cmLogistics`** (cliente Ice) es quien consumirá estos servicios — actualmente es un stub sin implementación.

**Requirements:**
1. El operador puede autenticarse con `(codigoOperador, password)`; el servidor retorna `true` si las credenciales existen en la tabla `OPERADORES`.
2. El operador puede listar todas las máquinas que tiene asignadas (`asignacionMaquina`), recibiendo una lista de strings `"idMaquina-ubicacion"`.
3. El operador puede listar solo las máquinas con alarmas activas (`asignacionMaquinasDesabastecidas`), recibiendo strings `"idMaquina#ubicacion#fechaIni#idAlarma#descripAlarma"`.
4. El servidor retorna solo las máquinas asignadas al operador que hace la consulta.
5. Las alarmas activas son aquellas con `FECHA_FINAL IS NULL` en `ALARMA_MAQUINA`.

**Edge cases:**
- Código de operador = 0 o contraseña nula: `existeOperador()` en `ServerControl` retorna `false` sin consultar la BD.
- Operador sin asignaciones: `listaAsignaciones()` retorna lista vacía (no `null`).
- Operador sin alarmas activas: `asignacionMaquinasDesabastecidas()` retorna lista vacía.
- `consultarAlarmasPendientes()` IMPLEMENTADO en Spec 08: consulta `AlarmasPendientesRepositorio` y filtra por maquinas asignadas al operador (Secure Messaging).

**Acceptance criteria:**
- **Given** operador con `id=1` y `password="admin"` existen en BD, **when** se llama `inicioSesion(1, "admin")`, **then** retorna `true`.
- **Given** credenciales inválidas, **when** se llama `inicioSesion(99, "wrong")`, **then** retorna `false`.
- **Given** operador `id=1` tiene asignadas máquinas 2 y 3, **when** se llama `asignacionMaquina(1)`, **then** retorna lista con exactamente 2 elementos en formato `"idMaquina-ubicacion"`.
- **Given** la máquina 2 tiene alarma activa (FECHA_FINAL IS NULL), **when** se llama `asignacionMaquinasDesabastecidas(1)`, **then** la lista contiene al menos 1 elemento con el formato correcto.
- **Given** ninguna máquina asignada al operador tiene alarma activa, **when** se llama `asignacionMaquinasDesabastecidas`, **then** retorna lista vacía (no null).

---

## 2. Plan

**Architecture:**
- `ControlComLogistica` implementa `ServicioComLogistica` (Ice). Registrado con identidad `"logistica"` en el `ObjectAdapter` del servidor.
- Delega toda la lógica de negocio a `ServerControl`, que a su vez usa `ManejadorDatos`.
- `cmLogistics` conecta al servidor en `localhost:12345` (vía `CmLogistic.cfg`) y también tiene un proxy a la máquina en `localhost:12346` para poder llamar `abastecer`.

**Data model (PostgreSQL):**
- `OPERADORES(IDOPERADOR PK, NOMBRE, CORREO, CONTRASENA)`
- `MAQUINA(IDMAQUINA PK, UBICACION)`
- `ASIGNACION_MAQUINA(ID_MAQUINA FK, ID_OPERADOR FK)`
- `ALARMA_MAQUINA(CONSECUTIVO, ID_ALARMA FK, ID_MAQUINA FK, FECHA_INICIAL, FECHA_FINAL nullable)`

**API contracts (Ice — `ServicioComLogistica`):**
```
StringSeq asignacionMaquina(int codigoOperador)
  // retorna ["idMaquina-ubicacion", ...]

StringSeq asignacionMaquinasDesabastecidas(int codigoOperador)
  // retorna ["idMaquina#ubicacion#fechaIni#idAlarma#descripAlarma", ...]

StringSeq consultarAlarmasPendientes(int codigoOperador)
  // declarado pero NO implementado

bool inicioSesion(int codigoOperador, string password)
```

**Testing strategy:** No hay tests automatizados. Verificación manual requeriría implementar el cliente `cmLogistics`.

**Security constraints:**
- La contraseña se almacena y compara en texto plano en la tabla `OPERADORES`.
- No hay cifrado del canal Ice; las credenciales viajan en claro.

**Dependencies:** ZeroC Ice 3.7.6, PostgreSQL, JDBC, tablas `OPERADORES`, `MAQUINA`, `ASIGNACION_MAQUINA`, `ALARMA_MAQUINA`.

---

## 3. Tasks (completadas / pendientes)

```
Task 1: Servant ControlComLogistica (ServidorCentral)
Depends on: none
What was built: Implementación de asignacionMaquina(), asignacionMaquinasDesabastecidas()
  e inicioSesion(). consultarAlarmasPendientes() no está implementado.
Acceptance criteria:
- El servant queda registrado con identidad "logistica" en el ObjectAdapter.
- inicioSesion(0, null) retorna false sin llegar a la BD.

Task 2: ServerControl — orquestación de consultas BD
Depends on: Task 1
What was built: listaAsignaciones(), listaAsignacionesMDanada(), existeOperador(),
  darCorreoOperador(). Cada método abre su propia conexión BD.
Acceptance criteria:
- listaAsignaciones() retorna lista vacía (no null) cuando el operador no tiene asignaciones.
- listaAsignacionesMDanada() solo incluye máquinas con FECHA_FINAL IS NULL.

Task 3: ManejadorDatos — SQL de logística
Depends on: Task 2
What was built: listaAsignaciones() (JOIN ASIGNACION_MAQUINA + MAQUINA),
  listaAsignacionMaquinasDanadas() (JOIN con ALARMA_MAQUINA WHERE FECHA_FINAL IS NULL),
  existeOperador() (SELECT con id + contraseña).
Acceptance criteria:
- listaAsignacionMaquinasDanadas() retorna formato "idMaquina#ubicacion#fechaIni#idAlarma#descrip".
- existeOperador() retorna false para contraseña errónea del mismo operador.

Task 4 (completada — Spec 10): Cliente cmLogistics con GUI Swing
Depends on: Task 1
What was built: CmLogistics.java inicializa los 3 proxies Ice y lanza ControladorLogistica
  en el EDT via SwingUtilities.invokeLater. El cliente tiene interfaz Swing (InterfazLogistica)
  con panel de sesion, panel de alarmas (PanelAlarmas), log de eventos e inventario.
  TecnicoMantenimiento encapsula el estado del operador autenticado.
  ControlAlarma encapsula la logica de consulta y resolucion de alarmas.
Acceptance criteria:
- Login fallido muestra mensaje en lblEstado y no permite consultar alarmas.
- Alarmas pendientes se muestran formateadas en comboBoxAlarmas.
- Resolver alarma ejecuta el flujo completo: bodega -> callback a maquina.

Task 5 (completada — Spec 08): consultarAlarmasPendientes()
Depends on: Task 1, Spec 08
What was built: ControlComLogistica.consultarAlarmasPendientes() consulta
  AlarmasPendientesRepositorio (repositorio local del servidor) y filtra por
  maquinas asignadas al operador (Secure Messaging via ASIGNACION_MAQUINA).
  Retorna lista de strings en formato pipe-delimited.
Acceptance criteria:
- Retorna solo alarmas de maquinas asignadas al operador.
- Retorna lista vacia (no null) si el operador no tiene alarmas pendientes.

Task 6 (completada — Spec 12): try-with-resources en metodos de ManejadorDatos
Depends on: Task 3
What was built: existeOperador() y darOperador() convertidos a try-with-resources
  para garantizar cierre de PreparedStatement y ResultSet ante excepciones.
Acceptance criteria:
- PreparedStatement se cierra correctamente incluso si executeQuery() lanza excepcion.
```

---

## Assumptions to review

1. **La contraseña se guarda en texto plano** — Impact: HIGH
   Correct this if: hay requisitos de seguridad o el sistema se expone a una red no confiable.

2. **`consultarAlarmasPendientes()` IMPLEMENTADO** — Impact: resuelto.
   Implementado en Spec 08 via `AlarmasPendientesRepositorio` + filtro por asignacion de operador.

3. **`cmLogistics` puede conectarse directamente a la máquina (:12346) sin pasar por el servidor** — Impact: MEDIUM
   Correct this if: se requiere que el servidor centralice y audite todas las órdenes de abastecimiento.
