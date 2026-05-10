package modelo;

import java.io.*;
import java.util.*;

/**
 * Repositorio local de alarmas pendientes. Patron Repository + Singleton standalone.
 * No hereda de coffeeMach/interfaces/Repositorio (modulo Gradle separado).
 * Persiste en alarmasPendientes.bd.
 *
 * Spec 08 - Task 2
 */
public class AlarmasPendientesRepositorio implements Serializable {
    private static final long serialVersionUID = 1L;
    private static transient AlarmasPendientesRepositorio instance;

    private HashMap<String, AlarmaPendiente> alarmas;
    private static final String ARCHIVO = "alarmasPendientes.bd";

    private AlarmasPendientesRepositorio() {
        alarmas = new HashMap<>();
        cargar();
    }

    public static synchronized AlarmasPendientesRepositorio getInstance() {
        if (instance == null) {
            instance = new AlarmasPendientesRepositorio();
        }
        return instance;
    }

    public synchronized void guardarAlarma(AlarmaPendiente alarma) {
        String key = alarma.getIdMaquina() + "-" + alarma.getIdAlarma();
        alarmas.put(key, alarma);
        guardar();
    }

    public synchronized void cerrarAlarma(int idMaquina, int idAlarma) {
        String key = idMaquina + "-" + idAlarma;
        AlarmaPendiente alarma = alarmas.get(key);
        if (alarma != null) {
            alarma.setResuelta(true);
            guardar();
        }
    }

    public synchronized List<AlarmaPendiente> listarPendientes() {
        List<AlarmaPendiente> pendientes = new ArrayList<>();
        for (AlarmaPendiente alarma : alarmas.values()) {
            if (!alarma.isResuelta()) {
                pendientes.add(alarma);
            }
        }
        return pendientes;
    }

    @SuppressWarnings("unchecked")
    private void cargar() {
        try {
            File file = new File(ARCHIVO);
            if (!file.exists()) return;
            try (ObjectInputStream in = new ObjectInputStream(
                    new FileInputStream(file))) {
                alarmas = (HashMap<String, AlarmaPendiente>) in.readObject();
            }
        } catch (Exception e) {
            System.err.println("Error al cargar alarmasPendientes.bd: " + e.getMessage());
            alarmas = new HashMap<>();
        }
    }

    private void guardar() {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(ARCHIVO))) {
            out.writeObject(alarmas);
        } catch (Exception e) {
            System.err.println("Error al guardar alarmasPendientes.bd");
            e.printStackTrace();
        }
    }
}
