package McControlador;

import interfaces.Repositorio;

public class VentaRepositorio extends Repositorio<String, Venta> {

    private static VentaRepositorio instance;

    public static synchronized VentaRepositorio getInstance() {
        if (instance == null) {
            instance = new VentaRepositorio();
        }
        return instance;
    }

    private VentaRepositorio() {
        super("ventas.bd");
    }

    public void loadDataP() {

    }
}
