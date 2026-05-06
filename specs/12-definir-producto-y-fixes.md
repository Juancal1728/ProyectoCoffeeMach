# Spec 12 — definirProducto + correcciones en ServidorCentral

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Implementar el metodo `definirProducto()` en `ProductoReceta` que actualmente lanza `UnsupportedOperationException`. Agregar busqueda de ingrediente por nombre en `ManejadorDatos`. Hacer configurable la consola de administracion. Corregir orden de activacion del adapter.

**Requirements:**
1. `definirProducto(nombre, precio, ingredientes)` registra la receta con sus ingredientes en una sola operacion.
2. El mapa de ingredientes puede usar IDs (como String) o nombres; si es nombre, se busca el ID via `buscarIdIngredientePorNombre()`.
3. La consola de administracion solo arranca si `ConsolaAdministracion.enabled=true` en `server.cfg`.
4. `adapter.activate()` debe ejecutarse ANTES de `controladorRecetas.run()` para no bloquear los servicios Ice.

**Acceptance criteria:**
- **Given** ingrediente "Cafe" con id=2 existe en BD, **when** `definirProducto("Latte", 2000, {"Cafe": 100, "2": 50})`, **then** se crea receta "Latte" con precio 2000 y dos filas en RECETA_INGREDIENTE.
- **Given** ingrediente "Inexistente" no existe en BD, **when** se usa en el mapa, **then** se ignora esa entrada.
- **Given** `ConsolaAdministracion.enabled=false`, **when** ServidorCentral arranca, **then** no se crea hilo de consola.
- **Given** adapter.activate() antes de controladorRecetas.run(), **when** arranca el servidor, **then** los servicios Ice estan disponibles inmediatamente.

---

## 3. Tasks

```
Task 1: Implementar definirProducto() en ProductoReceta
Depends on: Task 2
What to build: Registrar receta, parsear retorno de registrarReceta(), iterar mapa de ingredientes.
Files: ServidorCentral/src/main/java/receta/ProductoReceta.java

Task 2: Agregar buscarIdIngredientePorNombre() en ManejadorDatos
Depends on: none
What to build: SELECT IDINGREDIENTE FROM INGREDIENTE WHERE LOWER(NOMBRE) = LOWER(?).
Files: ServidorCentral/src/main/java/modelo/ManejadorDatos.java

Task 3: Hacer configurable ConsolaAdministracion
Depends on: none
What to build: Leer propiedad en ServerControl constructor, solo arrancar si "true".
Files: ServidorCentral/src/main/java/ServerControl/ServerControl.java,
       ServidorCentral/src/main/resources/server.cfg

Task 4: Mover adapter.activate() antes de controladorRecetas.run()
Depends on: none
What to build: Reordenar lineas en main().
Files: ServidorCentral/src/main/java/ServidorCentral.java
```
