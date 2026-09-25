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

    // Persistencia: solo se permite guardar si la carga inicial terminó bien (evita sobrescribir la BD
    // con datos incompletos). Cada cambio se guarda al momento, no solo al cerrar la ventana.
    private boolean persistenciaActiva = false;
    private boolean errorGuardadoMostrado = false;

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
            persistenciaActiva = true;
        } catch (SQLException | RuntimeException ex) {
            persistenciaActiva = false;
            vista.logGUI("[BD] No se pudieron cargar los datos: " + ex.getMessage());
            JOptionPane.showMessageDialog(vista,
                    "No se pudieron cargar los datos guardados:\n" + ex.getMessage()
                            + "\n\nPara no borrar información, los cambios de esta sesión NO se guardarán en la base de datos.",
                    "Base de datos", JOptionPane.WARNING_MESSAGE);
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

    /** Guardado al cerrar la ventana (deja constancia en la consola). */
    public void guardarEnBD() {
        if (guardarDatos()) vista.logGUI("[BD] Datos guardados correctamente.");
    }

    /** Guardado inmediato tras cada cambio (silencioso; solo avisa si falla). */
    private void guardarCambios() {
        guardarDatos();
    }

    /** Escribe tareas y empleados en la BD. Devuelve true si se guardó. */
    private boolean guardarDatos() {
        if (!persistenciaActiva || tareaRepositorio == null || empleadoRepositorio == null) {
            return false;
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
            errorGuardadoMostrado = false;
            return true;
        } catch (SQLException ex) {
            vista.logGUI("[BD] No se pudieron guardar los datos: " + ex.getMessage());
            ex.printStackTrace();
            if (!errorGuardadoMostrado) {       // un solo aviso por racha de errores
                errorGuardadoMostrado = true;
                JOptionPane.showMessageDialog(vista, "No se pudieron guardar los datos en la base de datos:\n"
                        + ex.getMessage(), "Base de datos", JOptionPane.ERROR_MESSAGE);
            }
            return false;
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
        vista.getBtnEliminarEmpleado().addActionListener(e -> eliminarEmpleado());
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
        vista.getBtnLimpiarGrafo().addActionListener(e -> limpiarGrafo());
        // Filtro por departamento: al cambiar la tarea previa se recalculan las opciones de la siguiente
        vista.addCambioTareaPreviaListener(this::filtrarTareasSiguientesPorDepartamento);
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
        guardarCambios();                                            // persistir de inmediato
    }

    /**
     * Elimina un empleado por ID (campo de texto o, si está vacío, la fila seleccionada en la tabla).
     * Sus tareas activas quedan con Responsable Directo = null ("Sin Asignar"), de modo que en
     * Distribución de Tareas se reasignan o, si el departamento queda sin personal, aparece la alerta roja.
     */
    private void eliminarEmpleado() {
        String id = vista.getBuscarEmpleadoIdInput();
        if (id.isEmpty()) {
            String seleccionado = vista.getEmpleadoIdSeleccionado();
            id = seleccionado == null ? "" : seleccionado.trim();
        }
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Escribe el ID del empleado o selecciónalo en la tabla.",
                    "Eliminar empleado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Empleado empleado = arbolEmpleados.buscarPorId(id);          // búsqueda en el árbol binario
        if (empleado == null) {
            JOptionPane.showMessageDialog(vista, "ID de empleado no encontrado: " + id,
                    "Eliminar empleado", JOptionPane.WARNING_MESSAGE);
            vista.logGUI("[EMPLEADOS] Eliminación fallida: ID " + id + " no encontrado.");
            return;
        }

        // Tareas activas que tiene asignadas
        final String idEliminar = empleado.getId();
        List<Tarea> asignadas = new ArrayList<>();
        for (Tarea t : obtenerTodasLasTareas()) {
            if (t.tieneResponsable() && idEliminar.equals(t.getResponsableDirecto().getId())) asignadas.add(t);
        }

        int respuesta = JOptionPane.showConfirmDialog(vista,
                "¿Eliminar al empleado " + empleado.getNombre() + " (ID " + idEliminar + ", " + empleado.getDepartamento() + ")?"
                        + (asignadas.isEmpty() ? "" : "\nSus " + asignadas.size() + " tarea(s) pendiente(s) quedarán \"Sin Asignar\"."),
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) return;

        // a) Quitarlo de todas las estructuras de empleados
        arbolEmpleados.eliminar(idEliminar);
        gestorHashYAlgoritmos.eliminarEmpleado(idEliminar);
        listaEmpleadosMemoria.removeIf(e -> idEliminar.equals(e.getId()));
        if (empleadosMostrados != listaEmpleadosMemoria) {                 // vista filtrada por departamento
            empleadosMostrados.removeIf(e -> idEliminar.equals(e.getId()));
        }

        // b) Sus tareas quedan sin responsable
        for (Tarea t : asignadas) t.setResponsableDirecto(null);

        // Refrescar vista: tabla de empleados, selector de responsables y todas las tablas de tareas
        vista.limpiarBuscarEmpleadoId();
        refrescarResponsablesDisponibles();
        actualizarTablasYMetricas();

        boolean deptoSinPersonal = arbolEmpleados.obtenerPorDepartamento(empleado.getDepartamento()).isEmpty();
        vista.logGUI("[EMPLEADOS] Eliminado " + empleado.getNombre() + " (ID " + idEliminar + "). "
                + asignadas.size() + " tarea(s) quedaron Sin Asignar."
                + (deptoSinPersonal ? " El departamento " + empleado.getDepartamento() + " se quedó sin personal." : ""));
        JOptionPane.showMessageDialog(vista, "Empleado " + empleado.getNombre() + " eliminado.\n"
                        + asignadas.size() + " tarea(s) quedaron \"Sin Asignar\"."
                        + (deptoSinPersonal ? "\nAviso: " + empleado.getDepartamento() + " ya no tiene personal registrado." : ""),
                "Empleado eliminado", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(vista, "Ingrese un ID de tarea numérico válido.", "Atención", JOptionPane.WARNING_MESSAGE);
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
        vista.logGUI("[BÚSQUEDA] ID " + folio + (encontrada != null ? " encontrado" : " no encontrado")
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
                        + "El primer ID siempre es la raíz del árbol; sirve para copiar o reconstruir el árbol con la misma forma.";
            }
            case "Postorden" -> {
                secuencia = arbolTareas.postorden();
                explicacion = "Postorden (Izquierda → Derecha → Raíz): visita los hijos antes que el padre, así que la raíz "
                        + "queda al final. Es el orden que se usa para eliminar el árbol empezando por las hojas.";
            }
            default -> {
                secuencia = arbolTareas.inorden();
                explicacion = "Inorden (Izquierda → Raíz → Derecha): en un Árbol Binario de Búsqueda devuelve los IDs "
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

    // ==========================================================
    // MÓDULO GRAFO DE DEPENDENCIAS
    // ==========================================================

    /** Tareas activas (Pila, Cola, Lista y Cola de Prioridad) indexadas por folio. */
    private Map<Integer, Tarea> obtenerTareasActivasPorId() {
        Map<Integer, Tarea> activas = new TreeMap<>();
        for (Tarea t : obtenerTodasLasTareas()) activas.put(t.getId(), t);
        return activas;
    }

    /**
     * Convierte lo elegido/escrito ("#5 · Backup BD", "#5" o "5") en una tarea ACTIVA.
     * Si no es un folio válido, no existe o ya finalizó, muestra el error y devuelve null.
     */
    private Tarea validarTareaActiva(String texto, String campo, Map<Integer, Tarea> activas) {
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Selecciona o escribe el ID de la " + campo + ".",
                    "Dato faltante", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^#?\\s*(\\d+)\\s*(·.*)?$").matcher(texto);
        if (!m.find()) {
            JOptionPane.showMessageDialog(vista, "\"" + texto + "\" no es un ID válido para la " + campo
                    + ".\nEscribe solo el número (ej. 5) o elige una tarea de la lista.", "ID inválido", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(m.group(1));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "El ID de la " + campo + " es demasiado grande.", "ID inválido", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        Tarea activa = activas.get(id);
        if (activa == null) {
            // Distingue entre "ya finalizada" (existió) y "no existe"
            boolean existio = gestorHashYAlgoritmos.buscarTareaPorHash(id) != null;
            JOptionPane.showMessageDialog(vista,
                    existio ? "La tarea #" + id + " ya fue finalizada (salió de su estructura).\nSolo se pueden usar tareas activas."
                            : "No existe ninguna tarea con el ID #" + id + ".",
                    "Tarea no válida para la " + campo, JOptionPane.ERROR_MESSAGE);
            vista.logGUI("[GRAFO] Bloqueado: ID #" + id + (existio ? " ya finalizado." : " inexistente."));
            return null;
        }
        return activa;
    }

    private void agregarDependenciaGrafo() {
        Map<Integer, Tarea> activas = obtenerTareasActivasPorId();
        Tarea previa = validarTareaActiva(vista.getGrafoTareaPreviaInput(), "tarea previa", activas);
        if (previa == null) return;
        Tarea siguiente = validarTareaActiva(vista.getGrafoTareaSiguienteInput(), "tarea siguiente", activas);
        if (siguiente == null) return;

        try {
            // Valida la regla de mismo departamento + autodependencia, duplicado y ciclo
            grafoDependencias.agregarDependencia(previa, siguiente);
        } catch (GrafoDependencias.DependenciaEntreDepartamentosException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage(), "Transición no permitida", JOptionPane.WARNING_MESSAGE);
            vista.logGUI("[GRAFO] Bloqueado (departamentos distintos): #" + previa.getId() + " [" + previa.getDepartamento()
                    + "] → #" + siguiente.getId() + " [" + siguiente.getDepartamento() + "]");
            return;
        } catch (IllegalArgumentException ex) {   // misma tarea, duplicada o ciclo
            JOptionPane.showMessageDialog(vista, ex.getMessage(), "Dependencia no permitida", JOptionPane.ERROR_MESSAGE);
            vista.logGUI("[GRAFO] Bloqueado: " + ex.getMessage());
            return;
        }
        vista.logGUI("[GRAFO] Dependencia agregada: #" + previa.getId() + " → #" + siguiente.getId());
        vista.limpiarSelectoresGrafo();
        refrescarGrafo();
    }

    private void calcularOrdenTopologico() {
        refrescarGrafo();
        if (grafoDependencias.estaVacio()) {
            JOptionPane.showMessageDialog(vista, "Aún no hay dependencias registradas.", "Orden de ejecución", JOptionPane.INFORMATION_MESSAGE);
        } else {
            vista.logGUI("[GRAFO] Orden de ejecución: " + grafoDependencias.calcularOrdenEjecucion().secuencia());
        }
    }

    private void limpiarGrafo() {
        if (grafoDependencias.estaVacio()) return;
        int r = JOptionPane.showConfirmDialog(vista, "¿Eliminar todas las dependencias registradas?",
                "Limpiar grafo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        grafoDependencias.limpiar();
        vista.logGUI("[GRAFO] Se eliminaron todas las dependencias.");
        refrescarGrafo();
    }

    private GestionTareasView.OpcionTarea opcionDe(Tarea t) {
        return new GestionTareasView.OpcionTarea(t.getId(), t.getTitulo(), t.getDepartamento());
    }

    /**
     * FILTRO DEL FORMULARIO: si la tarea previa es una tarea activa válida, el selector de la
     * tarea siguiente solo ofrece tareas activas del MISMO departamento (excluyendo la propia previa).
     * Si aún no hay una previa válida, se muestran todas las tareas activas.
     */
    private void filtrarTareasSiguientesPorDepartamento() {
        Map<Integer, Tarea> activas = obtenerTareasActivasPorId();
        Tarea previa = null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^#?\\s*(\\d+)\\s*(·.*)?$")
                .matcher(vista.getGrafoTareaPreviaInput());
        if (m.find()) {
            try { previa = activas.get(Integer.parseInt(m.group(1))); } catch (NumberFormatException ignorado) { }
        }
        List<GestionTareasView.OpcionTarea> opciones = new ArrayList<>();
        String ayuda;
        if (previa == null) {
            for (Tarea t : activas.values()) opciones.add(opcionDe(t));
            ayuda = null;   // texto de ayuda general
        } else {
            for (Tarea t : activas.values()) {
                if (t.getId() != previa.getId() && GrafoDependencias.mismoDepartamento(t, previa)) opciones.add(opcionDe(t));
            }
            ayuda = opciones.isEmpty()
                    ? "No hay otras tareas activas de " + previa.getDepartamento() + " con las que relacionar la tarea #" + previa.getId() + "."
                    : "Filtro activo: solo tareas de " + previa.getDepartamento() + " (" + opciones.size()
                      + (opciones.size() == 1 ? " disponible" : " disponibles") + ").";
        }
        vista.setOpcionesTareaSiguiente(opciones, ayuda);
    }

    /** Quita del grafo las tareas que ya no están activas y actualiza selectores, orden y dibujo. */
    private void refrescarGrafo() {
        Map<Integer, Tarea> activas = obtenerTareasActivasPorId();
        for (Integer id : new ArrayList<>(grafoDependencias.getTareas())) {
            if (!activas.containsKey(id)) {
                grafoDependencias.eliminarTarea(id);
                vista.logGUI("[GRAFO] Tarea #" + id + " finalizada: se retiró del grafo con sus dependencias.");
            }
        }
        List<GestionTareasView.OpcionTarea> opciones = new ArrayList<>();
        for (Tarea t : activas.values()) opciones.add(opcionDe(t));
        vista.setTareasActivasGrafo(opciones);
        filtrarTareasSiguientesPorDepartamento();

        GrafoDependencias.OrdenEjecucion orden;
        try {
            orden = grafoDependencias.calcularOrdenEjecucion();
        } catch (IllegalStateException ex) {      // no debería ocurrir: los ciclos se bloquean al agregar
            orden = new GrafoDependencias.OrdenEjecucion(List.of(), Map.of());
            vista.logGUI("[GRAFO] " + ex.getMessage());
        }
        String info = grafoDependencias.estaVacio() ? "Sin dependencias registradas."
                : grafoDependencias.getTareas().size() + " tareas conectadas  ·  "
                  + grafoDependencias.getNumeroDependencias() + " dependencias  ·  cada flecha va de la tarea previa a la que depende de ella.";
        vista.mostrarGrafo(activas, grafoDependencias.getDependencias(), orden.secuencia(), orden.etapas(), info);
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
        // Pila (LIFO): el siguiente a atender es el TOPE = último elemento insertado (última fila)
        vista.getModeloPila().setRowCount(0);
        Tarea tope = pilaUrgentes.peek();
        for (Tarea t : pilaUrgentes.getPila()) vista.getModeloPila().addRow(filaConTurno(t, t == tope));

        // Cola (FIFO): el siguiente a atender es el FRENTE = primer elemento insertado (primera fila)
        vista.getModeloCola().setRowCount(0);
        Tarea frente = colaProgramadas.front();
        for (Tarea t : colaProgramadas.getCola()) vista.getModeloCola().addRow(filaConTurno(t, t == frente));

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

        // Grafo: solo tareas activas (las finalizadas salen con sus dependencias)
        refrescarGrafo();

        vista.actualizarDashboard(pilaUrgentes.getPila().size(), resueltasPila,
                colaProgramadas.getCola().size(), resueltasCola,
                listaGeneral.getLista().size(), resueltasLista,
                horasPorDepartamento, tareasPorDepartamento);

        // Persistir de inmediato (altas, procesamiento, distribución, eliminación de empleados...)
        guardarCambios();
    }

    // Fila estándar de las tablas Pila, Cola, Lista y Cola de Prioridad.
    // La fecha se envía como LocalDate: la Vista la formatea y calcula los días restantes al pintar.
    private Object[] filaTarea(Tarea t) {
        return new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getNombreResponsable(),
                t.getUrgencia(), t.getTiempoEstimado(), t.getFechaEntrega()};
    }

    // Fila de Pila/Cola: primera columna = ¿es el siguiente a atender? (la Vista dibuja la flecha)
    private Object[] filaConTurno(Tarea t, boolean siguiente) {
        Object[] base = filaTarea(t);
        Object[] fila = new Object[base.length + 1];
        fila[0] = siguiente;
        System.arraycopy(base, 0, fila, 1, base.length);
        return fila;
    }
}
