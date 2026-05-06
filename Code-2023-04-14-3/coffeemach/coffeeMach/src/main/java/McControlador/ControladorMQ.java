package McControlador;

import servicios.*;
import monedero.DepositoMonedas;
import monedero.MonedasRepositorio;
import productoReceta.Receta;
import productoReceta.RecetaRepositorio;

import java.util.*;
import java.io.*;
import java.util.Map.Entry;
import javax.swing.JFrame;
import java.awt.event.*;
import interfazUsuario.Interfaz;
import com.zeroc.Ice.Current;

import alarma.Alarma;
import alarma.AlarmaRepositorio;
import ingrediente.Ingrediente;
import ingrediente.IngredienteRepositorio;

/**
 * Controlador principal de la maquina de cafe.
 * Implementa ServicioAbastecimiento como servant Ice (callback receptor).
 * Patron: Controller + Callback (Servant) + Repository.
 *
 * Spec 02 (GUI/vending), Spec 06 (abastecimiento), Spec 11 (abastecerRecurso + fixes)
 */
public class ControladorMQ implements Runnable, ServicioAbastecimiento {

	private AlarmaServicePrx alarmaServicePrx;
	private VentaServicePrx ventasService;

	private AlarmaRepositorio alarmas = AlarmaRepositorio.getInstance();
	private IngredienteRepositorio ingredientes = IngredienteRepositorio.getInstance();
	private MonedasRepositorio monedas = MonedasRepositorio.getInstance();
	private RecetaRepositorio recetas = RecetaRepositorio.getInstance();
	private VentaRepositorio ventas = VentaRepositorio.getInstance();

	public void setVentas(VentaServicePrx ventasS) {
		this.ventasService = ventasS;
	}

	public void setAlarmaService(AlarmaServicePrx a) {
		alarmaServicePrx = a;
	}

	private RecetaServicePrx recetaServicePrx;

	public void setRecetaServicePrx(RecetaServicePrx recetaServicePrx) {
		this.recetaServicePrx = recetaServicePrx;
	}

	private Interfaz frame;
	private int codMaquina;
	private double suma;

