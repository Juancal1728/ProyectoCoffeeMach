package controlAlarma.gui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 * Panel Swing que encapsula los controles de gestion de alarmas.
 * Se embebe en InterfazLogistica.
 * Patron: MVC — vista pura, sin logica de negocio.
 */
public class PanelAlarmas extends JPanel {

    private JComboBox<String> comboBoxAlarmas;
    private JButton btnConsultarAlarmas;
    private JButton btnResolverAlarma;

    public PanelAlarmas() {
        setBorder(new BevelBorder(BevelBorder.LOWERED));
        setLayout(null);

        JLabel lblAlarmas = new JLabel("Alarmas pendientes:");
        lblAlarmas.setBounds(10, 12, 150, 20);
        add(lblAlarmas);

        comboBoxAlarmas = new JComboBox<>();
        comboBoxAlarmas.setBounds(10, 38, 530, 25);
        add(comboBoxAlarmas);

        btnConsultarAlarmas = new JButton("Consultar");
        btnConsultarAlarmas.setBounds(555, 12, 135, 25);
        add(btnConsultarAlarmas);

        btnResolverAlarma = new JButton("Resolver");
        btnResolverAlarma.setBounds(555, 48, 135, 25);
        add(btnResolverAlarma);
    }

    public JComboBox<String> getComboBoxAlarmas() { return comboBoxAlarmas; }
    public JButton getBtnConsultarAlarmas() { return btnConsultarAlarmas; }
    public JButton getBtnResolverAlarma() { return btnResolverAlarma; }
}
