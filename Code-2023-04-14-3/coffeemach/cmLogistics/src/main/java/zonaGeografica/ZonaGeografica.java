package zonaGeografica;

/**
 * Modelo de asignacion geografica de una maquina de cafe.
 * Representa el par (idMaquina, ubicacion) que retorna el servidor
 * cuando se consultan las asignaciones de un operador.
 * Formato de transporte del servidor: "idMaquina-ubicacion"
 */
public class ZonaGeografica {

    private int idMaquina;
    private String ubicacion;

    public ZonaGeografica(int idMaquina, String ubicacion) {
        this.idMaquina = idMaquina;
        this.ubicacion = ubicacion;
    }

    /**
     * Parsea el formato de transporte "idMaquina-ubicacion" retornado
     * por ServicioComLogistica.asignacionMaquina().
     *
     * @param dato string en formato "idMaquina-ubicacion"
     * @return ZonaGeografica parseada, o null si el formato es invalido
     */
    public static ZonaGeografica fromString(String dato) {
        if (dato == null || dato.isBlank()) return null;
        String[] partes = dato.split("-", 2);
        if (partes.length < 2) return null;
        try {
            int id = Integer.parseInt(partes[0].trim());
            return new ZonaGeografica(id, partes[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getIdMaquina() { return idMaquina; }
    public String getUbicacion() { return ubicacion; }

    @Override
    public String toString() {
        return "Maquina " + idMaquina + " — " + ubicacion;
    }
}
