# Spec 15 — Diagrama de Deployment: Tarjetas CRC y Patrones

> Tipo: Referencia de arquitectura. No genera codigo nuevo.

---

## 1. Proposito

Documentar los componentes del diagrama de deployment del sistema ProyectoCoffeeMach usando tarjetas CRC (Class-Responsibility-Collaborator), sus relaciones de comunicacion y los patrones de diseno implementados en cada nodo. Este spec sirve como guia para construir el diagrama en la sustentacion y para explicar la arquitectura distribuida.

---

## 2. Tarjetas CRC — Componentes del Deployment

### CRC 1: PostgreSQL (Base de datos)

| | |
|---|---|
| **Clase/Nodo** | PostgreSQL 14+ (puerto 5430/5432) |
| **Responsabilidades** | Persistir operadores, maquinas, ingredientes, alarmas, ventas, recetas y asignaciones. Garantizar integridad referencial via constraints. Proveer transacciones ACID. |
| **Colaboradores** | ServidorCentral (unico cliente directo via JDBC) |

**Detalles:**
- Base de datos: `coffeemachine`
- Usuario: `cofmachu`
- Tablas principales: `OPERADORES`, `MAQUINA`, `INGREDIENTE`, `RECETA`, `RECETA_INGREDIENTE`, `ALARMA_MAQUINA`, `VENTAS`, `ASIGNACION_MAQUINA`
- Conexion gestionada por `ConexionBD` + `ManejadorDatos` (patron DAO)

---

### CRC 2: ServidorCentral (nodo central)

| | |
|---|---|
| **Clase/Nodo** | ServidorCentral.jar (puerto Ice 12345) |
| **Responsabilidades** | Autenticar operadores. Registrar y cerrar alarmas en BD y repositorio local. Filtrar alarmas por asignacion de operador (Secure Messaging). Gestionar recetas via consola. Mantener sesiones de logistica. Persistir alarmas pendientes en `alarmasPendientes.bd`. |
| **Colaboradores** | PostgreSQL (via JDBC), coffeeMach (recibe notificaciones), cmLogistics (entrega alarmas y recibe confirmacion de abastecimiento) |

**Servants Ice (identidades):**

| Identidad | Interfaz Ice | Clase Java |
|---|---|---|
| `Alarmas` | `AlarmaService` | `alarma/Alarma.java` |
| `Ventas` | `VentaService` | `ventas/VentasManager.java` |
| `Recetas` | `RecetaService` | `receta/ProductoReceta.java` |
| `logistica` | `ServicioComLogistica` | `comunicacion/ControlComLogistica.java` |

**Patrones:**
- **Broker**: ObjectAdapter Ice en 12345 despacha llamadas a servants
- **Repository + Singleton**: `AlarmasPendientesRepositorio` (standalone, `synchronized`)
- **DAO**: `ConexionBD` + `ManejadorDatos` con PreparedStatement y try-with-resources
- **Secure Messaging**: `inicioSesion()` + filtro por `ASIGNACION_MAQUINA` en `consultarAlarmasPendientes()`

---

### CRC 3: coffeeMach (maquina expendedora)

| | |
|---|---|
| **Clase/Nodo** | coffeeMach.jar (puerto Ice 12346, GUI Swing) |
| **Responsabilidades** | Vender productos (debitar ingredientes y monedas). Detectar escasez y notificar alarmas al servidor. Recibir callbacks de abastecimiento desde cmLogistics. Persistir estado local en archivos `.bd`. Mostrar GUI al usuario final. |
| **Colaboradores** | ServidorCentral (envia alarmas y ventas, recibe recetas), cmLogistics (recibe callback de abastecimiento) |

**Servants Ice:**

| Identidad | Interfaz Ice | Clase Java |
|---|---|---|
| `abastecer` | `ServicioAbastecimiento` | `McControlador/ControladorMQ.java` |

**Repositorios locales:**

| Archivo | Repositorio |
|---|---|
| `alarmas.bd` | `AlarmaRepositorio` |
| `ingredientes.bd` | `IngredienteRepositorio` |
| `monedas.bd` | `MonedasRepositorio` |
| `recetas.bd` | `RecetaRepositorio` |
| `ventas.bd` | `VentaRepositorio` |

**Patrones:**
- **Broker**: Proxy `AlarmaServicePrx` (llamadas salientes), Servant `abastecer` (llamadas entrantes)
- **Callback**: `abastecerRecurso()` es el endpoint receptor del callback
- **Repository + Singleton**: Los 5 repositorios con `synchronized getInstance()`
- **Controller (MVC)**: `ControladorMQ` alambra la GUI y es el servant Ice
- `SwingUtilities.invokeLater` para actualizaciones de GUI desde el hilo Ice

---

### CRC 4: bodegaCentral (almacen de recursos)

| | |
|---|---|
| **Clase/Nodo** | BodegaCentral.jar (puerto Ice 12347) |
| **Responsabilidades** | Gestionar inventario de los 8 recursos del sistema. Verificar existencias antes de despachar. Decrementar inventario al despachar. Persistir inventario en `inventarioBodega.bd`. |
| **Colaboradores** | cmLogistics (recibe consultas de existencias y ordenes de despacho) |

**Servants Ice:**

| Identidad | Interfaz Ice | Clase Java |
|---|---|---|
| `bodega` | `ServicioBodega` | `bodega/BodegaService.java` |

