# Specs — ProyectoCoffeeMach

Manifiestos de funcionalidades del sistema.
- **Specs 01-07**: Post-mortem del codigo existente (Primera Parte).
- **Specs 08-12**: Segunda Parte — desarrollo de componentes faltantes.
- **Specs 13-14**: Verificacion del Plan Maestro y pruebas de integracion.

| # | Spec | Modulo principal | Estado |
|---|------|-----------------|--------|
| [01](01-persistencia-local.md) | Persistencia local en maquina de cafe | `coffeeMach/interfaces/Repositorio` | Implementado |
| [02](02-gui-vending.md) | Interfaz grafica y logica de venta | `coffeeMach/McControlador/ControladorMQ` + `interfazUsuario/Interfaz` | Implementado |
| [03](03-notificacion-alarmas.md) | Notificacion de alarmas al servidor central | `coffeeMach/alarma/*` -> `ServidorCentral/alarma/*` | Implementado |
| [04](04-reporte-ventas.md) | Reporte de ventas al servidor central | `coffeeMach/McControlador/VentaService` -> `ServidorCentral/ventas/VentasManager` | Implementado |
| [05](05-gestion-recetas.md) | Gestion de recetas e ingredientes | `ServidorCentral/receta/ProductoReceta` + `interfaz/ControladorRecetas` | Implementado |
| [06](06-abastecimiento-remoto.md) | Abastecimiento remoto de la maquina | `ServidorCentral` -> `coffeeMach/ServicioAbastecimiento` | Implementado (completado en Spec 11) |
| [07](07-servicio-logistica.md) | Servicio de logistica para operadores | `ServidorCentral/comunicacion/ControlComLogistica` | Implementado (completado en Specs 08+10) |
| [08](08-alarmas-pendientes.md) | Subsistema alarmas pendientes en ServidorCentral | `ServidorCentral/modelo/AlarmasPendientesRepositorio` | Implementado |
| [09](09-bodega-central.md) | Componente bodegaCentral | `bodegaCentral/bodega/BodegaService` | Implementado |
| [10](10-cmlogistics-client.md) | Cliente cmLogistics completo | `cmLogistics/logistica/LogisticaController` | Implementado |
| [11](11-abastecimiento-recurso.md) | abastecerRecurso + bug fixes en coffeeMach | `coffeeMach/McControlador/ControladorMQ` | Implementado |
| [12](12-definir-producto-y-fixes.md) | definirProducto + correcciones ServidorCentral | `ServidorCentral/receta/ProductoReceta` | Implementado |
| [13](13-plan-maestro-cobertura.md) | Cobertura del Plan Maestro (verificacion) | Todos los modulos | En verificacion |
| [14](14-build-e-integracion.md) | Build, config y pruebas de integracion | Todos los modulos | Pendiente (requiere JDK 11) |
