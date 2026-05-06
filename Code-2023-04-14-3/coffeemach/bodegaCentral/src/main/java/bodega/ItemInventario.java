package bodega;

import java.io.Serializable;

/**
 * Modelo de un item de inventario en la bodega central.
 *
 * Spec 09 - Task 1
 */
public class ItemInventario implements Serializable {
    private static final long serialVersionUID = 1L;

    private String recurso;
    private int cantidad;

    public ItemInventario(String recurso, int cantidad) {
        this.recurso = recurso;
        this.cantidad = cantidad;
    }

    public String getRecurso() { return recurso; }
    public int getCantidad() { return cantidad; }

    public void sumar(int valor) {
        cantidad += valor;
    }

    public boolean retirar(int valor) {
        if (cantidad < valor) return false;
        cantidad -= valor;
        return true;
    }

    @Override
    public String toString() {
        return recurso + ": " + cantidad;
    }
}
