# Spec 10 — Cliente cmLogistics completo

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Implementar el cliente de logistica que permite al operador autenticarse, consultar alarmas pendientes de sus maquinas asignadas, y resolver cada alarma coordinando bodegaCentral (despacho de recursos) y coffeeMach (callback de abastecimiento).

**Users:**
- **Operador de logistica** interactua via consola.

**Requirements:**
1. El operador inicia sesion con codigo y password via `ServicioComLogisticaPrx.inicioSesion()`.
2. Si la autenticacion falla, no puede realizar ninguna operacion (Secure Messaging).
3. El operador consulta alarmas pendientes via `ServicioComLogisticaPrx.consultarAlarmasPendientes()`.
4. Solo ve alarmas de maquinas asignadas a el (filtro en servidor).
5. Para resolver una alarma:
   a. Verifica existencias en bodega con `ServicioBodegaPrx.hayExistencias()`.
   b. Despacha desde bodega con `ServicioBodegaPrx.despachar()`.
   c. Llama callback a la maquina con `ServicioAbastecimientoPrx.abastecerRecurso()`.
6. Si cualquier paso falla, la operacion se detiene y se informa al operador.
7. La interfaz es por consola (no GUI Swing).

**Patron aplicado:** Broker (Client Proxy), Callback, Secure Messaging, Controller.

**Edge cases:**
- Operador no autenticado intenta consultar: se rechaza con mensaje.
- Bodega sin existencias: se informa "sin existencias", no se llama a la maquina.
- Maquina no responde (red): excepcion Ice, se informa al operador.
- Alarma con recurso invalido (no parseable como RecursoAbastecimiento): excepcion en valueOf().

**Acceptance criteria:**
- **Given** operador con credenciales validas, **when** selecciona "Iniciar sesion", **then** muestra "Sesion iniciada".
- **Given** operador autenticado con alarmas pendientes, **when** selecciona "Consultar alarmas", **then** muestra lista formateada.
- **Given** alarma seleccionada y bodega con stock, **when** resuelve alarma, **then** bodega descuenta, maquina recibe callback, y muestra "Alarma resuelta".
- **Given** bodega sin stock, **when** intenta resolver, **then** muestra "sin existencias" y la alarma permanece pendiente.

---

## 2. Plan

**Architecture:**
- `CmLogistics.java`: main + arranque Ice + proxies + consola interactiva.
- `LogisticaController.java`: coordinador — logica de iniciarSesion, consultarAlarmas y resolverAlarma.
- cmLogistics NO registra servants (solo es cliente).

**Proxies consumidos:**

| Property en cfg | Interfaz | Identidad | Host:Puerto |
|---|---|---|---|
| ServerCentral | ServicioComLogisticaPrx | logistica | :12345 |
| MaquinaCafe | ServicioAbastecimientoPrx | abastecer | :12346 |
| BodegaCentral | ServicioBodegaPrx | bodega | :12347 |

**Config:** `cmLogistics/src/main/resources/CmLogistic.cfg`
```
ServerCentral = logistica:tcp -h localhost -p 12345
MaquinaCafe = abastecer:tcp -h localhost -p 12346
BodegaCentral = bodega:tcp -h localhost -p 12347
```

**Formato de alarma recibida:** `"idMaquina|idAlarma|recurso|cantidad|ubicacion|descripcion"`
- `recurso` debe ser un valor valido de `RecursoAbastecimiento.valueOf()`.

---

## 3. Tasks

```
Task 1: LogisticaController.java
Depends on: Spec 08 (alarmas pendientes), Spec 09 (bodega)
What to build: Clase con constructor que recibe los 3 proxies. Metodos:
  iniciarSesion(), consultarAlarmas(), resolverAlarma().
  resolverAlarma parsea la alarma, verifica bodega, despacha, llama callback.
Files: cmLogistics/src/main/java/logistica/LogisticaController.java

Task 2: CmLogistics.java (reescribir)
Depends on: Task 1
What to build: main() inicializa 3 proxies Ice, crea LogisticaController,
  ejecuta consola interactiva con opciones login/consultar/salir.
Files: cmLogistics/src/main/java/CmLogistics.java

Task 3: Actualizar CmLogistic.cfg
Depends on: none
What to build: Agregar linea BodegaCentral, cambiar default a tcp en MaquinaCafe.
Files: cmLogistics/src/main/resources/CmLogistic.cfg
```
