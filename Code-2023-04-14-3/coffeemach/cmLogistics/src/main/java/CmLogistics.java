import java.util.*;
import java.util.concurrent.CountDownLatch;
import com.zeroc.Ice.*;
import interfaz.ControladorLogistica;
import servicios.*;
import logistica.LogisticaController;

/**
 * Componente cmLogistics - punto de entrada.
 * Cliente puro (no registra servants). Consume 3 proxies Ice:
 * - ServicioComLogisticaPrx (ServidorCentral)
 * - ServicioBodegaPrx (bodegaCentral)
 * - ServicioAbastecimientoPrx (coffeeMach)
 *
 * Patron: Broker (Client Proxy), Callback, Secure Messaging.
 *
 * Spec 10 - Task 2
 */
public class CmLogistics {

    public static void main(String[] args) {
        List<String> extArgs = new ArrayList<>();
        try (Communicator communicator = Util.initialize(args, "CmLogistic.cfg", extArgs)) {

            // Crear proxies Ice (Broker pattern)
            ServicioComLogisticaPrx servidor = ServicioComLogisticaPrx.checkedCast(
                    communicator.propertyToProxy("ServerCentral")).ice_twoway();

            ServicioBodegaPrx bodega = ServicioBodegaPrx.checkedCast(
                    communicator.propertyToProxy("BodegaCentral")).ice_twoway();

            ServicioAbastecimientoPrx maquina = ServicioAbastecimientoPrx.checkedCast(
                    communicator.propertyToProxy("MaquinaCafe")).ice_twoway();

            // Controller pattern
            LogisticaController controller = new LogisticaController(
                    servidor, bodega, maquina);

            System.out.println("CmLogistics conectado a servidor, bodega y maquina");

            CountDownLatch cierre = new CountDownLatch(1);
            ControladorLogistica controlador = new ControladorLogistica(controller);
            controlador.setOnClose(() -> cierre.countDown());
            controlador.run();
            cierre.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
