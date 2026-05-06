package interfaz;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import logistica.LogisticaController;

public class ControladorLogistica implements Runnable {

    private LogisticaController controller;
    private InterfazLogistica interfaz;
    private int codigoOperador;
    private boolean autenticado;
    private List<String> alarmasActuales = new ArrayList<>();
    private Runnable onClose;

    public ControladorLogistica(LogisticaController controller) {
        this.controller = controller;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public void run() {
        try {
            interfaz = new InterfazLogistica();
            interfaz.setLocationRelativeTo(null);
            interfaz.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            interfaz.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    if (onClose != null) {
                        onClose.run();
                    }
                }
            });
            interfaz.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        eventos();
        registrarEvento("CmLogistics conectado. Inicie sesion para operar.");
        actualizarInventario();
    }

    private void eventos() {
        interfaz.getBtnIniciarSesion().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                iniciarSesion();
            }
        });

        interfaz.getBtnConsultarAlarmas().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                consultarAlarmas();
            }
        });

        interfaz.getBtnResolverAlarma().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resolverAlarmaSeleccionada();
            }
        });

        interfaz.getBtnConsultarInventario().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actualizarInventario();
            }
        });
    }

    private void iniciarSesion() {
        try {
            codigoOperador = Integer.parseInt(interfaz.getTextFieldCodigoOperador().getText());
            String password = interfaz.getTextFieldPassword().getText();
            autenticado = controller.iniciarSesion(codigoOperador, password);

            if (autenticado) {
                interfaz.getLblEstado().setText("Sesion iniciada para operador " + codigoOperador);
                registrarEvento("Sesion iniciada correctamente.");
                consultarAlarmas();
            } else {
                interfaz.getLblEstado().setText("Credenciales invalidas");
                registrarEvento("Credenciales invalidas.");
            }
        } catch (NumberFormatException e) {
            interfaz.getLblEstado().setText("Codigo invalido");
            registrarEvento("Codigo de operador invalido.");
        } catch (com.zeroc.Ice.LocalException e) {
            interfaz.getLblEstado().setText("Error de comunicacion con servidor");
            registrarEvento("Error Ice al iniciar sesion: " + e.getMessage());
        }
    }

    private void consultarAlarmas() {
        if (!autenticado) {
            registrarEvento("Primero debe iniciar sesion.");
            return;
        }

        try {
            alarmasActuales = controller.consultarAlarmas(codigoOperador);
            interfaz.getComboBoxAlarmas().removeAllItems();

            if (alarmasActuales == null || alarmasActuales.isEmpty()) {
                registrarEvento("No hay alarmas pendientes.");
                return;
            }

            for (String alarma : alarmasActuales) {
                interfaz.getComboBoxAlarmas().addItem(formatearAlarma(alarma));
            }

            registrarEvento("Alarmas pendientes actualizadas: " + alarmasActuales.size());
        } catch (com.zeroc.Ice.LocalException e) {
            registrarEvento("Error Ice consultando alarmas: " + e.getMessage());
        }
    }

    private void resolverAlarmaSeleccionada() {
        if (!autenticado) {
            registrarEvento("Primero debe iniciar sesion.");
            return;
        }

        int index = interfaz.getComboBoxAlarmas().getSelectedIndex();
        if (index < 0 || index >= alarmasActuales.size()) {
            registrarEvento("Seleccione una alarma pendiente.");
            return;
        }

        try {
            String alarma = alarmasActuales.get(index);
            registrarEvento("Resolviendo: " + formatearAlarma(alarma));
            boolean resuelta = controller.resolverAlarma(alarma);

            if (resuelta) {
                registrarEvento("Alarma resuelta exitosamente.");
                consultarAlarmas();
                actualizarInventario();
            } else {
                registrarEvento("No se pudo resolver la alarma.");
            }
        } catch (com.zeroc.Ice.LocalException e) {
            registrarEvento("Error Ice resolviendo alarma: " + e.getMessage());
        } catch (Exception e) {
            registrarEvento("Error resolviendo alarma: " + e.getMessage());
        }
    }

    private void actualizarInventario() {
        try {
            interfaz.getTextAreaInventario().setText("");
            for (String item : controller.consultarInventario()) {
                interfaz.getTextAreaInventario().setText(
                        interfaz.getTextAreaInventario().getText() + item + "\n");
            }
        } catch (com.zeroc.Ice.LocalException e) {
            interfaz.getTextAreaInventario().setText("No se pudo consultar bodega");
            registrarEvento("Error Ice consultando inventario: " + e.getMessage());
        }
    }

    private void registrarEvento(String evento) {
        interfaz.getTextAreaEventos().setText(
                interfaz.getTextAreaEventos().getText() + evento + "\n");
    }

    private String formatearAlarma(String alarma) {
        String[] partes = alarma.split("\\|");
        if (partes.length < 6) {
            return alarma;
        }

        return "Maq " + partes[0] + " - " + partes[2] + " x" + partes[3]
                + " - " + partes[5];
    }
}
