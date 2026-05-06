# Spec 14 — Build, configuracion y pruebas de integracion

> Estado: Pendiente (requiere JDK 11).

---

## 1. Spec

**Purpose:** Verificar que el sistema completo compila, arranca correctamente y los flujos end-to-end funcionan segun el Plan Maestro pasos 18-20.

**Requirements:**
1. `./gradlew clean build` debe completar con BUILD SUCCESSFUL usando Java 11.
2. Los 4 JARs deben generarse correctamente.
3. Los 4 componentes deben arrancar en el orden correcto sin errores.
4. Los 8 escenarios de prueba del Plan Maestro deben pasar.

---

## 2. Prerequisitos

```bash
# Java 11 obligatorio
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # debe mostrar 11.x

# PostgreSQL en puerto 5430

```

---

## 3. Tasks

```
Task 1: Build limpio
Depends on: JDK 11
Command: cd Code-2023-04-14-3/coffeemach && ./gradlew clean build
Expected: BUILD SUCCESSFUL, 4 JARs generados
Files: todos los subprojects

Task 2: Verificar que no quedan stubs funcionales
Depends on: Task 1
Command: grep -rn "UnsupportedOperationException" --include="*.java" */src/
Expected: Solo en AlarmaServiceImp.java y SuministroServiceImp.java (dead code, no afecta)

Task 3: Prueba de arranque secuencial
Depends on: Task 1, PostgreSQL
Order: ServidorCentral -> bodegaCentral -> coffeeMach -> cmLogistics
Expected: Los 4 componentes arrancan sin excepcion

Task 4: Prueba end-to-end - Alarma por ingrediente
Depends on: Task 3
Steps:
  1. Comprar bebidas en coffeeMach hasta bajar ingrediente
  2. Ver alarma en GUI
  3. Login en cmLogistics (operador 1, password 1123)
  4. Consultar alarmas -> debe mostrar alarma
  5. Resolver alarma -> bodega despacha -> maquina recargada
Expected: Maquina recargada, alarma cerrada

Task 5: Prueba end-to-end - Alarma por monedas
Depends on: Task 3
Steps:
  1. Generar devoluciones hasta bajar deposito
  2. Resolver desde cmLogistics
Expected: Deposito recargado

Task 6: Prueba - Mantenimiento
Depends on: Task 3
Steps: Pulsar "Mantenimiento" -> resolver con KITREPARACION
Expected: Maquina rehabilitada

Task 7: Prueba - Bodega sin inventario
Depends on: Task 3
Steps: Agotar recurso en bodega -> intentar resolver alarma
Expected: "sin existencias", alarma permanece

Task 8: Prueba - Secure Messaging
Depends on: Task 3
Steps: Consultar sin login, login invalido, operador sin asignaciones
Expected: Rechazos apropiados en cada caso

Task 9: Prueba - Ventas no duplicadas
Depends on: Task 3
Steps: Comprar mismo producto 2 veces -> enviar reporte
Expected: Ambas ventas llegan al servidor

Task 10: Prueba - Receta nueva
Depends on: Task 3
Steps: Crear receta en InterfazRecetas -> actualizar en coffeeMach -> comprar
Expected: Producto disponible y comprable
```

---

## 4. Orden de ejecucion para laboratorio

### Local (todo localhost)

```
Terminal 1: PostgreSQL (puerto 5430, ya corriendo)
Terminal 2: java -jar ServidorCentral/build/libs/ServidorCentral.jar
Terminal 3: java -jar bodegaCentral/build/libs/BodegaCentral.jar
Terminal 4: echo 1 > codMaquina.cafe && java -jar coffeeMach/build/libs/coffeeMach.jar
Terminal 5: java -jar cmLogistics/build/libs/CmLogistics.jar
```

### Laboratorio (5 PCs + BD compartida)

| PC | Componente | Cambios en .cfg |
|---|---|---|
| BD | PostgreSQL (puerto 5432) | - |
| PC1 | ServidorCentral | Cambiar `localhost` por IP real, puerto BD 5432 |
| PC2 | bodegaCentral | Cambiar `localhost` por IP real |
| PC3-PC4 | coffeeMach (x2) | Cambiar `localhost` por IPs de PC1, codMaquina.cafe distinto |
| PC5 | cmLogistics | Cambiar `localhost` por IPs de PC1, PC2, PC3/PC4 |

Cada PC copia: JAR propio + .cfg propio + ice-3.7.6.jar + postgresql-42.3.1.jar (solo ServidorCentral) + codMaquina.cafe (solo coffeeMach).
