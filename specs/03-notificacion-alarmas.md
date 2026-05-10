# Spec 03 — Notificación de alarmas al servidor central

> Estado: Implementado. Bugs criticos en ManejadorDatos.registrarAlarma() corregidos (variable incorrecta + sentencias INSERT no ejecutadas). Metodos convertidos a try-with-resources.

---

## 1. Spec

**Purpose:** Permitir que las máquinas de café reporten condiciones anómalas (escasez de ingredientes, monedas, mal funcionamiento) al servidor central, que las persiste en base de datos y resuelve el operador responsable de atenderlas.

**Users:**
- **Máquina de café** (`ControladorMQ`) genera y envía alarmas.
- **Servidor central** (`AlarmasManager` + `ManejadorDatos`) recibe, valida y persiste alarmas.
- **Operador de logística** consulta alarmas pendientes (ver Spec 07).

**Requirements:**
1. La máquina envía una notificación de alarma vía Ice al servidor en cuatro escenarios: escasez de ingrediente, insuficiencia de moneda, mal funcionamiento, y confirmación de abastecimiento.
2. El servidor valida que el `idAlarma` corresponde a un registro en la tabla `ALARMA` antes de persistir.
3. El servidor verifica que no exista ya una alarma activa (sin `FECHA_FINAL`) del mismo tipo para la misma máquina antes de insertar.
4. Si ya existe, el servidor imprime el aviso pero no duplica el registro.
5. Al registrar, el servidor usa la secuencia `CONSECALARMA` de PostgreSQL para el consecutivo.
6. El servidor resuelve el operador asignado a la máquina y retorna `"Fallo de máquina: <alarma> - Atención por: <operador>"`.
7. Cuando la alarma es resuelta por abastecimiento, `desactivarAlarma()` actualiza `FECHA_FINAL` en la tabla `ALARMA_MAQUINA`.

**Edge cases:**
- `idAlarma` no existe en la tabla `ALARMA`: `darNombreAlarma()` retorna `null`, la alarma no se persiste.
- La máquina no tiene operador asignado: `darOperador()` retorna `null`, la alarma no se persiste.
- La conexión a BD falla: `SQLException` se imprime en consola, la operación falla silenciosamente.
- La misma alarma se dispara múltiples veces antes de ser resuelta: solo el primer registro se inserta.

**Acceptance criteria:**
- **Given** un `idAlarma` válido y una máquina con operador asignado, **when** la máquina llama `recibirNotificacionEscasezIngredientes`, **then** se inserta una fila en `ALARMA_MAQUINA` con `FECHA_FINAL = null`.
- **Given** que ya existe una fila activa con el mismo `idAlarma` e `idMaquina`, **when** llega la misma notificación, **then** no se inserta una segunda fila.
- **Given** `idAlarma` que no existe en la tabla `ALARMA`, **when** llega la notificación, **then** `registrarAlarma` no ejecuta el INSERT.
- **Given** una alarma activa, **when** se llama `desactivarAlarma(idMaquina, idAlarma, fechaFinal)`, **then** la fila en `ALARMA_MAQUINA` queda con `FECHA_FINAL` distinto de null.

---

## 2. Plan

**Architecture:**
- `Alarma` (en `ServidorCentral`) es el servant Ice que implementa `AlarmaService`. Delega toda la lógica a `AlarmasManager`.
- `AlarmasManager` abre una conexión BD (`ConexionBD`), crea `ManejadorDatos` y ejecuta las operaciones SQL.
- Cada invocación Ice abre y cierra su propia conexión BD (no hay pool).
- El servidor registra el servant con identidad `"Alarmas"` en el `ObjectAdapter`.

**Data model (PostgreSQL):**
- `ALARMA(IDALARMA PK, NOMBRE)` — catálogo de tipos de alarma.
- `ALARMA_MAQUINA(CONSECUTIVO PK, ID_ALARMA FK, ID_MAQUINA FK, FECHA_INICIAL, FECHA_FINAL nullable)` — instancias activas/resueltas.
- Secuencia: `CONSECALARMA`.

**API contracts (Ice — interfaz `AlarmaService`):**
```
void recibirNotificacionEscasezIngredientes(string iDing, int idMaq)
void recibirNotificacionInsuficienciaMoneda(Moneda moneda, int idMaq)
void recibirNotificacionEscasezSuministro(string idSumin, int idMaq)
void recibirNotificacionAbastesimiento(int idMaq, string idInsumo, int cantidad)
void recibirNotificacionMalFuncionamiento(int idMaq, string descri)
```

