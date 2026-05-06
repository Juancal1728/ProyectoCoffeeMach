# Spec 01 — Persistencia local en la máquina de café

> Post-mortem: feature completamente implementada.

---

## 1. Spec

**Purpose:** Mantener el estado operacional de la máquina de café (ingredientes, monedas, recetas, alarmas, ventas) entre reinicios, usando archivos locales, sin depender de conectividad con el servidor central.

**Users:**
- La propia máquina (`ControladorMQ`) lee y escribe el estado al arrancar y durante operación.
- El operador de logística puede resetear el estado borrando los archivos `.bd`.

**Requirements:**
1. Cada repositorio de dominio persiste su mapa de entidades a un archivo `.bd` en el directorio de trabajo.
2. La persistencia se ejecuta de forma automática tras cada mutación (`addElement`).
3. Al arrancar, si el archivo `.bd` no existe, el repositorio se inicializa con datos por defecto (`loadDataP()`).
4. Si el archivo existe pero la deserialización falla (e.g. cambio de estructura de clase), se loguea el error y la aplicación no arranca limpiamente.
5. Los repositorios disponibles son: `AlarmaRepositorio`, `IngredienteRepositorio`, `MonedasRepositorio`, `RecetaRepositorio`, `VentaRepositorio`.

**Edge cases:**
- Cambio de nombre de campo o paquete en una clase de dominio rompe la deserialización del `.bd` existente.
- Escritura concurrente no está protegida; el único escritor es `ControladorMQ` en el hilo de Swing.
- Si el directorio de trabajo no tiene permisos de escritura, `saveData()` falla silenciosamente (imprime stack trace).

**Acceptance criteria:**
- **Given** que no existe `ingredientes.bd`, **when** arranca `coffeeMach`, **then** `IngredienteRepositorio` se inicializa con los valores por defecto definidos en `loadDataP()` y crea el archivo `ingredientes.bd`.
- **Given** que existe `monedas.bd` con estado previo, **when** arranca `coffeeMach`, **then** `MonedasRepositorio` carga exactamente ese estado y no llama a `loadDataP()`.
- **Given** un estado cargado, **when** se llama a `addElement(key, value)`, **then** el archivo `.bd` correspondiente se sobreescribe sincrónicamente antes de que el método retorne.
- **Given** que la clase de dominio cambió de estructura, **when** se intenta deserializar el `.bd` anterior, **then** se imprime `Error al cargar AlarmaRepositorio` (o equivalente) y la excepción queda registrada en stderr.

---

## 2. Plan

**Architecture:** Clase abstracta genérica `Repositorio<K extends Serializable, T extends Serializable>` en el paquete `interfaces`. Cada repositorio concreto extiende esta clase como singleton (patrón `getInstance()`), pasa el nombre del archivo al constructor, e implementa `loadDataP()` para los defaults.

**Data model:**
- No hay base de datos; el modelo es un `HashMap<K, T>` serializado como `ObjectOutputStream` del objeto `Repositorio` completo.
- Archivos generados en el directorio de ejecución:

| Archivo | Clase clave | Clase valor |
|---------|-------------|-------------|
| `alarmas.bd` | `String` (id alarma) | `Alarma` |
| `ingredientes.bd` | `String` (nombre) | `Ingrediente` |
| `monedas.bd` | `String` (denominación) | `DepositoMonedas` |
| `recetas.bd` | `String` (id receta) | `Receta` |
| `ventas.bd` | `String` (id venta) | `Venta` |

**API contracts:** No expone API externa. Todos los accesos son llamadas Java directas.

**Testing strategy:** No hay tests automatizados. Verificación manual: borrar `.bd`, arrancar, verificar defaults en GUI; relanzar, verificar que el estado persiste.

**Security constraints:** Los archivos `.bd` no están cifrados; contienen datos operacionales, no credenciales.

**Dependencies:** Java serialization estándar (`java.io.ObjectOutputStream/InputStream`). Ninguna dependencia externa.

---

## 3. Tasks (completadas)

```
Task 1: Clase base Repositorio<K,T>
Depends on: none
What was built: Clase abstracta genérica con HashMap interno, saveData() con ObjectOutputStream,
  loadData() con ObjectInputStream, addElement() que persiste tras cada mutación, y
  loadDataP() abstracto para defaults.
Acceptance criteria:
- addElement() sobreescribe el archivo .bd sincrónicamente.
- loadData() restaura el HashMap completo si el archivo existe.
- Si el archivo no existe, loadData() delega en loadDataP() sin lanzar excepción.

Task 2: Repositorios concretos (singletons)
Depends on: Task 1
What was built: AlarmaRepositorio, IngredienteRepositorio, MonedasRepositorio,
  RecetaRepositorio, VentaRepositorio — cada uno implementando loadDataP() con
  los datos iniciales de la máquina (e.g. IngredienteRepositorio inicializa
  Agua/Cafe/Azucar/Vaso con cantidades por defecto).
Acceptance criteria:
- Cada repositorio devuelve instancia única vía getInstance().
- loadDataP() inserta al menos un elemento por defecto en el mapa.

Task 3: Integración con ControladorMQ
Depends on: Task 2
What was built: ControladorMQ obtiene las instancias singleton de todos los
  repositorios y llama a respaldarMaq() (que llama saveData() en cada uno)
  tras cada operación de venta o abastecimiento.
Acceptance criteria:
- Tras completar una venta, ventas.bd contiene la venta registrada.
- Tras un abastecimiento, ingredientes.bd refleja los nuevos niveles.
```

---

## Assumptions to review

1. **Un único hilo (Swing EDT) accede a los repositorios** — Impact: HIGH
   Correct this if: se introduce concurrencia (e.g. el hilo Ice de `abastecer` muta repositorios al mismo tiempo que el EDT).

2. **El directorio de trabajo tiene permisos de escritura** — Impact: MEDIUM
   Correct this if: la máquina se ejecuta en un entorno con filesystem de solo lectura.

3. **La serialización Java nativa es suficiente para el volumen de datos** — Impact: LOW
   Correct this if: el número de ventas acumuladas sin envío crece lo suficiente para que los archivos sean grandes.
