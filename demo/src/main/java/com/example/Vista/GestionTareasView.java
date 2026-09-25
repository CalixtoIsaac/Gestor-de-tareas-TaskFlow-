package com.example.Vista;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.event.ListSelectionListener;

import com.example.Modelo.ArbolTareasABB;
import com.example.Modelo.Empleado;
import com.example.Modelo.Tarea;


public class GestionTareasView extends JFrame {

    // Paleta de colores
    private static final Color COLOR_SIDEBAR_BG = new Color(30, 41, 59);
    private static final Color COLOR_FONDO_APP = new Color(241, 245, 249);
    private static final Color COLOR_TARJETA = new Color(255, 255, 255);
    private static final Color COLOR_PRIMARIO = new Color(37, 99, 235);
    private static final Color COLOR_TEXTO_DARK = new Color(30, 41, 59);
    private static final Color COLOR_BORDE = new Color(226, 232, 240);
    private static final Color COLOR_ROJO = new Color(220, 38, 38);
    private static final Color COLOR_VERDE = new Color(22, 163, 74);
    private static final Color COLOR_NEUTRO = new Color(71, 85, 105);
    private static final Color COLOR_ROJO_OSCURO_TEMA = new Color(248, 113, 113);

    // Paleta de identidad (la misma del Menú Principal: tarjetas y gráfica por departamento)
    private static final Color[] PALETA_MENU = {new Color(30, 64, 175), new Color(37, 99, 235),
            new Color(79, 70, 229), new Color(91, 33, 182)};
    // Variantes más claras de la misma paleta para que se lean sobre el fondo del modo oscuro
    private static final Color[] PALETA_MENU_OSCURO = {new Color(59, 130, 246), new Color(96, 165, 250),
            new Color(129, 140, 248), new Color(167, 139, 250)};
    private static final Color COLOR_AMARILLO_DESTACADO = new Color(250, 204, 21);

    // Columnas compartidas por todas las tablas de tareas
    public static final String COL_RESPONSABLE = "Responsable Directo";
    public static final String COL_FECHA_ENTREGA = "Fecha de Entrega y Días Restantes";
    public static final String OPCION_SIN_ASIGNAR = "Sin Asignar (se asignará en Distribución)";
    private static final String[] COLUMNAS_TAREAS = {"ID", "Título", "Departamento", COL_RESPONSABLE,
            "Urgencia", "Tiempo (hrs)", COL_FECHA_ENTREGA};

    // Contenedores y Navegación
    private CardLayout cardLayout;
    private JPanel panelContenidoCards;
    private JPanel panelSidebar;
    private boolean sidebarExpandido = true;
    private JButton botonNavActivo;
    private boolean temaOscuro = false;
    private JButton btnCambiarTema;

    // Control de Ventana y Pantalla Completa
    private boolean esVentanaCompleta = false;
    private Dimension dimensionesFlotante;
    private Point posicionFlotante;
    private JButton btnModoVentana;

    // Botones del Menú Lateral
    private JButton btnMenuToggle;
    private JButton btnNavDashboard, btnNavRegistro, btnNavPila, btnNavCola, btnNavLista;
    private JButton btnNavPrioridad, btnNavEmpleados, btnNavRecursivo, btnNavAlgoritmos, btnNavGrafo, btnNavTodas;

    // Métricas Dashboard
    private JLabel lblPilaMetricas, lblColaMetricas, lblListaMetricas;
    private JPanel panelHorasDepartamentos;
    private DashboardChartPanel panelGraficaDepartamentos;

    // Componentes del Formulario de Registro
    private JTextField txtTitulo, txtTiempoEstimado;
    private JComboBox<String> cbDepartamento, cbEstructura;
    private JComboBox<Integer> cbUrgencia;
    private JComboBox<Object> cbResponsable; // OPCION_SIN_ASIGNAR o un Empleado del mismo departamento
    private SelectorFechaPanel selectorFecha;
    private JButton btnAgregar;

    // Tablas de Tareas y Empleados
    private DefaultTableModel modeloPila, modeloCola, modeloLista, modeloPrioridad, modeloEmpleados, modeloTodas;
    private JTable tablaPila, tablaCola, tablaLista, tablaPrioridad, tablaEmpleados, tablaTodas;

    // Botones Pila, Cola, Lista
    private JButton btnPopPila, btnPeekPila, btnDequeueCola, btnFrontCola, btnEliminarLista, btnBuscarDepto, btnVerOrdenadas;

    // Componentes Cola de Prioridad
    private JButton btnExtraerPrioridad, btnVerPrioridad;

    // Componentes Árbol Binario (Empleados)
    private JTextField txtEmpleadoId, txtEmpleadoNombre, txtBuscarEmpleadoId;
    private JComboBox<String> cbEmpleadoDepto, cbFiltroDeptoEmp, cbFiltroDeptoLista;
    private JButton btnAgregarEmpleado, btnBuscarEmpleadoId, btnListarEmpleadoDepto, btnMostrarTodosEmpleados;
    public static final String COL_PENDIENTES = "Tareas Pendientes";
    private DefaultTableModel modeloPendientesEmpleado;
    private JTable tablaPendientesEmpleado;
    private JLabel lblTituloPendientes;
    private JSplitPane splitEmpleados;
    private JButton btnTogglePendientes;
    private boolean detallePendientesVisible = true;

    /** Resumen que se muestra como badges en la columna "Tareas Pendientes" de la tabla de empleados. */
    public record ResumenPendientes(int total, int vencidas, int criticas) {
        @Override public String toString() {
            return total == 0 ? "Sin pendientes" : total + (total == 1 ? " pendiente" : " pendientes");
        }
    }

    // Componentes Recursividad & Divide y Vencerás
    private JButton btnCalcularTiempoRecursivo, btnDistribuirDivideVenceras;
    // Resultados visuales (tarjetas KPI, alerta y tabla por empleado)
    private static final String TEMA_PROPIO = "tema.propio"; // componentes que se pintan solos según el tema
    private KpiCard kpiTotalTareas, kpiTiempoTotal;
    private AlertaPanel alertaSinPersonal;
    private JLabel lblResumenDistribucion;
    private JPanel panelGruposDistribucion;

    /** Una tarea dentro de la distribución y si ya tenía Responsable Directo antes de distribuir. */
    public record FilaAsignacion(Tarea tarea, boolean responsablePrevio) {}
    /** Tareas asignadas a un empleado en la distribución. */
    public record GrupoDistribucion(Empleado empleado, List<FilaAsignacion> filas) {}

    // Componentes del módulo Búsquedas (buscador unificado, recorridos y árbol ABB)
    private JButton btnQuickSortUrgencia;
    private JTextField txtBuscarFolio;
    private JButton btnBuscarFolio, btnBalancearArbol;
    private JToggleButton btnInorden, btnPreorden, btnPostorden;
    private DetalleTareaCard detalleBusqueda;
    private GraficaComparaciones graficaComparaciones;
    private JTextArea txtExplicacionRecorrido;
    private SecuenciaFolios secuenciaRecorrido;
    private ArbolVisual arbolVisual;
    private JLabel lblInfoArbol;

    /** Una barra de la comparativa: método, complejidad, comparaciones realizadas y si encontró la tarea. */
    public record MetodoBusqueda(String nombre, String complejidad, int comparaciones, boolean encontrado) {}

    // Componentes Grafo de Dependencias
    private JComboBox<Object> cbGrafoTareaPrevia, cbGrafoTareaSiguiente; // editables: se elige o se escribe el folio
    private JButton btnAgregarDependencia, btnCalcularOrdenTopologico, btnLimpiarGrafo;
    private OrdenEjecucionPanel panelOrdenEjecucion;
    private GrafoVisual grafoVisual;
    private JLabel lblInfoGrafo;

    /** Opción de los selectores del grafo: una tarea activa mostrada como "#folio · título". */
    public record OpcionTarea(int id, String titulo) {
        @Override public String toString() { return "#" + id + " · " + titulo; }
    }

    // Consola de eventos
    private JTextArea areaConsolaGUI;

    public GestionTareasView() {
        setTitle("Sistema Empresarial Avanzado de Gestión de Tareas - Dashboard");
        calcularTamanoInicial();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panelRaiz = new JPanel(new BorderLayout());
        panelRaiz.setBackground(COLOR_FONDO_APP);
        setContentPane(panelRaiz);

        initHeader(panelRaiz);
        initSidebar(panelRaiz);
        initMainCards(panelRaiz);
        configurarListenersVentana();
        configurarAtajosTeclado();
        aplicarTemaGeneral();
    }

