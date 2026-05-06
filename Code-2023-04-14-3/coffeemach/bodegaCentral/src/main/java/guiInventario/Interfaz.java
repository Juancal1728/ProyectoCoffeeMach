package guiInventario;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

public class Interfaz extends JFrame {

    private JTextArea textAreaInventario;
    private JComboBox<String> comboBoxRecurso;
    private JTextField textFieldCantidad;
    private JButton btnRecargar;
    private JButton btnActualizar;
    private JLabel lblEstado;

    public Interfaz() {
        setTitle("Bodega Central");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 500, 420);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JLabel lblTitle = new JLabel("Bodega Central - Inventario");
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setBounds(5, 5, 480, 20);
        contentPane.add(lblTitle);

        JLabel lblInventario = new JLabel("Inventario actual:");
        lblInventario.setBounds(10, 30, 200, 14);
        contentPane.add(lblInventario);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 50, 462, 180);
        contentPane.add(scrollPane);

        textAreaInventario = new JTextArea();
        textAreaInventario.setEditable(false);
        textAreaInventario.setFont(new Font("Monospaced", Font.PLAIN, 13));
        scrollPane.setViewportView(textAreaInventario);

        JPanel panelRecargar = new JPanel();
        panelRecargar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        panelRecargar.setBounds(10, 242, 462, 90);
        contentPane.add(panelRecargar);
        panelRecargar.setLayout(null);

        JLabel lblRecurso = new JLabel("Recurso:");
        lblRecurso.setBounds(10, 18, 65, 20);
        panelRecargar.add(lblRecurso);

        comboBoxRecurso = new JComboBox<>(new String[]{
            "MONEDA100", "MONEDA200", "MONEDA500",
            "AGUA", "CAFE", "AZUCAR", "VASO", "KITREPARACION"
        });
        comboBoxRecurso.setBounds(80, 15, 165, 25);
        panelRecargar.add(comboBoxRecurso);

        JLabel lblCantidad = new JLabel("Cantidad:");
        lblCantidad.setBounds(258, 18, 65, 20);
        panelRecargar.add(lblCantidad);

        textFieldCantidad = new JTextField();
        textFieldCantidad.setBounds(325, 15, 80, 25);
        panelRecargar.add(textFieldCantidad);

        btnRecargar = new JButton("Recargar");
        btnRecargar.setBounds(175, 55, 110, 25);
        panelRecargar.add(btnRecargar);

        btnActualizar = new JButton("Actualizar");
        btnActualizar.setBounds(10, 345, 110, 28);
        contentPane.add(btnActualizar);

        lblEstado = new JLabel("");
        lblEstado.setBounds(130, 349, 342, 20);
        contentPane.add(lblEstado);
    }

    public JTextArea getTextAreaInventario() { return textAreaInventario; }
    public JComboBox<String> getComboBoxRecurso() { return comboBoxRecurso; }
    public JTextField getTextFieldCantidad() { return textFieldCantidad; }
    public JButton getBtnRecargar() { return btnRecargar; }
    public JButton getBtnActualizar() { return btnActualizar; }
    public JLabel getLblEstado() { return lblEstado; }
}