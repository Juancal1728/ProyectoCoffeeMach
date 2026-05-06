module servicios {

    sequence<string> StringArr;

    ["java:type:java.util.ArrayList<String>"]
    sequence<string> StringSeq;

    dictionary<string,int> MapStrInt;

    enum Moneda {
        CIEN,
        DOCIENTOS,
        QUINIENTOS
    }

    enum RecursoAbastecimiento {
        MONEDA100,
        MONEDA200,
        MONEDA500,
        AGUA,
        CAFE,
        AZUCAR,
        VASO,
        KITREPARACION
    }



    interface ServicioComLogistica {
        StringSeq asignacionMaquina(int codigoOperador);
        StringSeq asignacionMaquinasDesabastecidas(int codigoOperador);
        StringSeq consultarAlarmasPendientes(int codigoOperador);
        bool inicioSesion(int codigoOperador, string password);
    }

    interface ServicioAbastecimiento {
        void abastecer(int codMaquina, int tipoAlarma);
        bool abastecerRecurso(int codMaquina, RecursoAbastecimiento recurso, int cantidad, int idAlarma);
    }

    interface ServicioBodega {
        StringSeq consultarInventario();
        bool hayExistencias(RecursoAbastecimiento recurso, int cantidad);
        bool despachar(RecursoAbastecimiento recurso, int cantidad, int codMaquina, int idAlarma);
    }

    interface AlarmaService {
        void recibirNotificacionEscasezIngredientes(string iDing, int idMaq);
        void recibirNotificacionInsuficienciaMoneda(Moneda moneda, int idMaq);
        void recibirNotificacionEscasezSuministro(string idSumin, int idMaq);
        void recibirNotificacionAbastesimiento(int idMaq, string idInsumo, int cantidad);
        void recibirNotificacionMalFuncionamiento(int idMaq, string descri);
    }

    interface VentaService {
        void registrarVenta(int codMaq, StringArr ventas);
    }

    interface RecetaService {
        StringArr consultarIngredientes();
        StringArr consultarRecetas();
        StringArr consultarProductos();
        void definirProducto(string nombre, int precio, MapStrInt ingredientes);
        void borrarReceta(int cod);
        void definirRecetaIngrediente(int idReceta, int idIngrediente, int valor);
        string registrarReceta(string nombre, int precio);
        string registrarIngrediente(string nombre);
    }
}
