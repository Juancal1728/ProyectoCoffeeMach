# Spec 05 — Gestión de recetas e ingredientes

> Estado: Implementado. definirProducto() implementado (Spec 12). Null check en consultarProductos() corregido. Retorno null cambiado a new String[0].

---

## 1. Spec

**Purpose:** Permitir al administrador del sistema crear, consultar y eliminar recetas e ingredientes en el servidor central, y exponer esa información a las máquinas de café para que puedan actualizar su catálogo de productos.

**Users:**
- **Administrador** interactúa con la GUI de recetas (`InterfazRecetas`) que arranca junto con `ServidorCentral`.
- **Máquinas de café** consultan recetas completas vía Ice (`RecetaService.consultarProductos()`).

**Requirements:**
1. El administrador puede registrar un ingrediente con nombre; el sistema asigna un ID secuencial y crea automáticamente dos alarmas asociadas (nivel bajo y crítico).
2. El administrador puede registrar una receta con nombre y precio; el sistema asigna un ID secuencial.
3. El administrador puede asociar un ingrediente a una receta especificando la cantidad en unidades (`definirRecetaIngrediente`).
4. El administrador puede eliminar una receta (cascada: se eliminan primero las filas `RECETA_INGREDIENTE`).
5. La máquina puede consultar la lista completa de productos con sus ingredientes en un único llamado (`consultarProductos()`).
6. El servidor retorna la lista en un formato delimitado que la máquina puede parsear sin conocimiento del esquema BD.
7. La GUI refleja el estado actual de recetas e ingredientes al arrancar y tras cada operación.

**Edge cases:**
- Registrar un ingrediente con nombre ya existente: se detecta el duplicado con un SELECT previo y no se inserta, retornando string vacío.
- Registrar una receta con nombre ya existente: igual — se detecta duplicado y no se inserta.
- Eliminar una receta con ingredientes asociados: `borrarReceta()` borra primero `RECETA_INGREDIENTE` y luego `RECETA` (evita FK violation).
- `consultarProductos()` verificaba `!listaAsociada.equals(null)` — bug corregido: cambiado a `listaAsociada != null && !listaAsociada.isEmpty()`. Ademas, el retorno `null` fue cambiado a `new String[0]` para evitar NullPointerException en el cliente.
- Ingrediente sin alarmas en el catalogo `ALARMA`: `validarAlarma()` retorna string vacio para `codIng` > 4.

**Acceptance criteria:**
- **Given** un nombre de ingrediente nuevo, **when** se llama `registrarIngrediente("Leche")`, **then** se inserta una fila en `INGREDIENTE` y dos filas en `ALARMA` (nivel bajo y crítico).
- **Given** un nombre de ingrediente ya existente, **when** se llama `registrarIngrediente("Cafe")`, **then** no se inserta ninguna fila nueva y el sistema imprime "Ingrediente Existe".
- **Given** receta y ingrediente existentes, **when** se llama `definirRecetaIngrediente(idRec, idIng, 50)`, **then** se inserta una fila en `RECETA_INGREDIENTE` con UNIDADES=50.
- **Given** receta con ingredientes asociados, **when** se llama `borrarReceta(id)`, **then** se eliminan primero las filas de `RECETA_INGREDIENTE` y luego la fila de `RECETA`.
- **Given** recetas con ingredientes en BD, **when** una máquina llama `consultarProductos()`, **then** el array retornado contiene un string por receta en formato `"id-nombre-precio#idIng-nomIng-alarmaBaja-alarmaCrit-unidades#..."`.

---

## 2. Plan

**Architecture:**
- `ProductoReceta` implementa `RecetaService` (Ice) y es el servant registrado con identidad `"Recetas"`.
- `ControladorRecetas` corre en su propio thread (`Runnable`) lanzado desde `ServidorCentral.main()` antes de activar el adapter.
- `InterfazRecetas` es el JFrame generado por el diseñador Swing de Eclipse.
- `ProductoReceta` delega toda la BD a `ManejadorDatos`; cada método abre y cierra su propia conexión.

**Data model (PostgreSQL):**
- `INGREDIENTE(IDINGREDIENTE PK, NOMBRE)`
- `RECETA(IDRECETA PK, NOMBRE, PRECIO)`
- `RECETA_INGREDIENTE(IDRECETA FK, IDINGREDIENTE FK, UNIDADES)`
- `ALARMA(IDALARMA PK, NOMBRE)` — creada automáticamente al registrar ingrediente
- Secuencias: `SEQ_INGREDIENTES`, `SEQ_ALARMAS`, `SEQ_RECETA`

