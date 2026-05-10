# Spec 12 — definirProducto + correcciones en ServidorCentral

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Implementar el metodo `definirProducto()` en `ProductoReceta` que actualmente lanza `UnsupportedOperationException`. Agregar busqueda de ingrediente por nombre en `ManejadorDatos`. Hacer configurable la consola de administracion. Corregir orden de activacion del adapter. Corregir bugs en `ManejadorDatos` que impedian registrar alarmas correctamente.

**Requirements:**
1. `definirProducto(nombre, precio, ingredientes)` registra la receta con sus ingredientes en una sola operacion.
2. El mapa de ingredientes puede usar IDs (como String) o nombres; si es nombre, se busca el ID via `buscarIdIngredientePorNombre()`.
3. La consola de administracion solo arranca si `ConsolaAdministracion.enabled=true` en `server.cfg`.
4. `adapter.activate()` debe ejecutarse ANTES de `controladorRecetas.run()` para no bloquear los servicios Ice.
5. El null check en `ProductoReceta.consultarProductos()` usa `listaAsociada != null && !listaAsociada.isEmpty()` (no `!listaAsociada.equals(null)` que siempre evalua a true).
6. Cuando `consultarProductos()` no tiene datos, retorna `new String[0]` (no null).
7. En `ManejadorDatos.registrarAlarma()`, la variable correcta para obtener el ResultSet es `sta` (no `st`) y ambas sentencias INSERT (`psx3`, `psx4`) se ejecutan con `executeUpdate()`.
8. Los metodos `registrarAlarma()`, `desactivarAlarma()`, `existeOperador()` y `darOperador()` usan try-with-resources para garantizar cierre de `PreparedStatement` y `ResultSet`.

**Acceptance criteria:**
- **Given** ingrediente "Cafe" con id=2 existe en BD, **when** `definirProducto("Latte", 2000, {"Cafe": 100, "2": 50})`, **then** se crea receta "Latte" con precio 2000 y dos filas en RECETA_INGREDIENTE.
- **Given** ingrediente "Inexistente" no existe en BD, **when** se usa en el mapa, **then** se ignora esa entrada.
- **Given** `ConsolaAdministracion.enabled=false`, **when** ServidorCentral arranca, **then** no se crea hilo de consola.
- **Given** adapter.activate() antes de controladorRecetas.run(), **when** arranca el servidor, **then** los servicios Ice estan disponibles inmediatamente.
- **Given** coffeeMach notifica alarma de escasez, **when** `registrarAlarma()` se ejecuta, **then** se registran las dos filas en ALARMA_MAQUINA sin excepcion de variable null.
- **Given** `listaAsociada` es null en `consultarProductos()`, **when** se llama al metodo, **then** retorna `new String[0]` sin NullPointerException.

---

## 3. Tasks (completadas)

```
Task 1: Implementar definirProducto() en ProductoReceta
Depends on: Task 2
What was built: Registrar receta, parsear retorno de registrarReceta(), iterar mapa de ingredientes.
Files: ServidorCentral/src/main/java/receta/ProductoReceta.java

Task 2: Agregar buscarIdIngredientePorNombre() en ManejadorDatos
Depends on: none
What was built: SELECT IDINGREDIENTE FROM INGREDIENTE WHERE LOWER(NOMBRE) = LOWER(?).
Files: ServidorCentral/src/main/java/modelo/ManejadorDatos.java

Task 3: Hacer configurable ConsolaAdministracion
Depends on: none
What was built: Leer propiedad en ServerControl constructor, solo arrancar si "true".
Files: ServidorCentral/src/main/java/ServerControl/ServerControl.java,
       ServidorCentral/src/main/resources/server.cfg

Task 4: Mover adapter.activate() antes de controladorRecetas.run()
Depends on: none
What was built: Reordenar lineas en main() de ServidorCentral.
Files: ServidorCentral/src/main/java/ServidorCentral.java

Task 5: Corregir null check en ProductoReceta.consultarProductos()
Depends on: none
What was built: Cambio de !listaAsociada.equals(null) a listaAsociada != null && !listaAsociada.isEmpty().
  Cambio de return null a return new String[0] en la rama sin datos.
  El patron !obj.equals(null) siempre retorna true en objetos no-null — era un bug silencioso.
Files: ServidorCentral/src/main/java/receta/ProductoReceta.java

Task 6: Corregir ManejadorDatos.registrarAlarma() — variable y sentencias faltantes
Depends on: none
What was built:
  a) Cambio de st.getResultSet() a sta.getResultSet() (linea ~465) — st era una variable
     distinta de la sentencia que ejecuto la secuencia SEQ_ALARMAS.
  b) Se agrego psx3.executeUpdate() y psx4.executeUpdate() — las dos sentencias INSERT
     en ALARMA_MAQUINA estaban preparadas pero nunca se ejecutaban.
Files: ServidorCentral/src/main/java/modelo/ManejadorDatos.java

Task 7: Convertir metodos ManejadorDatos a try-with-resources
Depends on: none
What was built: registrarAlarma(), desactivarAlarma(), existeOperador() y darOperador()
  convertidos a try-with-resources para garantizar cierre de PreparedStatement y ResultSet
  incluso ante excepciones.
Files: ServidorCentral/src/main/java/modelo/ManejadorDatos.java
```
