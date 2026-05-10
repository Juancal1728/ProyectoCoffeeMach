# Spec 02 — Interfaz gráfica y lógica de venta (coffeeMach)

> Estado: Implementado. Bug de thread-safety (frame desde hilo Ice) corregido con SwingUtilities.invokeLater. NPE en cargarRecetaMaquinas corregido.

---

## 1. Spec

**Purpose:** Permitir a un usuario final interactuar con la máquina de café — insertar monedas, seleccionar un producto, verificar el precio, ordenar y recibir el cambio — manteniendo la coherencia entre el saldo acumulado, los niveles de insumos y los depósitos de monedas.

**Users:**
- **Usuario final** (cliente) interactúa con la GUI física (representada por `Interfaz` en Swing).
- `ControladorMQ` actúa como mediador entre la GUI y los repositorios locales.

**Requirements:**
1. El usuario puede insertar monedas de 100, 200 o 500 pesos; el saldo acumulado se muestra en pantalla.
2. El usuario puede consultar el precio de un producto antes de ordenar.
3. El usuario puede ordenar un producto si el saldo es mayor o igual al precio; el saldo se descuenta al ordenar.
4. Si el saldo supera el precio, la máquina devuelve el cambio exacto usando la combinación greedy (500 → 200 → 100).
5. El usuario puede cancelar en cualquier momento; se devuelve el saldo completo.
6. Al ordenar, se decrementan los insumos de cada ingrediente según la receta del producto.
7. Tras cada operación de monedas o ingredientes, la GUI actualiza la vista de insumos.
8. La venta queda registrada localmente en `VentaRepositorio`.
9. El usuario puede actualizar la lista de productos desde el servidor central.
10. El estado de la máquina se respalda en disco tras cada mutación.

**Edge cases:**
- Saldo insuficiente: el pedido se rechaza con mensaje en pantalla, no se descuentan insumos.
- Cambio imposible: si los depósitos no tienen monedas suficientes para dar el vuelto exacto, la diferencia queda en `suma` distinta de cero; se imprime error en consola pero la operación no se revierte.
- Insumos en nivel mínimo: se genera alarma "low" y se notifica al servidor; la GUI permanece habilitada.
- Insumos en nivel crítico: se genera alarma "critical", se notifica al servidor y la GUI se deshabilita.
- La máquina aparece deshabilitada si tiene alarmas activas; se rehabilita solo cuando todas las alarmas han sido resueltas por abastecimiento.

**Acceptance criteria:**
- **Given** saldo=0, **when** el usuario inserta una moneda de 100, **then** el campo de saldo muestra `100`.
- **Given** saldo=500 y producto cuesta 300, **when** el usuario ordena, **then** saldo pasa a 0 y el área de devolución muestra la devolución correcta de 200.
- **Given** saldo=200 y producto cuesta 300, **when** el usuario intenta ordenar, **then** aparece el mensaje "Saldo insuficiente" y los insumos no cambian.
- **Given** el usuario presiona Cancelar con saldo=600, **then** saldo pasa a 0 y el área de devolución muestra la devolución de 600.
- **Given** un ingrediente cae a nivel crítico tras una venta, **then** la GUI queda deshabilitada (`interfazDeshabilitada()`) y se registra la alarma en `AlarmaRepositorio`.
- **Given** el usuario presiona "Actualizar", **then** `RecetaRepositorio` se recarga con los productos del servidor y el combobox refleja los nuevos productos.

---

## 2. Plan

**Architecture:**
- `ControladorMQ` implementa `Runnable` y `ServicioAbastecimiento`. Se instancia en `CoffeeMach.main()` y corre en el hilo principal.
- `Interfaz` es un `JFrame` generado por el diseñador Swing de Eclipse; `ControladorMQ` accede a sus componentes directamente vía getters.
- Los proxies Ice (`AlarmaServicePrx`, `VentaServicePrx`, `RecetaServicePrx`) se inyectan en el controlador desde `CoffeeMach.main()`.

**Data model (local):**
- `Receta`: id, descripción, valor, `HashMap<Ingrediente, Double>` de ingredientes con unidades requeridas.
- `Ingrediente`: nombre, cantidad, mínimo, crítico, máximo, `codAlarma`.
- `DepositoMonedas`: tipo (100/200/500), cantidad, mínimo, crítico.
- `Venta`: id, descripción, valor, fecha.

