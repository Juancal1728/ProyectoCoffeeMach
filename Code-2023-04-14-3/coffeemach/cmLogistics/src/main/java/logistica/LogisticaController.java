package logistica;

import controlAlarma.ControlAlarma;
import servicios.ServicioAbastecimientoPrx;
import servicios.ServicioBodegaPrx;
import servicios.ServicioComLogisticaPrx;
import tecnicoMantenimiento.TecnicoMantenimiento;
import zonaGeografica.ZonaGeografica;

import java.util.List;

/**
 * Orquestador principal de cmLogistics.
 * Patron: Controller + Broker (Client Proxy) + Secure Messaging.
 *
 * Delega:
 *   - Gestion de alarmas → ControlAlarma
 *   - Estado del operador → TecnicoMantenimiento
 *   - Zonas geograficas → ZonaGeografica (via ControlAlarma)
 *   - Inventario bodega → ServicioBodegaPrx directo
 */
public class LogisticaController {

    private ServicioComLogisticaPrx servidor;
    private ServicioBodegaPrx bodega;
    private ServicioAbastecimientoPrx maquina;

    private ControlAlarma controlAlarma;
    private TecnicoMantenimiento tecnicoActual;

    public LogisticaController(ServicioComLogisticaPrx servidor,
            ServicioBodegaPrx bodega, ServicioAbastecimientoPrx maquina) {
        this.servidor = servidor;
        this.bodega = bodega;
        this.maquina = maquina;
        this.controlAlarma = new ControlAlarma(servidor, bodega, maquina);
    }

    /**
     * Autentica al operador contra el servidor central.
     * Crea un TecnicoMantenimiento con estado autenticado si las credenciales son validas.
     * Secure Messaging: sin autenticacion, no se permite resolver alarmas.
     */
    public boolean iniciarSesion(int codigoOperador, String password) {
        boolean ok = servidor.inicioSesion(codigoOperador, password);
        tecnicoActual = new TecnicoMantenimiento(codigoOperador);
        tecnicoActual.setAutenticado(ok);
        return ok;
    }

    /**
     * Consulta alarmas pendientes del tecnico autenticado.
     * Delega en ControlAlarma.
     */
    public List<String> consultarAlarmas(int codigoOperador) {
        return controlAlarma.consultarAlarmas(codigoOperador);
    }

    /**
     * Consulta las zonas geograficas (maquinas asignadas) al tecnico.
     */
    public List<ZonaGeografica> consultarZonasAsignadas(int codigoOperador) {
        return controlAlarma.consultarZonasAsignadas(codigoOperador);
    }

    /**
     * Resuelve una alarma. Delega en ControlAlarma.
     *
     * @param alarmaTexto formato pipe-delimited de AlarmaPendiente.toTransportString()
     * @return true si la alarma se resolvio exitosamente
     */
    public boolean resolverAlarma(String alarmaTexto) {
        return controlAlarma.resolverAlarma(alarmaTexto);
    }

    /**
     * Consulta el inventario de bodegaCentral directamente.
     */
    public List<String> consultarInventario() {
        return bodega.consultarInventario();
    }

    public TecnicoMantenimiento getTecnicoActual() {
        return tecnicoActual;
    }
}