**Wire format (`consultarProductos`):**
```
"recetaId-recetaNombre-precio#ingId-ingNombre-alarmaBaja-alarmaCrit-unidades#..."
```
Una entrada por receta; cada bloque `#...` representa un ingrediente asociado.

**API contracts (Ice — `RecetaService`):**
```
String[] consultarIngredientes()      // "id-nombre"
String[] consultarRecetas()           // "id-nombre-precio"
String[] consultarProductos()         // formato compuesto descrito arriba
void     definirProducto(...)         // no implementado (UnsupportedOperationException)
void     borrarReceta(int cod)
void     definirRecetaIngrediente(int idReceta, int idIngrediente, int valor)
String   registrarReceta(String nombre, int precio)   // retorna "id-nombre-precio"
String   registrarIngrediente(String nombre)          // retorna "id-nombre-alarmaBaja-alarmaCrit"
```

**Testing strategy:** No hay tests automatizados. Verificación manual: abrir la GUI del servidor, crear recetas e ingredientes, observar la tabla y cargar desde la máquina.

**Security constraints:** La GUI está disponible para quien tenga acceso físico al servidor. No hay autenticación.

**Dependencies:** ZeroC Ice 3.7.6, PostgreSQL, JDBC, tablas pre-creadas por `coffeemach-ddl.sql` e inicializadas por `coffeemach-inserts.sql`.

---

## 3. Tasks (completadas)

```
Task 1: Servant ProductoReceta (ServidorCentral)
Depends on: none
What was built: Clase ProductoReceta implementando RecetaService (Ice).
  Todos los metodos abren conexion BD, crean ManejadorDatos, delegan y cierran.
  definirProducto() implementado en Spec 12 (ya no lanza UnsupportedOperationException).
  consultarProductos() corregido: null check y retorno new String[0] (Spec 12, Task 5).
Acceptance criteria:
- registrarReceta("Espresso", 1500) retorna string no vacio con el id asignado.
- borrarReceta(id) no lanza excepcion para id existente.
- consultarProductos() retorna new String[0] cuando no hay recetas (no null).

Task 2: ManejadorDatos — SQL de recetas e ingredientes
Depends on: Task 1
What was built: registrarIngrediente(), registrarReceta(), registrarRecetaIngrediente(),
  borrarReceta(), consultarRecetas(), consultarIngredientes(), consultaRecetasCompleta(),
  validarAlarma().
Acceptance criteria:
- registrarIngrediente para nombre nuevo inserta en INGREDIENTE y en ALARMA (×2).
- consultaRecetasCompleta() retorna formato correcto con bloque #ing por cada ingrediente asociado.
- borrarReceta() borra primero RECETA_INGREDIENTE y luego RECETA.

Task 3: GUI de administración — InterfazRecetas + ControladorRecetas
Depends on: Task 1
What was built: InterfazRecetas (JFrame Swing) con campos para nombre y precio de
  receta, campo para asociación "idReceta-idIngrediente-unidades", botones
  AgregarReceta / BorrarReceta / RIC (relación ingrediente-receta) y áreas de
  texto para recetas e ingredientes.
Acceptance criteria:
- Presionar AgregarReceta con nombre y precio no vacíos llama registrarReceta y refresca la vista.
- Presionar BorrarReceta con id válido llama borrarReceta y refresca la vista.
- Presionar RIC con "2-1-50" llama definirRecetaIngrediente(2, 1, 50).
```

---

## Assumptions to review

1. **`validarAlarma()` solo mapea ingredientes con id 1–4 (Agua, Cafe, Azucar, Vaso)** — Impact: HIGH
   Correct this if: se registran nuevos ingredientes; sus alarmas no quedarán mapeadas en el formato de `consultarProductos()`.

2. **`definirProducto()` implementado en Spec 12** — Impact: resuelto.
   Itera el mapa de ingredientes, busca cada uno por nombre o ID via `buscarIdIngredientePorNombre()`, y registra la receta con sus ingredientes en una sola llamada.

3. **No hay transacción entre la inserción en INGREDIENTE y la inserción de sus dos ALARMA** — Impact: MEDIUM
   Correct this if: se requiere que ingrediente y sus alarmas se creen juntos o ninguno.
