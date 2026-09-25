package com.example.Controlador;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.sql.SQLException;
import java.util.*;

import com.example.Modelo.*;
import com.example.Vista.*;
import com.example.persistencia.*;

public class GestionTareasController {

    private GestionTareasView vista;
    private PilaTareas pilaUrgentes;
    private ColaTareas colaProgramadas;
    private ListaTareas listaGeneral;

    // Estructuras de Datos Avanzadas
    private ColaPrioridadTareas colaPrioridad;
    private ArbolEmpleados arbolEmpleados;
    private GestorTablasHashYAlgoritmos gestorHashYAlgoritmos;
    private GrafoDependencias grafoDependencias;
    private ArbolTareasABB arbolTareas;          // ABB de tareas activas por folio (módulo Búsquedas)
    private String recorridoActual;              // "Inorden" / "Preorden" / "Postorden" o null
    private List<Integer> rutaBusqueda = List.of();
    private Integer folioEncontrado;
    private List<Empleado> listaEmpleadosMemoria;
    private List<Empleado> empleadosMostrados; // lo que hay en la tabla de Empleados (todos o filtro por depto)
    private TareaRepositorio tareaRepositorio;
    private EmpleadoRepositorio empleadoRepositorio;

    private int resueltasPila = 0, resueltasCola = 0, resueltasLista = 0;

    public GestionTareasController(GestionTareasView vista, PilaTareas pila, ColaTareas cola, ListaTareas lista) {
        this.vista = vista;
        this.pilaUrgentes = pila;
        this.colaProgramadas = cola;
        this.listaGeneral = lista;

        this.colaPrioridad = new ColaPrioridadTareas();
        this.arbolEmpleados = new ArbolEmpleados();
        this.gestorHashYAlgoritmos = new GestorTablasHashYAlgoritmos();
        this.grafoDependencias = new GrafoDependencias();
        this.arbolTareas = new ArbolTareasABB();
        this.listaEmpleadosMemoria = new ArrayList<>();
        this.empleadosMostrados = listaEmpleadosMemoria;

        inicializarPersistencia();
        initControlador();
        refrescarResponsablesDisponibles();
        actualizarTablasYMetricas();
    }

    private void inicializarPersistencia() {
        try {
            ConexionBD conexionBD = new ConexionBD();
            tareaRepositorio = new TareaRepositorio(conexionBD);
            empleadoRepositorio = new EmpleadoRepositorio(conexionBD);
            cargarDesdeBD();
        } catch (SQLException ex) {
            vista.logGUI("[BD] No se pudieron cargar los datos: " + ex.getMessage());
        }
    }

    private void cargarDesdeBD() throws SQLException {
        // Primero los empleados: las tareas necesitan el árbol para resolver su Responsable Directo
        for (Empleado empleado : empleadoRepositorio.cargarTodos()) {
            listaEmpleadosMemoria.add(empleado);
            arbolEmpleados.insertar(empleado);
            gestorHashYAlgoritmos.guardarEmpleado(empleado);
        }
        for (Tarea tarea : tareaRepositorio.cargarTodas(arbolEmpleados::buscarPorId)) {
            gestorHashYAlgoritmos.guardarTarea(tarea);
            if (tarea.getTipoEstructura().startsWith("Pila")) {
                pilaUrgentes.push(tarea);
            } else if (tarea.getTipoEstructura().startsWith("Cola de Prioridad")) {
                colaPrioridad.agregarTarea(tarea);
            } else if (tarea.getTipoEstructura().startsWith("Cola")) {
                colaProgramadas.enqueue(tarea);
            } else {
                listaGeneral.insert(tarea);
            }
        }
        vista.logGUI("[BD] Datos cargados correctamente.");
    }

    public void guardarEnBD() {
        if (tareaRepositorio == null || empleadoRepositorio == null) {
            return;
        }
        try {
            Map<Integer, Tarea> tareasUnicas = new LinkedHashMap<>();
            for (Tarea tarea : obtenerTodasLasTareas()) {
                tareasUnicas.put(tarea.getId(), tarea);
            }
            tareaRepositorio.guardarTodas(tareasUnicas.values());
            Map<String, Empleado> empleadosUnicos = new LinkedHashMap<>();
            for (Empleado empleado : listaEmpleadosMemoria) {
                empleadosUnicos.put(empleado.getId(), empleado);
            }
            empleadoRepositorio.guardarTodos(empleadosUnicos.values());
            vista.logGUI("[BD] Datos guardados correctamente.");
        } catch (SQLException ex) {
            vista.logGUI("[BD] No se pudieron guardar los datos: " + ex.getMessage());
        }
    }

