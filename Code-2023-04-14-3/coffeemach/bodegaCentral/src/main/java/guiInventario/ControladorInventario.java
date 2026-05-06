package guiInventario;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import bodega.InventarioRepositorio;

public class ControladorInventario implements Runnable {

    private InventarioRepositorio inventario;
    private Interfaz interfaz;

    public ControladorInventario(InventarioRepositorio inventario) {
        this.inventario = inventario;
    }

    @Override
    public void run() {
        try {
            interfaz = new Interfaz();
            interfaz.setLocationRelativeTo(null);
            interfaz.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            interfaz.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        actualizarVista();
        eventos();
    }

    private void eventos() {
        interfaz.getBtnActualizar().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actualizarVista();
            }
        });

        interfaz.getBtnRecargar().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                recargarInventario();
            }
        });
    }

    private void recargarInventario() {
        try {
            String recurso = interfaz.getComboBoxRecurso().getSelectedItem().toString();
            int cantidad = Integer.parseInt(interfaz.getTextFieldCantidad().getText());

            if (cantidad <= 0) {
                interfaz.getLblEstado().setText("La cantidad debe ser mayor a cero");
                return;
            }

            inventario.recargar(recurso, cantidad);
            interfaz.getTextFieldCantidad().setText("");
            interfaz.getLblEstado().setText("Inventario actualizado: " + recurso + " +" + cantidad);
            actualizarVista();
        } catch (NumberFormatException e) {
            interfaz.getLblEstado().setText("Cantidad invalida");
        }
    }

    private void actualizarVista() {
        interfaz.getTextAreaInventario().setText("");
        for (String item : inventario.listar()) {
            interfaz.getTextAreaInventario().setText(
                    interfaz.getTextAreaInventario().getText() + item + "\n");
        }
    }
}