**API contracts (Ice outbound desde coffeeMach):**
- `AlarmaServicePrx.recibirNotificacionEscasezIngredientes(String iDing, int idMaq)`
- `AlarmaServicePrx.recibirNotificacionInsuficienciaMoneda(Moneda moneda, int idMaq)`
- `AlarmaServicePrx.recibirNotificacionMalFuncionamiento(int idMaq, String descri)`
- `AlarmaServicePrx.recibirNotificacionAbastesimiento(int idMaq, String idInsumo, int cantidad)`
- `VentaServicePrx.registrarVenta(int codMaq, String[] ventas)` — ventas codificadas como `"idReceta#precio"`
- `RecetaServicePrx.consultarProductos()` — retorna productos codificados como `"id-nombre-precio#idIng-nomIng-...-qty#..."`

**Testing strategy:** No hay tests automatizados. Verificación manual ejecutando `coffeeMach.jar`.

**Security constraints:** Ninguna autenticación en la GUI local. El acceso físico implica confianza.

**Dependencies:** ZeroC Ice 3.7.6 (Ice communicator y proxies), Spec 01 (repositorios locales).

---

## 3. Tasks (completadas)

```
Task 1: Diseño de la GUI (Interfaz.java)
Depends on: none
What was built: JFrame con botones de inserción de monedas (100, 200, 500),
  botón Cancelar, ComboBox de productos, botones Verificar/Ordenar/Mantenimiento/
  EnviarReporte/Actualizar, y áreas de texto para saldo, devolución, info,
  insumos, recetas y alarmas.
Acceptance criteria:
- Todos los botones y áreas de texto son accesibles mediante getters desde ControladorMQ.
- La ventana se muestra centrada al llamar setVisible(true).

Task 2: Lógica de inserción de monedas
Depends on: Task 1, Spec 01
What was built: ActionListeners en btnIngresar100/200/500 que incrementan `suma`,
  actualizan el saldo en pantalla y actualizan el depósito en MonedasRepositorio.
Acceptance criteria:
- Insertar moneda de 100 incrementa el saldo visualizado en 100 y el depósito "100" en 1.

Task 3: Lógica de ordenar producto
Depends on: Task 2
What was built: ActionListener en btnOrdenar que verifica saldo >= precio,
  descuenta insumos via disminuirInsumos(), llama devolverMonedas(), registra la
  venta en VentaRepositorio y llama respaldarMaq().
Acceptance criteria:
- Ordenar con saldo suficiente decrementa exactamente los ingredientes de la receta.
- La venta queda en ventas.bd con el id de receta y el precio correcto.

Task 4: Lógica de devolución de cambio
Depends on: Task 3
What was built: devolverMonedas() aplica algoritmo greedy (500→200→100),
  descuenta los depósitos y muestra el desglose en el área de devolución.
Acceptance criteria:
- Un cambio de 700 devuelve 1x500 + 1x200 y registra la reducción en MonedasRepositorio.

Task 5: Verificación de niveles y generación de alarmas
Depends on: Task 3, Spec 03
What was built: verificarProductos() y verificarMonedas() llamados tras cada
  venta/devolución; crean Alarma en AlarmaRepositorio y envían notificación Ice
  al servidor; deshabilitan la GUI en nivel crítico.
Acceptance criteria:
- Cuando cantidad de un ingrediente <= mínimo, se agrega la alarma "low" a AlarmaRepositorio.
- Cuando cantidad <= crítico, la GUI queda deshabilitada y se envía recibirNotificacionEscasezIngredientes.

Task 6: Carga de recetas desde servidor
Depends on: Task 1, Spec 05
What was built: cargarRecetaMaquinas() llama RecetaServicePrx.consultarProductos(),
  parsea el formato delimitado y reconstruye RecetaRepositorio; actualiza combobox e
  insumos en pantalla.
  Fix aplicado: si el servidor devuelve un ingrediente no existente en IngredienteRepositorio,
  se crea el Ingrediente y se guarda con ingredientes.addElement(nombre, ingred).
  Sin este fix, disminuirInsumos() lanzaba NPE al vender un producto con ese ingrediente.
Acceptance criteria:
- Tras presionar Actualizar, el combobox refleja exactamente los productos activos en el servidor.
- No hay NPE al vender productos con ingredientes que no estaban en el repositorio local.
```

---

## Assumptions to review

1. **El algoritmo greedy de cambio siempre puede satisfacer el vuelto con las monedas disponibles** — Impact: HIGH
   Correct this if: los depósitos de cierta denominación están vacíos y el cambio no es múltiplo de las denominaciones restantes.

2. **`suma` nunca acumula fracciones de centavo** — Impact: MEDIUM
   Correct this if: se introducen denominaciones distintas o precios no múltiplos de 100.

3. **La GUI Swing se actualiza desde el hilo Ice usando SwingUtilities.invokeLater** — Impact: HIGH
   CORREGIDO: `aplicarAbastecimiento()` envuelve todas las llamadas a `frame.*` y `actualizarXxxGraf()` en `SwingUtilities.invokeLater(...)`. El hilo Ice ya no modifica el EDT directamente.
