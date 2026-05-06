package logistica;

import java.util.List;
import servicios.*;

/**
 * Controlador de logistica. Coordina proxies de servidor, bodega y maquina
 * para resolver alarmas pendientes.
 * Patron: Controller + Broker (Client Proxy) + Callback + Secure Messaging.
 *
 * Spec 10 - Task 1
 */
public class LogisticaController {

    private ServicioComLogisticaPrx servidor;
    private ServicioBodegaPrx bodega;
    private ServicioAbastecimientoPrx maquina;

    public LogisticaController(ServicioComLogisticaPrx servidor,
            ServicioBodegaPrx bodega, ServicioAbastecimientoPrx maquina) {
        this.servidor = servidor;
        this.bodega = bodega;
        this.maquina = maquina;
    }

    /**
     * Autentica al operador contra el servidor central.
     * Secure Messaging: sin autenticacion, no se permite operar.
     */
    public boolean iniciarSesion(int codigoOperador, String password) {
        return servidor.inicioSesion(codigoOperador, password);
    }

    /**
     * Consulta alarmas pendientes del operador autenticado.
     * Solo retorna alarmas de maquinas asignadas al operador.
     */
    public List<String> consultarAlarmas(int codigoOperador) {
        return servidor.consultarAlarmasPendientes(codigoOperador);
    }

    public List<String> consultarInventario() {
        return bodega.consultarInventario();
    }

    /**
     * Resuelve una alarma coordinando bodega y maquina.
     *
     * Flujo (Callback pattern):
     * 1. Parsea la alarma (formato: idMaquina|idAlarma|recurso|cantidad|ubicacion|descripcion)
     * 2. Verifica existencias en bodega
     * 3. Despacha desde bodega
     * 4. Llama callback a coffeeMach (abastecerRecurso)
     *
     * @param alarmaTexto String en formato pipe-delimited de AlarmaPendiente.toTransportString()
     * @return true si la alarma se resolvio exitosamente
     */
    public boolean resolverAlarma(String alarmaTexto) {
        String[] partes = alarmaTexto.split("\\|");

        int idMaquina = Integer.parseInt(partes[0].trim());
        int idAlarma = Integer.parseInt(partes[1].trim());
        RecursoAbastecimiento recurso = RecursoAbastecimiento.valueOf(partes[2].trim());
        int cantidad = Integer.parseInt(partes[3].trim());

        // Paso 1: Verificar existencias en bodega
        if (!bodega.hayExistencias(recurso, cantidad)) {
            System.out.println("Bodega sin existencias de " + recurso.name()
                    + " (se necesitan " + cantidad + ")");
            return false;
        }

        // Paso 2: Despachar desde bodega
        boolean despacho = bodega.despachar(recurso, cantidad, idMaquina, idAlarma);
        if (!despacho) {
            System.out.println("No fue posible despachar desde bodega");
            return false;
        }

        // Paso 3: Callback a coffeeMach
        boolean abastecido = maquina.abastecerRecurso(
                idMaquina, recurso, cantidad, idAlarma);
        if (!abastecido) {
            System.out.println("La maquina no pudo aplicar el abastecimiento");
            return false;
        }

        return true;
    }
}