Mapeo de notificación a `idAlarma` (hardcodeado en `ControladorMQ`):

| Notificación | idAlarma |
|---|---|
| Mantenimiento | 1 |
| CIEN low/critical | 2 / 3 |
| DOCIENTOS low/critical | 4 / 5 |
| QUINIENTOS low/critical | 6 / 7 |
| Agua low/critical | 8 / 12 |
| Cafe low/critical | 9 / 13 |
| Azucar low/critical | 10 / 14 |
| Vaso low/critical | 11 / 15 |

**Testing strategy:** No hay tests automatizados. Verificación manual: ejecutar sistema completo, forzar escasez, consultar tabla `ALARMA_MAQUINA` en BD.

**Security constraints:** No hay autenticación en la interfaz Ice; cualquier cliente que conozca el endpoint puede enviar notificaciones falsas.

**Dependencies:** ZeroC Ice 3.7.6, PostgreSQL, JDBC, tablas `ALARMA` y `ALARMA_MAQUINA` pre-pobladas.

---

## 3. Tasks (completadas)

```
Task 1: Servant AlarmaService (ServidorCentral)
Depends on: none
What was built: Clase Alarma que implementa AlarmaService (Ice). Cada método
  extrae idMaquina del parámetro correspondiente y llama AlarmasManager.alarmaMaquina().
Acceptance criteria:
- El servant queda registrado con identidad "Alarmas" en el ObjectAdapter.
- recibirNotificacionMalFuncionamiento llama alarmaMaquina con idAlarma=1.

Task 2: AlarmasManager — registro y desactivación
Depends on: Task 1
What was built: alarmaMaquina() abre BD, llama darNombreAlarma() y darOperador(),
  si ambos son non-null llama registrarAlarma(AlarmaMaquina).
  desactivarAlarma() ejecuta UPDATE en ALARMA_MAQUINA.
Acceptance criteria:
- Si darNombreAlarma() o darOperador() retorna null, no se llama registrarAlarma.
- El UPDATE de desactivarAlarma filtra por ID_ALARMA e ID_MAQUINA correctamente.

Task 3: ManejadorDatos — SQL de alarmas
Depends on: Task 2
What was built: registrarAlarma() verifica duplicado con SELECT WHERE FECHA_FINAL IS NULL,
  luego usa NEXTVAL('CONSECALARMA') e INSERT INTO ALARMA_MAQUINA.
  desactivarAlarma() actualiza FECHA_FINAL.
Bugs corregidos (auditoria):
  a) st.getResultSet() cambiado a sta.getResultSet() — se usaba la variable erronea
     despues de ejecutar NEXTVAL en la sentencia sta.
  b) psx3.executeUpdate() y psx4.executeUpdate() agregados — los dos INSERT en
     ALARMA_MAQUINA estaban preparados pero nunca se ejecutaban (las alarmas no se guardaban).
  c) registrarAlarma(), desactivarAlarma(), existeOperador() y darOperador() convertidos
     a try-with-resources para garantizar cierre de PreparedStatement y ResultSet.
Acceptance criteria:
- Un segundo INSERT con misma alarma+maquina activa no ocurre.
- CONSECUTIVO usa la secuencia de BD, no un valor manual.
- registrarAlarma() inserta efectivamente en ALARMA_MAQUINA (ambas filas).

Task 4: Generación de alarmas en ControladorMQ (coffeeMach)
Depends on: Task 1, Spec 02
What was built: verificarProductos() y verificarMonedas() llaman los métodos
  del AlarmaServicePrx según el tipo de recurso y nivel (low/critical).
Acceptance criteria:
- ingrediente en nivel low → llama recibirNotificacionEscasezIngredientes con nombre del ingrediente.
- moneda en nivel crítico → llama recibirNotificacionInsuficienciaMoneda con enum Moneda correcto.
```

---

## Assumptions to review

1. **El mapeo idAlarma ↔ ingrediente/moneda es fijo y no se puede reconfigurar** — Impact: HIGH
   Correct this if: se agregan nuevos tipos de ingrediente o nuevas denominaciones de moneda (el mapeo en `validarAlarma()` y `ControladorMQ` requeriría cambio manual).

2. **Una conexión BD nueva por cada invocación Ice es aceptable** — Impact: MEDIUM
   Correct this if: el volumen de alarmas crece significativamente y la latencia de conexión se vuelve un problema.

3. **El operador está siempre asignado a la máquina antes de que ella pueda generar alarmas** — Impact: MEDIUM
   Correct this if: una máquina nueva se conecta sin operador asignado — sus alarmas se descartarían silenciosamente.
