package bodega;

import java.io.*;
import java.util.*;

/**
 * Repositorio local del inventario de bodega. Patron Repository + Singleton standalone.
 * Persiste en inventarioBodega.bd.
 * Los nombres de recurso corresponden al enum RecursoAbastecimiento de CoffeMach.ice
 * (sin guiones bajos: MONEDA100, MONEDA200, MONEDA500, AGUA, CAFE, AZUCAR, VASO, KITREPARACION).
 *
 * Spec 09 - Task 2
 */
public class InventarioRepositorio implements Serializable {
    private static final long serialVersionUID = 1L;
    private static transient InventarioRepositorio instance;

    private HashMap<String, ItemInventario> inventario;
    private static final String ARCHIVO = "inventarioBodega.bd";

    private InventarioRepositorio() {
        inventario = new HashMap<>();
        cargar();
        if (inventario.isEmpty()) cargarInicial();
    }

    public static synchronized InventarioRepositorio getInstance() {
        if (instance == null) {
            instance = new InventarioRepositorio();
        }
        return instance;
    }

    private void cargarInicial() {
        inventario.put("MONEDA100", new ItemInventario("MONEDA100", 100));
        inventario.put("MONEDA200", new ItemInventario("MONEDA200", 100));
        inventario.put("MONEDA500", new ItemInventario("MONEDA500", 100));
        inventario.put("AGUA", new ItemInventario("AGUA", 10000));
        inventario.put("CAFE", new ItemInventario("CAFE", 3000));
        inventario.put("AZUCAR", new ItemInventario("AZUCAR", 3000));
        inventario.put("VASO", new ItemInventario("VASO", 500));
        inventario.put("KITREPARACION", new ItemInventario("KITREPARACION", 10));
        guardar();
    }

    public synchronized boolean hayExistencias(String recurso, int cantidad) {
        ItemInventario item = inventario.get(recurso);
        return item != null && item.getCantidad() >= cantidad;
    }

    public synchronized boolean despachar(String recurso, int cantidad) {
        ItemInventario item = inventario.get(recurso);
        if (item == null) return false;
        boolean ok = item.retirar(cantidad);
        if (ok) guardar();
        return ok;
    }

    public synchronized void recargar(String recurso, int cantidad) {
        ItemInventario item = inventario.get(recurso);
        if (item == null) {
            item = new ItemInventario(recurso, 0);
            inventario.put(recurso, item);
        }
        item.sumar(cantidad);
        guardar();
    }

    public synchronized List<String> listar() {
        List<String> datos = new ArrayList<>();
        for (ItemInventario item : inventario.values()) {
            datos.add(item.toString());
        }
        return datos;
    }

    @SuppressWarnings("unchecked")
    private void cargar() {
        try {
            File file = new File(ARCHIVO);
            if (!file.exists()) return;
            try (ObjectInputStream in = new ObjectInputStream(
                    new FileInputStream(file))) {
                inventario = (HashMap<String, ItemInventario>) in.readObject();
            }
        } catch (Exception e) {
            System.err.println("Error al cargar inventarioBodega.bd: " + e.getMessage());
            inventario = new HashMap<>();
        }
    }

    private void guardar() {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(ARCHIVO))) {
            out.writeObject(inventario);
        } catch (Exception e) {
            System.err.println("Error al guardar inventarioBodega.bd");
            e.printStackTrace();
        }
    }
}
