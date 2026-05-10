# Spec 16 — Informe Unificado de Despliegue y Ejecucion

> Tipo: Referencia de sustentacion. No genera codigo nuevo.

---

## 1. Proposito

Documentar el orden y los pasos exactos necesarios para desplegar y ejecutar el sistema ProyectoCoffeeMach completo, empezando por la base de datos y terminando en los clientes. Este informe esta pensado para la sustentacion y para el despliegue en laboratorio con multiples PCs.

---

## 2. Vista del diagrama de deployment con orden de ejecucion

```
Orden de despliegue:

[1] PostgreSQL
     |
     | JDBC
     v
[2] ServidorCentral (:12345)
     |              \
     | Ice           \ Ice
     v                v
[3] bodegaCentral   [4] coffeeMach (:12346)
    (:12347)              |
        \                 |  Ice (callback)
         \                |
          \___________[5] cmLogistics
```

**Regla fundamental:** cada componente debe estar operativo antes de arrancar el que depende de el. Si se arrancan fuera de orden, se obtienen excepciones Ice de tipo `ConnectionRefused`.

---

## 3. Pasos por componente en orden de despliegue

---

### PASO 1 — PostgreSQL

**Quien lo hace:** administrador de BD / PC de base de datos.

**Prerequisitos:**
- PostgreSQL instalado (recomendado 14+).
- Puerto 5430 (local) o 5432 (laboratorio) disponible.

**Comandos de inicializacion (solo la primera vez):**
```bash
# Crear usuario y base de datos
psql -f Code-2023-04-14-3/scripts/postgres/coffeemach-user.sql

# Crear esquema de tablas
psql -d coffeemachine -U cofmachu -f Code-2023-04-14-3/scripts/postgres/coffeemach-ddl.sql

# Cargar datos iniciales (operadores, maquinas, ingredientes, asignaciones)
psql -d coffeemachine -U cofmachu -f Code-2023-04-14-3/scripts/postgres/coffeemach-inserts.sql
```

**Verificacion:**
```bash
psql -d coffeemachine -U cofmachu -c "SELECT * FROM OPERADORES;"
# Debe mostrar al menos un operador (ej. id=1, contrasena=1123)
```

**Datos criticos para el sistema:**
- La tabla `ASIGNACION_MAQUINA` debe tener filas que asocien operadores a maquinas. Sin esto, `consultarAlarmasPendientes()` siempre retorna lista vacia.
- La tabla `MAQUINA` debe tener registros con `IDMAQUINA` = el entero en `codMaquina.cafe`.

---

### PASO 2 — ServidorCentral

**Quien lo hace:** PC1 (en laboratorio) o Terminal 2 (local).

**Prerequisitos:** PostgreSQL corriendo y accesible.

**Configuracion relevante** (`ServidorCentral/src/main/resources/server.cfg`):
```
ServidorCentral.Endpoints = tcp -h localhost -p 12345
Ice.Default.Host = localhost
# Conexion BD
Ice.Default.Package = servicios
Database.Host = localhost
Database.Port = 5430
Database.Name = coffeemachine
Database.User = cofmachu
Database.Password = cofmachpwd
ConsolaAdministracion.enabled = false
```
- En laboratorio: cambiar `localhost` en `Endpoints` y `Database.Host` por la IP real del PC1.
- Para habilitar la consola de recetas: `ConsolaAdministracion.enabled = true`.

**Comando:**
```bash
cd Code-2023-04-14-3/coffeemach
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
java -jar ServidorCentral/build/libs/ServidorCentral.jar
```

**Verificacion:** El servidor imprime `"ServidorCentral iniciado en puerto 12345"` o similar. Los 4 servants (`Alarmas`, `Ventas`, `Recetas`, `logistica`) quedan registrados en el adapter.

**Artefactos generados:** `alarmasPendientes.bd` en el CWD.

---

### PASO 3 — bodegaCentral

**Quien lo hace:** PC2 (en laboratorio) o Terminal 3 (local).

**Prerequisitos:** Ninguno (no depende de ServidorCentral en tiempo de arranque).

**Configuracion relevante** (`bodegaCentral/src/main/resources/BodegaCentral.cfg`):
```
BodegaCentral.Endpoints = tcp -h localhost -p 12347
```
- En laboratorio: cambiar `localhost` por la IP del PC2.

**Comando:**
```bash
cd Code-2023-04-14-3/coffeemach
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
java -jar bodegaCentral/build/libs/BodegaCentral.jar
```

