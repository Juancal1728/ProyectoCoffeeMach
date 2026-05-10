# Spec 11 — Implementacion de abastecerRecurso (completa Spec 06)

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Implementar el metodo `abastecerRecurso(codMaquina, RecursoAbastecimiento, cantidad, idAlarma)` en coffeeMach, que permite a cmLogistics enviar un callback con recurso tipado y cantidad explicita. Refactorizar el metodo `abastecer()` existente para compartir logica. Corregir bugs criticos en el flujo de abastecimiento.

**Users:**
- **cmLogistics** llama `abastecerRecurso()` como callback tras despachar desde bodega.
- **coffeeMach** recibe, aplica el abastecimiento y confirma al servidor.

**Requirements:**
1. `abastecerRecurso()` verifica que `codMaquina` coincida con la maquina local.
2. Aplica el abastecimiento usando `recurso.name()` para mapear al repositorio local correcto.
3. La cantidad recibida se SUMA al nivel actual (no se fija a un valor).
4. Tras aplicar, limpia las alarmas del `AlarmaRepositorio` local por recurso (no por idAlarma del servidor).
5. Si no quedan alarmas, rehabilita la GUI.
6. Las actualizaciones de GUI se ejecutan en el EDT via `SwingUtilities.invokeLater`.
7. Respalda estado a disco y notifica al servidor via `AlarmaServicePrx`.
8. Retorna `true` si exitoso, `false` si codMaquina no coincide.
9. El metodo `abastecer()` existente se refactoriza para delegar a la misma logica.

**Bugs corregidos en esta spec:**
- **Alarm ID mismatch (critico)**: `quitarAlarma(idAlarma)` usaba el ID del servidor (ej. 1 para INGREDIENTE), pero coffeeMach guarda los ingredientes bajo claves "8"-"15". Corregido con `limpiarAlarmasDeRecurso(recurso)` que mapea por nombre de recurso y limpia los dos niveles (bajo + critico).
- **NPE en cargarRecetaMaquinas**: si el servidor devuelve un ingrediente no existente en el repositorio local, se crea un `Ingrediente` pero no se almacenaba. Corregido con `ingredientes.addElement(splitdeIng[1], ingred)`.
- **Operadores bitwise**: `|` cambiados a `||` en verificarMonedas (lineas 93, 103, 110, 117, 121, 125, 129).
- **Ventas duplicadas**: clave unica con timestamp para evitar colision.
- `quemarCodMaquina()`: manejo de archivo inexistente sin excepcion.

**Patron aplicado:** Callback (Servant receptor), Repository, Controller.

**Mapeo recurso -> alarmas locales (limpiarAlarmasDeRecurso):**

| RecursoAbastecimiento.name() | Alarmas locales eliminadas |
|---|---|
| MONEDA100 | "2" (bajo), "3" (critico) |
| MONEDA200 | "4" (bajo), "5" (critico) |
| MONEDA500 | "6" (bajo), "7" (critico) |
| AGUA | "8" (bajo), "12" (critico) |
| CAFE | "9" (bajo), "13" (critico) |
| AZUCAR | "10" (bajo), "14" (critico) |
| VASO | "11" (bajo), "15" (critico) |
| KITREPARACION | "1" (mantenimiento) |

**Mapeo recurso -> repositorio local (aplicarAbastecimiento):**

| RecursoAbastecimiento.name() | Repositorio | Clave en repo | Accion |
|---|---|---|---|
| MONEDA100 | MonedasRepositorio | "100" | sumar cantidad |
| MONEDA200 | MonedasRepositorio | "200" | sumar cantidad |
| MONEDA500 | MonedasRepositorio | "500" | sumar cantidad |
| AGUA | IngredienteRepositorio | "Agua" | recargar a maximo |
| CAFE | IngredienteRepositorio | "Cafe" | recargar a maximo |
| AZUCAR | IngredienteRepositorio | "Azucar" | recargar a maximo |
| VASO | IngredienteRepositorio | "Vaso" | recargar a maximo |
| KITREPARACION | (solo quita alarma) | - | quitar alarma 1 |

**Acceptance criteria:**
- **Given** codMaquina correcto y recurso=CAFE, **when** `abastecerRecurso(1, CAFE, 200, 1)`, **then** ingrediente Cafe queda en su maximo, alarmas "9" y "13" se quitan, retorna true.
- **Given** codMaquina incorrecto, **when** se llama `abastecerRecurso`, **then** retorna false sin modificar nada.
- **Given** unica alarma activa resuelta, **when** se aplica, **then** GUI rehabilitada via SwingUtilities.invokeLater.

---

## 3. Tasks (completadas)

```
Task 1: Agregar abastecerRecurso() a ControladorMQ
Depends on: none
What was built: Nuevo metodo que implementa la interfaz Ice ServicioAbastecimiento.abastecerRecurso.
  Verifica codMaquina, delega en aplicarAbastecimiento().
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 2: Crear aplicarAbastecimiento() como metodo comun
Depends on: Task 1
What was built: Logica compartida entre abastecer() y abastecerRecurso().
  Switch sobre nombre del recurso, mapea a repositorio correcto.
  Llama limpiarAlarmasDeRecurso(recurso) para eliminar alarmas correctas.
  Ejecuta actualizacion de GUI via SwingUtilities.invokeLater.
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 3: Crear limpiarAlarmasDeRecurso() (fix del alarm ID mismatch)
Depends on: none
What was built: Metodo privado que recibe el nombre del recurso y elimina del
  AlarmaRepositorio tanto la alarma de nivel bajo como la critica.
  Resuelve el bug donde quitarAlarma(idAlarma) usaba el ID del servidor (siempre 1
  para ingredientes) en lugar de los IDs locales (8-15 para ingredientes).
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 4: Fix NPE en cargarRecetaMaquinas
Depends on: none
What was built: Al crear un Ingrediente nuevo porque no existe en el repositorio local,
  se llama ingredientes.addElement(nombre, ingred) para almacenarlo y evitar NPE
  en disminuirInsumos() cuando se vende un producto.
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 5: Refactorizar abastecer() existente
Depends on: Task 2
What was built: Delegacion a aplicarAbastecimiento() con helpers recursoDesdeAlarma()
  y cantidadDesdeRecurso().
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 6: Corregir bugs existentes
Depends on: none
What was built: | -> || en verificarMonedas, ventas con clave unica (timestamp),
  quemarCodMaquina() mejorado para manejar archivo inexistente.
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java
```
