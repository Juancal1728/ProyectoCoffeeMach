package receta;

import java.util.List;
import java.util.Map;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;

import modelo.ConexionBD;
import modelo.ManejadorDatos;
import servicios.RecetaService;

public class ProductoReceta implements RecetaService {

    private Communicator communicator;

    /**
     * @param communicator the communicator to set
     */
    public void setCommunicator(Communicator communicator) {
        this.communicator = communicator;
    }

    @Override
    public String[] consultarIngredientes(Current current) {
        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        String[] ret = md.consultarIngredientes();

        cbd.cerrarConexion();

        return ret;
    }

    @Override
    public String[] consultarRecetas(Current current) {
        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        String[] ret = md.consultarRecetas();

        cbd.cerrarConexion();

        return ret;
    }

    @Override
    public String[] consultarProductos(Current current) {
        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        List<String> listaAsociada = md.consultaRecetasCompleta();

        cbd.cerrarConexion();

        if (!listaAsociada.equals(null)) {

            String[] retorno = new String[listaAsociada.size()];

            for (int i = 0; i < listaAsociada.size(); i++) {

                retorno[i] = listaAsociada.get(i);
            }

            return retorno;
        }

        return null;
    }

    /**
     * Define un nuevo producto (receta) con sus ingredientes asociados.
     *
     * Flujo:
     * 1. Registra la receta (nombre + precio) en la BD
     * 2. Para cada ingrediente del mapa, busca su ID por nombre
     * 3. Asocia cada ingrediente a la receta con la cantidad indicada
     *
     * @param nombre       Nombre del producto/receta
     * @param precio       Precio del producto
     * @param ingredientes Mapa nombre_ingrediente -> cantidad_unidades
     */
    @Override
    public void definirProducto(String nombre, int precio, Map<String, Integer> ingredientes, Current current) {
        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        // Paso 1: Registrar la receta y obtener su ID
        String resultado = md.registrarReceta(nombre, precio);

        if (resultado == null || resultado.isEmpty()) {
            System.out.println("No se pudo registrar la receta: " + nombre);
            cbd.cerrarConexion();
            return;
        }

        // resultado tiene formato "id-nombre-precio"
        String[] partes = resultado.split("-");
        int idReceta = Integer.parseInt(partes[0].trim());

        // Paso 2: Asociar cada ingrediente
        for (Map.Entry<String, Integer> entry : ingredientes.entrySet()) {
            String nombreIngrediente = entry.getKey();
            int cantidad = entry.getValue();

            int idIngrediente = md.buscarIdIngredientePorNombre(nombreIngrediente);
            if (idIngrediente == -1) {
                System.out.println("Ingrediente no encontrado: " + nombreIngrediente);
                continue;
            }

            md.registrarRecetaIngrediente(idReceta, idIngrediente, cantidad);
        }

        cbd.cerrarConexion();
        System.out.println("Producto definido: " + nombre + " (ID=" + idReceta + ")");
    }

    @Override
    public void borrarReceta(int cod, Current current) {
        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        md.borrarReceta(cod);

        cbd.cerrarConexion();
    }

    @Override
    public void definirRecetaIngrediente(int idReceta, int idIngrediente, int valor, Current current) {

        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        md.registrarRecetaIngrediente(idReceta, idIngrediente, valor);

        cbd.cerrarConexion();
    }

    @Override
    public String registrarReceta(String nombre, int precio, Current current) {
        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        String ret = md.registrarReceta(nombre, precio);

        cbd.cerrarConexion();

        return ret;
    }

    @Override
    public String registrarIngrediente(String nombre, Current current) {
        ConexionBD cbd = new ConexionBD(communicator);
        cbd.conectarBaseDatos();
        ManejadorDatos md = new ManejadorDatos();
        md.setConexion(cbd.getConnection());

        String ret = md.registrarIngrediente(nombre);

        cbd.cerrarConexion();

        return ret;
    }

}
