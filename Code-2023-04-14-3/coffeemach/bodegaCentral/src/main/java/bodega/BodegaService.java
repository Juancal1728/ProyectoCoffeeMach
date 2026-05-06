package bodega;

import java.util.List;
import com.zeroc.Ice.Current;
import servicios.RecursoAbastecimiento;
import servicios.ServicioBodega;

/**
 * Servant Ice que implementa ServicioBodega.
 * Patron: Broker (Servant) + Repository.
 *
 * Spec 09 - Task 3
 */
public class BodegaService implements ServicioBodega {

    private InventarioRepositorio inventario = InventarioRepositorio.getInstance();

    @Override
    public List<String> consultarInventario(Current current) {
        return inventario.listar();
    }

    @Override
    public boolean hayExistencias(RecursoAbastecimiento recurso, int cantidad, Current current) {
        return inventario.hayExistencias(recurso.name(), cantidad);
    }

    @Override
    public boolean despachar(RecursoAbastecimiento recurso, int cantidad,
            int codMaquina, int idAlarma, Current current) {
        System.out.println("Despachando " + cantidad + " de " + recurso.name()
                + " a maquina " + codMaquina + " (alarma " + idAlarma + ")");
        return inventario.despachar(recurso.name(), cantidad);
    }
}
