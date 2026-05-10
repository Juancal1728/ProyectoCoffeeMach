# Spec 01 — Persistencia local en la maquina de cafe

> Estado: Implementado.

---

## 1. Spec

**Purpose:** Mantener el estado operacional de la maquina de cafe (ingredientes, monedas, recetas, alarmas, ventas) entre reinicios, usando archivos locales, sin depender de conectividad con el servidor central.

**Users:**
- La propia maquina (`ControladorMQ`) lee y escribe el estado al arrancar y durante operacion.
- El operador de logistica puede resetear el estado borrando los archivos `.bd`.

**Requirements:**
1. Cada repositorio de dominio persiste su mapa de entidades a un archivo `.bd` en el directorio de trabajo.
2. La persistencia se ejecuta de forma automatica tras cada mutacion (`addElement`).
3. Al arrancar, si el archivo `.bd` no existe, el repositorio se inicializa con datos por defecto (`loadDataP()`).
4. Si el archivo existe pero la deserializacion falla (e.g. cambio de estructura de clase), se loguea el error y la aplicacion no arranca limpiamente.
5. Los repositorios disponibles son: `AlarmaRepositorio`, `IngredienteRepositorio`, `MonedasRepositorio`, `RecetaRepositorio`, `VentaRepositorio`.
6. El metodo `getInstance()` de cada repositorio es `synchronized` para garantizar seguridad bajo concurrencia (hilo Ice + hilo Swing EDT).

**Edge cases:**
- Cambio de nombre de campo o paquete en una clase de dominio rompe la deserializacion del `.bd` existente.
- El hilo Ice (`abastecer`) y el EDT de Swing pueden acceder concurrentemente a los repositorios — protegido con `synchronized getInstance()`.
- Si el directorio de trabajo no tiene permisos de escritura, `saveData()` falla silenciosamente (imprime stack trace).

**Acceptance criteria:**
- **Given** que no existe `ingredientes.bd`, **when** arranca `coffeeMach`, **then** `IngredienteRepositorio` se inicializa con los valores por defecto definidos en `loadDataP()` y crea el archivo `ingredientes.bd`.
- **Given** que existe `monedas.bd` con estado previo, **when** arranca `coffeeMach`, **then** `MonedasRepositorio` carga exactamente ese estado y no llama a `loadDataP()`.
- **Given** un estado cargado, **when** se llama a `addElement(key, value)`, **then** el archivo `.bd` correspondiente se sobreescribe sincronicamente antes de que el metodo retorne.
- **Given** que la clase de dominio cambio de estructura, **when** se intenta deserializar el `.bd` anterior, **then** se imprime `Error al cargar AlarmaRepositorio` (o equivalente) y la excepcion queda registrada en stderr.

---

## 2. Plan

**Architecture:** Clase abstracta generica `Repositorio<K extends Serializable, T extends Serializable>` en el paquete `interfaces`. Cada repositorio concreto extiende esta clase como singleton (patron `getInstance()`), pasa el nombre del archivo al constructor, e implementa `loadDataP()` para los defaults.

**Data model:**
- No hay base de datos; el modelo es un `HashMap<K, T>` serializado como `ObjectOutputStream` del objeto `Repositorio` completo.
- Archivos generados en el directorio de ejecucion:

| Archivo | Clase clave | Clase valor |
|---------|-------------|-------------|
| `alarmas.bd` | `String` (id alarma) | `Alarma` |
| `ingredientes.bd` | `String` (nombre) | `Ingrediente` |
| `monedas.bd` | `String` (denominacion) | `DepositoMonedas` |
| `recetas.bd` | `String` (id receta) | `Receta` |
| `ventas.bd` | `String` (id venta) | `Venta` |

**API contracts:** No expone API externa. Todos los accesos son llamadas Java directas.

**Testing strategy:** No hay tests automatizados. Verificacion manual: borrar `.bd`, arrancar, verificar defaults en GUI; relanzar, verificar que el estado persiste.

**Security constraints:** Los archivos `.bd` no estan cifrados; contienen datos operacionales, no credenciales.

**Dependencies:** Java serialization estandar (`java.io.ObjectOutputStream/InputStream`). Ninguna dependencia externa.

---

## 3. Tasks (completadas)

```
Task 1: Clase base Repositorio<K,T>
Depends on: none
What was built: Clase abstracta generica con HashMap interno, saveData() con ObjectOutputStream,
  loadData() con ObjectInputStream, addElement() que persiste tras cada mutacion, y
  loadDataP() abstracto para defaults.
Acceptance criteria:
- addElement() sobreescribe el archivo .bd sincronicamente.
- loadData() restaura el HashMap completo si el archivo existe.
- Si el archivo no existe, loadData() delega en loadDataP() sin lanzar excepcion.

Task 2: Repositorios concretos (singletons)
Depends on: Task 1
What was built: AlarmaRepositorio, IngredienteRepositorio, MonedasRepositorio,
  RecetaRepositorio, VentaRepositorio — cada uno implementando loadDataP() con
  los datos iniciales de la maquina (e.g. IngredienteRepositorio inicializa
  Agua/Cafe/Azucar/Vaso con cantidades por defecto).
Acceptance criteria:
- Cada repositorio devuelve instancia unica via getInstance().
- loadDataP() inserta al menos un elemento por defecto en el mapa.

Task 3: Integracion con ControladorMQ
Depends on: Task 2
What was built: ControladorMQ obtiene las instancias singleton de todos los
  repositorios y llama a respaldarMaq() (que llama saveData() en cada uno)
  tras cada operacion de venta o abastecimiento.
Acceptance criteria:
- Tras completar una venta, ventas.bd contiene la venta registrada.
- Tras un abastecimiento, ingredientes.bd refleja los nuevos niveles.

Task 4: Sincronizacion de getInstance() (bug fix de auditoria)
Depends on: Task 2
What was built: Se agrego la palabra clave synchronized al metodo getInstance()
  de los cinco repositorios concretos. Garantiza creacion atomica del singleton
  cuando el hilo Ice (abastecer servant) y el EDT de Swing acceden concurrentemente.
Files modificados:
  coffeeMach/src/main/java/alarma/AlarmaRepositorio.java
  coffeeMach/src/main/java/monedero/MonedasRepositorio.java
  coffeeMach/src/main/java/ingrediente/IngredienteRepositorio.java
  coffeeMach/src/main/java/productoReceta/RecetaRepositorio.java
  coffeeMach/src/main/java/McControlador/VentaRepositorio.java
Acceptance criteria:
- getInstance() es synchronized en los cinco repositorios.
- No hay double-checked locking incorrecto; el lock protege toda la creacion.
```

---

## Assumptions to review

1. **Un unico escritor muta el HashMap tras la inicializacion** — Impact: MEDIUM
   El synchronized en getInstance() protege la creacion, pero las operaciones addElement/removeElement
   no son synchronized individualmente. Correcto en el modelo actual donde solo ControladorMQ escribe
   (el hilo Ice lee para validar pero ControladorMQ aplica los cambios via SwingUtilities.invokeLater).

2. **El directorio de trabajo tiene permisos de escritura** — Impact: MEDIUM
   Correct this if: la maquina se ejecuta en un entorno con filesystem de solo lectura.

3. **La serializacion Java nativa es suficiente para el volumen de datos** — Impact: LOW
   Correct this if: el numero de ventas acumuladas sin envio crece lo suficiente para que los archivos sean grandes.
