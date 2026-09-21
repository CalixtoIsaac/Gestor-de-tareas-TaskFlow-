package com.example.Vista;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

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

    // Contenedores y Navegación
    private CardLayout cardLayout;
    private JPanel panelContenidoCards;
    private JPanel panelSidebar;
    private boolean sidebarExpandido = true;

    // Botones del Menú Lateral
    private JButton btnMenuToggle;
    private JButton btnNavDashboard, btnNavRegistro, btnNavPila, btnNavCola, btnNavLista;
    private JButton btnNavPrioridad, btnNavEmpleados, btnNavRecursivo, btnNavAlgoritmos, btnNavGrafo, btnNavTodas;

    // Métricas Dashboard
    private JLabel lblPilaActivas, lblPilaResueltas, lblColaActivas, lblColaResueltas, lblListaActivas, lblListaResueltas;

    // Componentes del Formulario de Registro
    private JTextField txtTitulo, txtTiempoEstimado;
    private JComboBox<String> cbDepartamento, cbEstructura;
    private JComboBox<Integer> cbUrgencia;
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
    private JComboBox<String> cbEmpleadoDepto, cbFiltroDeptoEmp;
    private JButton btnAgregarEmpleado, btnBuscarEmpleadoId, btnListarEmpleadoDepto, btnMostrarTodosEmpleados;

    // Componentes Recursividad & Divide y Vencerás
    private JButton btnCalcularTiempoRecursivo, btnDistribuirDivideVenceras;
    private JTextArea areaResultadoDistribuicion;

    // Componentes Tablas Hash, QuickSort y Búsqueda Binaria
    private JTextField txtBuscarHashId, txtBuscarBinariaId;
    private JButton btnBuscarHash, btnQuickSortUrgencia, btnBuscarBinaria;

    // Componentes Grafo de Dependencias
    private JTextField txtGrafoTareaPrevia, txtGrafoTareaSiguiente;
    private JButton btnAgregarDependencia, btnCalcularOrdenTopologico;
    private JTextArea areaOrdenTopologico;

    // Consola de eventos
    private JTextArea areaConsolaGUI;

    public GestionTareasView() {
        setTitle("Sistema Empresarial Avanzado de Gestión de Tareas - Dashboard");
        setSize(1180, 820);
        setMinimumSize(new Dimension(1000, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panelRaiz = new JPanel(new BorderLayout());
        panelRaiz.setBackground(COLOR_FONDO_APP);
        setContentPane(panelRaiz);

        initHeader(panelRaiz);
        initSidebar(panelRaiz);
        initMainCards(panelRaiz);
    }

    private void initHeader(JPanel panelRaiz) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_SIDEBAR_BG);
        header.setPreferredSize(new Dimension(getWidth(), 50));
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

        header.add(btnMenuToggle, BorderLayout.WEST);
        header.add(lblTituloApp, BorderLayout.CENTER);
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

        btn.addActionListener(e -> cardLayout.show(panelContenidoCards, cardName));
        return btn;
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
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        JLabel lblTitulo = new JLabel("Resumen Administrativo del Sistema");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelGridCards = new JPanel(new GridLayout(3, 2, 15, 15));
        panelGridCards.setOpaque(false);

        lblPilaActivas = new JLabel("0", SwingConstants.CENTER);
        lblPilaResueltas = new JLabel("0", SwingConstants.CENTER);
        lblColaActivas = new JLabel("0", SwingConstants.CENTER);
        lblColaResueltas = new JLabel("0", SwingConstants.CENTER);
        lblListaActivas = new JLabel("0", SwingConstants.CENTER);
        lblListaResueltas = new JLabel("0", SwingConstants.CENTER);

        panelGridCards.add(crearTarjetaMetrica("Tareas Urgentes (Pila) - Pendientes", lblPilaActivas, COLOR_ROJO));
        panelGridCards.add(crearTarjetaMetrica("Tareas Urgentes (Pila) - Resueltas", lblPilaResueltas, COLOR_VERDE));
        panelGridCards.add(crearTarjetaMetrica("Tareas Programadas (Cola) - Pendientes", lblColaActivas, COLOR_PRIMARIO));
        panelGridCards.add(crearTarjetaMetrica("Tareas Programadas (Cola) - Resueltas", lblColaResueltas, COLOR_VERDE));
        panelGridCards.add(crearTarjetaMetrica("Tareas Generales (Lista) - Pendientes", lblListaActivas, COLOR_NEUTRO));
        panelGridCards.add(crearTarjetaMetrica("Tareas Generales (Lista) - Resueltas", lblListaResueltas, COLOR_VERDE));

        panel.add(panelGridCards, BorderLayout.CENTER);
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

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblValor.setForeground(COLOR_TEXTO_DARK);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearCardRegistro() {
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

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.22;
        JLabel lblUrgencia = new JLabel("Urgencia (1-Baja a 5-Crítica):", SwingConstants.RIGHT);
        lblUrgencia.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUrgencia.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblUrgencia, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        cbUrgencia = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        cbUrgencia.setBackground(Color.WHITE); panel.add(cbUrgencia, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.22;
        JLabel lblTiempo = new JLabel("Tiempo Estimado (Horas):", SwingConstants.RIGHT);
        lblTiempo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTiempo.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblTiempo, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        txtTiempoEstimado = new JTextField("2"); estilarCampoTexto(txtTiempoEstimado); panel.add(txtTiempoEstimado, gbc);

        selectorFecha = new SelectorFechaPanel();

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.22;
        JLabel lblFecha = new JLabel("Fecha de Entrega:", SwingConstants.RIGHT);
        lblFecha.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFecha.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblFecha, gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.78;
        JLabel lblFormatoFecha = new JLabel("Formato requerido: " + selectorFecha.getFormatoTexto()
                + " (ejemplo: 2025-12-31). Si se deja vacío, se usará la fecha de hoy.");
        lblFormatoFecha.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFormatoFecha.setForeground(COLOR_NEUTRO);
        panel.add(lblFormatoFecha, gbc);

        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.78; gbc.insets = new Insets(0, 12, 10, 12);
        panel.add(selectorFecha, gbc);
        gbc.insets = new Insets(10, 12, 10, 12);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.22;
        JLabel lblEstructura = new JLabel("Asignar a Estructura:", SwingConstants.RIGHT);
        lblEstructura.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstructura.setForeground(COLOR_TEXTO_DARK);
        panel.add(lblEstructura, gbc);
        gbc.gridx = 1; gbc.weightx = 0.78;
        cbEstructura = new JComboBox<>(new String[]{"Pila (Urgente)", "Cola (Secuencial)", "Lista (General)", "Cola de Prioridad (Urgencia/Fecha)"});
        cbEstructura.setBackground(Color.WHITE); panel.add(cbEstructura, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(18, 15, 8, 15);
        btnAgregar = crearBotonEstilizado("  +  Agregar Tarea al Sistema  ", COLOR_PRIMARIO, Color.WHITE);
        btnAgregar.setPreferredSize(new Dimension(320, 42));
        panel.add(btnAgregar, gbc);
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.LINE_START;

        return panel;
    }

    private JPanel crearCardPila() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloPila = new DefaultTableModel(new String[]{"ID", "Título", "Departamento", "Urgencia", "Tiempo (hrs)"}, 0);
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
        modeloCola = new DefaultTableModel(new String[]{"ID", "Título", "Departamento", "Urgencia", "Tiempo (hrs)"}, 0);
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
        modeloLista = new DefaultTableModel(new String[]{"ID", "Título", "Departamento", "Urgencia", "Tiempo (hrs)"}, 0);
        tablaLista = crearTablaEstilizada(modeloLista);
        JScrollPane scroll = new JScrollPane(tablaLista);
        scroll.setBorder(crearBordeSeccion(" Listas - Tareas Generales ", 14));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); panelBotones.setOpaque(false);
        btnEliminarLista = crearBotonEstilizado("Eliminar por ID", COLOR_NEUTRO, Color.WHITE);
        btnBuscarDepto = crearBotonEstilizado("Buscar por Depto", COLOR_NEUTRO, Color.WHITE);
        panelBotones.add(btnEliminarLista); panelBotones.add(btnBuscarDepto);

        panel.add(scroll, BorderLayout.CENTER); panel.add(panelBotones, BorderLayout.SOUTH);
        return panel;
    }

    // --- Card Cola de Prioridad ---
    private JPanel crearCardPrioridad() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloPrioridad = new DefaultTableModel(new String[]{"ID", "Título", "Departamento", "Urgencia", "Tiempo (hrs)", "Fecha Límite"}, 0);
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

        modeloEmpleados = new DefaultTableModel(new String[]{"ID", "Nombre Empleado", "Departamento"}, 0);
        tablaEmpleados = crearTablaEstilizada(modeloEmpleados);
        JScrollPane scrollTabla = new JScrollPane(tablaEmpleados);
        scrollTabla.setBorder(crearBordeSeccion(" Empleados Registrados ", 12));

        JPanel panelCentro = new JPanel(new BorderLayout(5, 5)); panelCentro.setOpaque(false);
        panelCentro.add(panelAcciones, BorderLayout.NORTH); panelCentro.add(scrollTabla, BorderLayout.CENTER);

        panel.add(panelForm, BorderLayout.NORTH); panel.add(panelCentro, BorderLayout.CENTER);
        return panel;
    }

    // --- Card Recursividad y Divide y Vencerás ---
    private JPanel crearCardRecursivo() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10)); panelBotones.setOpaque(false);
        btnCalcularTiempoRecursivo = crearBotonEstilizado("Calcular Tiempo Total Estimado", COLOR_PRIMARIO, Color.WHITE);
        btnDistribuirDivideVenceras = crearBotonEstilizado("Distribuir Tareas entre Empleados", COLOR_VERDE, Color.WHITE);
        panelBotones.add(btnCalcularTiempoRecursivo); panelBotones.add(btnDistribuirDivideVenceras);

        areaResultadoDistribuicion = new JTextArea(15, 70);
        areaResultadoDistribuicion.setEditable(false);
        areaResultadoDistribuicion.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaResultadoDistribuicion.setBackground(new Color(15, 23, 42));
        areaResultadoDistribuicion.setForeground(new Color(56, 189, 248));
        JScrollPane scroll = new JScrollPane(areaResultadoDistribuicion);
        scroll.setBorder(crearBordeSeccion(" Resultados de Cálculos y Distribución de Tareas ", 14));

        panel.add(panelBotones, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // --- Card Tablas Hash, QuickSort y Búsqueda Binaria ---
    private JPanel crearCardAlgoritmos() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10)); panel.setOpaque(false);

        // Subpanel 1: HashMap
        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); p1.setBackground(COLOR_TARJETA);
        p1.setBorder(crearBordeSeccion(" Buscar Tarea por ID (Todas las Tareas Registradas) ", 12));
        txtBuscarHashId = new JTextField(10); estilarCampoTexto(txtBuscarHashId);
        btnBuscarHash = crearBotonEstilizado("Buscar Tarea por ID", COLOR_PRIMARIO, Color.WHITE);
        p1.add(new JLabel("ID Tarea:")); p1.add(txtBuscarHashId); p1.add(btnBuscarHash);

        // Subpanel 2: QuickSort
        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); p2.setBackground(COLOR_TARJETA);
        p2.setBorder(crearBordeSeccion(" Ordenar Tareas por Urgencia ", 12));
        btnQuickSortUrgencia = crearBotonEstilizado("Ordenar Tareas de la Lista por Urgencia", COLOR_TEXTO_DARK, Color.WHITE);
        p2.add(btnQuickSortUrgencia);

        // Subpanel 3: Búsqueda Binaria
        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); p3.setBackground(COLOR_TARJETA);
        p3.setBorder(crearBordeSeccion(" Buscar Tarea por ID (Solo Tareas Activas: Pila, Cola y Lista) ", 12));
        txtBuscarBinariaId = new JTextField(10); estilarCampoTexto(txtBuscarBinariaId);
        btnBuscarBinaria = crearBotonEstilizado("Buscar Tarea por ID", COLOR_VERDE, Color.WHITE);
        p3.add(new JLabel("ID Tarea:")); p3.add(txtBuscarBinariaId); p3.add(btnBuscarBinaria);

        panel.add(p1); panel.add(p2); panel.add(p3);
        return panel;
    }

    // --- Card Grafo de Dependencias ---
    private JPanel crearCardGrafo() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);

        JPanel panelForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); panelForm.setBackground(COLOR_TARJETA);
        panelForm.setBorder(crearBordeSeccion(" Registrar Dependencia entre Tareas ", 12));

        txtGrafoTareaPrevia = new JTextField(8); estilarCampoTexto(txtGrafoTareaPrevia);
        txtGrafoTareaSiguiente = new JTextField(8); estilarCampoTexto(txtGrafoTareaSiguiente);
        btnAgregarDependencia = crearBotonEstilizado("+ Agregar Dependencia", COLOR_NEUTRO, Color.WHITE);
        btnCalcularOrdenTopologico = crearBotonEstilizado("Calcular Secuencia de Ejecución", COLOR_PRIMARIO, Color.WHITE);

        panelForm.add(new JLabel("ID Tarea Previa:")); panelForm.add(txtGrafoTareaPrevia);
        panelForm.add(new JLabel("ID Tarea Siguiente:")); panelForm.add(txtGrafoTareaSiguiente);
        panelForm.add(btnAgregarDependencia); panelForm.add(btnCalcularOrdenTopologico);

        areaOrdenTopologico = new JTextArea(15, 70);
        areaOrdenTopologico.setEditable(false);
        areaOrdenTopologico.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaOrdenTopologico.setBackground(new Color(15, 23, 42));
        areaOrdenTopologico.setForeground(new Color(34, 197, 94));
        JScrollPane scroll = new JScrollPane(areaOrdenTopologico);
        scroll.setBorder(crearBordeSeccion(" Grafo de Dependencias - Orden de Ejecución Solucionado ", 14));

        panel.add(panelForm, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearCardTodas() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setOpaque(false);
        modeloTodas = new DefaultTableModel(new String[]{"ID", "Título", "Departamento", "Urgencia", "Estructura"}, 0);
        tablaTodas = crearTablaEstilizada(modeloTodas);
        JScrollPane scrollTabla = new JScrollPane(tablaTodas);
        scrollTabla.setBorder(crearBordeSeccion(" Consolidado General de Tareas ", 14));

        JPanel panelTopAction = new JPanel(new FlowLayout(FlowLayout.LEFT)); panelTopAction.setOpaque(false);
        btnVerOrdenadas = crearBotonEstilizado("Ver Todas (Ordenadas por Urgencia/Depto)", COLOR_TEXTO_DARK, Color.WHITE);
        panelTopAction.add(btnVerOrdenadas);

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
        return table;
    }

    private JButton crearBotonEstilizado(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto); btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(bg); btn.setForeground(fg); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        return btn;
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

    public String getEmpleadoIdInput() { return txtEmpleadoId.getText().trim(); }
    public String getEmpleadoNombreInput() { return txtEmpleadoNombre.getText().trim(); }
    public String getEmpleadoDeptoSeleccionado() { return (String) cbEmpleadoDepto.getSelectedItem(); }
    public String getBuscarEmpleadoIdInput() { return txtBuscarEmpleadoId.getText().trim(); }
    public String getFiltroDeptoEmpSeleccionado() { return (String) cbFiltroDeptoEmp.getSelectedItem(); }

    public String getBuscarHashIdInput() { return txtBuscarHashId.getText().trim(); }
    public String getBuscarBinariaIdInput() { return txtBuscarBinariaId.getText().trim(); }
    public String getGrafoTareaPreviaInput() { return txtGrafoTareaPrevia.getText().trim(); }
    public String getGrafoTareaSiguienteInput() { return txtGrafoTareaSiguiente.getText().trim(); }

    public DefaultTableModel getModeloPila() { return modeloPila; }
    public DefaultTableModel getModeloCola() { return modeloCola; }
    public DefaultTableModel getModeloLista() { return modeloLista; }
    public DefaultTableModel getModeloPrioridad() { return modeloPrioridad; }
    public DefaultTableModel getModeloEmpleados() { return modeloEmpleados; }
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
    public JButton getBtnBuscarHash() { return btnBuscarHash; }
    public JButton getBtnQuickSortUrgencia() { return btnQuickSortUrgencia; }
    public JButton getBtnBuscarBinaria() { return btnBuscarBinaria; }
    public JButton getBtnAgregarDependencia() { return btnAgregarDependencia; }
    public JButton getBtnCalcularOrdenTopologico() { return btnCalcularOrdenTopologico; }

    public void setResultadoRecursivo(String texto) { areaResultadoDistribuicion.setText(texto); }
    public void setOrdenTopologico(String texto) { areaOrdenTopologico.setText(texto); }

    public void actualizarMetricas(int pilaAct, int pilaRes, int colaAct, int colaRes, int listaAct, int listaRes) {
        lblPilaActivas.setText(String.valueOf(pilaAct)); lblPilaResueltas.setText(String.valueOf(pilaRes));
        lblColaActivas.setText(String.valueOf(colaAct)); lblColaResueltas.setText(String.valueOf(colaRes));
        lblListaActivas.setText(String.valueOf(listaAct)); lblListaResueltas.setText(String.valueOf(listaRes));
    }

    public void logGUI(String mensaje) {
        areaConsolaGUI.append(mensaje + "\n");
        areaConsolaGUI.setCaretPosition(areaConsolaGUI.getDocument().getLength());
    }
}