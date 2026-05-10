package controlAlarma;

import servicios.RecursoAbastecimiento;
import servicios.ServicioAbastecimientoPrx;
import servicios.ServicioBodegaPrx;
import servicios.ServicioComLogisticaPrx;
import zonaGeografica.ZonaGeografica;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de control de alarmas para cmLogistics.
 * Encapsula la logica de consulta y resolucion de alarmas,
 * extrayendola de LogisticaController para mayor cohesion.
 *
 * Patron: Controller + Broker (Client Proxy) + Callback.
 *
 * Flujo de resolverAlarma:
 *   1. Verifica existencias en bodegaCentral
 *   2. Despacha desde bodegaCentral
 *   3. Callback a coffeeMach (abastecerRecurso)
 *   El cierre de alarma en ServidorCentral lo dispara coffeeMach
 *   via recibirNotificacionAbastesimiento (Callback Pattern).
 */
public class ControlAlarma {

    private ServicioComLogisticaPrx servidor;
    private ServicioBodegaPrx bodega;
    private ServicioAbastecimientoPrx maquina;

    public ControlAlarma(ServicioComLogisticaPrx servidor,
            ServicioBodegaPrx bodega,
            ServicioAbastecimientoPrx maquina) {
        this.servidor = servidor;
        this.bodega = bodega;
        this.maquina = maquina;
    }

    /**
     * Consulta las alarmas pendientes del operador autenticado.
     * Solo retorna alarmas de maquinas asignadas al operador (Secure Messaging).
     *
     * @param codigoOperador codigo del tecnico autenticado
     * @return lista de strings en formato pipe-delimited de AlarmaPendiente
     */
    public List<String> consultarAlarmas(int codigoOperador) {
        return servidor.consultarAlarmasPendientes(codigoOperador);
    }

    /**
     * Consulta las zonas geograficas (maquinas asignadas) al operador.
     *
     * @param codigoOperador codigo del tecnico autenticado
     * @return lista de ZonaGeografica parseadas desde el servidor
     */
    public List<ZonaGeografica> consultarZonasAsignadas(int codigoOperador) {
        List<String> datos = servidor.asignacionMaquina(codigoOperador);
        List<ZonaGeografica> zonas = new ArrayList<>();
        for (String dato : datos) {
            ZonaGeografica zona = ZonaGeografica.fromString(dato);
            if (zona != null) zonas.add(zona);
        }
        return zonas;
    }

    /**
     * Resuelve una alarma pendiente coordinando bodega y maquina.
     *
     * Flujo (Callback pattern):
     *   1. Parsea la alarma (formato: idMaquina|idAlarma|recurso|cantidad|ubicacion|descripcion)
     *   2. Verifica existencias en bodega
     *   3. Despacha desde bodega
     *   4. Llama callback a coffeeMach (abastecerRecurso)
     *
     * @param alarmaTexto string en formato pipe-delimited de AlarmaPendiente.toTransportString()
     * @return true si la alarma se resolvio exitosamente
     */
    public boolean resolverAlarma(String alarmaTexto) {
        String[] partes = alarmaTexto.split("\\|");
        if (partes.length < 4) {
            System.out.println("Formato de alarma invalido: " + alarmaTexto);
            return false;
        }

        int idMaquina = Integer.parseInt(partes[0].trim());
        int idAlarma  = Integer.parseInt(partes[1].trim());
        RecursoAbastecimiento recurso = RecursoAbastecimiento.valueOf(partes[2].trim());
        int cantidad  = Integer.parseInt(partes[3].trim());

        if (!bodega.hayExistencias(recurso, cantidad)) {
            System.out.println("Bodega sin existencias de " + recurso.name()
                    + " (necesita " + cantidad + ")");
            return false;
        }

        if (!bodega.despachar(recurso, cantidad, idMaquina, idAlarma)) {
            System.out.println("Bodega no pudo despachar el recurso");
            return false;
        }

        if (!maquina.abastecerRecurso(idMaquina, recurso, cantidad, idAlarma)) {
            System.out.println("La maquina no pudo aplicar el abastecimiento");
            return false;
        }

        return true;
    }
}