**Verificacion:** Consola muestra el inventario inicial con los 8 recursos. Si existe `inventarioBodega.bd`, carga el estado previo.

**Artefactos generados:** `inventarioBodega.bd` en el CWD.

**Nota:** Si se despliega por primera vez o se quiere resetear el inventario, borrar `inventarioBodega.bd` antes de arrancar.

---

### PASO 4 — coffeeMach

**Quien lo hace:** PC3/PC4 (en laboratorio) o Terminal 4 (local).

**Prerequisitos:** ServidorCentral corriendo (coffeeMach llama a ServidorCentral al arrancar para cargar recetas).

**Prerequisito critico:** El archivo `codMaquina.cafe` debe existir en el directorio de trabajo con el ID de maquina como entero:
```bash
echo 1 > codMaquina.cafe   # maquina con ID 1 en la BD
```

**Configuracion relevante** (`coffeeMach/src/main/resources/coffeMach.cfg`):
```
CoffeMach.Endpoints = tcp -h localhost -p 12346
alarmas = Alarmas:tcp -h localhost -p 12345
ventas = Ventas:tcp -h localhost -p 12345
recetas = Recetas:tcp -h localhost -p 12345
```
- En laboratorio: cambiar `localhost` en `Endpoints` por IP del PC3/PC4; cambiar el segundo `localhost` (proxies) por IP del PC1 (ServidorCentral).

**Comando:**
```bash
cd Code-2023-04-14-3/coffeemach
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
java -jar coffeeMach/build/libs/coffeeMach.jar
```

**Verificacion:** Ventana Swing de la maquina expendedora abre. Muestra los productos cargados desde el servidor. El servant `abastecer` esta escuchando en puerto 12346.

**Artefactos generados:** `alarmas.bd`, `ingredientes.bd`, `monedas.bd`, `recetas.bd`, `ventas.bd` en el CWD.

**Nota:** Si hay varios coffeeMach en laboratorio, cada PC debe tener un `codMaquina.cafe` distinto con el ID correspondiente en la BD.

---

### PASO 5 — cmLogistics

**Quien lo hace:** PC5 (en laboratorio) o Terminal 5 (local).

**Prerequisitos:** ServidorCentral, bodegaCentral y coffeeMach corriendo.

**Configuracion relevante** (`cmLogistics/src/main/resources/CmLogistic.cfg`):
```
ServerCentral = logistica:tcp -h localhost -p 12345
MaquinaCafe = abastecer:tcp -h localhost -p 12346
BodegaCentral = bodega:tcp -h localhost -p 12347
```
- En laboratorio: cambiar `localhost` en `ServerCentral` por IP del PC1; en `MaquinaCafe` por IP del PC3 o PC4; en `BodegaCentral` por IP del PC2.
- Si hay multiples maquinas, cmLogistics solo puede conectar a una a la vez via el proxy `MaquinaCafe`. El servidor filtra por asignacion, no por conexion directa.

**Comando:**
```bash
cd Code-2023-04-14-3/coffeemach
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
java -jar cmLogistics/build/libs/CmLogistics.jar
```

**Verificacion:** Ventana Swing de cmLogistics abre. El panel de sesion permite ingresar codigo y password. Al hacer login con un operador valido (ej. codigo=1, password=1123) el lblEstado cambia a "Sesion iniciada para operador 1".

---

## 4. Resolucion de una alarma — flujo completo paso a paso

Este es el flujo distribuido que debe demostrarse en la sustentacion:

```
1. [coffeeMach] Usuario compra productos hasta agotar ingrediente
   → coffeeMach detecta nivel bajo en verificarIngredientes()
   → Llama AlarmaServicePrx.recibirNotificacionEscasezIngredientes("Cafe", 1)

2. [ServidorCentral] Recibe notificacion
   → Registra alarma en tabla ALARMA_MAQUINA (BD)
   → Guarda AlarmaPendiente en AlarmasPendientesRepositorio (alarmasPendientes.bd)
   → GUI de coffeeMach bloquea frame si es nivel critico

3. [cmLogistics] Operador abre la aplicacion
   → Ingresa codigo=1 y password=1123, click Iniciar Sesion
   → LogisticaController.iniciarSesion() llama ServicioComLogisticaPrx.inicioSesion()
   → ServidorCentral valida en BD, retorna true
   → TecnicoMantenimiento.setAutenticado(true)

4. [cmLogistics] Operador hace click en "Consultar"
   → ControlAlarma.consultarAlarmas(1) llama consultarAlarmasPendientes(1)
   → ServidorCentral filtra por ASIGNACION_MAQUINA del operador 1
   → Retorna lista de strings pipe-delimited
   → comboBoxAlarmas muestra "Maq 1 - CAFE x500 - Escasez de cafe"

5. [cmLogistics] Operador selecciona alarma y hace click en "Resolver"
   → ControlAlarma.resolverAlarma("1|1|CAFE|500|Sala A|Escasez de cafe")
   a) bodega.hayExistencias(CAFE, 500) → true
   b) bodega.despachar(CAFE, 500, 1, 1) → true, inventario bodega -500
   c) maquina.abastecerRecurso(1, CAFE, 500, 1) → CALLBACK a coffeeMach

6. [coffeeMach] Recibe callback abastecerRecurso()
   → ControladorMQ.aplicarAbastecimiento("CAFE", 500, 1)
   → IngredienteRepositorio actualiza Cafe a maximo
   → limpiarAlarmasDeRecurso("CAFE") elimina alarmas "9" y "13"
   → respaldarMaq() guarda .bd
   → SwingUtilities.invokeLater: actualizarInsumosGraf(), actualizarAlarmasGraf()
   → Si no quedan alarmas: frame.setEnabled(true), interfazHabilitada()
   → Llama AlarmaServicePrx.recibirNotificacionAbastesimiento(idAlarma, idMaquina)

7. [ServidorCentral] Recibe confirmacion de abastecimiento
   → AlarmasPendientesRepositorio.cerrarAlarma(1, 1)
   → ManejadorDatos.desactivarAlarma(1, 1) en BD

8. [cmLogistics] Muestra resultado
   → Log: "Alarma resuelta exitosamente."
   → Consulta alarmas de nuevo: comboBoxAlarmas vacio o sin la alarma resuelta
```

---

## 5. Tabla de puertos y dependencias de red

| Componente | Puerto | Recibe llamadas de | Llama a |
|---|---|---|---|
| PostgreSQL | 5430 | ServidorCentral | — |
| ServidorCentral | 12345 | coffeeMach, cmLogistics | PostgreSQL |
| coffeeMach | 12346 | cmLogistics (callback) | ServidorCentral |
| bodegaCentral | 12347 | cmLogistics | — |
| cmLogistics | — (cliente puro) | — | ServidorCentral, coffeeMach, bodegaCentral |

---

## 6. Troubleshooting rapido

| Sintoma | Causa probable | Solucion |
|---|---|---|
| `ConnectionRefusedException` al arrancar coffeeMach | ServidorCentral no esta corriendo | Arrancar ServidorCentral primero |
| `ConnectionRefusedException` en cmLogistics al resolver | coffeeMach o bodegaCentral caido | Verificar que ambos esten corriendo |
| `ClassNotFoundException` al deserializar `.bd` | Clase de dominio cambio de estructura o paquete | Borrar el archivo `.bd` correspondiente |
| Alarmas pendientes no aparecen en cmLogistics | ASIGNACION_MAQUINA no tiene filas para el operador | Verificar datos en BD con el script de inserts |
| Login invalido en cmLogistics | Operador no existe en BD o contrasena incorrecta | Verificar tabla OPERADORES (default: id=1, pwd=1123) |
| coffeeMach no carga productos | ServidorCentral no tiene recetas en BD | Ejecutar coffeemach-inserts.sql o crear receta via consola admin |
| Gradle falla con JDK 17 | Gradle 6.6 incompatible con JDK 17+ | Usar `export JAVA_HOME=$(/usr/libexec/java_home -v 11)` |

---

## 7. Checklist de sustentacion

```
[ ] PostgreSQL corriendo con datos iniciales (operadores, maquinas, asignaciones)
[ ] ServidorCentral arrancado — muestra "iniciado en puerto 12345"
[ ] bodegaCentral arrancada — muestra inventario inicial de 8 recursos
[ ] coffeeMach arrancada — GUI Swing visible con productos cargados
[ ] codMaquina.cafe contiene el ID correcto de la maquina en BD
[ ] cmLogistics arrancada — GUI Swing visible con campos de login
[ ] Login exitoso en cmLogistics con operador asignado a la maquina
[ ] Provocar alarma: comprar hasta agotar ingrediente en coffeeMach
[ ] Consultar alarmas en cmLogistics — la alarma aparece en comboBox
[ ] Resolver alarma — maquina recargada, GUI de coffeeMach rehabilitada
[ ] Log de cmLogistics muestra "Alarma resuelta exitosamente."
[ ] Inventario de bodega actualizado (boton Actualizar en cmLogistics)
```
