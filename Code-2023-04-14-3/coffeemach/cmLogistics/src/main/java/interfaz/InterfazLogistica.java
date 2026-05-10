package interfaz;

import controlAlarma.gui.PanelAlarmas;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

/**
 * Ventana principal de cmLogistics.
 * Compuesta por:
 *   - Panel de sesion (autenticacion del tecnico)
 *   - PanelAlarmas (controlAlarma.gui) — gestion de alarmas pendientes
 *   - Panel de eventos (log de operaciones)
 *   - Panel de inventario (consulta a bodegaCentral)
 */
public class InterfazLogistica extends JFrame {

    private JTextField textFieldCodigoOperador;
    private JTextField textFieldPassword;
    private JButton btnIniciarSesion;
    private JButton btnConsultarInventario;
    private JTextArea textAreaEventos;
    private JTextArea textAreaInventario;
    private JLabel lblEstado;

    private PanelAlarmas panelAlarmas;

    public InterfazLogistica() {
        setTitle("CmLogistics");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 760, 455);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JLabel lblTitulo = new JLabel("CmLogistics - Panel de Operador");
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setBounds(5, 5, 730, 20);
        contentPane.add(lblTitulo);

        // --- Panel sesion ---
        JPanel panelSesion = new JPanel();
        panelSesion.setBorder(new BevelBorder(BevelBorder.LOWERED));
        panelSesion.setBounds(15, 35, 710, 70);
        contentPane.add(panelSesion);
        panelSesion.setLayout(null);

        JLabel lblCodigo = new JLabel("Codigo:");
        lblCodigo.setBounds(10, 12, 70, 20);
        panelSesion.add(lblCodigo);

        textFieldCodigoOperador = new JTextField();
        textFieldCodigoOperador.setBounds(75, 12, 95, 22);
        panelSesion.add(textFieldCodigoOperador);

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setBounds(185, 12, 80, 20);
        panelSesion.add(lblPassword);

        textFieldPassword = new JTextField();
        textFieldPassword.setBounds(260, 12, 120, 22);
        panelSesion.add(textFieldPassword);

        btnIniciarSesion = new JButton("Iniciar Sesion");
        btnIniciarSesion.setBounds(395, 10, 140, 25);
        panelSesion.add(btnIniciarSesion);

        lblEstado = new JLabel("Sin sesion");
        lblEstado.setBounds(10, 42, 680, 20);
        panelSesion.add(lblEstado);

        // --- Panel alarmas (componente reutilizable de controlAlarma.gui) ---
        panelAlarmas = new PanelAlarmas();
        panelAlarmas.setBounds(15, 115, 710, 95);
        contentPane.add(panelAlarmas);

        // --- Panel eventos ---
        JPanel panelEventos = new JPanel();
        panelEventos.setBorder(new BevelBorder(BevelBorder.LOWERED));
        panelEventos.setBounds(15, 220, 350, 180);
        contentPane.add(panelEventos);
        panelEventos.setLayout(null);

        JLabel lblEventos = new JLabel("Eventos");
        lblEventos.setHorizontalAlignment(SwingConstants.CENTER);
        lblEventos.setBounds(10, 8, 330, 18);
        panelEventos.add(lblEventos);

        JScrollPane scrollEventos = new JScrollPane();
        scrollEventos.setBounds(10, 32, 330, 135);
        panelEventos.add(scrollEventos);

        textAreaEventos = new JTextArea();
        textAreaEventos.setEditable(false);
        scrollEventos.setViewportView(textAreaEventos);

        // --- Panel inventario ---
        JPanel panelInventario = new JPanel();
        panelInventario.setBorder(new BevelBorder(BevelBorder.LOWERED));
        panelInventario.setBounds(375, 220, 350, 180);
        contentPane.add(panelInventario);
        panelInventario.setLayout(null);

        JLabel lblInventario = new JLabel("Inventario Bodega");
        lblInventario.setHorizontalAlignment(SwingConstants.CENTER);
        lblInventario.setBounds(10, 8, 330, 18);
        panelInventario.add(lblInventario);

        btnConsultarInventario = new JButton("Actualizar");
        btnConsultarInventario.setBounds(112, 147, 125, 25);
        panelInventario.add(btnConsultarInventario);

        JScrollPane scrollInventario = new JScrollPane();
        scrollInventario.setBounds(10, 32, 330, 110);
        panelInventario.add(scrollInventario);

        textAreaInventario = new JTextArea();
        textAreaInventario.setEditable(false);
        scrollInventario.setViewportView(textAreaInventario);
    }

    public JTextField getTextFieldCodigoOperador() { return textFieldCodigoOperador; }
    public JTextField getTextFieldPassword() { return textFieldPassword; }
    public JButton getBtnIniciarSesion() { return btnIniciarSesion; }
    public JButton getBtnConsultarInventario() { return btnConsultarInventario; }
    public JTextArea getTextAreaEventos() { return textAreaEventos; }
    public JTextArea getTextAreaInventario() { return textAreaInventario; }
    public JLabel getLblEstado() { return lblEstado; }
    public PanelAlarmas getPanelAlarmas() { return panelAlarmas; }
}