    private void initControlador() {
        // Eventos base
        vista.getBtnAgregar().addActionListener(e -> agregarTarea());
        // Regla de negocio: el selector de Responsable solo muestra empleados del departamento de la tarea
        vista.addCambioDepartamentoListener(e -> refrescarResponsablesDisponibles());
        vista.getBtnPopPila().addActionListener(e -> procesarPopPila());
        vista.getBtnPeekPila().addActionListener(e -> consultarPeekPila());
        vista.getBtnDequeueCola().addActionListener(e -> procesarDequeueCola());
        vista.getBtnFrontCola().addActionListener(e -> consultarFrontCola());
        vista.getBtnEliminarLista().addActionListener(e -> eliminarDeLista());
        vista.getBtnBuscarDepto().addActionListener(e -> buscarPorDepartamento());
        vista.getBtnVerOrdenadas().addActionListener(e -> mostrarTodasOrdenadas());

        // Eventos Cola de Prioridad
        vista.getBtnExtraerPrioridad().addActionListener(e -> extraerColaPrioridad());
        vista.getBtnVerPrioridad().addActionListener(e -> verColaPrioridad());

        // Eventos Árbol Binario (Empleados)
        vista.getBtnAgregarEmpleado().addActionListener(e -> agregarEmpleado());
        vista.getBtnBuscarEmpleadoId().addActionListener(e -> buscarEmpleadoBST());
        vista.getBtnListarEmpleadoDepto().addActionListener(e -> listarEmpleadosDeptoBST());
        vista.getBtnMostrarTodosEmpleados().addActionListener(e -> mostrarTodosLosEmpleados());
        // Al seleccionar un empleado se despliega el detalle de sus tareas pendientes
        vista.addSeleccionEmpleadoListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarPendientesEmpleadoSeleccionado();
        });

        // Eventos Recursividad y Divide & Vencerás
        vista.getBtnCalcularTiempoRecursivo().addActionListener(e -> calcularTiempoRecursivo());
        vista.getBtnDistribuirDivideVenceras().addActionListener(e -> distribuirDivideVenceras());

        // Eventos Tablas Hash y Algoritmos
        // Eventos Búsquedas: buscador unificado, recorridos y árbol
        vista.getBtnBuscarFolio().addActionListener(e -> buscarPorFolio());
        vista.getBtnInorden().addActionListener(e -> mostrarRecorrido("Inorden"));
        vista.getBtnPreorden().addActionListener(e -> mostrarRecorrido("Preorden"));
        vista.getBtnPostorden().addActionListener(e -> mostrarRecorrido("Postorden"));
        vista.getBtnBalancearArbol().addActionListener(e -> balancearArbol());
        vista.getBtnQuickSortUrgencia().addActionListener(e -> ordenarQuickSort());

        // Eventos Grafo
        vista.getBtnAgregarDependencia().addActionListener(e -> agregarDependenciaGrafo());
        vista.getBtnCalcularOrdenTopologico().addActionListener(e -> calcularOrdenTopologico());
    }

        private void agregarTarea() {
        String titulo = vista.getTituloInput();
        if (titulo.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "El título no puede estar vacío.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate fechaEntrega;
        try {
            // null si el campo se deja vacío -> Tarea asignará automáticamente la fecha de hoy
            fechaEntrega = vista.getSelectorFecha().obtenerFechaValidada();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(vista,
                    "La fecha de entrega debe tener un formato válido: " + vista.getSelectorFecha().getFormatoTexto()
                            + " (ejemplo: 2025-12-31), o formatos comunes como 31/12/2025.",
                    "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String depto = vista.getDepartamentoSeleccionado();
        int urgencia = vista.getUrgenciaSeleccionada();
        String tipoEst = vista.getEstructuraSeleccionada();
        int tiempo = vista.getTiempoEstimadoInput();
        Empleado responsable = vista.getResponsableSeleccionado(); // null = "Sin Asignar"

        // Validación defensiva de la regla de negocio (el selector ya viene filtrado por departamento)
        if (responsable != null && !responsable.getDepartamento().equalsIgnoreCase(depto)) {
            JOptionPane.showMessageDialog(vista,
                    "El responsable seleccionado no pertenece al departamento " + depto + ".",
                    "Atención", JOptionPane.WARNING_MESSAGE);
            refrescarResponsablesDisponibles();
            return;
        }

        Tarea nueva = new Tarea(titulo, depto, urgencia, tipoEst, tiempo, fechaEntrega, responsable);
        gestorHashYAlgoritmos.guardarTarea(nueva);

        if (tipoEst.startsWith("Pila")) {
            pilaUrgentes.push(nueva);
            vista.logGUI("[PUSH] Tarea en Pila: " + titulo);
        } else if (tipoEst.startsWith("Cola de Prioridad")) {
            colaPrioridad.agregarTarea(nueva);
            vista.logGUI("[PRIORITY QUEUE] Tarea en Cola Prioritaria: " + titulo);
        } else if (tipoEst.startsWith("Cola")) {
            colaProgramadas.enqueue(nueva);
            vista.logGUI("[ENQUEUE] Tarea en Cola Secuencial: " + titulo);
        } else {
            listaGeneral.insert(nueva);
            vista.logGUI("[INSERT] Tarea en Lista General: " + titulo);
        }

        vista.logGUI("   Responsable Directo: " + nueva.getNombreResponsable()
                + " | Entrega: " + nueva.getFechaEntregaFormateada());

        vista.limpiarTituloInput();
        vista.getSelectorFecha().limpiar();
        vista.limpiarResponsable();
        actualizarTablasYMetricas();
    }

    // Recarga el selector "Responsable Directo" con los empleados del departamento elegido (recorrido del árbol)
    private void refrescarResponsablesDisponibles() {
        String depto = vista.getDepartamentoSeleccionado();
        vista.setResponsablesDisponibles(depto == null ? List.of() : arbolEmpleados.obtenerPorDepartamento(depto));
    }

    private void extraerColaPrioridad() {
        Tarea t = colaPrioridad.extraerSiguienteTarea();
        if (t != null) {
            vista.logGUI("[PRIORITY POLL] Atendida tarea prioritaria: " + t.getTitulo());
            JOptionPane.showMessageDialog(vista, "Tarea Prioritaria Atendida:\n" + t.toString(), "Cola de Prioridad", JOptionPane.INFORMATION_MESSAGE);
        } else {
            vista.logGUI("[PRIORITY] La cola de prioridad está vacía.");
        }
        actualizarTablasYMetricas();
    }

    private void verColaPrioridad() {
        Tarea t = colaPrioridad.verSiguienteTarea();
        if (t != null) {
            JOptionPane.showMessageDialog(vista, "Siguiente tarea con mayor prioridad:\n" + t.toString(), "Peek Prioridad", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(vista, "La cola de prioridad está vacía.", "Peek Prioridad", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarEmpleado() {
        String id = vista.getEmpleadoIdInput();
        String nombre = vista.getEmpleadoNombreInput();
        String depto = vista.getEmpleadoDeptoSeleccionado();

        if (id.isEmpty() || nombre.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese ID y Nombre del empleado.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Empleado emp = new Empleado(id, nombre, depto);
        arbolEmpleados.insertar(emp);
        gestorHashYAlgoritmos.guardarEmpleado(emp);
        listaEmpleadosMemoria.add(emp);

        vista.logGUI("[BST INSERT] Empleado registrado en Árbol Binario: " + nombre + " (" + id + ")");
        actualizarTablaEmpleados(listaEmpleadosMemoria);
        refrescarResponsablesDisponibles();
    }

    private void buscarEmpleadoBST() {
        String id = vista.getBuscarEmpleadoIdInput();
        Empleado emp = arbolEmpleados.buscarPorId(id);
        if (emp != null) {
            JOptionPane.showMessageDialog(vista, "Empleado Encontrado en Árbol BST:\nID: " + emp.getId() + "\nNombre: " + emp.getNombre() + "\nDepto: " + emp.getDepartamento(), "Búsqueda BST", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(vista, "No se encontró ningún empleado con ID: " + id, "Búsqueda BST", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void listarEmpleadosDeptoBST() {
        String depto = vista.getFiltroDeptoEmpSeleccionado();
        List<Empleado> emps = arbolEmpleados.obtenerPorDepartamento(depto);
        actualizarTablaEmpleados(emps);
        vista.logGUI("[BST TRAVERSAL] Se encontraron " + emps.size() + " empleados en " + depto);
    }

    private void mostrarTodosLosEmpleados() {
        actualizarTablaEmpleados(listaEmpleadosMemoria);
        vista.logGUI("[BST TRAVERSAL] Se muestran todos los empleados registrados.");
    }

    private void calcularTiempoRecursivo() {
        List<Tarea> todas = obtenerTodasLasTareas();
        int tiempoTotal = ProcesadorRecursivo.calcularTiempoTotalEstimado(todas, 0);
        String detalle = "Pila " + pilaUrgentes.getPila().size()
                + "  ·  Cola " + colaProgramadas.getCola().size()
                + "  ·  Lista " + listaGeneral.getLista().size()
                + "  ·  Prioridad " + colaPrioridad.obtenerTareasOrdenadas().size();
        vista.mostrarKpisTiempo(todas.size(), tiempoTotal, detalle);
        vista.logGUI("[RECURSIÓN] Cálculo de tiempo finalizado: " + tiempoTotal + " hrs.");
    }

    private void distribuirDivideVenceras() {
        List<Tarea> tareas = obtenerTodasLasTareas();
        if (tareas.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "No hay tareas activas para distribuir.", "Distribución", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tareas que ya tenían Responsable Directo antes de distribuir (se respetan)
        Set<Tarea> conResponsablePrevio = new HashSet<>();
        for (Tarea t : tareas) if (t.tieneResponsable()) conResponsablePrevio.add(t);

        Map<String, List<Tarea>> distribucion = ProcesadorRecursivo.distribuirTareasDivideYVenceras(tareas, arbolEmpleados);

        // Las tareas "Sin Asignar" quedan asignadas al empleado que les tocó en la distribución
        int nuevasAsignaciones = 0;
        Set<Tarea> asignadas = new HashSet<>();
        for (Map.Entry<String, List<Tarea>> entrada : distribucion.entrySet()) {
            Empleado empleado = arbolEmpleados.buscarPorId(entrada.getKey());
            if (empleado == null) continue;
            for (Tarea t : entrada.getValue()) {
                asignadas.add(t);
                if (!t.tieneResponsable()) {
                    t.setResponsableDirecto(empleado);
                    nuevasAsignaciones++;
                }
            }
        }

        // REGLA DE ALERTA: departamentos con tareas pendientes pero sin personal registrado
        Set<String> departamentosSinPersonal = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        int tareasSinPersonal = 0;
        for (Tarea tarea : tareas) {
            if (!asignadas.contains(tarea)) {
                departamentosSinPersonal.add(tarea.getDepartamento());
                tareasSinPersonal++;
            }
        }

        // Un grupo por empleado (sin duplicados), ordenado por departamento y nombre;
        // dentro de cada grupo, las tareas por fecha de entrega más próxima.
        Map<String, Empleado> empleadosUnicos = new LinkedHashMap<>();
        for (Empleado emp : listaEmpleadosMemoria) empleadosUnicos.putIfAbsent(emp.getId(), emp);
        List<Empleado> ordenados = new ArrayList<>(empleadosUnicos.values());
        ordenados.sort(Comparator.comparing(Empleado::getDepartamento, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Empleado::getNombre, String.CASE_INSENSITIVE_ORDER));

        List<GestionTareasView.GrupoDistribucion> grupos = new ArrayList<>();
        for (Empleado emp : ordenados) {
            List<Tarea> suyas = new ArrayList<>(distribucion.getOrDefault(emp.getId(), List.of()));
            suyas.sort(Comparator.comparing(Tarea::getFechaEntrega));
            List<GestionTareasView.FilaAsignacion> filas = new ArrayList<>();
            for (Tarea t : suyas) filas.add(new GestionTareasView.FilaAsignacion(t, conResponsablePrevio.contains(t)));
            grupos.add(new GestionTareasView.GrupoDistribucion(emp, filas));
        }

        vista.mostrarDistribucion(grupos, departamentosSinPersonal, tareasSinPersonal, nuevasAsignaciones);
        vista.logGUI("[DIVIDE Y VENCERÁS] Distribución realizada entre " + ordenados.size() + " empleados. "
                + nuevasAsignaciones + " tarea(s) sin asignar recibieron responsable.");
        if (!departamentosSinPersonal.isEmpty()) {
            vista.logGUI("[ALERTA] Sin personal en: " + String.join(", ", departamentosSinPersonal));
        }
        actualizarTablasYMetricas();
    }

    private void ordenarQuickSort() {
        List<Tarea> lista = new ArrayList<>(listaGeneral.getLista());
        if (lista.size() < 2) {
            JOptionPane.showMessageDialog(vista, "Se requieren al menos 2 tareas en la Lista General para ordenar.", "QuickSort", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        GestorTablasHashYAlgoritmos.ordenarTareasPorUrgenciaQuickSort(lista, 0, lista.size() - 1);
        StringBuilder sb = new StringBuilder("=== TAREAS DE LA LISTA ORDENADAS POR QUICKSORT (URGENCIA) ===\n\n");
        for (Tarea t : lista) sb.append(t.toString()).append("\n");
        JOptionPane.showMessageDialog(vista, new JScrollPane(new JTextArea(sb.toString())), "QuickSort Completado", JOptionPane.INFORMATION_MESSAGE);
        vista.logGUI("[QUICKSORT] Tareas ordenadas por urgencia.");
    }

    // ==========================================================
    // MÓDULO BÚSQUEDAS
    // ==========================================================

    /** Busca un folio con los 4 métodos y muestra el detalle + la comparativa de comparaciones. */
    private void buscarPorFolio() {
        int folio;
        try {
            folio = Integer.parseInt(vista.getFolioBuscado());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Ingrese un folio (ID) numérico válido.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Tarea> activas = obtenerTodasLasTareas();          // Pila, Cola, Lista y Cola de Prioridad
        List<Tarea> ordenadasPorId = new ArrayList<>(activas);
        ordenadasPorId.sort(Comparator.comparingInt(Tarea::getId));

        GestorTablasHashYAlgoritmos.ResultadoBusqueda porHash = gestorHashYAlgoritmos.buscarPorHashConConteo(folio);
        ArbolTareasABB.ResultadoBusquedaABB porArbol = arbolTareas.buscarConConteo(folio);
        GestorTablasHashYAlgoritmos.ResultadoBusqueda porBinaria =
                GestorTablasHashYAlgoritmos.busquedaBinariaConConteo(ordenadasPorId, folio);
        GestorTablasHashYAlgoritmos.ResultadoBusqueda porSecuencial =
                GestorTablasHashYAlgoritmos.busquedaSecuencialConConteo(activas, folio);

        // La tabla hash guarda también tareas ya atendidas: se indica si sigue activa o no
        Tarea encontrada = porArbol.tarea() != null ? porArbol.tarea() : porHash.tarea();
        String estado = null;
        if (encontrada != null) {
            estado = porArbol.tarea() != null ? "Activa" : "Ya atendida";
        }

        List<GestionTareasView.MetodoBusqueda> metodos = List.of(
                new GestionTareasView.MetodoBusqueda("Tabla hash", "O(1) · acceso directo", porHash.comparaciones(), porHash.tarea() != null),
                new GestionTareasView.MetodoBusqueda("Árbol (ABB)", "O(log n) · altura " + arbolTareas.altura(), porArbol.comparaciones(), porArbol.tarea() != null),
                new GestionTareasView.MetodoBusqueda("Búsqueda binaria", "O(log n) · lista ordenada", porBinaria.comparaciones(), porBinaria.tarea() != null),
                new GestionTareasView.MetodoBusqueda("Búsqueda secuencial", "O(n) · uno por uno", porSecuencial.comparaciones(), porSecuencial.tarea() != null));

        vista.mostrarResultadoBusqueda(folio, encontrada, estado, metodos, activas.size());
        rutaBusqueda = porArbol.ruta();
        folioEncontrado = porArbol.tarea() != null ? folio : null;
        vista.resaltarRutaArbol(rutaBusqueda, folioEncontrado);
        vista.logGUI("[BÚSQUEDA] Folio " + folio + (encontrada != null ? " encontrado" : " no encontrado")
                + " | Hash: " + porHash.comparaciones() + ", ABB: " + porArbol.comparaciones()
                + ", Binaria: " + porBinaria.comparaciones() + ", Secuencial: " + porSecuencial.comparaciones() + " comparaciones.");
    }

    private void mostrarRecorrido(String tipo) {
        recorridoActual = tipo;
        List<Integer> secuencia;
        String explicacion;
        switch (tipo) {
            case "Preorden" -> {
                secuencia = arbolTareas.preorden();
                explicacion = "Preorden (Raíz → Izquierda → Derecha): visita primero cada raíz y después sus subárboles. "
                        + "El primer folio siempre es la raíz del árbol; sirve para copiar o reconstruir el árbol con la misma forma.";
            }
            case "Postorden" -> {
                secuencia = arbolTareas.postorden();
                explicacion = "Postorden (Izquierda → Derecha → Raíz): visita los hijos antes que el padre, así que la raíz "
                        + "queda al final. Es el orden que se usa para eliminar el árbol empezando por las hojas.";
            }
            default -> {
                secuencia = arbolTareas.inorden();
                explicacion = "Inorden (Izquierda → Raíz → Derecha): en un Árbol Binario de Búsqueda devuelve los folios "
                        + "ordenados de menor a mayor, porque todo lo menor queda a la izquierda y todo lo mayor a la derecha.";
            }
        }
        vista.mostrarRecorrido(tipo, explicacion, secuencia);
        vista.logGUI("[RECORRIDO " + tipo.toUpperCase() + "] " + secuencia);
    }

    private void balancearArbol() {
        if (arbolTareas.estaVacio()) {
            JOptionPane.showMessageDialog(vista, "No hay tareas activas en el árbol.", "Balancear árbol", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int[] alturas = arbolTareas.balancear();
        rutaBusqueda = List.of();          // la forma cambió: la ruta anterior ya no aplica
        folioEncontrado = null;
        refrescarArbolBusquedas();
        vista.logGUI("[DIVIDE Y VENCERÁS] Árbol balanceado: altura " + alturas[0] + " → " + alturas[1] + " niveles.");
    }

    /** Mantiene el ABB igual a las tareas activas: inserta las nuevas y elimina las ya atendidas. */
    private void sincronizarArbolTareas() {
        Map<Integer, Tarea> activas = new TreeMap<>();            // TreeMap: se insertan en orden de folio
        for (Tarea t : obtenerTodasLasTareas()) activas.put(t.getId(), t);
        for (Integer id : arbolTareas.inorden()) {
            if (!activas.containsKey(id)) arbolTareas.eliminar(id);
        }
        for (Tarea t : activas.values()) {
            if (!arbolTareas.contiene(t.getId())) arbolTareas.insertar(t);
        }
    }

    private void refrescarArbolBusquedas() {
        String info;
        if (arbolTareas.estaVacio()) {
            info = "Sin tareas activas.";
        } else {
            int altura = arbolTareas.altura(), minima = arbolTareas.alturaMinima();
            info = arbolTareas.getTamano() + " nodos  ·  altura " + altura + " niveles  ·  mínima posible " + minima
                    + (altura > minima ? "  ·  Desbalanceado: usa \"Balancear árbol\" para reducir comparaciones."
                                       : "  ·  Árbol balanceado.");
        }
        vista.actualizarArbol(arbolTareas.getRaiz(), info, rutaBusqueda, folioEncontrado);
        if (recorridoActual != null) mostrarRecorrido(recorridoActual);
    }

    private void agregarDependenciaGrafo() {
        String previa = vista.getGrafoTareaPreviaInput();
        String siguiente = vista.getGrafoTareaSiguienteInput();
        if (previa.isEmpty() || siguiente.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el ID/Nombre de ambas tareas.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        grafoDependencias.agregarDependencia(previa, siguiente);
        vista.logGUI("[GRAFO] Dependencia agregada: " + previa + " -> " + siguiente);
        JOptionPane.showMessageDialog(vista, "Dependencia vinculada: Tarea " + previa + " antecede a Tarea " + siguiente, "Grafo Dirigido", JOptionPane.INFORMATION_MESSAGE);
    }

    private void calcularOrdenTopologico() {
        try {
            List<String> orden = grafoDependencias.obtenerOrdenEjecucion();
            StringBuilder sb = new StringBuilder("=== SECUENCIA LÓGICA DE EJECUCIÓN (ORDEN TOPOLÓGICO) ===\n\n");
            for (int i = 0; i < orden.size(); i++) {
                sb.append("Paso ").append(i + 1).append(": Tarea [").append(orden.get(i)).append("]\n");
            }
            vista.setOrdenTopologico(sb.toString());
            vista.logGUI("[GRAFO] Secuencia de ejecución calculada exitosamente.");
        } catch (Exception ex) {
            vista.setOrdenTopologico("ERROR: " + ex.getMessage());
            JOptionPane.showMessageDialog(vista, ex.getMessage(), "Error en Grafo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Tarea> obtenerTodasLasTareas() {
        List<Tarea> todas = new ArrayList<>();
        todas.addAll(pilaUrgentes.getPila());
        todas.addAll(colaProgramadas.getCola());
        todas.addAll(listaGeneral.getLista());
        todas.addAll(colaPrioridad.obtenerTareasOrdenadas());
        return todas;
    }

    // ==========================================================
    // TAREAS PENDIENTES POR EMPLEADO
    // ==========================================================

    /**
     * Recorre TODAS las estructuras activas (Pila, Cola, Lista y Cola de Prioridad) y agrupa las
     * tareas por el ID de su Responsable Directo. Las tareas "Sin Asignar" no se incluyen.
     * Cada lista queda ordenada por fecha de entrega (más próxima primero) y luego por urgencia.
     */
    private Map<String, List<Tarea>> obtenerPendientesPorEmpleado() {
        Map<String, List<Tarea>> pendientes = new HashMap<>();
        for (Tarea t : obtenerTodasLasTareas()) {
            if (t.tieneResponsable()) {
                pendientes.computeIfAbsent(t.getResponsableDirecto().getId(), id -> new ArrayList<>()).add(t);
            }
        }
        Comparator<Tarea> orden = Comparator.comparing(Tarea::getFechaEntrega)
                .thenComparing(Comparator.comparingInt(Tarea::getUrgencia).reversed());
        for (List<Tarea> lista : pendientes.values()) lista.sort(orden);
        return pendientes;
    }

    private GestionTareasView.ResumenPendientes resumir(List<Tarea> tareas) {
        if (tareas == null || tareas.isEmpty()) return new GestionTareasView.ResumenPendientes(0, 0, 0);
        int vencidas = 0, criticas = 0;
        for (Tarea t : tareas) {
            if (t.estaVencida()) vencidas++;
            if (t.getUrgencia() >= 5) criticas++;
        }
        return new GestionTareasView.ResumenPendientes(tareas.size(), vencidas, criticas);
    }

    private void actualizarTablaEmpleados(List<Empleado> empleados) {
        empleadosMostrados = empleados;
        String seleccionPrevia = vista.getEmpleadoIdSeleccionado();
        Map<String, List<Tarea>> pendientes = obtenerPendientesPorEmpleado();

        vista.getModeloEmpleados().setRowCount(0);
        for (Empleado e : empleados) {
            vista.getModeloEmpleados().addRow(new Object[]{e.getId(), e.getNombre(), e.getDepartamento(),
                    resumir(pendientes.get(e.getId()))});
        }
        // Conserva el empleado seleccionado para que su detalle se mantenga actualizado
        if (!vista.seleccionarEmpleado(seleccionPrevia)) {
            mostrarPendientesEmpleadoSeleccionado();
        }
    }

    // Llena la sub-tabla de detalle con las tareas pendientes del empleado seleccionado
    private void mostrarPendientesEmpleadoSeleccionado() {
        vista.getModeloPendientesEmpleado().setRowCount(0);
        String id = vista.getEmpleadoIdSeleccionado();
        Empleado empleado = id == null ? null : arbolEmpleados.buscarPorId(id);
        if (empleado == null) {
            vista.setTituloPendientes("Selecciona un empleado para ver sus tareas pendientes.");
            return;
        }
        List<Tarea> tareas = obtenerPendientesPorEmpleado().getOrDefault(empleado.getId(), List.of());
        for (Tarea t : tareas) {
            vista.getModeloPendientesEmpleado().addRow(new Object[]{t.getId(), t.getTitulo(), t.getUrgencia(),
                    t.getFechaEntrega(), t.getTipoEstructura()});
        }
        vista.setTituloPendientes(tareas.isEmpty()
                ? empleado.getNombre() + " (" + empleado.getDepartamento() + ") no tiene tareas pendientes."
                : "Tareas pendientes de " + empleado.getNombre() + " (" + empleado.getDepartamento() + "): " + tareas.size());
    }

    private void procesarPopPila() {
        Tarea t = pilaUrgentes.pop();
        if (t != null) { resueltasPila++; vista.logGUI("[POP] Atendida: " + t.getTitulo()); }
        actualizarTablasYMetricas();
    }

    private void consultarPeekPila() {
        Tarea t = pilaUrgentes.peek();
        if (t != null) JOptionPane.showMessageDialog(vista, t.toString(), "Peek Pila", JOptionPane.INFORMATION_MESSAGE);
    }

    private void procesarDequeueCola() {
        Tarea t = colaProgramadas.dequeue();
        if (t != null) { resueltasCola++; vista.logGUI("[DEQUEUE] Atendida: " + t.getTitulo()); }
        actualizarTablasYMetricas();
    }

    private void consultarFrontCola() {
        Tarea t = colaProgramadas.front();
        if (t != null) JOptionPane.showMessageDialog(vista, t.toString(), "Front Cola", JOptionPane.INFORMATION_MESSAGE);
    }

    private void eliminarDeLista() {
        String input = JOptionPane.showInputDialog(vista, "ID a eliminar:");
        if (input != null && !input.isEmpty()) {
            try {
                int id = Integer.parseInt(input);
                if (listaGeneral.delete(id)) {
                    resueltasLista++;
                    vista.logGUI("[DELETE] Eliminada ID: " + id);
                } else {
                    JOptionPane.showMessageDialog(vista, "No existe ninguna tarea con ID: " + id + " en la Lista General.", "Atención", JOptionPane.WARNING_MESSAGE);
                }
                actualizarTablasYMetricas();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(vista, "Ingrese un ID numérico válido.", "Atención", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void buscarPorDepartamento() {
        String depto = vista.getFiltroDeptoListaSeleccionado();
        ArrayList<Tarea> resultados = new ArrayList<>(listaGeneral.find(depto));
        StringBuilder sb = new StringBuilder("=== TAREAS EN DEPARTAMENTO " + depto + " ===\n\n");
        for (Tarea t : resultados) sb.append(t.toString()).append("\n");
        JOptionPane.showMessageDialog(vista, new JScrollPane(new JTextArea(sb.toString())), "Búsqueda Depto", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarTodasOrdenadas() {
        List<Tarea> todas = obtenerTodasLasTareas();
        todas.sort((t1, t2) -> {
            int comp = Integer.compare(t2.getUrgencia(), t1.getUrgencia());
            return comp != 0 ? comp : t1.getDepartamento().compareToIgnoreCase(t2.getDepartamento());
        });
        StringBuilder sb = new StringBuilder("=== CONSOLIDADO GENERAL DE TAREAS ===\n\n");
        for (Tarea t : todas) sb.append(t.toString()).append("\n");
        JOptionPane.showMessageDialog(vista, new JScrollPane(new JTextArea(sb.toString())), "Consolidado", JOptionPane.PLAIN_MESSAGE);
    }

    private void actualizarTablasYMetricas() {
        vista.getModeloPila().setRowCount(0);
        for (Tarea t : pilaUrgentes.getPila()) vista.getModeloPila().addRow(filaTarea(t));

        vista.getModeloCola().setRowCount(0);
        for (Tarea t : colaProgramadas.getCola()) vista.getModeloCola().addRow(filaTarea(t));

        vista.getModeloLista().setRowCount(0);
        for (Tarea t : listaGeneral.getLista()) vista.getModeloLista().addRow(filaTarea(t));

        vista.getModeloPrioridad().setRowCount(0);
        for (Tarea t : colaPrioridad.obtenerTareasOrdenadas()) vista.getModeloPrioridad().addRow(filaTarea(t));

        List<Tarea> todas = obtenerTodasLasTareas();
        vista.getModeloTodas().setRowCount(0);
        for (Tarea t : todas) vista.getModeloTodas().addRow(new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(),
                t.getNombreResponsable(), t.getUrgencia(), t.getFechaEntrega(), t.getTipoEstructura()});

        Map<String, Integer> horasPorDepartamento = new LinkedHashMap<>();
        Map<String, Integer> tareasPorDepartamento = new LinkedHashMap<>();
        for (Tarea tarea : todas) {
            String departamento = tarea.getDepartamento();
            if (departamento == null) continue;
            horasPorDepartamento.merge(departamento, tarea.getTiempoEstimado(), Integer::sum);
            tareasPorDepartamento.merge(departamento, 1, Integer::sum);
        }

        // Los conteos de pendientes dependen de las tareas: se refrescan junto con las demás tablas
        actualizarTablaEmpleados(empleadosMostrados);

        // Árbol de Búsquedas: refleja las tareas activas (nuevas se insertan, atendidas se eliminan)
        sincronizarArbolTareas();
        refrescarArbolBusquedas();

        vista.actualizarDashboard(pilaUrgentes.getPila().size(), resueltasPila,
                colaProgramadas.getCola().size(), resueltasCola,
                listaGeneral.getLista().size(), resueltasLista,
                horasPorDepartamento, tareasPorDepartamento);
    }

    // Fila estándar de las tablas Pila, Cola, Lista y Cola de Prioridad.
    // La fecha se envía como LocalDate: la Vista la formatea y calcula los días restantes al pintar.
    private Object[] filaTarea(Tarea t) {
        return new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getNombreResponsable(),
                t.getUrgencia(), t.getTiempoEstimado(), t.getFechaEntrega()};
    }
}
