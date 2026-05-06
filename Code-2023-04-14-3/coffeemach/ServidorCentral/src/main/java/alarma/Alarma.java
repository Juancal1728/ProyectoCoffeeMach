package alarma;

import java.util.Date;

import com.zeroc.Ice.Current;

import modelo.AlarmaPendiente;
import modelo.AlarmasPendientesRepositorio;
import servicios.AlarmaService;
import servicios.Moneda;

/**
 * Servant Ice que implementa AlarmaService.
 * Recibe notificaciones de alarmas desde coffeeMach.
 * Registra en BD (via AlarmasManager) y en repositorio local (AlarmasPendientesRepositorio).
 *
 * Spec 03 (existente) + Spec 08 (alarmas pendientes)
 */
public class Alarma implements AlarmaService {

    public static final int ALARMA_INGREDIENTE = 1;
    public static final int ALARMA_MONEDA_CIEN = 2;
    public static final int ALARMA_MONEDA_DOS = 3;
    public static final int ALARMA_MONEDA_QUI = 4;
    public static final int ALARMA_SUMINISTRO = 5;
    public static final int ALARMA_MAL_FUNCIONAMIENTO = 6;

    private AlarmasManager manager;

    public Alarma(AlarmasManager manager) {
        this.manager = manager;
    }

    // --- Helpers Spec 08 ---

    private void registrarPendiente(int idMaq, int idAlarma, String recurso,
            int cantidad, String descripcion) {
        String ubicacion = "Maquina " + idMaq;
        AlarmaPendiente pendiente = new AlarmaPendiente(
                idMaq, idAlarma, recurso, cantidad, ubicacion, descripcion);
        AlarmasPendientesRepositorio.getInstance().guardarAlarma(pendiente);
    }

    private int idAlarmaDesdeInsumo(String idInsumo) {
        try {
            return Integer.parseInt(idInsumo);
        } catch (Exception ignored) {}

        switch (idInsumo) {
            case "MONEDA100": return ALARMA_MONEDA_CIEN;
            case "MONEDA200": return ALARMA_MONEDA_DOS;
            case "MONEDA500": return ALARMA_MONEDA_QUI;
            case "AGUA":
            case "CAFE":
            case "AZUCAR":
                return ALARMA_INGREDIENTE;
            case "VASO":
                return ALARMA_SUMINISTRO;
            case "KITREPARACION":
                return ALARMA_MAL_FUNCIONAMIENTO;
            default:
                return -1;
        }
    }

    // --- Servant methods ---

    @Override
    public void recibirNotificacionEscasezIngredientes(String iDing, int idMaq, Current current) {
        manager.alarmaMaquina(ALARMA_INGREDIENTE, idMaq, new Date());

        String recurso = iDing.toUpperCase();
        if ("CAFÉ".equals(recurso)) recurso = "CAFE";

        registrarPendiente(idMaq, ALARMA_INGREDIENTE, recurso, 500, "Escasez de " + iDing);
    }

    @Override
    public void recibirNotificacionInsuficienciaMoneda(Moneda moneda, int idMaq, Current current) {
        int idAlarma;
        String recurso;

        switch (moneda) {
            case CIEN:
                idAlarma = ALARMA_MONEDA_CIEN;
                recurso = "MONEDA100";
                break;
            case DOCIENTOS:
                idAlarma = ALARMA_MONEDA_DOS;
                recurso = "MONEDA200";
                break;
            case QUINIENTOS:
                idAlarma = ALARMA_MONEDA_QUI;
                recurso = "MONEDA500";
                break;
            default:
                return;
        }

        manager.alarmaMaquina(idAlarma, idMaq, new Date());
        registrarPendiente(idMaq, idAlarma, recurso, 20, "Insuficiencia de " + recurso);
    }

    @Override
    public void recibirNotificacionEscasezSuministro(String idSumin, int idMaq, Current current) {
        manager.alarmaMaquina(ALARMA_SUMINISTRO, idMaq, new Date());
        registrarPendiente(idMaq, ALARMA_SUMINISTRO, "VASO", 100,
                "Escasez de suministro " + idSumin);
    }

    @Override
    public void recibirNotificacionAbastesimiento(int idMaq, String idInsumo,
            int cantidad, Current current) {
        int idAlarma = idAlarmaDesdeInsumo(idInsumo);

        if (idAlarma != -1) {
            manager.desactivarAlarma(idAlarma, idMaq, new Date());
            AlarmasPendientesRepositorio.getInstance().cerrarAlarma(idMaq, idAlarma);
        } else {
            System.out.println("No se pudo mapear abastecimiento: " + idInsumo);
        }
    }

    @Override
    public void recibirNotificacionMalFuncionamiento(int idMaq, String descri, Current current) {
        manager.alarmaMaquina(ALARMA_MAL_FUNCIONAMIENTO, idMaq, new Date());
        registrarPendiente(idMaq, ALARMA_MAL_FUNCIONAMIENTO, "KITREPARACION", 1, descri);
    }

}
