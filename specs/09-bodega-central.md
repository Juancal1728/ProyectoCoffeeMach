# Spec 09 — Componente bodegaCentral

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Implementar el componente de bodega central que gestiona el inventario fisico de suministros, ingredientes y herramientas de reparacion. Expone el servicio `ServicioBodega` via Ice para que cmLogistics pueda consultar existencias y despachar recursos hacia las maquinas de cafe.

**Users:**
- **Operador de logistica** (via cmLogistics) consulta inventario y solicita despachos.
- **Administrador de bodega** interactua con la consola local para ver y recargar inventario.

**Requirements:**
1. El componente arranca como servidor Ice en el puerto 12347.
2. Registra un servant `ServicioBodega` con identidad `"bodega"`.
3. El inventario se persiste localmente en `inventarioBodega.bd` usando patron Repository + Singleton standalone.
4. Si el archivo no existe, se inicializa con cantidades por defecto para los 8 recursos del enum `RecursoAbastecimiento`.
5. `consultarInventario()` retorna la lista completa del inventario como strings `"recurso: cantidad"`.
6. `hayExistencias(recurso, cantidad)` verifica si hay stock suficiente sin modificarlo.
7. `despachar(recurso, cantidad, codMaquina, idAlarma)` descuenta del inventario y retorna true/false.
8. La consola local permite ver inventario y recargar recursos manualmente.
9. Los metodos `hayExistencias()`, `despachar()`, `recargar()` y `listar()` de `InventarioRepositorio` son `synchronized` para proteger acceso concurrente de multiples llamadas Ice desde cmLogistics.

**Patron aplicado:** Broker (Servant), Repository + Singleton, Controller.

**Edge cases:**
- Despacho de recurso inexistente: retorna false.
- Despacho de cantidad mayor a la disponible: retorna false, inventario no se modifica.
- Archivo `.bd` corrupto al arranque: se arranca con inventario vacio y se carga el inicial.

**Acceptance criteria:**
- **Given** bodegaCentral arrancada, **when** cmLogistics llama `hayExistencias(CAFE, 500)`, **then** retorna `true` (inventario inicial tiene 3000).
- **Given** inventario de CAFE=3000, **when** se llama `despachar(CAFE, 500, 1, 1)`, **then** CAFE queda en 2500 y retorna `true`.
- **Given** inventario de KITREPARACION=0, **when** se llama `despachar(KITREPARACION, 1, 1, 6)`, **then** retorna `false`.
- **Given** consola de bodega, **when** se selecciona "Recargar" con recurso=CAFE cantidad=1000, **then** inventario de CAFE aumenta en 1000.

---

## 2. Plan

**Architecture:**
- `BodegaCentral.java`: main + arranque Ice + hilo daemon para consola.
- `BodegaService`: servant Ice que implementa `ServicioBodega`. Delega a `InventarioRepositorio`.
- `InventarioRepositorio`: Singleton standalone (NO hereda de `Repositorio<K,T>` de coffeeMach). Serializa HashMap a `inventarioBodega.bd`.
- `ItemInventario`: POJO serializable para cada recurso.

**Data model (local):**
- Archivo: `inventarioBodega.bd`
- Contenido: `HashMap<String, ItemInventario>` serializado.
- Claves: nombres del enum sin guiones bajos (`MONEDA100`, `MONEDA200`, `MONEDA500`, `AGUA`, `CAFE`, `AZUCAR`, `VASO`, `KITREPARACION`).

| Recurso | Cantidad inicial |
|---|---|
| MONEDA100 | 100 |
| MONEDA200 | 100 |
| MONEDA500 | 100 |
| AGUA | 10000 |
| CAFE | 3000 |
| AZUCAR | 3000 |
| VASO | 500 |
| KITREPARACION | 10 |

**Config:** `bodegaCentral/src/main/resources/BodegaCentral.cfg`
```
BodegaCentral.Endpoints = tcp -h localhost -p 12347
```

**Nota sobre clases existentes:** El modulo ya contiene `bodega/Bodega.java`, `mantenimientoExistencias/Inventario.java` y `guiInventario/Interfaz.java` como interfaces/stubs vacios. Se mantienen en el proyecto (no afectan compilacion) pero no se usan en la implementacion final.

---

## 3. Tasks

```
Task 1: ItemInventario.java
Depends on: none
What to build: POJO serializable con recurso, cantidad, metodos sumar/retirar.
Files: bodegaCentral/src/main/java/bodega/ItemInventario.java

Task 2: InventarioRepositorio.java
Depends on: Task 1
What to build: Singleton standalone con HashMap, serializacion a inventarioBodega.bd,
  cargarInicial() con 8 recursos, metodos hayExistencias/despachar/recargar/listar.
Files: bodegaCentral/src/main/java/bodega/InventarioRepositorio.java

Task 3: BodegaService.java
Depends on: Task 2
What to build: Servant Ice que implementa ServicioBodega. Delega a InventarioRepositorio.
Files: bodegaCentral/src/main/java/bodega/BodegaService.java

Task 4: BodegaCentral.java (reescribir)
Depends on: Task 3
What to build: main() con Ice communicator, ObjectAdapter, registro de servant,
  hilo daemon para consola interactiva.
Files: bodegaCentral/src/main/java/BodegaCentral.java

Task 5: BodegaCentral.cfg
Depends on: none
What to build: Archivo de configuracion con endpoint tcp puerto 12347.
Files: bodegaCentral/src/main/resources/BodegaCentral.cfg
```
