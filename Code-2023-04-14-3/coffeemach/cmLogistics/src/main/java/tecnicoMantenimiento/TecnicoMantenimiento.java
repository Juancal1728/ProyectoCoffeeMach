package tecnicoMantenimiento;

import java.io.Serializable;

/**
 * Modelo del tecnico de mantenimiento (operador logistico autenticado).
 * Encapsula la identidad y estado de sesion del operador en cmLogistics.
 * Patron: Secure Messaging — se crea solo despues de autenticacion exitosa.
 */
public class TecnicoMantenimiento implements Serializable {
    private static final long serialVersionUID = 1L;

    private int codigoOperador;
    private boolean autenticado;

    public TecnicoMantenimiento(int codigoOperador) {
        this.codigoOperador = codigoOperador;
        this.autenticado = false;
    }

    public int getCodigoOperador() { return codigoOperador; }

    public boolean isAutenticado() { return autenticado; }

    public void setAutenticado(boolean autenticado) {
        this.autenticado = autenticado;
    }

    @Override
    public String toString() {
        return "Tecnico[cod=" + codigoOperador
                + ", autenticado=" + autenticado + "]";
    }
}