**Patrones:**
- **Broker**: ObjectAdapter Ice en 12347
- **Repository + Singleton**: `InventarioRepositorio` (standalone, todos los metodos `synchronized`)

---

### CRC 5: cmLogistics (cliente operador)

| | |
|---|---|
| **Clase/Nodo** | CmLogistics.jar (solo cliente, no expone servants) |
| **Responsabilidades** | Autenticar operador de mantenimiento. Consultar alarmas pendientes de sus maquinas. Coordinar resolucion de alarmas (bodega -> maquina). Mostrar GUI Swing al operador. |
| **Colaboradores** | ServidorCentral (autenticacion + consulta alarmas), bodegaCentral (verificar existencias + despachar), coffeeMach (enviar callback de abastecimiento) |

**Proxies consumidos:**

| Proxy | Identidad | Puerto |
|---|---|---|
| `ServicioComLogisticaPrx` | `logistica` | 12345 |
| `ServicioAbastecimientoPrx` | `abastecer` | 12346 |
| `ServicioBodegaPrx` | `bodega` | 12347 |

**Paquetes Java relevantes:**

| Paquete | Clase principal | Rol |
|---|---|---|
| `logistica` | `LogisticaController` | Orquestador MVC |
| `controlAlarma` | `ControlAlarma` | Logica de alarmas |
| `controlAlarma.gui` | `PanelAlarmas` | Vista del panel de alarmas |
| `tecnicoMantenimiento` | `TecnicoMantenimiento` | Modelo del operador |
| `zonaGeografica` | `ZonaGeografica` | Modelo de zona/maquina |
| `interfaz` | `InterfazLogistica`, `ControladorLogistica` | Vista + Controlador MVC |

**Patrones:**
- **Broker**: Los 3 proxies Ice
- **Callback**: Invoca `abastecerRecurso()` en coffeeMach como paso 3 de la resolucion
- **Secure Messaging**: `TecnicoMantenimiento.isAutenticado()` bloquea operaciones si no hay sesion
- **Controller (MVC)**: `ControladorLogistica` -> `LogisticaController` -> `ControlAlarma`

---

## 3. Relaciones en el Diagrama de Deployment

```
                    ┌──────────────────────────────────────────┐
                    │            PostgreSQL :5430               │
                    │      (motor de persistencia central)      │
                    └───────────────────┬──────────────────────┘
                                        │ JDBC
                    ┌───────────────────▼──────────────────────┐
                    │         ServidorCentral :12345            │
                    │  Alarmas | Ventas | Recetas | logistica   │
                    └──────┬─────────────────────────┬─────────┘
              Ice:12345    │                         │ Ice:12345
          (notif. alarmas) │                         │ (consulta/resolucion alarmas)
                    ┌──────▼──────┐         ┌────────▼──────────┐
                    │  coffeeMach │         │    cmLogistics     │
                    │   :12346    │◄────────┤  (solo cliente)    │
                    │  abastecer  │Ice:12346│                    │
                    └─────────────┘callback └────────┬──────────┘
                                                      │ Ice:12347
                                            ┌─────────▼──────────┐
                                            │   bodegaCentral    │
                                            │       :12347       │
                                            │      bodega        │
                                            └────────────────────┘
```

**Flujo de alarma (Callback Pattern — sentido de las llamadas):**
1. `coffeeMach` → `ServidorCentral` : `recibirNotificacionEscasez*()`
2. `cmLogistics` → `ServidorCentral` : `inicioSesion()`, `consultarAlarmasPendientes()`
3. `cmLogistics` → `bodegaCentral` : `hayExistencias()`, `despachar()`
4. `cmLogistics` → `coffeeMach` : `abastecerRecurso()` **(el callback)**
5. `coffeeMach` → `ServidorCentral` : `recibirNotificacionAbastesimiento()` (cierre de alarma)

---

## 4. Patrones transversales y donde aparecen

| Patron | Nodo(s) | Clase(s) clave |
|---|---|---|
| **Broker** | Todos | ObjectAdapter Ice, `*Prx`, `.cfg` files |
| **Callback** | coffeeMach + cmLogistics | `abastecerRecurso()` en `ControladorMQ`, invocado desde `ControlAlarma` |
| **Secure Messaging** | ServidorCentral + cmLogistics | `inicioSesion()`, `consultarAlarmasPendientes()` filtrado, `TecnicoMantenimiento.isAutenticado()` |
| **Repository + Singleton** | coffeeMach, ServidorCentral, bodegaCentral | `*Repositorio`, `AlarmasPendientesRepositorio`, `InventarioRepositorio` |
| **Controller (MVC)** | coffeeMach, cmLogistics, ServidorCentral | `ControladorMQ`, `ControladorLogistica`/`LogisticaController`, `ControlComLogistica` |
| **DAO** | ServidorCentral | `ConexionBD` + `ManejadorDatos` |

---

## 5. Notas para el diagrama UML de deployment

- Cada JAR es un `<<artifact>>` deployado en su `<<node>>` (PC o proceso).
- Los ObjectAdapters Ice son `<<component>>` dentro de cada nodo servidor.
- Las lineas de comunicacion tienen estereotipo `<<TCP/IP Ice>>` con el numero de puerto.
- La conexion JDBC entre ServidorCentral y PostgreSQL tiene estereotipo `<<JDBC>>`.
- Las dependencias de proxy (cmLogistics -> coffeeMach, etc.) se marcan con `<<use>>` + direccion de llamada.
- El Callback se visualiza como una flecha inversa: cmLogistics invoca un servant en coffeeMach.
