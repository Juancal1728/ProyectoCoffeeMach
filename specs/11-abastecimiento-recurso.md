# Spec 11 — Implementacion de abastecerRecurso (completa Spec 06)

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Implementar el metodo `abastecerRecurso(codMaquina, RecursoAbastecimiento, cantidad, idAlarma)` en coffeeMach, que permite a cmLogistics enviar un callback con recurso tipado y cantidad explicita. Refactorizar el metodo `abastecer()` existente para compartir logica. Corregir bugs existentes.

**Users:**
- **cmLogistics** llama `abastecerRecurso()` como callback tras despachar desde bodega.
- **coffeeMach** recibe, aplica el abastecimiento y confirma al servidor.

**Requirements:**
1. `abastecerRecurso()` verifica que `codMaquina` coincida con la maquina local.
2. Aplica el abastecimiento usando `recurso.name()` para mapear al repositorio local correcto.
3. La cantidad recibida se SUMA al nivel actual (no se fija a un valor).
4. Tras aplicar, quita la alarma del `AlarmaRepositorio` local.
5. Si no quedan alarmas, rehabilita la GUI.
6. Respalda estado a disco y notifica al servidor via `AlarmaServicePrx`.
7. Retorna `true` si exitoso, `false` si codMaquina no coincide.
8. El metodo `abastecer()` existente se refactoriza para delegar a la misma logica.

**Bugs corregidos en esta spec:**
- Operadores bitwise `|` cambiados a logicos `||` (lineas 93, 103, 110, 117, 121, 125, 129 del original).
- Monedas se SUMAN en vez de fijarse a 20.
- Ventas duplicadas: clave unica con timestamp.
- `quemarCodMaquina()`: manejo de archivo inexistente sin excepcion.

**Patron aplicado:** Callback (Servant receptor), Repository.

**Mapeo recurso -> repositorio local:**

| RecursoAbastecimiento.name() | Repositorio | Clave en repo | Accion |
|---|---|---|---|
| MONEDA100 | MonedasRepositorio | "100" | sumar cantidad |
| MONEDA200 | MonedasRepositorio | "200" | sumar cantidad |
| MONEDA500 | MonedasRepositorio | "500" | sumar cantidad |
| AGUA | IngredienteRepositorio | "Agua" | recargar a maximo |
| CAFE | IngredienteRepositorio | "Cafe" | recargar a maximo |
| AZUCAR | IngredienteRepositorio | "Azucar" | recargar a maximo |
| VASO | IngredienteRepositorio | "Vaso" | recargar a maximo |
| KITREPARACION | (solo quita alarma) | - | quitar alarma |

**Acceptance criteria:**
- **Given** codMaquina correcto y recurso=CAFE, **when** `abastecerRecurso(1, CAFE, 200, 9)`, **then** ingrediente Cafe queda en su maximo, alarma "9" se quita, retorna true.
- **Given** codMaquina incorrecto, **when** se llama `abastecerRecurso`, **then** retorna false sin modificar nada.
- **Given** unica alarma activa resuelta, **when** se aplica, **then** GUI rehabilitada.

---

## 3. Tasks

```
Task 1: Agregar abastecerRecurso() a ControladorMQ
Depends on: none
What to build: Nuevo metodo que implementa la interfaz Ice ServicioAbastecimiento.abastecerRecurso.
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 2: Crear aplicarAbastecimiento() como metodo comun
Depends on: Task 1
What to build: Logica compartida entre abastecer() y abastecerRecurso().
  Switch sobre nombre del recurso, mapea a repositorio correcto.
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 3: Refactorizar abastecer() existente
Depends on: Task 2
What to build: Delegacion a aplicarAbastecimiento() con helpers recursoDesdeAlarma() y cantidadDesdeRecurso().
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java

Task 4: Corregir bugs existentes
Depends on: none
What to build: | -> ||, ventas con clave unica, quemarCodMaquina mejorado.
Files: coffeeMach/src/main/java/McControlador/ControladorMQ.java
```
