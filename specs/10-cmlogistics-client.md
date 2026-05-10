# Spec 10 — Cliente cmLogistics completo

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Implementar el cliente de logistica que permite al operador autenticarse, consultar alarmas pendientes de sus maquinas asignadas, y resolver cada alarma coordinando bodegaCentral (despacho de recursos) y coffeeMach (callback de abastecimiento). La interfaz es Swing (GUI), no consola.

**Users:**
- **Operador de logistica (tecnico de mantenimiento)** interactua via ventana Swing.

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
7. La interfaz es Swing (`InterfazLogistica`) con panel de sesion, panel de alarmas, log de eventos e inventario.
8. El estado del operador autenticado se encapsula en `TecnicoMantenimiento`.
9. La gestion de alarmas se delega a `ControlAlarma` (separacion de responsabilidades).
10. Las zonas/maquinas asignadas se modelan con `ZonaGeografica`.

**Patron aplicado:** Broker (Client Proxy), Callback, Secure Messaging, Controller (MVC), Repository (TecnicoMantenimiento como estado del operador).

**Edge cases:**
- Operador no autenticado intenta consultar: se rechaza con mensaje en el log de eventos.
- Bodega sin existencias: se informa "sin existencias", no se llama a la maquina.
- Maquina no responde (red): excepcion Ice capturada, se informa al operador.
- Alarma con recurso invalido (no parseable como RecursoAbastecimiento): excepcion en valueOf().

**Acceptance criteria:**
- **Given** operador con credenciales validas, **when** click en "Iniciar Sesion", **then** lblEstado muestra "Sesion iniciada para operador N".
- **Given** operador autenticado con alarmas pendientes, **when** click en "Consultar", **then** comboBoxAlarmas muestra alarmas formateadas.
- **Given** alarma seleccionada y bodega con stock, **when** click en "Resolver", **then** bodega descuenta, maquina recibe callback, log muestra "Alarma resuelta exitosamente".
- **Given** bodega sin stock, **when** intenta resolver, **then** log muestra "Bodega sin existencias" y la alarma permanece pendiente.

---

## 2. Plan

**Architecture:**

```
cmLogistics/src/main/java/
  CmLogistics.java                     — main: Ice + proxies + lanza ControladorLogistica en EDT
  controlAlarma/
    ControlAlarma.java                 — servicio de alarmas: consultar, resolver, flujo bodega->maquina
    gui/
      PanelAlarmas.java                — JPanel reutilizable: comboBox + botones Consultar/Resolver
  tecnicoMantenimiento/
    TecnicoMantenimiento.java          — modelo del operador autenticado (codigoOperador + autenticado)
  zonaGeografica/
    ZonaGeografica.java                — modelo de maquina asignada (idMaquina + ubicacion)
  logistica/
    LogisticaController.java          — orquestador: delega en ControlAlarma + TecnicoMantenimiento
  interfaz/
    InterfazLogistica.java            — JFrame principal (MVC vista)
    ControladorLogistica.java         — MVC controlador: wiring de eventos Swing
```

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

**Formato mostrado en UI:** `"Maq <id> - <recurso> x<cantidad> - <descripcion>"`

---

## 3. Tasks (completadas)

```
Task 1: TecnicoMantenimiento.java
Depends on: none
What was built: Clase Serializable con campos codigoOperador (int) y autenticado (boolean).
  Encapsula el estado del operador activo en cmLogistics.
  Implementa toString() para el log de eventos.
Files: cmLogistics/src/main/java/tecnicoMantenimiento/TecnicoMantenimiento.java

Task 2: ZonaGeografica.java
Depends on: none
What was built: Clase con campos idMaquina (int) y ubicacion (String).
  Metodo estatico fromString(String dato) parsea formato "idMaquina-ubicacion".
  Retorna null si el formato es invalido.
Files: cmLogistics/src/main/java/zonaGeografica/ZonaGeografica.java

Task 3: ControlAlarma.java
Depends on: Spec 08 (alarmas pendientes en servidor), Spec 09 (bodega)
What was built: Servicio de alarmas extraido de LogisticaController.
  consultarAlarmas(codigoOperador): delega en ServicioComLogisticaPrx.
  consultarZonasAsignadas(codigoOperador): parsea asignaciones via ZonaGeografica.fromString().
  resolverAlarma(alarmaTexto): flujo completo — parsea alarma, hayExistencias, despachar, abastecerRecurso.
  El flujo de resolucion implementa el patron Callback: bodega -> coffeeMach.
Files: cmLogistics/src/main/java/controlAlarma/ControlAlarma.java

Task 4: PanelAlarmas.java
Depends on: none
What was built: JPanel con layout absoluto. Contiene:
  - JLabel "Alarmas pendientes:"
  - JComboBox<String> comboBoxAlarmas (lista de alarmas formateadas)
  - JButton btnConsultarAlarmas
  - JButton btnResolverAlarma
  Vista pura sin logica de negocio (MVC).
Files: cmLogistics/src/main/java/controlAlarma/gui/PanelAlarmas.java

Task 5: LogisticaController.java (refactorizado)
Depends on: Task 1, Task 2, Task 3
What was built: Orquestador que recibe los 3 proxies Ice y crea ControlAlarma.
  iniciarSesion(): autentica y crea TecnicoMantenimiento.
  consultarAlarmas(): delega en ControlAlarma.
  resolverAlarma(): delega en ControlAlarma.
  consultarZonasAsignadas(): delega en ControlAlarma.
  consultarInventario(): llama directamente a ServicioBodegaPrx.
  getTecnicoActual(): expone el operador autenticado.
Files: cmLogistics/src/main/java/logistica/LogisticaController.java

Task 6: InterfazLogistica.java (actualizada)
Depends on: Task 4
What was built: JFrame principal con paneles de sesion, PanelAlarmas embebido,
  panel de eventos (log) y panel de inventario.
  PanelAlarmas se instancia e incrusta como componente reutilizable.
Files: cmLogistics/src/main/java/interfaz/InterfazLogistica.java

Task 7: ControladorLogistica.java (actualizado)
Depends on: Task 5, Task 6
What was built: Controlador MVC que alambra todos los eventos Swing.
  Usa TecnicoMantenimiento para guardar estado del operador.
  Accede a controles de alarma via interfaz.getPanelAlarmas().
  Implementa Runnable para lanzarse desde SwingUtilities.invokeLater en CmLogistics.main().
Files: cmLogistics/src/main/java/interfaz/ControladorLogistica.java

Task 8: CmLogistics.java (actualizado)
Depends on: Task 5, Task 7
What was built: main() crea Ice communicator, resuelve los 3 proxies via checkedCast,
  crea LogisticaController, lanza ControladorLogistica en EDT via SwingUtilities.invokeLater.
Files: cmLogistics/src/main/java/CmLogistics.java

Task 9: Actualizar CmLogistic.cfg
Depends on: none
What was built: Tres lineas de configuracion: ServerCentral, MaquinaCafe, BodegaCentral.
Files: cmLogistics/src/main/resources/CmLogistic.cfg
```
