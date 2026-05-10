# Spec 04 — Reporte de ventas al servidor central

> Estado: Implementado. Ventas ahora usan clave unica con timestamp para evitar sobrescritura al vender el mismo producto varias veces.

---

## 1. Spec

**Purpose:** Permitir que la máquina de café envíe al servidor central un resumen de todas las ventas acumuladas desde el último reporte, para que el servidor las persista en la base de datos con su desglose por receta.

**Users:**
- **Operador de logística / técnico** activa el reporte manualmente desde la GUI de la máquina (botón "Enviar Reporte").
- **Servidor central** (`VentasManager`) recibe y persiste el reporte.

**Requirements:**
1. Al presionar "Enviar Reporte", la máquina recopila todas las ventas de `VentaRepositorio` y las serializa como array de strings con formato `"idReceta#precio"`.
2. La máquina envía el array al servidor mediante `VentaService.registrarVenta(codMaq, ventas[])`.
3. El servidor crea un registro en `VENTAS` con `IDMAQUINA`, `FECHA_INICIAL`, `FECHA_FINAL` (ambas = `now()`) y `VALOR` calculado como suma de los valores de la venta.
4. Por cada venta individual en el array, el servidor inserta una fila en `VENTAS_RECETA` con `ID_RECETA` y el `CONSECUTIVO` del registro padre.
5. El valor total de la venta en `VENTAS` se actualiza con la suma al final del procesamiento.
6. Cada registro usa secuencias PostgreSQL (`CONSECUTIVO`, `CONSECUTIVO1`) para sus IDs.

**Edge cases:**
- Array de ventas vacío: se crea un registro `VENTAS` con `VALOR = 0` y sin filas en `VENTAS_RECETA`.
- `idReceta` en el array no existe en la tabla `RECETA`: el INSERT en `VENTAS_RECETA` fallará con `SQLException` (FK violation).
- Fallo de BD a mitad del batch: el registro `VENTAS` puede quedar sin todas las filas `VENTAS_RECETA` — no hay transacción explícita.
- No hay mecanismo que limpie `VentaRepositorio` local tras el envío — las mismas ventas se podrían reportar múltiples veces.

**Acceptance criteria:**
- **Given** `VentaRepositorio` con 3 ventas (recetas A, B, C), **when** el usuario presiona "Enviar Reporte", **then** se inserta 1 fila en `VENTAS` y 3 filas en `VENTAS_RECETA`.
- **Given** el array enviado contiene `"5#300"`, **then** la fila en `VENTAS_RECETA` tiene `ID_RECETA = 5` y `CONSECUTIVO_VENTAS` igual al consecutivo del registro padre en `VENTAS`.
- **Given** el valor de las 3 ventas es 300, 400, 500, **then** la fila en `VENTAS` tiene `VALOR = 1200`.
- **Given** un array vacío, **then** se inserta 1 fila en `VENTAS` con `VALOR = 0` y ninguna fila en `VENTAS_RECETA`.

---

## 2. Plan

**Architecture:**
- `VentasManager` es el servant Ice que implementa `VentaService`. Está registrado con identidad `"Ventas"` en el `ObjectAdapter` del servidor.
- `reporteVentas()` abre una conexión BD, construye el grafo de objetos `VentasMaquina` + `List<VentasReceta>` y delega la persistencia a `ManejadorDatos.registrarReporteVentas()`.
- No hay pool de conexiones; cada llamada abre y cierra su propia conexión.

**Data model (PostgreSQL):**
- `VENTAS(CONSECUTIVO PK, IDMAQUINA FK, VALOR, FECHA_INICIAL, FECHA_FINAL)` — encabezado del reporte.
- `VENTAS_RECETA(CONSECUTIVO PK, ID_RECETA FK, CONSECUTIVO_VENTAS FK)` — detalle por receta.
- Secuencias: `CONSECUTIVO` (para `VENTAS`), `CONSECUTIVO1` (para `VENTAS_RECETA`).

**Wire format (Ice):**
```
VentaService.registrarVenta(int codMaq, StringArr ventas)
// ventas[i] = "idReceta#precio"
```

**Testing strategy:** No hay tests automatizados. Verificación manual: registrar ventas en la máquina, presionar "Enviar Reporte" y consultar tablas `VENTAS` y `VENTAS_RECETA`.

**Security constraints:** No hay autenticación. Cualquier cliente puede llamar `registrarVenta` con cualquier `codMaq`.

**Dependencies:** ZeroC Ice 3.7.6, PostgreSQL, JDBC, Spec 01 (`VentaRepositorio`), Spec 02 (botón "Enviar Reporte").

---

## 3. Tasks (completadas)

```
Task 1: Servant VentasManager (ServidorCentral)
Depends on: none
What was built: Clase VentasManager implementando VentaService (Ice). Registrada
  con identidad "Ventas". registrarVenta() parsea cada string "idReceta#precio",
  construye VentasMaquina y VentasReceta, y llama ManejadorDatos.registrarReporteVentas().
Acceptance criteria:
- El servant queda registrado con identidad "Ventas" en el ObjectAdapter.
- El parsing de "5#300" produce VentasReceta con idReceta=5 y valorReceta=300.

Task 2: ManejadorDatos.registrarReporteVentas()
Depends on: Task 1
What was built: Inserta fila en VENTAS con NEXTVAL('CONSECUTIVO'), itera la lista
  de VentasReceta para insertar en VENTAS_RECETA con NEXTVAL('CONSECUTIVO1'),
  y actualiza VALOR total en VENTAS con un UPDATE final.
Acceptance criteria:
- La suma de valorReceta de todas las filas en VentasReceta == VALOR en la fila padre de VENTAS.
- Cada fila de VENTAS_RECETA tiene CONSECUTIVO_VENTAS igual al CONSECUTIVO del padre.

Task 3: Registro local de ventas en coffeeMach
Depends on: Spec 02
What was built: En ControladorMQ.btnOrdenar, se crea una instancia Venta con
  idReceta, precio y fecha, y se agrega a VentaRepositorio.
  Fix aplicado: la clave del HashMap usa timestamp (System.currentTimeMillis()) concatenado
  al idReceta para evitar que dos ventas del mismo producto se sobrescriban en el repositorio.
Acceptance criteria:
- Tras ordenar la misma receta dos veces, VentaRepositorio tiene dos entradas distintas (no una).
- El array enviado al servidor contiene una entrada por cada venta registrada.
  
Task 4: Envío del reporte desde coffeeMach
Depends on: Task 1, Task 3
What was built: btnEnviarReporte recupera todas las ventas de VentaRepositorio,
  las serializa a String[] en formato "idReceta#precio" y llama
  VentaServicePrx.registrarVenta(codMaquina, arregloVentas).
Acceptance criteria:
- El array enviado contiene exactamente una entrada por cada venta en VentaRepositorio.
```

---

## Assumptions to review

1. **VentaRepositorio no se limpia tras el envío** — Impact: HIGH
   Correct this if: se desea que cada reporte contenga solo las ventas del período actual. Actualmente el operador podría reportar las mismas ventas múltiples veces.

2. **FECHA_INICIAL y FECHA_FINAL del reporte son ambas `new Date()` (mismo instante)** — Impact: MEDIUM
   Correct this if: se requiere un período real de corte entre reportes.

3. **No hay transacción que agrupe INSERT en VENTAS + todos los INSERT en VENTAS_RECETA** — Impact: MEDIUM
   Correct this if: se requiere atomicidad del reporte completo (un fallo parcial deja datos inconsistentes).
