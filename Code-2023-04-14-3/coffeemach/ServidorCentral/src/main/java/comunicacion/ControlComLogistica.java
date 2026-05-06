package comunicacion;

import ServerControl.*;
import servicios.ServicioComLogistica;
import com.zeroc.Ice.*;
import java.util.*;

import modelo.AlarmaPendiente;
import modelo.AlarmasPendientesRepositorio;

/**
 * Servant Ice que implementa ServicioComLogistica.
 * Expone servicios de logistica al operador (cmLogistics).
 *
 * Spec 07 (existente) + Spec 08 (consultarAlarmasPendientes)
 */
public class ControlComLogistica implements ServicioComLogistica {

    private ServerControl control;

    public ControlComLogistica(ServerControl con) {
        control = con;
    }

    @Override
    public List<String> asignacionMaquina(int codigoOperador, Current current) {
        return control.listaAsignaciones(codigoOperador);
    }

    @Override
    public List<String> asignacionMaquinasDesabastecidas(int codigoOperador, Current current) {
        return control.listaAsignacionesMDanada(codigoOperador);
    }

    @Override
    public boolean inicioSesion(int codigoOperador, String password, Current current) {
        return control.existeOperador(codigoOperador, password);
    }

    /**
     * Retorna alarmas pendientes filtradas por maquinas asignadas al operador.
     * Patron: Secure Messaging (solo alarmas de maquinas del operador autenticado).
     *
     * Spec 08 - Task 4
     */
    @Override
    public List<String> consultarAlarmasPendientes(int codigoOperador, Current current) {
        List<String> resultado = new ArrayList<>();

        // Obtener maquinas asignadas al operador
        List<String> asignaciones = control.listaAsignaciones(codigoOperador);
        HashSet<Integer> maquinasAsignadas = new HashSet<>();

        for (String dato : asignaciones) {
            String[] partes = dato.split("-");
            try {
                maquinasAsignadas.add(Integer.parseInt(partes[0].trim()));
            } catch (java.lang.Exception e) {
                e.printStackTrace();
            }
        }

        // Filtrar alarmas pendientes por maquinas asignadas
        List<AlarmaPendiente> pendientes =
                AlarmasPendientesRepositorio.getInstance().listarPendientes();

        for (AlarmaPendiente alarma : pendientes) {
            if (maquinasAsignadas.contains(alarma.getIdMaquina())) {
                resultado.add(alarma.toTransportString());
            }
        }

        return resultado;
    }
}
