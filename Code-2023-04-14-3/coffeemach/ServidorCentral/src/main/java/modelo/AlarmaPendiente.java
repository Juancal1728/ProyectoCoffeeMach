package modelo;

import java.io.Serializable;

/**
 * Representa una alarma pendiente de resolucion.
 * Se almacena en AlarmasPendientesRepositorio (archivo alarmasPendientes.bd).
 *
 * Spec 08 - Task 1
 */
public class AlarmaPendiente implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idMaquina;
    private int idAlarma;
    private String recurso;
    private int cantidad;
    private String ubicacion;
    private String descripcion;
    private boolean resuelta;

    public AlarmaPendiente(int idMaquina, int idAlarma, String recurso,
            int cantidad, String ubicacion, String descripcion) {
        this.idMaquina = idMaquina;
        this.idAlarma = idAlarma;
        this.recurso = recurso;
        this.cantidad = cantidad;
        this.ubicacion = ubicacion;
        this.descripcion = descripcion;
        this.resuelta = false;
    }

    /**
     * Formato de transporte para cmLogistics.
     * "idMaquina|idAlarma|recurso|cantidad|ubicacion|descripcion"
     */
    public String toTransportString() {
        return idMaquina + "|" + idAlarma + "|" + recurso + "|" + cantidad
                + "|" + ubicacion + "|" + descripcion;
    }

    public int getIdMaquina() { return idMaquina; }
    public int getIdAlarma() { return idAlarma; }
    public String getRecurso() { return recurso; }
    public int getCantidad() { return cantidad; }
    public String getUbicacion() { return ubicacion; }
    public String getDescripcion() { return descripcion; }
    public boolean isResuelta() { return resuelta; }
    public void setResuelta(boolean resuelta) { this.resuelta = resuelta; }
}