	public void run() {
		try {
			frame = new Interfaz();
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		arrancarMaquina();
		eventos();
	}

	// ==================== Spec 11 - abastecerRecurso (NUEVO) ====================

	/**
	 * Callback receptor: cmLogistics llama este metodo para abastecer un recurso
	 * especifico con cantidad explicita.
	 * Patron: Callback (Servant receptor).
	 *
	 * Spec 11 - Task 1
	 */
	@Override
	public boolean abastecerRecurso(int codMaquina, RecursoAbastecimiento recurso,
			int cantidad, int idAlarma, Current current) {
		if (codMaquina != this.codMaquina) {
			return false;
		}

		boolean aplicado = aplicarAbastecimiento(recurso.name(), cantidad,
				idAlarma + "");

		if (aplicado) {
			alarmaServicePrx.recibirNotificacionAbastesimiento(
					codMaquina, recurso.name(), cantidad);
		}

		return aplicado;
	}

	/**
	 * Logica comun de abastecimiento compartida entre abastecer() y abastecerRecurso().
	 * Mapea el nombre del recurso al repositorio local correspondiente.
	 *
	 * Spec 11 - Task 2
	 */
	private boolean aplicarAbastecimiento(String recurso, int cantidad, String idAlarma) {
		switch (recurso) {
			case "MONEDA100": {
				DepositoMonedas moneda = monedas.findByKey("100");
				moneda.setCantidad(moneda.getCantidad() + cantidad);
				monedas.addElement("100", moneda);
				break;
			}
			case "MONEDA200": {
				DepositoMonedas moneda = monedas.findByKey("200");
				moneda.setCantidad(moneda.getCantidad() + cantidad);
				monedas.addElement("200", moneda);
				break;
			}
			case "MONEDA500": {
				DepositoMonedas moneda = monedas.findByKey("500");
				moneda.setCantidad(moneda.getCantidad() + cantidad);
				monedas.addElement("500", moneda);
				break;
			}
			case "AGUA":
				recargarIngredienteEspecifico("Agua");
				break;
			case "CAFE":
				recargarIngredienteEspecifico("Cafe");
				break;
			case "AZUCAR":
				recargarIngredienteEspecifico("Azucar");
				break;
			case "VASO":
				recargarIngredienteEspecifico("Vaso");
				break;
			case "KITREPARACION":
				// Solo quitar alarma, no recargar nada
				break;
			default:
				return false;
		}

		quitarAlarma(idAlarma);
		respaldarMaq();
		actualizarRecetasGraf();
		actualizarInsumosGraf();
		actualizarAlarmasGraf();

		if (alarmas.getValues().isEmpty()) {
			frame.setEnabled(true);
			frame.interfazHabilitada();
		}

		return true;
	}

	// ==================== Spec 11 - Task 3: Refactorizar abastecer() ====================

	/**
	 * Metodo legacy de abastecimiento por idAlarma.
	 * Refactorizado para delegar a aplicarAbastecimiento().
	 */
	@Override
	public void abastecer(int codMaquina, int idAlarma, Current current) {
		System.out.println("Entra a abastecer: maq=" + codMaquina + " alarma=" + idAlarma);

		if (codMaquina != this.codMaquina) return;

		String recurso = recursoDesdeAlarma(idAlarma);
		if (recurso == null) return;

		int cantidad = cantidadDesdeRecurso(recurso);
		boolean aplicado = aplicarAbastecimiento(recurso, cantidad, idAlarma + "");

		if (aplicado) {
			alarmaServicePrx.recibirNotificacionAbastesimiento(
					codMaquina, recurso, cantidad);
		}
	}

	/**
	 * Mapea idAlarma (como usado en coffeeMach) al nombre del RecursoAbastecimiento.
	 */
	private String recursoDesdeAlarma(int idAlarma) {
		if (idAlarma == 1) return "KITREPARACION";
		if (idAlarma == 2 || idAlarma == 3) return "MONEDA100";
		if (idAlarma == 4 || idAlarma == 5) return "MONEDA200";
		if (idAlarma == 6 || idAlarma == 7) return "MONEDA500";
		if (idAlarma == 8 || idAlarma == 12) return "AGUA";
		if (idAlarma == 9 || idAlarma == 13) return "CAFE";
		if (idAlarma == 10 || idAlarma == 14) return "AZUCAR";
		if (idAlarma == 11 || idAlarma == 15) return "VASO";
		return null;
	}

	private int cantidadDesdeRecurso(String recurso) {
		if (recurso.startsWith("MONEDA")) return 20;
		if ("VASO".equals(recurso)) return 100;
		if ("KITREPARACION".equals(recurso)) return 1;
		return 500; // AGUA, CAFE, AZUCAR
	}

	// ==================== Metodos de soporte ====================

	public void quitarAlarma(String tipo) {
		alarmas.removeElement(tipo);
	}

	public void recargarIngredienteEspecifico(String ingrediente) {
		Ingrediente ing = ingredientes.findByKey(ingrediente);
		ing.setCantidad(ing.getMaximo());
		ingredientes.addElement(ingrediente, ing);
	}

	// ==================== Eventos GUI (Spec 02) ====================

	public void eventos() {

		frame.getBtnIngresar100().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int saldo = Integer.parseInt(frame.getTextAreaSaldo().getText());
				frame.getTextAreaSaldo().setText((saldo + 100) + "");
				suma += 100;
				DepositoMonedas moneda = monedas.findByKey("100");
				moneda.setCantidad(moneda.getCantidad() + 1);
				monedas.addElement("100", moneda);
				actualizarInsumosGraf();
			}
		});

		frame.getBtnIngresar200().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int saldo = Integer.parseInt(frame.getTextAreaSaldo().getText());
				frame.getTextAreaSaldo().setText((saldo + 200) + "");
				suma += 200;
				DepositoMonedas moneda = monedas.findByKey("200");
				moneda.setCantidad(moneda.getCantidad() + 1);
				monedas.addElement("200", moneda);
				actualizarInsumosGraf();
			}
		});

		frame.getBtnIngresar500().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int saldo = Integer.parseInt(frame.getTextAreaSaldo().getText());
				frame.getTextAreaSaldo().setText((saldo + 500) + "");
				suma += 500;
				DepositoMonedas moneda = monedas.findByKey("500");
				moneda.setCantidad(moneda.getCantidad() + 1);
				monedas.addElement("500", moneda);
				actualizarInsumosGraf();
			}
		});

		frame.getBtnCancelar().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.getTextAreaSaldo().setText("0");
				if (suma > 0) {
					frame.getTextAreaDevuelta().setText(
							frame.getTextAreaDevuelta().getText()
									+ "Se devolvio: " + suma + "\n");
					devolverMonedas();
				}
			}
		});

		frame.getBtnVerificar().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int precio = 0;
				List<Receta> rec = recetas.getValues();
				for (int i = 0; i < rec.size(); i++) {
					if (frame.getComboBoxProducto().getSelectedItem()
							.equals(rec.get(i).getDescripcion())) {
						precio = rec.get(i).getValor();
					}
				}

				frame.getTextAreaInfo().setText(
						frame.getTextAreaInfo().getText()
								+ "El producto cuesta: " + precio + "\n");
				frame.getTextAreaInfo().repaint();
			}
		});

		frame.getBtnOrdenar().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int precio = 0;
				Receta temp = null;
				List<Receta> rec = recetas.getValues();
				for (int i = 0; i < rec.size(); i++) {
					temp = rec.get(i);

					if (frame.getComboBoxProducto().getSelectedItem()
							.equals(temp.getDescripcion())) {
						precio = rec.get(i).getValor();

						if (Integer.valueOf(frame.getTextAreaSaldo().getText()) >= precio) {

							frame.getTextAreaInfo().setText(
									frame.getTextAreaInfo().getText()
											+ "Se ordeno: "
											+ frame.getComboBoxProducto().getSelectedItem()
											+ "\n");

							frame.getTextAreaSaldo().setText(
									Integer.valueOf(frame.getTextAreaSaldo().getText())
											- precio + "");

							suma -= precio;

							disminuirInsumos(temp);
							devolverMonedas();
							verificarProductos();

							// Spec 11 - Task 4: Clave unica con timestamp para evitar sobreescritura
							String idV = rec.get(i).getId() + "-" + System.currentTimeMillis();
							ventas.addElement(idV, new Venta(
									frame.getComboBoxProducto().getSelectedItem().toString(),
									rec.get(i).getId(),
									precio, new Date()));

							respaldarMaq();
							frame.getTextAreaSaldo().setText("0");

						} else {
							frame.getTextAreaInfo().setText(
									frame.getTextAreaInfo().getText()
											+ "Saldo insuficiente \n");
						}
					}
				}
			}
		});

		frame.getBtnMantenimiento().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Alarma temp = new Alarma("1", "Se requiere mantenimiento", new Date());

				frame.getTextAreaAlarmas().setText(
						frame.getTextAreaAlarmas().getText()
								+ "Se genero una alarma de: Mantenimiento" + "\n");

				alarmaServicePrx.recibirNotificacionMalFuncionamiento(
						codMaquina, "Se requiere mantenimiento");

				alarmas.addElement("1", temp);
				frame.interfazDeshabilitada();
			}
		});

		frame.getBtnEnviarReporte().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<Venta> vents = ventas.getValues();
				String[] arregloVentas = new String[vents.size()];
				for (int i = 0; i < arregloVentas.length; i++) {
					arregloVentas[i] = vents.get(i).getId() + "#"
							+ vents.get(i).getValor();
					System.out.println(arregloVentas[i]);
				}
				ventasService.registrarVenta(codMaquina, arregloVentas);
			}
		});

		frame.getBtnActualizar().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cargarRecetaMaquinas();
			}
		});
	}

	// ==================== Carga de recetas (Spec 02 Task 6) ====================

	public void cargarRecetaMaquinas() {
		recetas.setElements(new HashMap<String, Receta>());

		String[] recetasServer = recetaServicePrx.consultarProductos();

		for (int i = 0; i < recetasServer.length; i++) {
			String[] splitInicial = recetasServer[i].split("#");
			String[] receta = splitInicial[0].split("-");

			HashMap<Ingrediente, Double> listaIngredientes = new HashMap<Ingrediente, Double>();

			for (int i2 = 1; i2 < splitInicial.length; i2++) {
				String[] splitdeIng = splitInicial[i2].split("-");
				Ingrediente ingred = ingredientes.findByKey(splitdeIng[1]);
				if (ingred == null) {
					ingred = new Ingrediente(splitdeIng[1], splitdeIng[2], 500, 50, 1000, 1000);
				}
				listaIngredientes.put(ingred, Double.parseDouble(splitdeIng[4]));
			}

			Receta r = new Receta(receta[1], receta[0],
					Integer.parseInt(receta[2]), listaIngredientes);
			recetas.addElement(receta[0], r);
		}

		recetas.saveData();
		actualizarInsumosGraf();
		actualizarRecetasGraf();
		actualizarRecetasCombo();
	}

	// ==================== Estado y respaldo ====================

	public void respaldarMaq() {
		alarmas.saveData();
		ingredientes.saveData();
		monedas.saveData();
		recetas.saveData();
		ventas.saveData();
	}

	// ==================== Verificacion de niveles (Spec 03) ====================

	public void verificarProductos() {
		Iterator<Ingrediente> itIng = ingredientes.getValues().iterator();

		while (itIng.hasNext()) {
			Ingrediente ing = itIng.next();

			// Nivel minimo (alarma low)
			if (ing.getCantidad() <= ing.getMinimo()
					&& ing.getCantidad() > ing.getCritico()) {

				Alarma alIng = new Alarma(ing.getCodAlarma(), ing.getNombre(), new Date());

				if (alarmas.findByKey(ing.getCodAlarma()) == null) {
					alarmas.addElement(ing.getCodAlarma(), alIng);
					alarmaServicePrx.recibirNotificacionEscasezIngredientes(
							ing.getNombre(), codMaquina);
					frame.getTextAreaAlarmas().setText(
							frame.getTextAreaAlarmas().getText()
									+ "Se genero una alarma de Ingrediente: "
									+ alIng.getMensaje() + "\n");
				}
			}

			// Nivel critico
			if (ing.getCantidad() <= ing.getCritico()) {
				int codAlarma = Integer.parseInt(ing.getCodAlarma()) + 4;
				Alarma alIng = new Alarma(codAlarma + "", ing.getNombre(), new Date());
				alarmas.addElement(codAlarma + "", alIng);
				alarmaServicePrx.recibirNotificacionEscasezIngredientes(
						ing.getNombre(), codMaquina);
				frame.getTextAreaAlarmas().setText(
						frame.getTextAreaAlarmas().getText()
								+ "Se genero una alarma de: Critico de "
								+ alIng.getMensaje() + "\n");
				frame.interfazDeshabilitada();
			}
		}
	}

	public void disminuirInsumos(Receta r) {
		Iterator<Entry<Ingrediente, Double>> receta = r.getListaIngredientes()
				.entrySet().iterator();
		while (receta.hasNext()) {
			Map.Entry<Ingrediente, Double> ingRec = (Map.Entry<Ingrediente, Double>) receta.next();
			Ingrediente ingrediente = ingredientes.findByKey(ingRec.getKey().getNombre());
			ingrediente.setCantidad(ingrediente.getCantidad() - ingRec.getValue());
			ingredientes.addElement(ingrediente.getNombre(), ingrediente);
		}
		actualizarInsumosGraf();
	}

	// ==================== Arranque (Spec 11 - Task 4: mejorado) ====================

	public void arrancarMaquina() {
		codMaquina = quemarCodMaquina();

		if (codMaquina == -1) {
			frame.getTextAreaAlarmas().setText(
					"No existe codMaquina.cafe. Cree con: echo 1 > codMaquina.cafe");
			frame.interfazDeshabilitada();
			return;
		}

		actualizarRecetasCombo();
		actualizarRecetasGraf();
		actualizarInsumosGraf();
		actualizarAlarmasGraf();
	}

	// ==================== Actualizacion GUI ====================

	public void actualizarAlarmasGraf() {
		frame.getTextAreaAlarmas().setText("");
	}

	public void actualizarInsumosGraf() {
		frame.getTextAreaInsumos().setText("");

		Iterator<Ingrediente> it = ingredientes.getValues().iterator();
		while (it.hasNext()) {
			Ingrediente ing = it.next();
			frame.getTextAreaInsumos().setText(
					frame.getTextAreaInsumos().getText()
							+ ing.getNombre() + ": "
							+ ing.getCantidad() + "\n");
		}
		DepositoMonedas dep = monedas.findByKey("100");
		frame.getTextAreaInsumos().setText(
				frame.getTextAreaInsumos().getText() + "Deposito "
						+ dep.getTipo() + ": " + dep.getCantidad() + "\n");
		dep = monedas.findByKey("200");
		frame.getTextAreaInsumos().setText(
				frame.getTextAreaInsumos().getText() + "Deposito "
						+ dep.getTipo() + ": " + dep.getCantidad() + "\n");
		dep = monedas.findByKey("500");
		frame.getTextAreaInsumos().setText(
				frame.getTextAreaInsumos().getText() + "Deposito "
						+ dep.getTipo() + ": " + dep.getCantidad() + "\n");
	}

	public void actualizarRecetasGraf() {
		frame.getTextAreaRecetas().setText("");
		Iterator<Receta> it2 = recetas.getValues().iterator();
		while (it2.hasNext()) {
			Receta temp = it2.next();
			frame.getTextAreaRecetas().setText(
					frame.getTextAreaRecetas().getText()
							+ temp.getDescripcion() + "\n");
		}
	}

	public void actualizarRecetasCombo() {
		frame.getComboBoxProducto().removeAllItems();
		List<Receta> rec = recetas.getValues();
		for (int i = 0; i < rec.size(); i++) {
			frame.getComboBoxProducto().addItem(rec.get(i).getDescripcion());
		}
	}

	// ==================== Lectura de codMaquina (Spec 11 - Task 4: mejorado) ====================

	private int quemarCodMaquina() {
		try {
			File file = new File("codMaquina.cafe");
			if (!file.exists()) {
				return -1;
			}

			BufferedReader buffer = new BufferedReader(
					new InputStreamReader(new FileInputStream(file)));
			int retorno = Integer.parseInt(buffer.readLine().trim());
			buffer.close();
			return retorno;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	// ==================== Devolucion de monedas (Spec 02) ====================

	public void devolverMonedas() {
		int monedas100 = 0;
		int monedas200 = 0;
		int monedas500 = 0;

		if (suma / 500 > 0) {
			monedas500 += (int) suma / 500;
			DepositoMonedas moneda = monedas.findByKey("500");
			moneda.setCantidad(moneda.getCantidad() - monedas500);
			monedas.addElement("500", moneda);
			suma -= 500 * ((int) suma / 500);
		}

		if (suma / 200 > 0) {
			monedas200 += (int) suma / 200;
			DepositoMonedas moneda = monedas.findByKey("200");
			moneda.setCantidad(moneda.getCantidad() - monedas200);
			monedas.addElement("200", moneda);
			suma -= 200 * ((int) suma / 200);
		}

		if (suma / 100 > 0) {
			monedas100 += (int) suma / 100;
			DepositoMonedas moneda = monedas.findByKey("100");
			moneda.setCantidad(moneda.getCantidad() - monedas100);
			monedas.addElement("100", moneda);
			suma -= 100 * ((int) suma / 100);
		}

		if (suma != 0) {
			System.out.println("Ocurrio un error en dar devueltas: " + suma);
		}

		frame.getTextAreaDevuelta().setText(
				frame.getTextAreaDevuelta().getText() + "Se devolvieron: "
						+ monedas500 + " monedas de 500, " + monedas200
						+ " monedas de 200 y " + monedas100
						+ " monedas de 100 \n");

		actualizarInsumosGraf();
		verificarMonedas();
	}

	// ==================== Verificacion de monedas (Spec 03) - Fix: || en vez de | ====================

	public void verificarMonedas() {
		// Monedas de 100
		DepositoMonedas moneda = monedas.findByKey("100");
		if (moneda.getCantidad() <= moneda.getMinimo()
				&& moneda.getCantidad() > moneda.getCritico()) {
			Alarma alMon = new Alarma("2", "Faltan monedas de 100", new Date());
			if (alarmas.findByKey("2") == null) {
				alarmas.addElement("2", alMon);
				alarmaServicePrx.recibirNotificacionInsuficienciaMoneda(Moneda.CIEN, codMaquina);
				frame.getTextAreaAlarmas().setText(
						frame.getTextAreaAlarmas().getText()
								+ "Se genero una alarma de: Monedas de 100\n");
			}
		}
		if (moneda.getCantidad() <= moneda.getCritico()) {
			Alarma alMon = new Alarma("3", "ESTADO CRITICO: Faltan monedas de 100", new Date());
			alarmas.addElement("3", alMon);
			alarmaServicePrx.recibirNotificacionInsuficienciaMoneda(Moneda.CIEN, codMaquina);
			frame.getTextAreaAlarmas().setText(
					frame.getTextAreaAlarmas().getText()
							+ "Se genero una alarma de: Critica Monedas de 100\n");
			frame.interfazDeshabilitada();
		}

		// Monedas de 200
		moneda = monedas.findByKey("200");
		if (moneda.getCantidad() <= moneda.getMinimo()
				&& moneda.getCantidad() > moneda.getCritico()) {
			Alarma alMon = new Alarma("4", "Faltan monedas de 200", new Date());
			if (alarmas.findByKey("4") == null) {
				alarmas.addElement("4", alMon);
				alarmaServicePrx.recibirNotificacionInsuficienciaMoneda(Moneda.DOCIENTOS, codMaquina);
				frame.getTextAreaAlarmas().setText(
						frame.getTextAreaAlarmas().getText()
								+ "Se genero una alarma de: Monedas de 200\n");
			}
		}
		if (moneda.getCantidad() <= moneda.getCritico()) {
			Alarma alMon = new Alarma("5", "ESTADO CRITICO: Faltan monedas de 200", new Date());
			alarmas.addElement("5", alMon);
			alarmaServicePrx.recibirNotificacionInsuficienciaMoneda(Moneda.DOCIENTOS, codMaquina);
			frame.getTextAreaAlarmas().setText(
					frame.getTextAreaAlarmas().getText()
							+ "Se genero una alarma de: Critica de Monedas de 200\n");
			frame.interfazDeshabilitada();
		}

		// Monedas de 500
		moneda = monedas.findByKey("500");
		if (moneda.getCantidad() <= moneda.getMinimo()
				&& moneda.getCantidad() > moneda.getCritico()) {
			Alarma alMon = new Alarma("6", "Faltan monedas de 500", new Date());
			if (alarmas.findByKey("6") == null) {
				alarmas.addElement("6", alMon);
				alarmaServicePrx.recibirNotificacionInsuficienciaMoneda(Moneda.QUINIENTOS, codMaquina);
				frame.getTextAreaAlarmas().setText(
						frame.getTextAreaAlarmas().getText()
								+ "Se genero una alarma de: Monedas de 500\n");
			}
		}
		if (moneda.getCantidad() <= moneda.getCritico()) {
			Alarma alMon = new Alarma("7", "ESTADO CRITICO: Faltan monedas de 500", new Date());
			alarmas.addElement("7", alMon);
			alarmaServicePrx.recibirNotificacionInsuficienciaMoneda(Moneda.QUINIENTOS, codMaquina);
			frame.getTextAreaAlarmas().setText(
					frame.getTextAreaAlarmas().getText()
							+ "Se genero una alarma de: Critica Monedas de 500\n");
			frame.interfazDeshabilitada();
		}
	}
}