    private void initHeader(JPanel panelRaiz) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_SIDEBAR_BG);
        header.setPreferredSize(new Dimension(0, 52));
        header.setBorder(new EmptyBorder(5, 10, 5, 20));

        btnMenuToggle = new JButton(" ||| ");
        btnMenuToggle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnMenuToggle.setForeground(Color.WHITE);
        btnMenuToggle.setBackground(COLOR_SIDEBAR_BG);
        btnMenuToggle.setFocusPainted(false);
        btnMenuToggle.setBorderPainted(false);
        btnMenuToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMenuToggle.addActionListener(e -> alternarSidebar());

        JLabel lblTituloApp = new JLabel("  SISTEMA AVANZADO DE GESTIÓN DE TAREAS ORGANIZACIONAL");
        lblTituloApp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTituloApp.setForeground(Color.WHITE);

        // Panel de acciones en la cabecera (Ventana Completa / Flotante)
        JPanel panelHeaderAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panelHeaderAcciones.setOpaque(false);

        btnModoVentana = new JButton("Pantalla Completa");
        btnModoVentana.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnModoVentana.setForeground(Color.WHITE);
        btnModoVentana.setBackground(new Color(51, 65, 85));
        btnModoVentana.setFocusPainted(false);
        btnModoVentana.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnModoVentana.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 116, 139), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        btnModoVentana.setToolTipText("Ajustar automáticamente a pantalla completa (F11)");
        btnModoVentana.addActionListener(e -> alternarModoVentana());
        btnModoVentana.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnModoVentana.setBackground(temaOscuro ? new Color(59, 130, 246) : new Color(71, 85, 105));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                actualizarColorBotonVentana();
            }
        });

        panelHeaderAcciones.add(btnModoVentana);

        header.add(btnMenuToggle, BorderLayout.WEST);
        header.add(lblTituloApp, BorderLayout.CENTER);
        header.add(panelHeaderAcciones, BorderLayout.EAST);
        panelRaiz.add(header, BorderLayout.NORTH);
    }

    private void initSidebar(JPanel panelRaiz) {
        panelSidebar = new JPanel();
        panelSidebar.setLayout(new BoxLayout(panelSidebar, BoxLayout.Y_AXIS));
        panelSidebar.setBackground(COLOR_SIDEBAR_BG);
        panelSidebar.setPreferredSize(new Dimension(220, getHeight()));
        panelSidebar.setBorder(new EmptyBorder(10, 5, 10, 5));

        btnNavDashboard = crearBotonNav("Menú Principal", "DASHBOARD");
        btnNavRegistro = crearBotonNav("Registrar Tarea", "REGISTRO");
        btnNavPila = crearBotonNav("Pilas (Urgentes)", "PILA");
        btnNavCola = crearBotonNav("Colas (Programadas)", "COLA");
        btnNavLista = crearBotonNav("Listas (Generales)", "LISTA");
        btnNavPrioridad = crearBotonNav("Cola Prioridad", "PRIORIDAD");
        btnNavEmpleados = crearBotonNav("Empleados", "EMPLEADOS");
        btnNavRecursivo = crearBotonNav("Cálculos y Distribución", "RECURSIVO");
        btnNavAlgoritmos = crearBotonNav("Búsquedas", "ALGORITMOS");
        btnNavGrafo = crearBotonNav("Grafo Dependencias", "GRAFO");
        btnNavTodas = crearBotonNav("Ver Todas / Consola", "TODAS");

        panelSidebar.add(btnNavDashboard);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavRegistro);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavPila);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavCola);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavLista);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavPrioridad);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavEmpleados);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavRecursivo);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavAlgoritmos);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavGrafo);
        panelSidebar.add(Box.createRigidArea(new Dimension(0, 3)));
        panelSidebar.add(btnNavTodas);

        establecerBotonNavActivo(btnNavDashboard);
        aplicarTemaGeneral();
        panelRaiz.add(panelSidebar, BorderLayout.WEST);
    }

    private JButton crearBotonNav(String texto, String cardName) {
        JButton btn = new JButton(texto);
        btn.setMaximumSize(new Dimension(210, 38));
        btn.setPreferredSize(new Dimension(210, 38));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(COLOR_SIDEBAR_BG);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        guardarColoresOriginales(btn);

        btn.addActionListener(e -> {
            cardLayout.show(panelContenidoCards, cardName);
            establecerBotonNavActivo(btn);
        });
        return btn;
    }

    private void establecerBotonNavActivo(JButton botonSeleccionado) {
        if (botonNavActivo != null && botonNavActivo != botonSeleccionado) {
            botonNavActivo.setBackground(temaOscuro ? new Color(15, 23, 42) : COLOR_SIDEBAR_BG);
            botonNavActivo.setBorderPainted(false);
        }

        botonNavActivo = botonSeleccionado;
        botonNavActivo.setBackground(new Color(96, 165, 250));
        botonNavActivo.setForeground(Color.WHITE);
        botonNavActivo.setBorderPainted(true);
        botonNavActivo.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(191, 219, 254)));
    }

    private void alternarSidebar() {
        sidebarExpandido = !sidebarExpandido;
        int nuevoAncho = sidebarExpandido ? 220 : 60;
        panelSidebar.setPreferredSize(new Dimension(nuevoAncho, getHeight()));

        btnNavDashboard.setText(sidebarExpandido ? "Menú Principal" : "MENU");
        btnNavRegistro.setText(sidebarExpandido ? "Registrar Tarea" : "REG");
        btnNavPila.setText(sidebarExpandido ? "Pilas (Urgentes)" : "PILA");
        btnNavCola.setText(sidebarExpandido ? "Colas (Programadas)" : "COLA");
        btnNavLista.setText(sidebarExpandido ? "Listas (Generales)" : "LIST");
        btnNavPrioridad.setText(sidebarExpandido ? "Cola Prioridad" : "PRIO");
        btnNavEmpleados.setText(sidebarExpandido ? "Empleados" : "EMP");
        btnNavRecursivo.setText(sidebarExpandido ? "Cálculos y Distribución" : "CALC");
        btnNavAlgoritmos.setText(sidebarExpandido ? "Búsquedas" : "BUS");
        btnNavGrafo.setText(sidebarExpandido ? "Grafo Dependencias" : "GRAF");
        btnNavTodas.setText(sidebarExpandido ? "Ver Todas / Consola" : "ALL");

        panelSidebar.revalidate();
        panelSidebar.repaint();
    }

    private void initMainCards(JPanel panelRaiz) {
        cardLayout = new CardLayout();
        panelContenidoCards = new JPanel(cardLayout);
        panelContenidoCards.setOpaque(false);
        panelContenidoCards.setBorder(new EmptyBorder(15, 15, 15, 15));

        panelContenidoCards.add(crearCardDashboard(), "DASHBOARD");
        panelContenidoCards.add(crearCardRegistro(), "REGISTRO");
        panelContenidoCards.add(crearCardPila(), "PILA");
        panelContenidoCards.add(crearCardCola(), "COLA");
        panelContenidoCards.add(crearCardLista(), "LISTA");
        panelContenidoCards.add(crearCardPrioridad(), "PRIORIDAD");
        panelContenidoCards.add(crearCardEmpleados(), "EMPLEADOS");
        panelContenidoCards.add(crearCardRecursivo(), "RECURSIVO");
        panelContenidoCards.add(crearCardAlgoritmos(), "ALGORITMOS");
        panelContenidoCards.add(crearCardGrafo(), "GRAFO");
        panelContenidoCards.add(crearCardTodas(), "TODAS");

        panelRaiz.add(panelContenidoCards, BorderLayout.CENTER);
    }

    private JPanel crearCardDashboard() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JLabel lblTitulo = new JLabel("Resumen Administrativo del Sistema");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblTitulo, BorderLayout.NORTH);

        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));

        JPanel panelGridCards = new JPanel(new GridLayout(1, 3, 12, 12));
        panelGridCards.setOpaque(false);
        panelGridCards.setPreferredSize(new Dimension(0, 102));
        panelGridCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 102));

        lblPilaMetricas = new JLabel("Pendientes: 0  |  Resueltas: 0", SwingConstants.CENTER);
        lblColaMetricas = new JLabel("Pendientes: 0  |  Resueltas: 0", SwingConstants.CENTER);
        lblListaMetricas = new JLabel("Pendientes: 0  |  Resueltas: 0", SwingConstants.CENTER);

        panelGridCards.add(crearTarjetaMetrica("PILA · Tareas urgentes", lblPilaMetricas, new Color(30, 64, 175)));
        panelGridCards.add(crearTarjetaMetrica("COLA · Tareas programadas", lblColaMetricas, new Color(37, 99, 235)));
        panelGridCards.add(crearTarjetaMetrica("LISTA · Tareas generales", lblListaMetricas, new Color(91, 33, 182)));

        contenido.add(panelGridCards);
        contenido.add(Box.createVerticalStrut(10));

        panelHorasDepartamentos = new JPanel(new GridLayout(1, 5, 8, 0));
        panelHorasDepartamentos.setOpaque(false);
        panelHorasDepartamentos.setPreferredSize(new Dimension(0, 68));
        panelHorasDepartamentos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        TitledBorder bordeHoras = BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(4, 0, 0, 0),
            "Horas estimadas por departamento",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13), COLOR_TEXTO_DARK);
        panelHorasDepartamentos.setBorder(bordeHoras);
        contenido.add(panelHorasDepartamentos);
        contenido.add(Box.createVerticalStrut(10));

        panelGraficaDepartamentos = new DashboardChartPanel();
        panelGraficaDepartamentos.setPreferredSize(new Dimension(0, 260));
        panelGraficaDepartamentos.setMinimumSize(new Dimension(0, 220));
        panelGraficaDepartamentos.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDE), new EmptyBorder(8, 8, 4, 8)));
        contenido.add(panelGraficaDepartamentos);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTarjetaMetrica(String titulo, JLabel lblValor, Color colorAcento) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(COLOR_TARJETA);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, colorAcento),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTit.setForeground(COLOR_TEXTO_DARK);

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblValor.setForeground(COLOR_TEXTO_DARK);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearMiniTarjetaDepartamento(String departamento, int horas, Color acento) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(COLOR_TARJETA);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, acento),
                new EmptyBorder(5, 7, 5, 7)));
        JLabel nombre = new JLabel(departamento, SwingConstants.CENTER);
        nombre.setFont(new Font("Segoe UI", Font.BOLD, 10));
        nombre.setForeground(COLOR_TEXTO_DARK);
        JLabel valor = new JLabel(horas + " h", SwingConstants.CENTER);
        valor.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valor.setForeground(acento);
        card.add(nombre, BorderLayout.NORTH);
        card.add(valor, BorderLayout.CENTER);
        return card;
    }

    private JComponent crearCardRegistro() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_TARJETA);
        panel.setBorder(crearBordeSeccion(" Registrar Nueva Tarea ", 16));

        Font fuenteNegrita = new Font("Segoe UI", Font.BOLD, 12);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.22;
        JLabel lblTitulo = new JLabel("Título de Tarea:", SwingConstants.RIGHT);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitulo.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblTitulo, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        txtTitulo = new JTextField(); estilarCampoTexto(txtTitulo); panel.add(txtTitulo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.22;
        JLabel lblDepto = new JLabel("Departamento:", SwingConstants.RIGHT);
        lblDepto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDepto.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblDepto, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        cbDepartamento = new JComboBox<>(new String[]{"Sistemas", "Ventas", "Recursos Humanos", "Finanzas", "Logística"});
        cbDepartamento.setBackground(Color.WHITE); panel.add(cbDepartamento, gbc);

        // Responsable Directo (opcional): el Controlador lo llena solo con empleados del departamento elegido
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.22;
        JLabel lblResponsable = new JLabel("Responsable Directo :", SwingConstants.RIGHT);
        lblResponsable.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblResponsable.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblResponsable, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        cbResponsable = new JComboBox<>(new Object[]{OPCION_SIN_ASIGNAR});
        cbResponsable.setBackground(Color.WHITE);
        cbResponsable.setToolTipText("Solo se listan empleados del departamento seleccionado");
        panel.add(cbResponsable, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.78; gbc.insets = new Insets(0, 12, 10, 12);
        JLabel lblAyudaResponsable = new JLabel("Solo aparecen empleados del departamento seleccionado. "
                + "Si no eliges a nadie, la tarea quedará \"Sin Asignar\" para el módulo de Distribución.");
        lblAyudaResponsable.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblAyudaResponsable.setForeground(COLOR_NEUTRO);
        panel.add(lblAyudaResponsable, gbc);
        gbc.insets = new Insets(10, 12, 10, 12);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.22;
        JLabel lblUrgencia = new JLabel("Urgencia (1-Baja a 5-Crítica):", SwingConstants.RIGHT);
        lblUrgencia.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUrgencia.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblUrgencia, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        cbUrgencia = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        cbUrgencia.setBackground(Color.WHITE); panel.add(cbUrgencia, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.22;
        JLabel lblTiempo = new JLabel("Tiempo Estimado (Horas):", SwingConstants.RIGHT);
        lblTiempo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTiempo.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblTiempo, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        txtTiempoEstimado = new JTextField("2"); estilarCampoTexto(txtTiempoEstimado); panel.add(txtTiempoEstimado, gbc);

        selectorFecha = new SelectorFechaPanel();

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.22;
        JLabel lblFecha = new JLabel("Fecha de Entrega:", SwingConstants.RIGHT);
        lblFecha.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFecha.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblFecha, gbc);

        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 0.78;
        JLabel lblFormatoFecha = new JLabel("Formato requerido: " + selectorFecha.getFormatoTexto()
                + " (ejemplo: 2025-12-31). Si se deja vacío, se usará la fecha de hoy.");
        lblFormatoFecha.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFormatoFecha.setForeground(COLOR_NEUTRO);
        panel.add(lblFormatoFecha, gbc);

        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.78; gbc.insets = new Insets(0, 12, 10, 12);
        panel.add(selectorFecha, gbc);
        gbc.insets = new Insets(10, 12, 10, 12);

        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.22;
        JLabel lblEstructura = new JLabel("Asignar a Estructura:", SwingConstants.RIGHT);
        lblEstructura.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstructura.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblEstructura, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        cbEstructura = new JComboBox<>(new String[]{"Pila (Urgente)", "Cola (Secuencial)", "Lista (General)", "Cola de Prioridad (Urgencia/Fecha)"});
        cbEstructura.setBackground(Color.WHITE); panel.add(cbEstructura, gbc);

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(18, 15, 8, 15);
        btnAgregar = crearBotonEstilizado("  +  Agregar Tarea al Sistema  ", COLOR_PRIMARIO, Color.WHITE);
        btnAgregar.setPreferredSize(new Dimension(320, 42));
        panel.add(btnAgregar, gbc);
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.LINE_START;

        JScrollPane scrollRegistro = new JScrollPane(panel);
        scrollRegistro.setBorder(null);
        scrollRegistro.setOpaque(false);
        scrollRegistro.getViewport().setOpaque(false);
        scrollRegistro.getVerticalScrollBar().setUnitIncrement(16);
        return scrollRegistro;
    }

    private JPanel crearCardPila() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloPila = new DefaultTableModel(COLUMNAS_TAREAS, 0);
        tablaPila = crearTablaEstilizada(modeloPila);
        JScrollPane scroll = new JScrollPane(tablaPila);
        scroll.setBorder(crearBordeSeccion(" Pilas - Tareas Urgentes ", 14));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); panelBotones.setOpaque(false);
        btnPopPila = crearBotonEstilizado("Procesar Pila (Pop)", COLOR_ROJO, Color.WHITE);
        btnPeekPila = crearBotonEstilizado("Consultar Pila (Peek)", COLOR_VERDE, Color.WHITE);
        panelBotones.add(btnPopPila); panelBotones.add(btnPeekPila);

        panel.add(scroll, BorderLayout.CENTER); panel.add(panelBotones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearCardCola() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloCola = new DefaultTableModel(COLUMNAS_TAREAS, 0);
        tablaCola = crearTablaEstilizada(modeloCola);
        JScrollPane scroll = new JScrollPane(tablaCola);
        scroll.setBorder(crearBordeSeccion(" Colas - Tareas Programadas ", 14));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); panelBotones.setOpaque(false);
        btnDequeueCola = crearBotonEstilizado("Procesar Cola (Dequeue)", COLOR_ROJO, Color.WHITE);
        btnFrontCola = crearBotonEstilizado("Consultar Cola (Front)", COLOR_VERDE, Color.WHITE);
        panelBotones.add(btnDequeueCola); panelBotones.add(btnFrontCola);

        panel.add(scroll, BorderLayout.CENTER); panel.add(panelBotones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearCardLista() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloLista = new DefaultTableModel(COLUMNAS_TAREAS, 0);
        tablaLista = crearTablaEstilizada(modeloLista);
        JScrollPane scroll = new JScrollPane(tablaLista);
        scroll.setBorder(crearBordeSeccion(" Listas - Tareas Generales ", 14));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); panelBotones.setOpaque(false);
        panelBotones.add(new JLabel("Departamento:"));
        cbFiltroDeptoLista = new JComboBox<>(new String[]{"Sistemas", "Ventas", "Recursos Humanos", "Finanzas", "Logística"});
        cbFiltroDeptoLista.setBackground(Color.WHITE);
        panelBotones.add(cbFiltroDeptoLista);
        btnEliminarLista = crearBotonEstilizado("Eliminar por ID", COLOR_NEUTRO, Color.WHITE);
        btnBuscarDepto = crearBotonEstilizado("Buscar por Depto", COLOR_NEUTRO, Color.WHITE);
        btnQuickSortUrgencia = crearBotonEstilizado("Ordenar Tareas por Urgencia", COLOR_TEXTO_DARK, Color.WHITE);
        panelBotones.add(btnEliminarLista); panelBotones.add(btnBuscarDepto); panelBotones.add(btnQuickSortUrgencia);

        panel.add(scroll, BorderLayout.CENTER); panel.add(panelBotones, BorderLayout.SOUTH);
        return panel;
    }

    // --- Card Cola de Prioridad ---
    private JPanel crearCardPrioridad() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloPrioridad = new DefaultTableModel(COLUMNAS_TAREAS, 0);
        tablaPrioridad = crearTablaEstilizada(modeloPrioridad);
        JScrollPane scroll = new JScrollPane(tablaPrioridad);
        scroll.setBorder(crearBordeSeccion(" Cola de Prioridad - Ordenada por Urgencia y Fecha ", 14));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); panelBotones.setOpaque(false);
        btnExtraerPrioridad = crearBotonEstilizado("Extraer Mayor Prioridad (Poll)", COLOR_ROJO, Color.WHITE);
        btnVerPrioridad = crearBotonEstilizado("Consultar Siguiente (Peek)", COLOR_VERDE, Color.WHITE);
        panelBotones.add(btnExtraerPrioridad); panelBotones.add(btnVerPrioridad);

        panel.add(scroll, BorderLayout.CENTER); panel.add(panelBotones, BorderLayout.SOUTH);
        return panel;
    }

    // --- Card Árbol Binario de Empleados ---
    private JPanel crearCardEmpleados() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);

        JPanel panelForm = new JPanel(new GridLayout(4, 2, 8, 8));
        panelForm.setBackground(COLOR_TARJETA);
        panelForm.setBorder(crearBordeSeccion(" Registrar Empleado ", 14));

        txtEmpleadoId = new JTextField(); estilarCampoTexto(txtEmpleadoId);
        txtEmpleadoNombre = new JTextField(); estilarCampoTexto(txtEmpleadoNombre);
        cbEmpleadoDepto = new JComboBox<>(new String[]{"Sistemas", "Ventas", "Recursos Humanos", "Finanzas", "Logística"});
        btnAgregarEmpleado = crearBotonEstilizado("+ Registrar Empleado", COLOR_PRIMARIO, Color.WHITE);

        panelForm.add(new JLabel("ID Empleado:")); panelForm.add(txtEmpleadoId);
        panelForm.add(new JLabel("Nombre Completo:")); panelForm.add(txtEmpleadoNombre);
        panelForm.add(new JLabel("Departamento:")); panelForm.add(cbEmpleadoDepto);
        panelForm.add(new JLabel("")); panelForm.add(btnAgregarEmpleado);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); panelAcciones.setOpaque(false);
        txtBuscarEmpleadoId = new JTextField(10); estilarCampoTexto(txtBuscarEmpleadoId);
        btnBuscarEmpleadoId = crearBotonEstilizado("Buscar por ID", COLOR_NEUTRO, Color.WHITE);
        cbFiltroDeptoEmp = new JComboBox<>(new String[]{"Sistemas", "Ventas", "Recursos Humanos", "Finanzas", "Logística"});
        btnListarEmpleadoDepto = crearBotonEstilizado("Listar por Depto", COLOR_NEUTRO, Color.WHITE);
        btnMostrarTodosEmpleados = crearBotonEstilizado("Ver Todos", COLOR_VERDE, Color.WHITE);

        panelAcciones.add(new JLabel("ID:")); panelAcciones.add(txtBuscarEmpleadoId); panelAcciones.add(btnBuscarEmpleadoId);
        panelAcciones.add(Box.createHorizontalStrut(15));
        panelAcciones.add(new JLabel("Depto:")); panelAcciones.add(cbFiltroDeptoEmp); panelAcciones.add(btnListarEmpleadoDepto);
        panelAcciones.add(Box.createHorizontalStrut(10));
        panelAcciones.add(btnMostrarTodosEmpleados);

        modeloEmpleados = new DefaultTableModel(new String[]{"ID", "Nombre Empleado", "Departamento", COL_PENDIENTES}, 0);
        tablaEmpleados = crearTablaEstilizada(modeloEmpleados);
        tablaEmpleados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaEmpleados.getColumnModel().getColumn(3).setCellRenderer(new PendientesBadgeRenderer());
        tablaEmpleados.getColumnModel().getColumn(3).setPreferredWidth(260);
        tablaEmpleados.setToolTipText("Selecciona un empleado para ver el detalle de sus tareas pendientes");
        JScrollPane scrollTabla = new JScrollPane(tablaEmpleados);
        scrollTabla.setBorder(crearBordeSeccion(" Empleados Registrados ", 12));

        // --- Sección desplegable: detalle de tareas pendientes del empleado seleccionado ---
        modeloPendientesEmpleado = new DefaultTableModel(new String[]{"ID", "Título", "Urgencia", COL_FECHA_ENTREGA, "Estructura"}, 0);
        tablaPendientesEmpleado = crearTablaEstilizada(modeloPendientesEmpleado);
        tablaPendientesEmpleado.getColumnModel().getColumn(2).setCellRenderer(new UrgenciaBadgeRenderer());
        tablaPendientesEmpleado.getColumnModel().getColumn(2).setPreferredWidth(110);
        tablaPendientesEmpleado.getColumnModel().getColumn(1).setPreferredWidth(220);

        lblTituloPendientes = new JLabel("Selecciona un empleado para ver sus tareas pendientes.");
        lblTituloPendientes.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTogglePendientes = crearBotonEstilizado("▼ Ocultar detalle", COLOR_NEUTRO, Color.WHITE);
        btnTogglePendientes.addActionListener(e -> alternarDetallePendientes());

        JPanel cabeceraDetalle = new JPanel(new BorderLayout(8, 0)); cabeceraDetalle.setOpaque(false);
        cabeceraDetalle.setBorder(new EmptyBorder(4, 4, 4, 4));
        cabeceraDetalle.add(lblTituloPendientes, BorderLayout.CENTER);
        cabeceraDetalle.add(btnTogglePendientes, BorderLayout.EAST);

        JScrollPane scrollPendientes = new JScrollPane(tablaPendientesEmpleado);
        scrollPendientes.setBorder(crearBordeSeccion(" Tareas Pendientes del Empleado ", 12));

        JPanel panelDetalle = new JPanel(new BorderLayout(4, 4)); panelDetalle.setOpaque(false);
        panelDetalle.add(cabeceraDetalle, BorderLayout.NORTH);
        panelDetalle.add(scrollPendientes, BorderLayout.CENTER);

        splitEmpleados = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabla, panelDetalle);
        splitEmpleados.setResizeWeight(0.55);
        splitEmpleados.setBorder(null);
        splitEmpleados.setOpaque(false);
        splitEmpleados.setContinuousLayout(true);

        JPanel panelCentro = new JPanel(new BorderLayout(5, 5)); panelCentro.setOpaque(false);
        panelCentro.add(panelAcciones, BorderLayout.NORTH); panelCentro.add(splitEmpleados, BorderLayout.CENTER);

        panel.add(panelForm, BorderLayout.NORTH); panel.add(panelCentro, BorderLayout.CENTER);
        return panel;
    }

    // --- Card Cálculos y Distribución (tarjetas KPI + alerta + tabla por empleado) ---
    private JPanel crearCardRecursivo() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10)); panelBotones.setOpaque(false);
        btnCalcularTiempoRecursivo = crearBotonEstilizado("Calcular Tiempo Total Estimado", COLOR_PRIMARIO, Color.WHITE);
        btnDistribuirDivideVenceras = crearBotonEstilizado("Distribuir Tareas entre Empleados", COLOR_VERDE, Color.WHITE);
        panelBotones.add(btnCalcularTiempoRecursivo); panelBotones.add(btnDistribuirDivideVenceras);

        // Alerta roja (oculta hasta que haya departamentos sin personal)
        alertaSinPersonal = new AlertaPanel();
        alertaSinPersonal.setVisible(false);

        // Tarjetas de métricas
        kpiTotalTareas = new KpiCard("TOTAL DE TAREAS ANALIZADAS", COLOR_PRIMARIO, new Color(96, 165, 250));
        kpiTiempoTotal = new KpiCard("TIEMPO TOTAL ESTIMADO ACUMULADO", COLOR_VERDE, new Color(74, 222, 128));
        JPanel filaKpis = new JPanel(new GridLayout(1, 2, 12, 0)); filaKpis.setOpaque(false);
        filaKpis.add(kpiTotalTareas); filaKpis.add(kpiTiempoTotal);

        JPanel superior = new JPanel(); superior.setOpaque(false);
        superior.setLayout(new BoxLayout(superior, BoxLayout.Y_AXIS));
        alertaSinPersonal.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaKpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        superior.add(alertaSinPersonal);
        superior.add(Box.createVerticalStrut(10));
        superior.add(filaKpis);

        // Distribución organizada por empleado
        lblResumenDistribucion = new JLabel("Pulsa \"Distribuir Tareas entre Empleados\" para ver cómo queda repartido el trabajo.");
        lblResumenDistribucion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblResumenDistribucion.setBorder(new EmptyBorder(2, 4, 6, 4));

        panelGruposDistribucion = new PanelAnchoViewport();
        panelGruposDistribucion.setLayout(new BoxLayout(panelGruposDistribucion, BoxLayout.Y_AXIS));
        panelGruposDistribucion.setBackground(COLOR_TARJETA);
        JScrollPane scrollGrupos = new JScrollPane(panelGruposDistribucion);
        scrollGrupos.setBorder(null);
        scrollGrupos.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panelDistribucion = new JPanel(new BorderLayout()); panelDistribucion.setBackground(COLOR_TARJETA);
        panelDistribucion.setBorder(crearBordeSeccion(" Distribución de Tareas por Empleado ", 14));
        panelDistribucion.add(lblResumenDistribucion, BorderLayout.NORTH);
        panelDistribucion.add(scrollGrupos, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 12)); centro.setOpaque(false);
        centro.add(superior, BorderLayout.NORTH);
        centro.add(panelDistribucion, BorderLayout.CENTER);

        panel.add(panelBotones, BorderLayout.NORTH); panel.add(centro, BorderLayout.CENTER);
        return panel;
    }

    /** Actualiza las tarjetas KPI del cálculo de tiempo total. */
    public void mostrarKpisTiempo(int totalTareas, int horasTotales, String detalleTareas) {
        kpiTotalTareas.setDatos(String.format("%,d", totalTareas), totalTareas == 1 ? "tarea activa" : "tareas activas", detalleTareas);
        String promedio = totalTareas == 0 ? "0" : String.format("%.1f", horasTotales / (double) totalTareas);
        kpiTiempoTotal.setDatos(String.format("%,d", horasTotales), horasTotales == 1 ? "hora" : "horas",
                String.format("≈ %.1f jornadas de 8 h  ·  promedio %s h por tarea", horasTotales / 8.0, promedio));
    }

    /**
     * Pinta la distribución: alerta roja si hay departamentos sin personal y una tabla por empleado
     * con sus tareas asignadas.
     */
    public void mostrarDistribucion(List<GrupoDistribucion> grupos, java.util.Collection<String> departamentosSinPersonal,
                                    int tareasSinPersonal, int nuevasAsignaciones) {
        // Alerta
        if (departamentosSinPersonal.isEmpty()) {
            alertaSinPersonal.setVisible(false);
        } else {
            alertaSinPersonal.setMensaje("ATENCIÓN: No hay personal disponible en los siguientes departamentos: "
                    + String.join(", ", departamentosSinPersonal) + ". Sus tareas no fueron asignadas ("
                    + tareasSinPersonal + (tareasSinPersonal == 1 ? " tarea)." : " tareas)."));
            alertaSinPersonal.setVisible(true);
        }

        // Resumen
        int totalAsignadas = 0;
        for (GrupoDistribucion g : grupos) totalAsignadas += g.filas().size();
        lblResumenDistribucion.setText(grupos.size() + " empleado(s)  ·  " + totalAsignadas + " tarea(s) asignada(s)  ·  "
                + nuevasAsignaciones + " asignada(s) en esta distribución"
                + (tareasSinPersonal > 0 ? "  ·  " + tareasSinPersonal + " sin personal disponible" : ""));

        // Secciones por empleado
        panelGruposDistribucion.removeAll();
        if (grupos.isEmpty()) {
            JLabel vacio = new JLabel("No hay empleados registrados en los departamentos con tareas.");
            vacio.setBorder(new EmptyBorder(12, 8, 12, 8));
            panelGruposDistribucion.add(vacio);
        }
        for (GrupoDistribucion grupo : grupos) {
            JPanel seccion = crearSeccionEmpleado(grupo);
            seccion.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelGruposDistribucion.add(seccion);
            panelGruposDistribucion.add(Box.createVerticalStrut(10));
        }
        aplicarTemaGeneral(); // los componentes recién creados toman el tema actual (claro/oscuro)
        panelGruposDistribucion.revalidate();
        panelGruposDistribucion.repaint();
    }

    private JPanel crearSeccionEmpleado(GrupoDistribucion grupo) {
        Empleado emp = grupo.empleado();
        int horas = 0;
        for (FilaAsignacion f : grupo.filas()) horas += f.tarea().getTiempoEstimado();

        EncabezadoEmpleado encabezado = new EncabezadoEmpleado(emp.getNombre(),
                "ID " + emp.getId() + "  ·  " + emp.getDepartamento(),
                grupo.filas().size() + (grupo.filas().size() == 1 ? " tarea" : " tareas"), horas + " h de carga");

        JPanel seccion = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        seccion.setBackground(COLOR_TARJETA);
        seccion.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
        seccion.add(encabezado, BorderLayout.NORTH);

        if (grupo.filas().isEmpty()) {
            JLabel sinTareas = new JLabel("Sin tareas asignadas.");
            sinTareas.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            sinTareas.setBorder(new EmptyBorder(8, 14, 10, 14));
            seccion.add(sinTareas, BorderLayout.CENTER);
            return seccion;
        }

        DefaultTableModel modelo = new DefaultTableModel(new String[]{"ID", "Título", "Departamento", "Urgencia",
                "Tiempo Est.", COL_FECHA_ENTREGA, "Estructura", "Asignación"}, 0);
        for (FilaAsignacion f : grupo.filas()) {
            Tarea t = f.tarea();
            modelo.addRow(new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getUrgencia(),
                    t.getTiempoEstimado() + " h", t.getFechaEntrega(), t.getTipoEstructura(),
                    f.responsablePrevio() ? "Directa" : "Nueva"});
        }
        JTable tabla = crearTablaEstilizada(modelo);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new UrgenciaBadgeRenderer());
        tabla.getColumnModel().getColumn(3).setPreferredWidth(105);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(7).setHeaderValue("Asignación");
        tabla.setToolTipText("Asignación: Directa = ya tenía Responsable Directo; Nueva = asignada en esta distribución");

        JPanel contenedorTabla = new JPanel(new BorderLayout());
        contenedorTabla.setBorder(new EmptyBorder(0, 10, 10, 10));
        contenedorTabla.add(tabla.getTableHeader(), BorderLayout.NORTH);
        contenedorTabla.add(tabla, BorderLayout.CENTER);
        seccion.add(contenedorTabla, BorderLayout.CENTER);
        return seccion;
    }

    /** Panel que se ajusta al ancho del scroll (para que las tablas no queden cortadas). */
    private static class PanelAnchoViewport extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Tarjeta de métrica: título pequeño, número destacado con su unidad y una línea de detalle. */
    private class KpiCard extends JComponent {
        private final String titulo;
        private final Color acentoClaro, acentoOscuro;
        private String valor = "—", unidad = "", detalle = "Pulsa \"Calcular Tiempo Total Estimado\"";

        KpiCard(String titulo, Color acentoClaro, Color acentoOscuro) {
            this.titulo = titulo; this.acentoClaro = acentoClaro; this.acentoOscuro = acentoOscuro;
            putClientProperty(TEMA_PROPIO, true);
            setPreferredSize(new Dimension(300, 112));
        }

        void setDatos(String valor, String unidad, String detalle) {
            this.valor = valor; this.unidad = unidad; this.detalle = detalle;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color acento = temaOscuro ? acentoOscuro : acentoClaro;
            int w = getWidth(), h = getHeight();
            g.setColor(temaOscuro ? new Color(30, 41, 59) : Color.WHITE);
            g.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g.setColor(temaOscuro ? new Color(71, 85, 105) : COLOR_BORDE);
            g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g.setColor(acento);
            g.fillRoundRect(0, 0, 6, h - 1, 6, 6);

            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.setColor(temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO);
            g.drawString(titulo, 22, 26);

            g.setFont(new Font("Segoe UI", Font.BOLD, 34));
            g.setColor(acento);
            g.drawString(valor, 22, 66);
            int xUnidad = 22 + g.getFontMetrics().stringWidth(valor) + 8;
            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g.setColor(temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK);
            g.drawString(unidad, xUnidad, 66);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.setColor(temaOscuro ? new Color(203, 213, 225) : COLOR_NEUTRO);
            g.drawString(detalle, 22, 92);
            g.dispose();
        }
    }

    /** Alerta destacada en rojo con ícono de advertencia dibujado (no depende de fuentes con emoji). */
    private class AlertaPanel extends JPanel {
        private final JTextArea texto = new JTextArea() {
            @Override public Color getForeground() { return temaOscuro ? new Color(254, 202, 202) : new Color(185, 28, 28); }
        };

        AlertaPanel() {
            super(new BorderLayout(14, 0));
            putClientProperty(TEMA_PROPIO, true);
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
            JComponent icono = new JComponent() {
                { setPreferredSize(new Dimension(38, 34)); }
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Polygon triangulo = new Polygon(new int[]{19, 37, 1}, new int[]{1, 33, 33}, 3);
                    g.setColor(COLOR_ROJO);
                    g.fillPolygon(triangulo);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 20));
                    g.drawString("!", 19 - g.getFontMetrics().stringWidth("!") / 2, 30);
                    g.dispose();
                }
            };
            JPanel contIcono = new JPanel(new BorderLayout()); contIcono.setOpaque(false);
            contIcono.add(icono, BorderLayout.NORTH);
            texto.setEditable(false); texto.setFocusable(false);
            texto.setLineWrap(true); texto.setWrapStyleWord(true); texto.setOpaque(false);
            texto.setFont(new Font("Segoe UI", Font.BOLD, 17));
            texto.setBorder(null);
            add(contIcono, BorderLayout.WEST);
            add(texto, BorderLayout.CENTER);
        }

        void setMensaje(String mensaje) { texto.setText(mensaje); revalidate(); }

        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(temaOscuro ? new Color(69, 10, 10) : new Color(254, 242, 242));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.setColor(COLOR_ROJO);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            g.fillRoundRect(0, 0, 7, getHeight() - 1, 7, 7);
            g.dispose();
        }
    }

    /** Encabezado de cada empleado en la distribución: nombre, ID/departamento y chips de carga. */
    private class EncabezadoEmpleado extends JComponent {
        private final String nombre, subtitulo, chipTareas, chipHoras;

        EncabezadoEmpleado(String nombre, String subtitulo, String chipTareas, String chipHoras) {
            this.nombre = nombre; this.subtitulo = subtitulo; this.chipTareas = chipTareas; this.chipHoras = chipHoras;
            putClientProperty(TEMA_PROPIO, true);
            setPreferredSize(new Dimension(400, 50));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(temaOscuro ? new Color(30, 41, 59) : new Color(248, 250, 252));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(temaOscuro ? new Color(96, 165, 250) : COLOR_PRIMARIO);
            g.fillRect(0, 0, 5, getHeight());

            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g.setColor(temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK);
            g.drawString(nombre, 16, 22);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.setColor(temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO);
            g.drawString(subtitulo, 16, 39);

            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int anchoHoras = fm.stringWidth(chipHoras) + 16, anchoTareas = fm.stringWidth(chipTareas) + 16;
            int x = getWidth() - 12 - anchoHoras - 6 - anchoTareas;
            x = pintarBadge(g, x, getHeight(), chipTareas,
                    temaOscuro ? new Color(30, 64, 175) : new Color(219, 234, 254),
                    temaOscuro ? Color.WHITE : new Color(30, 64, 175));
            pintarBadge(g, x, getHeight(), chipHoras,
                    temaOscuro ? new Color(20, 83, 45) : new Color(220, 252, 231),
                    temaOscuro ? new Color(187, 247, 208) : new Color(21, 128, 61));
            g.dispose();
        }
    }

    // --- Card Búsquedas: buscador + comparativa | recorridos | visualizador del árbol ---
    private JPanel crearCardAlgoritmos() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false);

        // ===== Panel izquierdo: buscador unificado y comparativa =====
        JPanel izquierdo = new JPanel(new BorderLayout(8, 8)); izquierdo.setBackground(COLOR_TARJETA);
        izquierdo.setBorder(crearBordeSeccion(" Buscar Tarea por Folio / ID ", 13));

        JPanel filaBusqueda = new JPanel(new BorderLayout(8, 0)); filaBusqueda.setOpaque(false);
        txtBuscarFolio = new JTextField(); estilarCampoTexto(txtBuscarFolio);
        txtBuscarFolio.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtBuscarFolio.setToolTipText("Escribe el folio (ID numérico) y presiona Enter o Buscar");
        btnBuscarFolio = crearBotonEstilizado("  Buscar  ", COLOR_PRIMARIO, Color.WHITE);
        btnBuscarFolio.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtBuscarFolio.addActionListener(e -> btnBuscarFolio.doClick()); // Enter = Buscar
        JLabel lblFolio = new JLabel("Folio:");
        lblFolio.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filaBusqueda.add(lblFolio, BorderLayout.WEST);
        filaBusqueda.add(txtBuscarFolio, BorderLayout.CENTER);
        filaBusqueda.add(btnBuscarFolio, BorderLayout.EAST);

        detalleBusqueda = new DetalleTareaCard();
        graficaComparaciones = new GraficaComparaciones();
        JPanel resultados = new JPanel(new BorderLayout(0, 8)); resultados.setOpaque(false);
        resultados.add(detalleBusqueda, BorderLayout.NORTH);
        resultados.add(graficaComparaciones, BorderLayout.CENTER);

        izquierdo.add(filaBusqueda, BorderLayout.NORTH);
        izquierdo.add(resultados, BorderLayout.CENTER);

        // ===== Panel derecho: recorridos recursivos =====
        JPanel derecho = new JPanel(new BorderLayout(8, 8)); derecho.setBackground(COLOR_TARJETA);
        derecho.setBorder(crearBordeSeccion(" Recorridos Recursivos del Árbol ", 13));

        btnInorden = crearBotonOpcion("Inorden");
        btnPreorden = crearBotonOpcion("Preorden");
        btnPostorden = crearBotonOpcion("Postorden");
        ButtonGroup grupoRecorridos = new ButtonGroup();
        grupoRecorridos.add(btnInorden); grupoRecorridos.add(btnPreorden); grupoRecorridos.add(btnPostorden);
        JPanel filaOpciones = new JPanel(new GridLayout(1, 3, 8, 0)); filaOpciones.setOpaque(false);
        filaOpciones.add(btnInorden); filaOpciones.add(btnPreorden); filaOpciones.add(btnPostorden);

        txtExplicacionRecorrido = new JTextArea("Elige un recorrido para ver en qué orden se visitan los folios del árbol.");
        txtExplicacionRecorrido.setEditable(false); txtExplicacionRecorrido.setFocusable(false);
        txtExplicacionRecorrido.setLineWrap(true); txtExplicacionRecorrido.setWrapStyleWord(true);
        txtExplicacionRecorrido.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtExplicacionRecorrido.setBorder(new EmptyBorder(6, 6, 6, 6));

        secuenciaRecorrido = new SecuenciaFolios();
        JScrollPane scrollSecuencia = new JScrollPane(secuenciaRecorrido);
        scrollSecuencia.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
        scrollSecuencia.getVerticalScrollBar().setUnitIncrement(12);

        JPanel cuerpoRecorridos = new JPanel(new BorderLayout(0, 6)); cuerpoRecorridos.setOpaque(false);
        cuerpoRecorridos.add(txtExplicacionRecorrido, BorderLayout.NORTH);
        cuerpoRecorridos.add(scrollSecuencia, BorderLayout.CENTER);

        derecho.add(filaOpciones, BorderLayout.NORTH);
        derecho.add(cuerpoRecorridos, BorderLayout.CENTER);

        JPanel superior = new JPanel(new GridLayout(1, 2, 12, 0)); superior.setOpaque(false);
        superior.add(izquierdo); superior.add(derecho);

        // ===== Panel inferior: visualizador del árbol =====
        JPanel inferior = new JPanel(new BorderLayout(6, 6)); inferior.setBackground(COLOR_TARJETA);
        inferior.setBorder(crearBordeSeccion(" Visualizador del Árbol de Tareas (ABB por Folio) ", 13));
        btnBalancearArbol = crearBotonEstilizado("Balancear árbol (Divide y Vencerás)", COLOR_VERDE, Color.WHITE);
        lblInfoArbol = new JLabel("Sin tareas registradas.");
        lblInfoArbol.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel barraArbol = new JPanel(new BorderLayout(10, 0)); barraArbol.setOpaque(false);
        barraArbol.add(lblInfoArbol, BorderLayout.CENTER);
        barraArbol.add(btnBalancearArbol, BorderLayout.EAST);

        arbolVisual = new ArbolVisual();
        JScrollPane scrollArbol = new JScrollPane(arbolVisual);
        scrollArbol.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
        scrollArbol.getHorizontalScrollBar().setUnitIncrement(16);
        scrollArbol.getVerticalScrollBar().setUnitIncrement(16);
        inferior.add(barraArbol, BorderLayout.NORTH);
        inferior.add(scrollArbol, BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, superior, inferior);
        division.setResizeWeight(0.5);
        division.setBorder(null);
        division.setOpaque(false);
        division.setContinuousLayout(true);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    // Botón de opción (tipo "pestaña") para elegir el recorrido
    private JToggleButton crearBotonOpcion(String texto) {
        JToggleButton b = new JToggleButton(texto) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sel = isSelected();
                Color fondo = sel ? COLOR_PRIMARIO : (temaOscuro ? new Color(30, 41, 59) : new Color(241, 245, 249));
                if (getModel().isRollover() && !sel) fondo = temaOscuro ? new Color(51, 65, 85) : new Color(226, 232, 240);
                g.setColor(fondo);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g.setColor(sel ? COLOR_PRIMARIO : (temaOscuro ? new Color(71, 85, 105) : COLOR_BORDE));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.setColor(sel ? Color.WHITE : (temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        };
        b.setPreferredSize(new Dimension(100, 34));
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.putClientProperty(TEMA_PROPIO, true);
        return b;
    }

    // ---------- API pública del módulo Búsquedas (la usa el Controlador) ----------

    /** Muestra el detalle de la tarea buscada y la comparativa de comparaciones por método. */
    public void mostrarResultadoBusqueda(int folio, Tarea tarea, String estado, List<MetodoBusqueda> metodos, int n) {
        detalleBusqueda.setDatos(folio, tarea, estado);
        graficaComparaciones.setDatos(metodos, n);
    }

    public void mostrarRecorrido(String nombre, String explicacion, List<Integer> secuencia) {
        txtExplicacionRecorrido.setText(explicacion);
        secuenciaRecorrido.setDatos(nombre, secuencia);
        arbolVisual.setOrdenRecorrido(secuencia);
    }

    /** Redibuja el árbol. 'ruta' resalta los nodos visitados en la última búsqueda (puede ser vacía). */
    public void actualizarArbol(ArbolTareasABB.Nodo raiz, String info, List<Integer> ruta, Integer encontrado) {
        lblInfoArbol.setText(info);
        arbolVisual.setDatos(raiz, ruta, encontrado);
    }

    /** Resalta en el árbol la ruta de la búsqueda sin reconstruirlo. */
    public void resaltarRutaArbol(List<Integer> ruta, Integer encontrado) {
        arbolVisual.setRuta(ruta, encontrado);
    }

    public String getFolioBuscado() { return txtBuscarFolio.getText().trim(); }
    public JButton getBtnBuscarFolio() { return btnBuscarFolio; }
    public JButton getBtnBalancearArbol() { return btnBalancearArbol; }
    public JToggleButton getBtnInorden() { return btnInorden; }
    public JToggleButton getBtnPreorden() { return btnPreorden; }
    public JToggleButton getBtnPostorden() { return btnPostorden; }

    // ---------- Componentes gráficos del módulo Búsquedas ----------

    /** Tarjeta con los datos de la tarea encontrada (o el aviso de "no encontrada"). */
    private class DetalleTareaCard extends JComponent {
        private Integer folio;
        private Tarea tarea;
        private String estado = "";

        DetalleTareaCard() {
            putClientProperty(TEMA_PROPIO, true);
            setPreferredSize(new Dimension(300, 150));
        }

        void setDatos(int folio, Tarea tarea, String estado) {
            this.folio = folio; this.tarea = tarea; this.estado = estado == null ? "" : estado;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean ok = tarea != null;
            Color acento = folio == null ? COLOR_NEUTRO : ok ? COLOR_VERDE : COLOR_ROJO;
            g.setColor(temaOscuro ? new Color(30, 41, 59) : new Color(248, 250, 252));
            g.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g.setColor(temaOscuro ? new Color(71, 85, 105) : COLOR_BORDE);
            g.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g.setColor(acento);
            g.fillRoundRect(0, 0, 6, h - 1, 6, 6);

            Color texto = temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK;
            Color suave = temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO;
            if (folio == null) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.setColor(suave);
                g.drawString("Escribe un folio y pulsa Buscar para ver sus datos", 20, h / 2 - 4);
                g.drawString("y cuántas comparaciones necesita cada método.", 20, h / 2 + 14);
                g.dispose();
                return;
            }
            if (!ok) {
                g.setFont(new Font("Segoe UI", Font.BOLD, 15));
                g.setColor(temaOscuro ? COLOR_ROJO_OSCURO_TEMA : COLOR_ROJO);
                g.drawString("Folio " + folio + " no encontrado", 20, 34);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.setColor(suave);
                g.drawString("No hay ninguna tarea activa con ese folio.", 20, 58);
                g.dispose();
                return;
            }
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.setColor(suave);
            g.drawString("FOLIO #" + tarea.getId(), 20, 22);
            if (!estado.isEmpty()) {
                FontMetrics fmE = g.getFontMetrics();
                int xE = w - 12 - fmE.stringWidth(estado) - 16;
                boolean activa = estado.startsWith("Activa");
                pintarBadgeEn(g, xE, 8, estado,
                        activa ? (temaOscuro ? new Color(20, 83, 45) : new Color(220, 252, 231)) : (temaOscuro ? new Color(113, 63, 18) : new Color(254, 243, 199)),
                        activa ? (temaOscuro ? new Color(187, 247, 208) : new Color(21, 128, 61)) : (temaOscuro ? new Color(254, 240, 138) : new Color(161, 98, 7)));
            }
            g.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g.setColor(texto);
            g.drawString(recortar(g, tarea.getTitulo(), w - 40), 20, 44);

            String[][] campos = {
                    {"Departamento", tarea.getDepartamento()},
                    {"Responsable", tarea.getNombreResponsable()},
                    {"Urgencia", tarea.getUrgencia() + " de 5"},
                    {"Tiempo est.", tarea.getTiempoEstimado() + " h"},
                    {"Estructura", tarea.getTipoEstructura()},
                    {"Entrega", tarea.getFechaEntregaFormateada()},
            };
            int colAncho = (w - 30) / 2;
            for (int i = 0; i < campos.length; i++) {
                boolean filaCompleta = i >= 4;                    // Estructura y Entrega usan todo el ancho
                int x = filaCompleta ? 20 : 20 + (i % 2) * colAncho;
                int y = filaCompleta ? 68 + (2 + (i - 4)) * 20 : 68 + (i / 2) * 20;
                int anchoValor = (filaCompleta ? w - 40 : colAncho) - 100;
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g.setColor(suave);
                g.drawString(campos[i][0] + ":", x, y);
                int xv = x + 96;
                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.setColor(i == 5 && tarea.estaVencida() ? (temaOscuro ? COLOR_ROJO_OSCURO_TEMA : COLOR_ROJO) : texto);
                g.drawString(recortar(g, campos[i][1], anchoValor), xv, y);
            }
            g.dispose();
        }
    }

    private static String recortar(Graphics2D g, String texto, int ancho) {
        if (texto == null) return "";
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(texto) <= ancho) return texto;
        while (texto.length() > 1 && fm.stringWidth(texto + "…") > ancho) texto = texto.substring(0, texto.length() - 1);
        return texto + "…";
    }

    private static void pintarBadgeEn(Graphics2D g, int x, int y, String texto, Color fondo, Color colorTexto) {
        FontMetrics fm = g.getFontMetrics();
        int ancho = fm.stringWidth(texto) + 16, alto = 20;
        g.setColor(fondo);
        g.fillRoundRect(x, y, ancho, alto, alto, alto);
        g.setColor(colorTexto);
        g.drawString(texto, x + 8, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
    }

    /** Gráfica de barras horizontales: comparaciones que hizo cada método para encontrar el folio. */
    private class GraficaComparaciones extends JComponent {
        private List<MetodoBusqueda> metodos = List.of();
        private int n;

        GraficaComparaciones() {
            putClientProperty(TEMA_PROPIO, true);
            setPreferredSize(new Dimension(300, 190));
        }

        void setDatos(List<MetodoBusqueda> metodos, int n) {
            this.metodos = metodos; this.n = n;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color texto = temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK;
            Color suave = temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO;

            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.setColor(texto);
            g.drawString("Comparativa de rendimiento (número de comparaciones)", 4, 14);
            if (metodos.isEmpty()) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g.setColor(suave);
                g.drawString("Aparecerá al realizar una búsqueda.", 4, 34);
                g.dispose();
                return;
            }
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.setColor(suave);
            g.drawString("Sobre " + n + " tarea(s) activa(s). Menos comparaciones = búsqueda más rápida.", 4, 30);

            Color[] colores = temaOscuro ? PALETA_MENU_OSCURO : PALETA_MENU;
            int max = 1;
            for (MetodoBusqueda m : metodos) max = Math.max(max, m.comparaciones());
            int etiquetaAncho = 175, valorAncho = 70;
            int areaBarra = Math.max(40, w - etiquetaAncho - valorAncho - 8);
            int filaAlto = Math.max(26, Math.min(36, (h - 44) / metodos.size()));
            for (int i = 0; i < metodos.size(); i++) {
                MetodoBusqueda m = metodos.get(i);
                int y = 42 + i * filaAlto;
                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.setColor(texto);
                g.drawString(m.nombre(), 4, y + 12);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.setColor(suave);
                g.drawString(m.complejidad(), 4, y + 24);

                int xBarra = etiquetaAncho;
                g.setColor(temaOscuro ? new Color(51, 65, 85) : new Color(241, 245, 249));
                g.fillRoundRect(xBarra, y + 4, areaBarra, 16, 8, 8);
                int largo = Math.max(6, (int) Math.round(areaBarra * (m.comparaciones() / (double) max)));
                Color c = colores[i % colores.length];
                g.setColor(c);
                g.fillRoundRect(xBarra, y + 4, largo, 16, 8, 8);

                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.setColor(texto);
                String valor = m.comparaciones() + (m.comparaciones() == 1 ? " comp." : " comps.");
                g.drawString(valor, xBarra + areaBarra + 8, y + 17);
                if (!m.encontrado()) {
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    g.setColor(temaOscuro ? COLOR_ROJO_OSCURO_TEMA : COLOR_ROJO);
                    g.drawString("no encontrado", xBarra + areaBarra + 8, y + 29);
                }
            }
            g.dispose();
        }
    }

    /** Secuencia de folios de un recorrido como "chips" numerados que se acomodan en varias líneas. */
    private class SecuenciaFolios extends JComponent implements Scrollable {
        private List<Integer> secuencia = List.of();
        private String nombre = "";
        private static final int CHIP_ALTO = 26, SEPARACION = 22;

        SecuenciaFolios() { putClientProperty(TEMA_PROPIO, true); }

        void setDatos(String nombre, List<Integer> secuencia) {
            this.nombre = nombre; this.secuencia = secuencia;
            revalidate(); repaint();
        }

        private int anchoChip(FontMetrics fm, int folio) { return Math.max(34, fm.stringWidth(String.valueOf(folio)) + 20); }

        @Override public Dimension getPreferredSize() {
            int ancho = getParent() != null ? Math.max(100, getParent().getWidth()) : 300;
            FontMetrics fm = getFontMetrics(new Font("Segoe UI", Font.BOLD, 12));
            int x = 10, lineas = 1;
            for (int folio : secuencia) {
                int a = anchoChip(fm, folio);
                if (x + a > ancho - 10 && x > 10) { lineas++; x = 10; }
                x += a + SEPARACION;
            }
            return new Dimension(ancho, 34 + lineas * (CHIP_ALTO + 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(temaOscuro ? new Color(30, 41, 59) : Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            Color suave = temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO;
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.setColor(suave);
            if (nombre.isEmpty()) { g.dispose(); return; }
            g.drawString(secuencia.isEmpty() ? "El árbol está vacío." : "Secuencia " + nombre.toLowerCase()
                    + " (" + secuencia.size() + " folios):", 10, 18);

            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int x = 10, y = 30;
            for (int i = 0; i < secuencia.size(); i++) {
                int folio = secuencia.get(i), a = anchoChip(fm, folio);
                if (x + a > getWidth() - 10 && x > 10) { x = 10; y += CHIP_ALTO + 12; }
                g.setColor(temaOscuro ? new Color(30, 64, 175) : new Color(219, 234, 254));
                g.fillRoundRect(x, y, a, CHIP_ALTO, CHIP_ALTO, CHIP_ALTO);
                g.setColor(temaOscuro ? Color.WHITE : new Color(30, 64, 175));
                String t = String.valueOf(folio);
                g.drawString(t, x + (a - fm.stringWidth(t)) / 2, y + (CHIP_ALTO + fm.getAscent() - fm.getDescent()) / 2);
                if (i < secuencia.size() - 1) {       // flecha hacia el siguiente folio
                    int ax = x + a + 4, ay = y + CHIP_ALTO / 2;
                    g.setColor(suave);
                    g.drawLine(ax, ay, ax + SEPARACION - 9, ay);
                    g.fillPolygon(new int[]{ax + SEPARACION - 8, ax + SEPARACION - 13, ax + SEPARACION - 13},
                            new int[]{ay, ay - 4, ay + 4}, 3);
                }
                x += a + SEPARACION;
            }
            g.dispose();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(300, 120); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 12; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /**
     * Dibujo del ABB: cada nodo se ubica horizontalmente según su posición inorden y
     * verticalmente según su profundidad; las ramas unen cada nodo con sus hijos.
     */
    private class ArbolVisual extends JComponent {
        private static final int RADIO = 18, SEP_X = 48, SEP_Y = 56, MARGEN = 26;
        private ArbolTareasABB.Nodo raiz;
        private final Map<Integer, Point> posiciones = new java.util.HashMap<>();
        private java.util.Set<Integer> ruta = java.util.Set.of();
        private Integer encontrado;
        private Map<Integer, Integer> ordenRecorrido = Map.of();
        private int columnas, niveles;

        ArbolVisual() { putClientProperty(TEMA_PROPIO, true); }

        void setDatos(ArbolTareasABB.Nodo raiz, List<Integer> ruta, Integer encontrado) {
            this.raiz = raiz;
            posiciones.clear();
            columnas = 0; niveles = 0;
            calcularPosiciones(raiz, 0);
            setRuta(ruta, encontrado);
            revalidate();
        }

        void setRuta(List<Integer> ruta, Integer encontrado) {
            this.ruta = ruta == null ? java.util.Set.of() : new java.util.HashSet<>(ruta);
            this.encontrado = encontrado;
            repaint();
        }

        void setOrdenRecorrido(List<Integer> secuencia) {
            Map<Integer, Integer> orden = new java.util.HashMap<>();
            for (int i = 0; i < secuencia.size(); i++) orden.put(secuencia.get(i), i + 1);
            this.ordenRecorrido = orden;
            repaint();
        }

        // Recorrido inorden recursivo para asignar la columna (x) de cada nodo
        private void calcularPosiciones(ArbolTareasABB.Nodo n, int profundidad) {
            if (n == null) return;
            calcularPosiciones(n.getIzquierdo(), profundidad + 1);
            posiciones.put(n.getId(), new Point(columnas++, profundidad));
            niveles = Math.max(niveles, profundidad + 1);
            calcularPosiciones(n.getDerecho(), profundidad + 1);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(Math.max(200, MARGEN * 2 + Math.max(0, columnas - 1) * SEP_X + RADIO * 2),
                    Math.max(120, MARGEN * 2 + Math.max(0, niveles - 1) * SEP_Y + RADIO * 2));
        }

        private Point centro(int id) {
            Point celda = posiciones.get(id);
            int anchoArbol = Math.max(0, columnas - 1) * SEP_X;
            int desplazamiento = Math.max(MARGEN + RADIO, (getWidth() - anchoArbol) / 2); // centrado si sobra espacio
            return new Point(desplazamiento + celda.x * SEP_X, MARGEN + RADIO + celda.y * SEP_Y);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(temaOscuro ? new Color(15, 23, 42) : new Color(248, 250, 252));
            g.fillRect(0, 0, getWidth(), getHeight());
            if (raiz == null) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g.setColor(temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO);
                g.drawString("No hay tareas activas: registra tareas para construir el árbol.", 20, 40);
                g.dispose();
                return;
            }
            dibujarRamas(g, raiz);
            dibujarNodos(g, raiz);
            dibujarLeyenda(g);
            g.dispose();
        }

        private void dibujarLeyenda(Graphics2D g) {
            Rectangle visible = getVisibleRect();              // fija en la esquina aunque haya scroll
            int x = visible.x + 12, y = visible.y + 14;
            Object[][] items = {
                    {"Tarea", temaOscuro ? PALETA_MENU[1] : PALETA_MENU[0]},
                    {"Ruta de búsqueda", temaOscuro ? new Color(124, 58, 237) : PALETA_MENU[3]},
                    {"Encontrado", COLOR_AMARILLO_DESTACADO}};
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            for (Object[] it : items) {
                g.setColor((Color) it[1]);
                g.fillOval(x, y - 9, 11, 11);
                g.setColor(temaOscuro ? new Color(203, 213, 225) : COLOR_NEUTRO);
                g.drawString((String) it[0], x + 16, y);
                x += 16 + g.getFontMetrics().stringWidth((String) it[0]) + 16;
            }
        }

        private void dibujarRamas(Graphics2D g, ArbolTareasABB.Nodo n) {
            if (n == null) return;
            Point p = centro(n.getId());
            for (ArbolTareasABB.Nodo hijo : new ArbolTareasABB.Nodo[]{n.getIzquierdo(), n.getDerecho()}) {
                if (hijo == null) continue;
                Point c = centro(hijo.getId());
                boolean enRuta = ruta.contains(n.getId()) && ruta.contains(hijo.getId());
                g.setStroke(new BasicStroke(enRuta ? 3f : 1.6f));
                g.setColor(enRuta ? (temaOscuro ? PALETA_MENU_OSCURO[3] : PALETA_MENU[3])
                        : (temaOscuro ? new Color(100, 116, 139) : new Color(148, 163, 184)));
                g.drawLine(p.x, p.y, c.x, c.y);
                dibujarRamas(g, hijo);
            }
        }

        private void dibujarNodos(Graphics2D g, ArbolTareasABB.Nodo n) {
            if (n == null) return;
            Point p = centro(n.getId());
            boolean esEncontrado = encontrado != null && encontrado == n.getId();
            boolean enRuta = ruta.contains(n.getId());
            // Colores del Menú Principal: azul = nodo, violeta = ruta de búsqueda, amarillo = encontrado
            Color relleno = esEncontrado ? COLOR_AMARILLO_DESTACADO
                    : enRuta ? (temaOscuro ? new Color(124, 58, 237) : PALETA_MENU[3])
                    : (temaOscuro ? PALETA_MENU[1] : PALETA_MENU[0]);
            if (esEncontrado) {                              // halo para llamar la atención
                g.setColor(new Color(250, 204, 21, 90));
                g.fillOval(p.x - RADIO - 7, p.y - RADIO - 7, (RADIO + 7) * 2, (RADIO + 7) * 2);
            }
            g.setColor(relleno);
            g.fillOval(p.x - RADIO, p.y - RADIO, RADIO * 2, RADIO * 2);
            g.setStroke(new BasicStroke(2f));
            g.setColor(temaOscuro ? new Color(15, 23, 42) : Color.WHITE);
            g.drawOval(p.x - RADIO, p.y - RADIO, RADIO * 2, RADIO * 2);

            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            String t = String.valueOf(n.getId());
            g.setColor(esEncontrado ? COLOR_TEXTO_DARK : Color.WHITE);   // texto oscuro sobre amarillo
            g.drawString(t, p.x - fm.stringWidth(t) / 2, p.y + (fm.getAscent() - fm.getDescent()) / 2);

            Integer orden = ordenRecorrido.get(n.getId());   // número de visita en el recorrido elegido
            if (orden != null) {
                g.setFont(new Font("Segoe UI", Font.BOLD, 9));
                FontMetrics f2 = g.getFontMetrics();
                String o = String.valueOf(orden);
                int bx = p.x + RADIO - 6, by = p.y - RADIO - 6, bw = Math.max(16, f2.stringWidth(o) + 8);
                g.setColor(temaOscuro ? new Color(71, 85, 105) : COLOR_SIDEBAR_BG);
                g.fillRoundRect(bx, by, bw, 16, 16, 16);
                g.setColor(Color.WHITE);
                g.drawString(o, bx + (bw - f2.stringWidth(o)) / 2, by + 12);
            }
            dibujarNodos(g, n.getIzquierdo());
            dibujarNodos(g, n.getDerecho());
        }
    }

    // --- Card Grafo de Dependencias ---
    private JPanel crearCardGrafo() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);

        // Formulario: solo se listan tareas activas (también se puede escribir el folio)
        JPanel panelForm = new JPanel(new BorderLayout(0, 4)); panelForm.setBackground(COLOR_TARJETA);
        panelForm.setBorder(crearBordeSeccion(" Registrar Dependencia entre Tareas Activas ", 12));

        cbGrafoTareaPrevia = new JComboBox<>(); cbGrafoTareaPrevia.setEditable(true);
        cbGrafoTareaSiguiente = new JComboBox<>(); cbGrafoTareaSiguiente.setEditable(true);
        cbGrafoTareaPrevia.setPreferredSize(new Dimension(250, 30));
        cbGrafoTareaSiguiente.setPreferredSize(new Dimension(250, 30));
        cbGrafoTareaPrevia.setToolTipText("Tarea que debe terminarse primero (elige de la lista o escribe su folio)");
        cbGrafoTareaSiguiente.setToolTipText("Tarea que no puede empezar hasta terminar la previa");
        btnAgregarDependencia = crearBotonEstilizado("+ Agregar Dependencia", COLOR_PRIMARIO, Color.WHITE);
        btnCalcularOrdenTopologico = crearBotonEstilizado("Calcular Orden de Ejecución", PALETA_MENU[0], Color.WHITE);
        btnLimpiarGrafo = crearBotonEstilizado("Limpiar Grafo", COLOR_NEUTRO, Color.WHITE);

        JPanel filaForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)); filaForm.setOpaque(false);
        JLabel lblPrevia = new JLabel("Tarea previa:"); lblPrevia.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel lblFlecha = new JLabel("→"); lblFlecha.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel lblSiguiente = new JLabel("Tarea siguiente:"); lblSiguiente.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filaForm.add(lblPrevia); filaForm.add(cbGrafoTareaPrevia); filaForm.add(lblFlecha);
        filaForm.add(lblSiguiente); filaForm.add(cbGrafoTareaSiguiente);
        filaForm.add(btnAgregarDependencia);

        JLabel ayuda = new JLabel("La previa debe terminarse antes que la siguiente. Solo tareas activas; "
                + "al finalizarse, una tarea sale del grafo.");
        ayuda.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ayuda.setBorder(new EmptyBorder(0, 8, 2, 8));
        JPanel botonesGrafo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); botonesGrafo.setOpaque(false);
        botonesGrafo.add(btnCalcularOrdenTopologico); botonesGrafo.add(btnLimpiarGrafo);
        JPanel filaAyuda = new JPanel(new BorderLayout(10, 0)); filaAyuda.setOpaque(false);
        filaAyuda.add(ayuda, BorderLayout.CENTER);
        filaAyuda.add(botonesGrafo, BorderLayout.EAST);
        panelForm.add(filaForm, BorderLayout.CENTER);
        panelForm.add(filaAyuda, BorderLayout.SOUTH);

        // Orden de ejecución destacado
        panelOrdenEjecucion = new OrdenEjecucionPanel();
        JScrollPane scrollOrden = new JScrollPane(panelOrdenEjecucion);
        scrollOrden.setBorder(null);
        scrollOrden.setPreferredSize(new Dimension(300, 150));
        scrollOrden.getVerticalScrollBar().setUnitIncrement(12);
        JPanel panelOrden = new JPanel(new BorderLayout()); panelOrden.setBackground(COLOR_TARJETA);
        panelOrden.setBorder(crearBordeSeccion(" Orden de Ejecución Calculado ", 14));
        panelOrden.add(scrollOrden, BorderLayout.CENTER);

        // Grafo por etapas
        grafoVisual = new GrafoVisual();
        JScrollPane scrollGrafo = new JScrollPane(grafoVisual);
        scrollGrafo.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
        scrollGrafo.getHorizontalScrollBar().setUnitIncrement(16);
        scrollGrafo.getVerticalScrollBar().setUnitIncrement(16);
        lblInfoGrafo = new JLabel("Sin dependencias registradas.");
        lblInfoGrafo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfoGrafo.setBorder(new EmptyBorder(0, 4, 4, 4));
        JPanel panelGrafo = new JPanel(new BorderLayout()); panelGrafo.setBackground(COLOR_TARJETA);
        panelGrafo.setBorder(crearBordeSeccion(" Grafo de Dependencias (flujo de izquierda a derecha) ", 14));
        panelGrafo.add(lblInfoGrafo, BorderLayout.NORTH);
        panelGrafo.add(scrollGrafo, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 10)); centro.setOpaque(false);
        centro.add(panelOrden, BorderLayout.NORTH);
        centro.add(panelGrafo, BorderLayout.CENTER);

        panel.add(panelForm, BorderLayout.NORTH); panel.add(centro, BorderLayout.CENTER);
        return panel;
    }

    // ---------- API pública del módulo Grafo ----------

    /** Recarga los selectores con las tareas activas, conservando lo que el usuario tenía escrito/elegido. */
    public void setTareasActivasGrafo(List<OpcionTarea> opciones) {
        for (JComboBox<Object> combo : List.of(cbGrafoTareaPrevia, cbGrafoTareaSiguiente)) {
            Object actual = combo.getEditor().getItem();
            combo.removeAllItems();
            for (OpcionTarea o : opciones) combo.addItem(o);
            combo.setSelectedItem(null);
            combo.getEditor().setItem(actual == null ? "" : actual);
        }
    }

    /** Texto elegido o escrito en el selector (p. ej. "#5 · Backup BD" o "5"). */
    public String getGrafoTareaPreviaInput() { return textoCombo(cbGrafoTareaPrevia); }
    public String getGrafoTareaSiguienteInput() { return textoCombo(cbGrafoTareaSiguiente); }

    private String textoCombo(JComboBox<Object> combo) {
        Object item = combo.isEditable() ? combo.getEditor().getItem() : combo.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    public void limpiarSelectoresGrafo() {
        cbGrafoTareaPrevia.getEditor().setItem("");
        cbGrafoTareaSiguiente.getEditor().setItem("");
    }

    /**
     * Pinta el grafo y el orden de ejecución.
     * @param tareas   datos de cada nodo (folio -> tarea)
     * @param aristas  pares {previa, siguiente}
     * @param orden    secuencia topológica (vacía si no hay dependencias)
     * @param etapas   folio -> etapa (1 = puede empezar ya)
     */
    public void mostrarGrafo(Map<Integer, Tarea> tareas, List<int[]> aristas, List<Integer> orden,
                             Map<Integer, Integer> etapas, String info) {
        lblInfoGrafo.setText(info);
        panelOrdenEjecucion.setDatos(tareas, orden, etapas);
        grafoVisual.setDatos(tareas, aristas, orden, etapas);
    }

    public JButton getBtnLimpiarGrafo() { return btnLimpiarGrafo; }

    private Color colorEtapa(int etapa) {
        Color[] paleta = temaOscuro ? PALETA_MENU_OSCURO : PALETA_MENU;
        return paleta[(Math.max(1, etapa) - 1) % paleta.length];
    }

    /** Franja destacada: pasos numerados con folio y título, agrupados por etapa, en varias líneas. */
    private class OrdenEjecucionPanel extends JComponent implements Scrollable {
        private Map<Integer, Tarea> tareas = Map.of();
        private List<Integer> orden = List.of();
        private Map<Integer, Integer> etapas = Map.of();
        private static final int PASO_ALTO = 46, SEP = 26;

        OrdenEjecucionPanel() { putClientProperty(TEMA_PROPIO, true); }

        void setDatos(Map<Integer, Tarea> tareas, List<Integer> orden, Map<Integer, Integer> etapas) {
            this.tareas = tareas; this.orden = orden; this.etapas = etapas;
            revalidate(); repaint();
        }

        private String titulo(int id) {
            Tarea t = tareas.get(id);
            String tit = t == null ? "" : t.getTitulo();
            return tit.length() > 22 ? tit.substring(0, 21) + "…" : tit;
        }

        private int anchoPaso(FontMetrics fmTitulo, int id) {
            return Math.max(120, fmTitulo.stringWidth(titulo(id)) + 58);
        }

        @Override public Dimension getPreferredSize() {
            int ancho = getParent() != null ? Math.max(200, getParent().getWidth()) : 600;
            FontMetrics fm = getFontMetrics(new Font("Segoe UI", Font.PLAIN, 11));
            int x = 12, lineas = 1;
            for (int id : orden) {
                int a = anchoPaso(fm, id);
                if (x + a > ancho - 12 && x > 12) { lineas++; x = 12; }
                x += a + SEP;
            }
            return new Dimension(ancho, 40 + lineas * (PASO_ALTO + 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(temaOscuro ? new Color(51, 65, 85) : COLOR_TARJETA);
            g.fillRect(0, 0, getWidth(), getHeight());
            Color texto = temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK;
            Color suave = temaOscuro ? new Color(203, 213, 225) : COLOR_NEUTRO;

            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(suave);
            if (orden.isEmpty()) {
                g.drawString("Agrega dependencias entre tareas activas para calcular en qué orden deben resolverse.", 12, 22);
                g.dispose();
                return;
            }
            int totalEtapas = 0;
            for (int e : etapas.values()) totalEtapas = Math.max(totalEtapas, e);
            g.drawString("Resuelve las tareas en este orden exacto para no bloquear ninguna dependencia  ·  "
                    + orden.size() + " tareas en " + totalEtapas + (totalEtapas == 1 ? " etapa" : " etapas")
                    + " (las de la misma etapa no dependen entre sí).", 12, 18);

            Font fTitulo = new Font("Segoe UI", Font.PLAIN, 11);
            FontMetrics fmT = g.getFontMetrics(fTitulo);
            int x = 12, y = 30;
            for (int i = 0; i < orden.size(); i++) {
                int id = orden.get(i), a = anchoPaso(fmT, id);
                if (x + a > getWidth() - 12 && x > 12) { x = 12; y += PASO_ALTO + 14; }
                int etapa = etapas.getOrDefault(id, 1);
                Color c = colorEtapa(etapa);
                // Tarjeta del paso
                g.setColor(temaOscuro ? new Color(30, 41, 59) : new Color(248, 250, 252));
                g.fillRoundRect(x, y, a, PASO_ALTO, 12, 12);
                g.setColor(c);
                g.setStroke(new BasicStroke(1.6f));
                g.drawRoundRect(x, y, a, PASO_ALTO, 12, 12);
                // Círculo con el número de paso
                g.fillOval(x + 8, y + 9, 28, 28);
                g.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g.setColor(Color.WHITE);
                String num = String.valueOf(i + 1);
                FontMetrics fmN = g.getFontMetrics();
                g.drawString(num, x + 22 - fmN.stringWidth(num) / 2, y + 23 + (fmN.getAscent() - fmN.getDescent()) / 2 - 5);
                // Folio + título + etapa
                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.setColor(texto);
                g.drawString("#" + id, x + 44, y + 18);
                int xEtapa = x + 44 + g.getFontMetrics().stringWidth("#" + id) + 6;
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.setColor(c);
                g.drawString("Etapa " + etapa, xEtapa, y + 18);
                g.setFont(fTitulo);
                g.setColor(suave);
                g.drawString(titulo(id), x + 44, y + 35);
                // Flecha al siguiente paso
                if (i < orden.size() - 1) {
                    int ax = x + a + 5, ay = y + PASO_ALTO / 2;
                    g.setColor(suave);
                    g.setStroke(new BasicStroke(1.6f));
                    g.drawLine(ax, ay, ax + SEP - 12, ay);
                    g.fillPolygon(new int[]{ax + SEP - 9, ax + SEP - 15, ax + SEP - 15}, new int[]{ay, ay - 5, ay + 5}, 3);
                }
                x += a + SEP;
            }
            g.dispose();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(600, 150); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 12; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /**
     * Dibujo del grafo dirigido por columnas: cada columna es una etapa (izquierda = se puede
     * empezar ya). Las flechas van de la tarea previa a la siguiente.
     */
    private class GrafoVisual extends JComponent {
        private static final int NODO_ANCHO = 150, NODO_ALTO = 50, SEP_COL = 90, SEP_FILA = 22, MARGEN = 30;
        private Map<Integer, Tarea> tareas = Map.of();
        private List<int[]> aristas = List.of();
        private Map<Integer, Integer> pasos = Map.of();
        private Map<Integer, Integer> etapas = Map.of();
        private final Map<Integer, Rectangle> posiciones = new java.util.HashMap<>();
        private int columnas, filasMax, carriles, yCarril;
        private final Map<String, Integer> carrilDeArista = new java.util.HashMap<>();
        private static final int SEP_CARRIL = 12;

        GrafoVisual() { putClientProperty(TEMA_PROPIO, true); }

        void setDatos(Map<Integer, Tarea> tareas, List<int[]> aristas, List<Integer> orden, Map<Integer, Integer> etapas) {
            this.tareas = tareas; this.aristas = aristas; this.etapas = etapas;
            Map<Integer, Integer> p = new java.util.HashMap<>();
            for (int i = 0; i < orden.size(); i++) p.put(orden.get(i), i + 1);
            this.pasos = p;
            // Agrupa por etapa respetando el orden de ejecución dentro de cada columna
            Map<Integer, List<Integer>> porEtapa = new java.util.TreeMap<>();
            for (int id : orden) porEtapa.computeIfAbsent(etapas.getOrDefault(id, 1), k -> new java.util.ArrayList<>()).add(id);
            posiciones.clear();
            columnas = porEtapa.size(); filasMax = 0;
            for (List<Integer> col : porEtapa.values()) filasMax = Math.max(filasMax, col.size());

            // Reduce cruces: cada tarea se acomoda cerca de la altura promedio de sus requisitos
            Map<Integer, Double> filaDe = new java.util.HashMap<>();
            for (List<Integer> col : porEtapa.values()) {
                for (int id : col) {
                    double suma = 0; int n = 0;
                    for (int[] a : aristas) if (a[1] == id && filaDe.containsKey(a[0])) { suma += filaDe.get(a[0]); n++; }
                    filaDe.put(id, n == 0 ? Double.MAX_VALUE : suma / n);
                }
                java.util.List<Integer> copia = new java.util.ArrayList<>(col);
                col.sort(java.util.Comparator.comparingDouble((Integer id) -> filaDe.get(id)).thenComparingInt(copia::indexOf));
                for (int f = 0; f < col.size(); f++) filaDe.put(col.get(f), (double) f);
            }
            int c = 0;
            for (List<Integer> col : porEtapa.values()) {
                int altoCol = col.size() * NODO_ALTO + (col.size() - 1) * SEP_FILA;
                int altoTotal = filasMax * NODO_ALTO + (filasMax - 1) * SEP_FILA;
                int y0 = MARGEN + 22 + (altoTotal - altoCol) / 2;      // centra verticalmente cada columna
                for (int f = 0; f < col.size(); f++) {
                    posiciones.put(col.get(f), new Rectangle(MARGEN + c * (NODO_ANCHO + SEP_COL),
                            y0 + f * (NODO_ALTO + SEP_FILA), NODO_ANCHO, NODO_ALTO));
                }
                c++;
            }
            // Las flechas que saltan etapas viajan por carriles debajo de los nodos (no los atraviesan)
            carrilDeArista.clear();
            carriles = 0;
            yCarril = MARGEN + 22 + filasMax * NODO_ALTO + Math.max(0, filasMax - 1) * SEP_FILA + 20;
            for (int[] a : aristas) {
                if (etapas.getOrDefault(a[1], 1) - etapas.getOrDefault(a[0], 1) > 1) {
                    carrilDeArista.put(a[0] + ">" + a[1], carriles++);
                }
            }
            revalidate(); repaint();
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(Math.max(300, MARGEN * 2 + columnas * NODO_ANCHO + Math.max(0, columnas - 1) * SEP_COL),
                    Math.max(160, MARGEN * 2 + 22 + filasMax * NODO_ALTO + Math.max(0, filasMax - 1) * SEP_FILA
                            + (carriles > 0 ? 20 + carriles * SEP_CARRIL : 0)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(temaOscuro ? new Color(15, 23, 42) : new Color(248, 250, 252));
            g.fillRect(0, 0, getWidth(), getHeight());
            Color suave = temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO;
            if (posiciones.isEmpty()) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g.setColor(suave);
                g.drawString("El grafo aparecerá aquí al registrar la primera dependencia.", 20, 40);
                g.dispose();
                return;
            }
            // Encabezados de columna (etapas)
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            for (int c = 0; c < columnas; c++) {
                String t = c == 0 ? "ETAPA 1 · puede empezar ya" : "ETAPA " + (c + 1);
                g.setColor(colorEtapa(c + 1));
                g.drawString(t, MARGEN + c * (NODO_ANCHO + SEP_COL), MARGEN + 6);
            }
            // Flechas (curvas) previa -> siguiente
            for (int[] a : aristas) {
                Rectangle r1 = posiciones.get(a[0]), r2 = posiciones.get(a[1]);
                if (r1 == null || r2 == null) continue;
                int x1 = r1.x + r1.width, y1 = r1.y + r1.height / 2, x2 = r2.x - 4, y2 = r2.y + r2.height / 2;
                Color c = colorEtapa(etapas.getOrDefault(a[0], 1));
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
                g.setStroke(new BasicStroke(2f));
                Integer carril = carrilDeArista.get(a[0] + ">" + a[1]);
                if (carril == null) {                           // etapa contigua: curva directa
                    int dx = Math.max(40, (x2 - x1) / 2);
                    g.draw(new java.awt.geom.CubicCurve2D.Float(x1, y1, x1 + dx, y1, x2 - dx, y2, x2, y2));
                } else {                                        // salta etapas: baja al carril, avanza y sube
                    int yc = yCarril + carril * SEP_CARRIL, h = 36;
                    java.awt.geom.Path2D.Float ruta = new java.awt.geom.Path2D.Float();
                    ruta.moveTo(x1, y1);
                    ruta.curveTo(x1 + h, y1, x1 + h / 2f, yc, x1 + h * 1.5f, yc);
                    ruta.lineTo(x2 - h * 1.5f, yc);
                    ruta.curveTo(x2 - h / 2f, yc, x2 - h, y2, x2, y2);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{7f, 5f}, 0f));
                    g.draw(ruta);
                    g.setStroke(new BasicStroke(2f));
                }
                g.fillPolygon(new int[]{x2 + 4, x2 - 7, x2 - 7}, new int[]{y2, y2 - 6, y2 + 6}, 3);
            }
            // Nodos
            for (Map.Entry<Integer, Rectangle> e : posiciones.entrySet()) {
                int id = e.getKey();
                Rectangle r = e.getValue();
                Color c = colorEtapa(etapas.getOrDefault(id, 1));
                g.setColor(temaOscuro ? new Color(30, 41, 59) : Color.WHITE);
                g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
                g.setColor(c);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
                g.fillRoundRect(r.x, r.y, 6, r.height, 6, 6);

                Tarea t = tareas.get(id);
                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.setColor(temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK);
                g.drawString("#" + id, r.x + 14, r.y + 19);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g.setColor(suave);
                g.drawString(recortar(g, t == null ? "" : t.getTitulo(), r.width - 22), r.x + 14, r.y + 37);

                Integer paso = pasos.get(id);                   // número de paso en el orden calculado
                if (paso != null) {
                    g.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    String ps = "Paso " + paso;
                    int bw = g.getFontMetrics().stringWidth(ps) + 12;
                    g.setColor(c);
                    g.fillRoundRect(r.x + r.width - bw - 6, r.y + 6, bw, 17, 17, 17);
                    g.setColor(Color.WHITE);
                    g.drawString(ps, r.x + r.width - bw, r.y + 18);
                }
            }
            g.dispose();
        }
    }

    private JPanel crearCardTodas() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloTodas = new DefaultTableModel(new String[]{"ID", "Título", "Departamento", COL_RESPONSABLE,
                "Urgencia", COL_FECHA_ENTREGA, "Estructura"}, 0);
        tablaTodas = crearTablaEstilizada(modeloTodas);
        JScrollPane scrollTabla = new JScrollPane(tablaTodas);
        scrollTabla.setBorder(crearBordeSeccion(" Consolidado General de Tareas ", 14));

        JPanel panelTopAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); panelTopAction.setOpaque(false);
        btnVerOrdenadas = crearBotonEstilizado("Ver Todas (Ordenadas por Urgencia/Depto)", COLOR_TEXTO_DARK, Color.WHITE);
        btnCambiarTema = crearBotonEstilizado("Cambiar a modo oscuro", new Color(15, 23, 42), Color.WHITE);
        btnCambiarTema.addActionListener(e -> cambiarTema());
        panelTopAction.add(btnVerOrdenadas); panelTopAction.add(btnCambiarTema);

        areaConsolaGUI = new JTextArea(6, 80); areaConsolaGUI.setEditable(false);
        areaConsolaGUI.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaConsolaGUI.setBackground(new Color(15, 23, 42)); areaConsolaGUI.setForeground(new Color(34, 197, 94));
        JScrollPane scrollConsola = new JScrollPane(areaConsolaGUI);
        scrollConsola.setBorder(crearBordeSeccion(" Consola de Eventos en Tiempo Real ", 12));

        JPanel panelCentro = new JPanel(new BorderLayout(5, 5)); panelCentro.setOpaque(false);
        panelCentro.add(panelTopAction, BorderLayout.NORTH); panelCentro.add(scrollTabla, BorderLayout.CENTER);

        panel.add(panelCentro, BorderLayout.CENTER); panel.add(scrollConsola, BorderLayout.SOUTH);
        return panel;
    }

    private JTable crearTablaEstilizada(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12)); table.setRowHeight(26);
        table.setGridColor(COLOR_BORDE); table.setSelectionBackground(new Color(219, 234, 254));
        JTableHeader header = table.getTableHeader(); header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(COLOR_TEXTO_DARK); header.setForeground(Color.WHITE);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
        configurarColumnasTareas(table);
        return table;
    }

    // Instala los renderers de "Responsable Directo" y "Fecha de Entrega" en las tablas que tengan esas columnas
    private void configurarColumnasTareas(JTable table) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            String nombre = table.getColumnName(i);
            if (COL_FECHA_ENTREGA.equals(nombre)) {
                table.getColumnModel().getColumn(i).setCellRenderer(new FechaEntregaRenderer());
                table.getColumnModel().getColumn(i).setPreferredWidth(230);
            } else if (COL_RESPONSABLE.equals(nombre)) {
                table.getColumnModel().getColumn(i).setCellRenderer(new ResponsableRenderer());
                table.getColumnModel().getColumn(i).setPreferredWidth(150);
            } else if ("ID".equals(nombre)) {
                table.getColumnModel().getColumn(i).setPreferredWidth(40);
            }
        }
    }

    /**
     * Recibe un LocalDate y muestra "dd/MM/yyyy (N días restantes)".
     * Si la fecha ya venció, el texto se pinta en rojo con el tiempo transcurrido.
     * Como se calcula al pintar, los días se mantienen correctos respecto a la fecha actual.
     */
    private class FechaEntregaRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object valor, boolean seleccionado,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(table, valor, seleccionado, foco, fila, columna);
            setFont(table.getFont());
            Color colorTexto = seleccionado ? table.getSelectionForeground() : table.getForeground();
            if (valor instanceof LocalDate fecha) {
                setText(Tarea.describirFechaEntrega(fecha));
                if (Tarea.calcularDiasRestantes(fecha) < 0) {
                    colorTexto = temaOscuro ? COLOR_ROJO_OSCURO_TEMA : COLOR_ROJO;
                    setFont(table.getFont().deriveFont(Font.BOLD));
                }
            }
            setForeground(colorTexto);
            return this;
        }
    }

    // Muestra "Sin Asignar" en cursiva y gris para distinguirlo de un empleado real
    private class ResponsableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object valor, boolean seleccionado,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(table, valor, seleccionado, foco, fila, columna);
            boolean sinAsignar = valor == null || "Sin Asignar".equals(valor);
            setText(sinAsignar ? "Sin Asignar" : valor.toString());
            setFont(sinAsignar ? table.getFont().deriveFont(Font.ITALIC) : table.getFont());
            if (seleccionado) {
                setForeground(table.getSelectionForeground());
            } else if (sinAsignar) {
                setForeground(temaOscuro ? new Color(148, 163, 184) : COLOR_NEUTRO);
            } else {
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    // Expande / contrae la sección de detalle de tareas pendientes
    private void alternarDetallePendientes() {
        detallePendientesVisible = !detallePendientesVisible;
        JComponent detalle = (JComponent) splitEmpleados.getBottomComponent();
        Component tablaDetalle = ((BorderLayout) detalle.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        tablaDetalle.setVisible(detallePendientesVisible);
        btnTogglePendientes.setText(detallePendientesVisible ? "▼ Ocultar detalle" : "▲ Mostrar detalle");
        if (detallePendientesVisible) {
            splitEmpleados.setDividerLocation(0.55);
        } else {
            // Deja visible solo la cabecera (título + botón) para poder volver a expandir
            Component cabecera = ((BorderLayout) detalle.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            splitEmpleados.setDividerLocation(splitEmpleados.getHeight() - splitEmpleados.getDividerSize()
                    - cabecera.getPreferredSize().height - 6);
        }
        splitEmpleados.revalidate();
    }

    // Pinta una "píldora" redondeada con texto (usado por los badges)
    private static int pintarBadge(Graphics2D g, int x, int altoCelda, String texto, Color fondo, Color textoColor) {
        FontMetrics fm = g.getFontMetrics();
        int ancho = fm.stringWidth(texto) + 16, alto = Math.min(20, altoCelda - 6);
        int y = (altoCelda - alto) / 2;
        g.setColor(fondo);
        g.fillRoundRect(x, y, ancho, alto, alto, alto);
        g.setColor(textoColor);
        g.drawString(texto, x + 8, y + (alto + fm.getAscent() - fm.getDescent()) / 2);
        return x + ancho + 6;
    }

    /** Columna "Tareas Pendientes": badge con el total y, si aplica, badges de vencidas y críticas. */
    private class PendientesBadgeRenderer extends JComponent implements javax.swing.table.TableCellRenderer {
        private ResumenPendientes resumen;
        private boolean seleccionado;
        private JTable tabla;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object valor, boolean isSelected,
                                                       boolean foco, int fila, int columna) {
            this.tabla = table;
            this.resumen = valor instanceof ResumenPendientes r ? r : new ResumenPendientes(0, 0, 0);
            this.seleccionado = isSelected;
            setToolTipText(resumen.total() == 0 ? "Este empleado no tiene tareas pendientes"
                    : resumen.total() + " pendiente(s), " + resumen.vencidas() + " vencida(s), "
                      + resumen.criticas() + " crítica(s) (urgencia 5)");
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(seleccionado ? tabla.getSelectionBackground() : tabla.getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            int x = 6;
            if (resumen.total() == 0) {
                x = pintarBadge(g, x, getHeight(), "Sin pendientes",
                        temaOscuro ? new Color(71, 85, 105) : new Color(226, 232, 240),
                        temaOscuro ? new Color(203, 213, 225) : COLOR_NEUTRO);
            } else {
                x = pintarBadge(g, x, getHeight(), resumen.toString(),
                        temaOscuro ? new Color(30, 64, 175) : new Color(219, 234, 254),
                        temaOscuro ? Color.WHITE : new Color(30, 64, 175));
                if (resumen.vencidas() > 0) {
                    x = pintarBadge(g, x, getHeight(), resumen.vencidas() + (resumen.vencidas() == 1 ? " vencida" : " vencidas"),
                            temaOscuro ? new Color(127, 29, 29) : new Color(254, 226, 226),
                            temaOscuro ? new Color(254, 202, 202) : COLOR_ROJO);
                }
                if (resumen.criticas() > 0) {
                    pintarBadge(g, x, getHeight(), resumen.criticas() + (resumen.criticas() == 1 ? " crítica" : " críticas"),
                            temaOscuro ? new Color(124, 45, 18) : new Color(255, 237, 213),
                            temaOscuro ? new Color(254, 215, 170) : new Color(194, 65, 12));
                }
            }
            g.dispose();
        }
    }

    /** Columna "Urgencia" del detalle: badge de color según el nivel (1-5). */
    private class UrgenciaBadgeRenderer extends JComponent implements javax.swing.table.TableCellRenderer {
        private int urgencia;
        private boolean seleccionado;
        private JTable tabla;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object valor, boolean isSelected,
                                                       boolean foco, int fila, int columna) {
            this.tabla = table;
            this.urgencia = valor instanceof Integer u ? u : 0;
            this.seleccionado = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(seleccionado ? tabla.getSelectionBackground() : tabla.getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            Color fondo, texto;
            if (urgencia >= 5)      { fondo = new Color(220, 38, 38);  texto = Color.WHITE; }
            else if (urgencia == 4) { fondo = new Color(234, 88, 12);  texto = Color.WHITE; }
            else if (urgencia == 3) { fondo = new Color(250, 204, 21); texto = COLOR_TEXTO_DARK; }
            else                    { fondo = temaOscuro ? new Color(71, 85, 105) : new Color(226, 232, 240);
                                      texto = temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK; }
            pintarBadge(g, 6, getHeight(), "Urgencia " + urgencia, fondo, texto);
            g.dispose();
        }
    }

    private JButton crearBotonEstilizado(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto); btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(bg); btn.setForeground(fg); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        return btn;
    }

    private void cambiarTema() {
        temaOscuro = !temaOscuro;
        aplicarTemaGeneral();
        if (btnCambiarTema != null) {
            btnCambiarTema.setText(temaOscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
        }
    }

    private void aplicarTemaGeneral() {
        Color fondoApp = temaOscuro ? new Color(15, 23, 42) : COLOR_FONDO_APP;
        Color panelOscuro = temaOscuro ? new Color(30, 41, 59) : COLOR_TARJETA;
        Color fondoTarjeta = temaOscuro ? new Color(51, 65, 85) : COLOR_TARJETA;
        Color textoClaro = temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK;
        Color textoSuave = temaOscuro ? new Color(203, 213, 225) : COLOR_NEUTRO;

        if (panelSidebar != null) {
            panelSidebar.setBackground(temaOscuro ? new Color(15, 23, 42) : COLOR_SIDEBAR_BG);
        }
        if (panelContenidoCards != null) {
            panelContenidoCards.setBackground(fondoApp);
            panelContenidoCards.setOpaque(temaOscuro);
        }

        if (botonNavActivo != null) {
            establecerBotonNavActivo(botonNavActivo);
        }

        aplicarTemaRecursivo(panelContenidoCards, fondoTarjeta, textoClaro, textoSuave, panelOscuro);
        aplicarTemaRecursivo(panelSidebar, fondoTarjeta, textoClaro, textoSuave, panelOscuro);

        if (panelSidebar != null) {
            panelSidebar.setBackground(temaOscuro ? new Color(15, 23, 42) : COLOR_SIDEBAR_BG);
        }

        if (!temaOscuro && panelContenidoCards != null) {
            panelContenidoCards.setBackground(COLOR_FONDO_APP);
            panelContenidoCards.setOpaque(false);
        }

        if (btnCambiarTema != null) {
            btnCambiarTema.setForeground(Color.WHITE);
            btnCambiarTema.setBackground(temaOscuro ? new Color(59, 130, 246) : new Color(15, 23, 42));
        }

        if (btnModoVentana != null) {
            actualizarColorBotonVentana();
        }
        repaint(); // repinta también los componentes con tema propio
    }

    private void aplicarTemaRecursivo(Container contenedor, Color fondoTarjeta, Color textoClaro, Color textoSuave, Color panelOscuro) {
        if (contenedor == null) return;

        if (contenedor instanceof JComponent componenteRaiz) {
            guardarColoresOriginales(componenteRaiz);
            if (componenteRaiz instanceof JPanel panel) {
                panel.setBackground(temaOscuro ? fondoTarjeta : COLOR_TARJETA);
                panel.setOpaque(temaOscuro || opacidadOriginal(panel));
            }
        }

        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JComponent propio && Boolean.TRUE.equals(propio.getClientProperty(TEMA_PROPIO))) {
                continue; // tarjetas KPI, alerta y encabezados leen 'temaOscuro' al pintarse
            }
            if (componente instanceof JComponent componenteSwing) {
                guardarColoresOriginales(componenteSwing);
            }

            if (componente instanceof JPanel panel) {
                panel.setBackground(temaOscuro ? fondoTarjeta : COLOR_TARJETA);
                panel.setOpaque(temaOscuro || opacidadOriginal(panel));
                aplicarTemaRecursivo(panel, fondoTarjeta, textoClaro, textoSuave, panelOscuro);
            } else if (componente instanceof JLabel label) {
                label.setForeground(temaOscuro ? textoClaro : COLOR_TEXTO_DARK);
            } else if (componente instanceof JTextField field) {
                field.setBackground(temaOscuro ? new Color(30, 41, 59) : Color.WHITE);
                field.setForeground(temaOscuro ? Color.WHITE : Color.BLACK);
                field.setCaretColor(temaOscuro ? Color.WHITE : Color.BLACK);
            } else if (componente instanceof JTextArea area) {
                area.setBackground(temaOscuro ? new Color(15, 23, 42) : Color.WHITE);
                area.setForeground(temaOscuro ? new Color(236, 253, 245) : Color.BLACK);
            } else if (componente instanceof JComboBox<?> combo) {
                aplicarEstiloCombo(combo);
            } else if (componente instanceof JTable table) {
                table.setBackground(temaOscuro ? new Color(51, 65, 85) : Color.WHITE);
                table.setForeground(temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK);
                table.setSelectionBackground(temaOscuro ? new Color(59, 130, 246) : new Color(219, 234, 254));
                table.setSelectionForeground(temaOscuro ? Color.WHITE : Color.BLACK);
                table.setGridColor(temaOscuro ? new Color(71, 85, 105) : COLOR_BORDE);
                JTableHeader header = table.getTableHeader();
                header.setBackground(temaOscuro ? new Color(30, 41, 59) : COLOR_TEXTO_DARK);
                header.setForeground(Color.WHITE);
            } else if (componente instanceof JScrollPane scroll) {
                scroll.getViewport().setBackground(temaOscuro ? new Color(51, 65, 85) : Color.WHITE);
                scroll.setBackground(temaOscuro ? new Color(51, 65, 85) : Color.WHITE);
            } else if (componente instanceof JButton button) {
                if (button != btnCambiarTema && button != botonNavActivo && button != btnModoVentana) {
                    button.setBackground(temaOscuro ? new Color(71, 85, 105) : colorOriginal(button, "tema.background"));
                    button.setForeground(temaOscuro ? Color.WHITE : colorOriginal(button, "tema.foreground"));
                }
            }

            if (componente instanceof Container contenedorHijo && !(componente instanceof JPanel)) {
                aplicarTemaRecursivo(contenedorHijo, fondoTarjeta, textoClaro, textoSuave, panelOscuro);
            }
        }
    }

    private void guardarColoresOriginales(JComponent componente) {
        if (componente.getClientProperty("tema.background") == null) {
            componente.putClientProperty("tema.background", componente.getBackground());
            componente.putClientProperty("tema.foreground", componente.getForeground());
            componente.putClientProperty("tema.opaque", componente.isOpaque());
        }
    }

    private Color colorOriginal(JComponent componente, String propiedad) {
        return (Color) componente.getClientProperty(propiedad);
    }

    private boolean opacidadOriginal(JComponent componente) {
        return Boolean.TRUE.equals(componente.getClientProperty("tema.opaque"));
    }

    private void aplicarEstiloCombo(JComboBox<?> combo) {
        Color fondo = temaOscuro ? new Color(30, 41, 59) : Color.WHITE;
        Color texto = temaOscuro ? Color.WHITE : COLOR_TEXTO_DARK;

        combo.setBackground(fondo);
        combo.setForeground(texto);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean tieneFoco) {
                JLabel etiqueta = (JLabel) super.getListCellRendererComponent(
                        lista, valor, indice, seleccionado, tieneFoco);
                etiqueta.setBackground(seleccionado && temaOscuro ? new Color(59, 130, 246) : fondo);
                etiqueta.setForeground(seleccionado && temaOscuro ? Color.WHITE : texto);
                return etiqueta;
            }
        });
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new BasicArrowButton(
                    SwingConstants.SOUTH, fondo, fondo, texto, fondo);
            }
        });
    }

    private CompoundBorder crearBordeSeccion(String titulo, int tamanoFuente) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDE, 1),
                titulo, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, tamanoFuente), COLOR_TEXTO_DARK
        );
        return BorderFactory.createCompoundBorder(titledBorder, new EmptyBorder(4, 6, 6, 6));
    }

    private void estilarCampoTexto(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDE, 1), BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
    }

    // =========================================================================
    // Métodos de Gestión de Pantalla Completa y Ventana Flotante
    // =========================================================================

    private void calcularTamanoInicial() {
        Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int screenWidth = maxBounds.width;
        int screenHeight = maxBounds.height;

        int targetWidth = 1180;
        int targetHeight = 820;

        if (targetWidth >= screenWidth || targetHeight >= screenHeight) {
            targetWidth = Math.max(900, (int) (screenWidth * 0.94));
            targetHeight = Math.max(650, (int) (screenHeight * 0.94));
            setMinimumSize(new Dimension(Math.min(880, screenWidth), Math.min(620, screenHeight)));
        } else {
            setMinimumSize(new Dimension(980, 680));
        }

        dimensionesFlotante = new Dimension(targetWidth, targetHeight);
        setSize(dimensionesFlotante);
        setLocationRelativeTo(null);
        posicionFlotante = getLocation();
    }

    private void configurarListenersVentana() {
        addWindowStateListener(e -> {
            int nuevoEstado = e.getNewState();
            boolean maximizado = (nuevoEstado & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            actualizarEstadoBotonVentana(maximizado);
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!esVentanaCompleta && (getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    dimensionesFlotante = getSize();
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (!esVentanaCompleta && (getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    posicionFlotante = getLocation();
                }
            }
        });
    }

    private void configurarAtajosTeclado() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "togglePantallaCompleta");
        actionMap.put("togglePantallaCompleta", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alternarModoVentana();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "salirPantallaCompleta");
        actionMap.put("salirPantallaCompleta", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (esVentanaCompleta) {
                    restaurarVentanaFlotante();
                }
            }
        });
    }

    public void alternarModoVentana() {
        if (esVentanaCompleta) {
            restaurarVentanaFlotante();
        } else {
            activarVentanaCompleta();
        }
    }

    public void activarVentanaCompleta() {
        if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
            dimensionesFlotante = getSize();
            posicionFlotante = getLocation();
        }
        setExtendedState(Frame.MAXIMIZED_BOTH);
        actualizarEstadoBotonVentana(true);
        logGUI("[VENTANA] Modo Pantalla Completa activado. Adaptado automáticamente al tamaño de la pantalla.");
    }

    public void restaurarVentanaFlotante() {
        setExtendedState(Frame.NORMAL);
        if (dimensionesFlotante != null) {
            setSize(dimensionesFlotante);
        }
        if (posicionFlotante != null) {
            setLocation(posicionFlotante);
        } else {
            setLocationRelativeTo(null);
        }
        actualizarEstadoBotonVentana(false);
        logGUI("[VENTANA] Modo Ventana Flotante restaurado.");
    }

    private void actualizarEstadoBotonVentana(boolean maximizado) {
        this.esVentanaCompleta = maximizado;
        if (btnModoVentana != null) {
            if (maximizado) {
                btnModoVentana.setText("🗗 Ventana Flotante");
                btnModoVentana.setToolTipText("Restaurar a ventana flotante (F11 o Esc)");
            } else {
                btnModoVentana.setText("🗖 Pantalla Completa");
                btnModoVentana.setToolTipText("Poner en ventana completa ajustada a la pantalla (F11)");
            }
        }
    }

    private void actualizarColorBotonVentana() {
        if (btnModoVentana != null) {
            btnModoVentana.setBackground(temaOscuro ? new Color(30, 41, 59) : new Color(51, 65, 85));
            btnModoVentana.setForeground(Color.WHITE);
            btnModoVentana.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(temaOscuro ? new Color(71, 85, 105) : new Color(100, 116, 139), 1),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
        }
    }

    // Getters
    public String getTituloInput() { return txtTitulo.getText().trim(); }
    public void limpiarTituloInput() { txtTitulo.setText(""); }
    public String getDepartamentoSeleccionado() { return (String) cbDepartamento.getSelectedItem(); }
    public int getUrgenciaSeleccionada() { return (Integer) cbUrgencia.getSelectedItem(); }
    public String getEstructuraSeleccionada() { return (String) cbEstructura.getSelectedItem(); }
    public int getTiempoEstimadoInput() {
        try { return Integer.parseInt(txtTiempoEstimado.getText().trim()); } catch (Exception e) { return 2; }
    }
    public SelectorFechaPanel getSelectorFecha() { return selectorFecha; }

    // --- Responsable Directo ---
    public void addCambioDepartamentoListener(ActionListener listener) { cbDepartamento.addActionListener(listener); }

    // Devuelve el Empleado elegido, o null si se dejó "Sin Asignar"
    public Empleado getResponsableSeleccionado() {
        Object seleccionado = cbResponsable.getSelectedItem();
        return seleccionado instanceof Empleado empleado ? empleado : null;
    }

    // Recarga el selector con los empleados del departamento de la tarea, conservando la selección si sigue siendo válida
    public void setResponsablesDisponibles(List<Empleado> empleados) {
        Empleado previo = getResponsableSeleccionado();
        cbResponsable.removeAllItems();
        cbResponsable.addItem(OPCION_SIN_ASIGNAR);
        Object aSeleccionar = OPCION_SIN_ASIGNAR;
        for (Empleado empleado : empleados) {
            cbResponsable.addItem(empleado);
            if (previo != null && previo.getId().equals(empleado.getId())) {
                aSeleccionar = empleado;
            }
        }
        cbResponsable.setSelectedItem(aSeleccionar);
        cbResponsable.setToolTipText(empleados.isEmpty()
                ? "No hay empleados registrados en " + getDepartamentoSeleccionado()
                : "Solo se listan empleados de " + getDepartamentoSeleccionado());
    }

    public void limpiarResponsable() { cbResponsable.setSelectedItem(OPCION_SIN_ASIGNAR); }

    public String getEmpleadoIdInput() { return txtEmpleadoId.getText().trim(); }
    public String getEmpleadoNombreInput() { return txtEmpleadoNombre.getText().trim(); }
    public String getEmpleadoDeptoSeleccionado() { return (String) cbEmpleadoDepto.getSelectedItem(); }
    public String getBuscarEmpleadoIdInput() { return txtBuscarEmpleadoId.getText().trim(); }
    public String getFiltroDeptoEmpSeleccionado() { return (String) cbFiltroDeptoEmp.getSelectedItem(); }
    public String getFiltroDeptoListaSeleccionado() { return (String) cbFiltroDeptoLista.getSelectedItem(); }


    public DefaultTableModel getModeloPila() { return modeloPila; }
    public DefaultTableModel getModeloCola() { return modeloCola; }
    public DefaultTableModel getModeloLista() { return modeloLista; }
    public DefaultTableModel getModeloPrioridad() { return modeloPrioridad; }
    public DefaultTableModel getModeloEmpleados() { return modeloEmpleados; }
    public DefaultTableModel getModeloPendientesEmpleado() { return modeloPendientesEmpleado; }

    // --- Detalle de tareas pendientes por empleado ---
    public void addSeleccionEmpleadoListener(ListSelectionListener listener) {
        tablaEmpleados.getSelectionModel().addListSelectionListener(listener);
    }

    /** ID del empleado seleccionado en la tabla, o null si no hay selección. */
    public String getEmpleadoIdSeleccionado() {
        int fila = tablaEmpleados.getSelectedRow();
        return fila < 0 ? null : String.valueOf(modeloEmpleados.getValueAt(fila, 0));
    }

    /** Vuelve a seleccionar al empleado (tras refrescar la tabla). Devuelve false si ya no está en la tabla. */
    public boolean seleccionarEmpleado(String id) {
        if (id == null) return false;
        for (int i = 0; i < modeloEmpleados.getRowCount(); i++) {
            if (id.equals(String.valueOf(modeloEmpleados.getValueAt(i, 0)))) {
                tablaEmpleados.setRowSelectionInterval(i, i);
                return true;
            }
        }
        return false;
    }

    public void setTituloPendientes(String texto) { lblTituloPendientes.setText(texto); }
    public DefaultTableModel getModeloTodas() { return modeloTodas; }

    public JButton getBtnAgregar() { return btnAgregar; }
    public JButton getBtnPopPila() { return btnPopPila; }
    public JButton getBtnPeekPila() { return btnPeekPila; }
    public JButton getBtnDequeueCola() { return btnDequeueCola; }
    public JButton getBtnFrontCola() { return btnFrontCola; }
    public JButton getBtnEliminarLista() { return btnEliminarLista; }
    public JButton getBtnBuscarDepto() { return btnBuscarDepto; }
    public JButton getBtnVerOrdenadas() { return btnVerOrdenadas; }

    public JButton getBtnExtraerPrioridad() { return btnExtraerPrioridad; }
    public JButton getBtnVerPrioridad() { return btnVerPrioridad; }
    public JButton getBtnAgregarEmpleado() { return btnAgregarEmpleado; }
    public JButton getBtnBuscarEmpleadoId() { return btnBuscarEmpleadoId; }
    public JButton getBtnListarEmpleadoDepto() { return btnListarEmpleadoDepto; }
    public JButton getBtnMostrarTodosEmpleados() { return btnMostrarTodosEmpleados; }
    public JButton getBtnCalcularTiempoRecursivo() { return btnCalcularTiempoRecursivo; }
    public JButton getBtnDistribuirDivideVenceras() { return btnDistribuirDivideVenceras; }
    public JButton getBtnQuickSortUrgencia() { return btnQuickSortUrgencia; }
    public JButton getBtnAgregarDependencia() { return btnAgregarDependencia; }
    public JButton getBtnCalcularOrdenTopologico() { return btnCalcularOrdenTopologico; }
    public JButton getBtnModoVentana() { return btnModoVentana; }
    public boolean isVentanaCompleta() { return esVentanaCompleta; }


    public void actualizarDashboard(int pilaAct, int pilaRes, int colaAct, int colaRes,
                                    int listaAct, int listaRes, Map<String, Integer> horas,
                                    Map<String, Integer> tareas) {
        lblPilaMetricas.setText("Pendientes: " + pilaAct + "  |  Resueltas: " + pilaRes);
        lblColaMetricas.setText("Pendientes: " + colaAct + "  |  Resueltas: " + colaRes);
        lblListaMetricas.setText("Pendientes: " + listaAct + "  |  Resueltas: " + listaRes);

        String[] departamentos = {"Sistemas", "Ventas", "RRHH", "Finanzas", "Logística"};
        String[] claves = {"Sistemas", "Ventas", "Recursos Humanos", "Finanzas", "Logística"};
        Color[] colores = {new Color(30, 64, 175), new Color(37, 99, 235),
                new Color(79, 70, 229), new Color(67, 56, 202), new Color(91, 33, 182)};
        panelHorasDepartamentos.removeAll();
        int[] horasGrafica = new int[departamentos.length];
        int[] tareasGrafica = new int[departamentos.length];
        for (int i = 0; i < departamentos.length; i++) {
            horasGrafica[i] = horas.getOrDefault(claves[i], 0);
            tareasGrafica[i] = tareas.getOrDefault(claves[i], 0);
            panelHorasDepartamentos.add(crearMiniTarjetaDepartamento(departamentos[i], horasGrafica[i], colores[i]));
        }
        panelGraficaDepartamentos.actualizarDatos(departamentos, tareasGrafica, colores);
        panelHorasDepartamentos.revalidate();
        panelHorasDepartamentos.repaint();
    }

    private static class DashboardChartPanel extends JPanel {
        private String[] departamentos = new String[0];
        private int[] tareas = new int[0];
        private Color[] colores = new Color[0];

        void actualizarDatos(String[] departamentos, int[] tareas, Color[] colores) {
            this.departamentos = departamentos;
            this.tareas = tareas;
            this.colores = colores;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (departamentos.length == 0) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 42, right = 15, top = 24, bottom = 42;
            int chartWidth = Math.max(1, getWidth() - left - right);
            int chartHeight = Math.max(1, getHeight() - top - bottom);
            int max = 1;
            for (int tarea : tareas) max = Math.max(max, tarea);
            int step = Math.max(1, (int) Math.ceil(max / 4.0));
            int maxAxis = step * 4;

            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g.setColor(new Color(148, 163, 184));
            for (int i = 0; i <= 4; i++) {
                int y = top + chartHeight - (i * chartHeight / 4);
                g.drawLine(left, y, left + chartWidth, y);
                String etiqueta = String.valueOf(i * step);
                g.drawString(etiqueta, left - g.getFontMetrics().stringWidth(etiqueta) - 6, y + 4);
            }

            int slot = chartWidth / departamentos.length;
            int barWidth = Math.max(18, Math.min(52, slot - 24));
            for (int i = 0; i < departamentos.length; i++) {
                int barHeight = tareas[i] * chartHeight / maxAxis;
                int x = left + i * slot + (slot - barWidth) / 2;
                int y = top + chartHeight - barHeight;
                GradientPaint gradiente = new GradientPaint(x, y, colores[i].brighter(), x, y + Math.max(1, barHeight), colores[i]);
                g.setPaint(gradiente);
                g.fillRoundRect(x, y, barWidth, Math.max(2, barHeight), 8, 8);
                g.setColor(COLOR_TEXTO_DARK);
                String valor = String.valueOf(tareas[i]);
                g.drawString(valor, x + (barWidth - g.getFontMetrics().stringWidth(valor)) / 2, Math.max(top - 5, y - 6));
                g.setColor(colores[i]);
                g.fillOval(x + barWidth / 2 - 4, top + chartHeight + 13, 8, 8);
                g.setColor(COLOR_TEXTO_DARK);
                String nombre = departamentos[i];
                g.drawString(nombre, x + (barWidth - g.getFontMetrics().stringWidth(nombre)) / 2, top + chartHeight + 34);
            }
            g.dispose();
        }
    }

    public void logGUI(String mensaje) {
        if (areaConsolaGUI != null) {
            areaConsolaGUI.append(mensaje + "\n");
            areaConsolaGUI.setCaretPosition(areaConsolaGUI.getDocument().getLength());
        }
    }
}