import java.util.*;
import com.zeroc.Ice.*;
import bodega.BodegaService;
import bodega.InventarioRepositorio;
import guiInventario.ControladorInventario;

/**
 * Componente bodegaCentral - punto de entrada.
 * Patron: Broker (ObjectAdapter + Servant).
 * Arranca servidor Ice en puerto 12347 con servant "bodega".
 * Hilo daemon para consola interactiva de administracion de inventario.
 *
 * Spec 09 - Task 4
 */
public class BodegaCentral {

    public static void main(String[] args) {
        List<String> extArgs = new ArrayList<>();
        try (Communicator communicator = Util.initialize(args, "BodegaCentral.cfg", extArgs)) {

            ObjectAdapter adapter = communicator.createObjectAdapter("BodegaCentral");
            adapter.add(new BodegaService(), Util.stringToIdentity("bodega"));
            adapter.activate();

            ControladorInventario controlador = new ControladorInventario(
                    InventarioRepositorio.getInstance());
            controlador.run();

            System.out.println("BodegaCentral lista en puerto 12347");
            communicator.waitForShutdown();
        }
    }
}
