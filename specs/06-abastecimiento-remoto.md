# Spec 06 — Abastecimiento remoto de la máquina

> Estado: Implementado completamente. `abastecer()` refactorizado, `abastecerRecurso()` implementado (Spec 11), bug de alarm ID mismatch corregido con `limpiarAlarmasDeRecurso()`, actualizaciones de GUI via SwingUtilities.invokeLater.

---

## 1. Spec

**Purpose:** Permitir que un cliente externo (operador de logística o servidor central) indique a una máquina de café que recargue un recurso específico (ingrediente o moneda) en respuesta a una alarma, rehabilitando la máquina si todas las alarmas quedan resueltas.

**Users:**
- **Operador de logística** (`cmLogistics`) o el propio servidor inicia el abastecimiento.
- **Máquina de café** (`ControladorMQ`) recibe la orden y actualiza su estado local.

**Requirements:**
1. El cliente llama `abastecer(codMaquina, idAlarma)` en el endpoint `12346` de la máquina objetivo.
2. La máquina verifica que `codMaquina` coincida con su propio código antes de procesar.
3. La máquina determina el recurso a recargar según el `idAlarma` (mapeo hardcodeado 2–15).
4. Cada recurso se recarga a su nivel máximo (ingredientes) o a cantidad fija 20 (monedas).
5. La alarma correspondiente se elimina de `AlarmaRepositorio`.
6. Si tras eliminar la alarma el repositorio queda vacío, la GUI se rehabilita.
7. Se respalda el estado en disco tras el abastecimiento.
8. Se notifica al servidor central via `AlarmaServicePrx.recibirNotificacionAbastesimiento`.

**Edge cases:**
- `codMaquina` no coincide: la máquina ignora la orden completamente (sin respuesta de error).
- `idAlarma == 1` (mantenimiento): la máquina entra al bloque pero no hace ninguna recarga (bloque vacío / TODO).
- `abastecerRecurso()` IMPLEMENTADO en Spec 11. Usa `recurso.name()` para determinar el recurso y la cantidad.
- El bug de thread-safety (`frame.setEnabled(true)` desde hilo Ice) fue CORREGIDO: todas las actualizaciones de GUI se ejecutan via `SwingUtilities.invokeLater()`.

**Acceptance criteria:**
- **Given** `codMaquina` correcto y `idAlarma = 9` (Cafe low), **when** se llama `abastecer`, **then** el ingrediente "Cafe" queda en su nivel máximo (`getMaximo()`).
- **Given** `codMaquina` correcto y `idAlarma = 3` (100-coin critical), **when** se llama `abastecer`, **then** el depósito "100" queda con cantidad 20.
- **Given** `codMaquina` distinto al de la máquina, **when** se llama `abastecer`, **then** ningún repositorio es modificado.
- **Given** era la única alarma activa, **when** se resuelve, **then** la GUI queda habilitada (`frame.setEnabled(true)`).
- **Given** quedaban más alarmas activas, **when** se resuelve una, **then** la GUI permanece deshabilitada.

---

## 2. Plan

**Architecture:**
- `ControladorMQ` implementa `ServicioAbastecimiento` directamente.
- El servant se registra con identidad `"abastecer"` en el `ObjectAdapter` de `coffeeMach` (endpoint `12346`).
- `cmLogistics` y/o el servidor acceden al endpoint `abastecer:default -h <ip_maquina> -p 12346`.
- Coordinación: el servidor (via `AlarmasManager.desactivarAlarma()`) actualiza la BD cuando el abastecimiento es confirmado.

**Data model (local en máquina):**
- `AlarmaRepositorio` — mapa `String(idAlarma) → Alarma`.
- `IngredienteRepositorio` — cantidad se restablece a `getMaximo()`.
- `MonedasRepositorio` — cantidad de monedas se fija en 20.

**API contracts (Ice — `ServicioAbastecimiento`):**
```
void abastecer(int codMaquina, int tipoAlarma)
bool abastecerRecurso(int codMaquina, RecursoAbastecimiento recurso, int cantidad, int idAlarma)
// abastecerRecurso: NO IMPLEMENTADO en la versión actual
```

**Testing strategy:** No hay tests automatizados. Verificación manual: forzar alarma en la máquina, llamar `abastecer` desde `cmLogistics` o directamente via Ice, observar GUI y archivos `.bd`.

**Security constraints:** El endpoint `12346` no tiene autenticación; quien conozca la IP y el puerto puede enviar órdenes de abastecimiento.

**Dependencies:** ZeroC Ice 3.7.6, Spec 01 (repositorios locales), Spec 02 (GUI `Interfaz`), Spec 03 (notificación de confirmación al servidor).

---

## 3. Tasks (completadas / pendientes)

```
Task 1: Registro del servant en coffeeMach
Depends on: Spec 01, Spec 02
What was built: CoffeeMach.main() registra ControladorMQ como servant de
  ServicioAbastecimiento con identidad "abastecer" en el ObjectAdapter en el
  puerto 12346.
Acceptance criteria:
- El ObjectAdapter queda activo con el servant "abastecer" escuchando en :12346.

Task 2: Implementación de abastecer()
Depends on: Task 1
What was built: switch-like if/else sobre idAlarma (2–15) que recarga el recurso
  correspondiente, elimina la alarma de AlarmaRepositorio, rehabilita la GUI si
  el repositorio queda vacío, respalda el estado y notifica al servidor.
Acceptance criteria:
- idAlarma=8 recarga ingrediente "Agua" a su máximo.
- idAlarma=2 fija depósito "100" a cantidad 20.
- Tras la operación, la alarma idAlarma ya no existe en AlarmaRepositorio.

Task 3 (completada — Spec 11): Implementacion de abastecerRecurso()
Depends on: Task 1
What was built: abastecerRecurso() implementado en ControladorMQ.
  Verifica codMaquina, delega en aplicarAbastecimiento(recurso.name(), cantidad, idAlarma).
  limpiarAlarmasDeRecurso(recurso) elimina las alarmas correctas del repositorio local
  mapeando por nombre de recurso (no por idAlarma del servidor, que siempre era 1 para ingredientes).
  Actualizacion de GUI via SwingUtilities.invokeLater.
Acceptance criteria:
- abastecerRecurso(codMaq, CAFE, 200, 1) recarga ingrediente "Cafe" a maximo y elimina alarmas "9" y "13".
- Retorna true si la operacion fue exitosa, false si codMaquina no coincide.
```

---

## Assumptions to review

1. **`abastecer()` y `abastecerRecurso()` son llamados desde el hilo Ice** — Impact: CORREGIDO.
   `SwingUtilities.invokeLater()` envuelve todas las llamadas a `frame.*` y `actualizarXxxGraf()` en `aplicarAbastecimiento()`. El hilo Ice no modifica el EDT directamente.

2. **La recarga de monedas siempre fija la cantidad en 20, ignorando cuánto se envió** — Impact: MEDIUM
   Correct this if: el operador puede enviar cantidades variables de monedas (en cuyo caso `abastecerRecurso` sería la API correcta).

3. **No se valida que la alarma a eliminar esté efectivamente activa antes de quitarla** — Impact: LOW
   Correct this if: llamadas duplicadas de abastecimiento podrían dejar el sistema en estado inconsistente.
