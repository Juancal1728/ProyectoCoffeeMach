# Spec 14 — Build, configuracion y pruebas de integracion

> Estado: Implementado (pendiente prueba end-to-end con infraestructura completa).

---

## 1. Spec

**Purpose:** Verificar que el sistema completo compila, arranca correctamente y los flujos end-to-end funcionan segun el Plan Maestro. Documentar el procedimiento de despliegue para laboratorio.

**Requirements:**
1. `./gradlew clean build` debe completar con BUILD SUCCESSFUL usando Java 11.
2. Los 4 JARs deben generarse correctamente.
3. Los 4 componentes deben arrancar en el orden correcto sin errores.
4. Los 8 escenarios de prueba del Plan Maestro deben pasar.

---

## 2. Prerequisitos

```bash
# Java 11 obligatorio — Gradle 6.6 es incompatible con JDK 17+
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # debe mostrar 11.x

# PostgreSQL en puerto 5430 (local) o 5432 (laboratorio)
# Esquema inicializado con los tres scripts SQL
```

---

## 3. Configuracion de build

**build.gradle — iceHome portable:**
```groovy
iceHome = project.hasProperty('iceHome') ? project.iceHome : '/opt/homebrew'
```
- Sobreescribir en CI/laboratorio: `./gradlew build -PiceHome=/usr/local`

**Verificacion de stubs en dead code (plantilla del profesor):**
```bash
grep -rn "UnsupportedOperationException" --include="*.java" */src/
# Esperado: solo AlarmaServiceImp.java y SuministroServiceImp.java
```

---

## 4. Tasks

```
Task 1: Build limpio
Depends on: JDK 11
Command: cd Code-2023-04-14-3/coffeemach && ./gradlew clean build
Expected: BUILD SUCCESSFUL, 4 JARs generados en */build/libs/

Task 2: Prueba de arranque secuencial
Depends on: Task 1, PostgreSQL
Order: ServidorCentral -> bodegaCentral -> coffeeMach -> cmLogistics
Expected: Los 4 componentes arrancan sin excepcion

Task 3: Prueba end-to-end — Alarma por ingrediente
Depends on: Task 2
Steps:
  1. Comprar bebidas en coffeeMach hasta bajar ingrediente bajo umbral
  2. Ver alarma en GUI de coffeeMach (frame bloqueado si es critico)
  3. Login en cmLogistics (operador 1, password 1123)
  4. Consultar alarmas -> debe mostrar alarma formateada en comboBox
  5. Seleccionar alarma y hacer click en Resolver
  6. Bodega despacha, maquina recibe callback abastecerRecurso
  7. coffeeMach aplica recarga, limpia alarmas correctas, rehabilita GUI
Expected: Maquina recargada, alarma cerrada en servidor

Task 4: Prueba end-to-end — Alarma por monedas
Depends on: Task 2
Steps:
  1. Generar devoluciones hasta bajar deposito de monedas
  2. Resolver desde cmLogistics con recurso MONEDA100/200/500
Expected: Deposito recargado, alarmas bajo+critico eliminadas

Task 5: Prueba — Mantenimiento
Depends on: Task 2
Steps: Activar mal funcionamiento -> resolver con KITREPARACION
Expected: Alarma 1 eliminada, maquina rehabilitada

Task 6: Prueba — Bodega sin inventario
Depends on: Task 2
Steps: Agotar recurso en bodega -> intentar resolver alarma desde cmLogistics
Expected: Log muestra "sin existencias", alarma permanece pendiente

Task 7: Prueba — Secure Messaging
Depends on: Task 2
Steps: Consultar sin login, login invalido, operador sin asignaciones
Expected: Rechazos apropiados: lblEstado o log de eventos informa el error

Task 8: Prueba — Ventas no duplicadas
Depends on: Task 2
Steps: Comprar mismo producto 2 veces -> enviar reporte
Expected: Ambas ventas llegan al servidor con claves distintas (timestamp)

Task 9: Prueba — Receta nueva
Depends on: Task 2
Steps: Crear receta en ConsolaAdministracion -> actualizar en coffeeMach -> comprar
Expected: Producto disponible y comprable
```

---

## 5. Orden de ejecucion para laboratorio

### Local (todo localhost)

```bash
# Working directory: Code-2023-04-14-3/coffeemach/
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"

# Terminal 1 — base de datos (debe estar corriendo antes de ServidorCentral)
# psql -f scripts/postgres/coffeemach-user.sql
# psql -d coffeemachine -U cofmachu -f scripts/postgres/coffeemach-ddl.sql
# psql -d coffeemachine -U cofmachu -f scripts/postgres/coffeemach-inserts.sql

# Terminal 2 — ServidorCentral (puerto 12345 + BD en 5430)
java -jar ServidorCentral/build/libs/ServidorCentral.jar

# Terminal 3 — bodegaCentral (puerto 12347)
java -jar bodegaCentral/build/libs/BodegaCentral.jar

# Terminal 4 — coffeeMach (puerto 12346, lee codMaquina.cafe del CWD)
java -jar coffeeMach/build/libs/coffeeMach.jar

# Terminal 5 — cmLogistics (cliente, conecta a 12345 + 12346 + 12347)
java -jar cmLogistics/build/libs/CmLogistics.jar
```

### Laboratorio (5 PCs + BD compartida)

| PC | Componente | Cambios en .cfg |
|---|---|---|
| BD | PostgreSQL (puerto 5432) | - |
| PC1 | ServidorCentral | IP real en server.cfg, puerto BD 5432 |
| PC2 | bodegaCentral | IP real de PC1 en BodegaCentral.cfg |
| PC3-PC4 | coffeeMach (x2) | IPs de PC1 en coffeMach.cfg; `codMaquina.cafe` distinto por maquina |
| PC5 | cmLogistics | IPs de PC1, PC2, PC3/PC4 en CmLogistic.cfg |

**Archivos necesarios por componente:**

| Componente | JAR | CFG | Extras |
|---|---|---|---|
| ServidorCentral | ServidorCentral.jar | server.cfg | postgresql-42.3.1.jar, ice-3.7.6.jar |
| bodegaCentral | BodegaCentral.jar | BodegaCentral.cfg | ice-3.7.6.jar |
| coffeeMach | coffeeMach.jar | coffeMach.cfg | ice-3.7.6.jar, codMaquina.cafe |
| cmLogistics | CmLogistics.jar | CmLogistic.cfg | ice-3.7.6.jar |

**Notas de despliegue:**
- Eliminar archivos `.bd` antes de desplegar en un entorno nuevo para evitar conflictos de serializacion.
- El archivo `codMaquina.cafe` debe contener el entero del ID de maquina asignado en la base de datos.
- Asegurar que los puertos 12345, 12346 y 12347 no esten bloqueados por firewall.
