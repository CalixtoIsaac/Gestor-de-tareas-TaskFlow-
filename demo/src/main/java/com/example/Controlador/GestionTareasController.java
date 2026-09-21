package com.example.Controlador;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

import com.example.Modelo.*;
import com.example.Vista.*;

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
    private List<Empleado> listaEmpleadosMemoria;

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
        this.listaEmpleadosMemoria = new ArrayList<>();

        initControlador();
        actualizarTablasYMetricas();
    }

    private void initControlador() {
        // Eventos base
        vista.getBtnAgregar().addActionListener(e -> agregarTarea());
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

        // Eventos Recursividad y Divide & Vencerás
        vista.getBtnCalcularTiempoRecursivo().addActionListener(e -> calcularTiempoRecursivo());
        vista.getBtnDistribuirDivideVenceras().addActionListener(e -> distribuirDivideVenceras());

        // Eventos Tablas Hash y Algoritmos
        vista.getBtnBuscarHash().addActionListener(e -> buscarHashO1());
        vista.getBtnQuickSortUrgencia().addActionListener(e -> ordenarQuickSort());
        vista.getBtnBuscarBinaria().addActionListener(e -> buscarBinaria());

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

        Tarea nueva = new Tarea(titulo, depto, urgencia, tipoEst, tiempo, fechaEntrega);
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

        vista.limpiarTituloInput();
        vista.getSelectorFecha().limpiar();
        actualizarTablasYMetricas();
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
        String res = "=== CÁLCULO RECURSIVO DE TIEMPOS ESTIMADOS ===\n\n" +
                "Total de tareas analizadas: " + todas.size() + "\n" +
                "Tiempo Total Estimado acumulado: " + tiempoTotal + " horas.\n";
        vista.setResultadoRecursivo(res);
        vista.logGUI("[RECURSIÓN] Cálculo de tiempo finalizado: " + tiempoTotal + " hrs.");
    }

    private void distribuirDivideVenceras() {
        List<Tarea> tareas = obtenerTodasLasTareas();
        if (tareas.isEmpty() || listaEmpleadosMemoria.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Debe haber al menos 1 tarea y 1 empleado registrado.", "Divide y Vencerás", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, List<Tarea>> distribucion = ProcesadorRecursivo.distribuirTareasDivideYVenceras(tareas, listaEmpleadosMemoria);

        StringBuilder sb = new StringBuilder("=== DISTRIBUCIÓN EQUILIBRADA DE TAREAS (DIVIDE Y VENCERÁS) ===\n\n");
        for (Empleado emp : listaEmpleadosMemoria) {
            sb.append("Empleado: ").append(emp.getNombre()).append(" [ID: ").append(emp.getId()).append("] - Depto: ").append(emp.getDepartamento()).append("\n");
            List<Tarea> asignadas = distribucion.get(emp.getId());
            if (asignadas != null && !asignadas.isEmpty()) {
                for (Tarea t : asignadas) {
                    sb.append("   -> ").append(t.toString()).append("\n");
                }
            } else {
                sb.append("   -> Sin tareas asignadas.\n");
            }
            sb.append("\n");
        }
        vista.setResultadoRecursivo(sb.toString());
        vista.logGUI("[DIVIDE Y VENCERÁS] Distribución realizada entre " + listaEmpleadosMemoria.size() + " empleados.");
    }

    private void buscarHashO1() {
        try {
            int id = Integer.parseInt(vista.getBuscarHashIdInput());
            Tarea t = gestorHashYAlgoritmos.buscarTareaPorHash(id);
            if (t != null) {
                JOptionPane.showMessageDialog(vista, "Tarea Encontrada mediante HashMap (O(1)):\n" + t.toString(), "Tabla Hash", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(vista, "No existe tarea registrada con ID: " + id, "Tabla Hash", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vista, "Ingrese un ID entero válido.", "Atención", JOptionPane.WARNING_MESSAGE);
        }
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

    private void buscarBinaria() {
        try {
            int id = Integer.parseInt(vista.getBuscarBinariaIdInput());
            List<Tarea> lista = obtenerTodasLasTareas();
            lista.sort(Comparator.comparingInt(Tarea::getId));

            Tarea t = GestorTablasHashYAlgoritmos.busquedaBinariaPorId(lista, id);
            if (t != null) {
                JOptionPane.showMessageDialog(vista, "Tarea Encontrada mediante Búsqueda Binaria:\n" + t.toString(), "Búsqueda Binaria", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(vista, "No se encontró la tarea con ID: " + id, "Búsqueda Binaria", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vista, "Ingrese un ID numérico.", "Atención", JOptionPane.WARNING_MESSAGE);
        }
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
        return todas;
    }

    private void actualizarTablaEmpleados(List<Empleado> empleados) {
        vista.getModeloEmpleados().setRowCount(0);
        for (Empleado e : empleados) {
            vista.getModeloEmpleados().addRow(new Object[]{e.getId(), e.getNombre(), e.getDepartamento()});
        }
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
        String depto = vista.getDepartamentoSeleccionado();
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
        for (Tarea t : pilaUrgentes.getPila()) vista.getModeloPila().addRow(new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getUrgencia(), t.getTiempoEstimado()});

        vista.getModeloCola().setRowCount(0);
        for (Tarea t : colaProgramadas.getCola()) vista.getModeloCola().addRow(new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getUrgencia(), t.getTiempoEstimado()});

        vista.getModeloLista().setRowCount(0);
        for (Tarea t : listaGeneral.getLista()) vista.getModeloLista().addRow(new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getUrgencia(), t.getTiempoEstimado()});

        vista.getModeloPrioridad().setRowCount(0);
        for (Tarea t : colaPrioridad.obtenerTareasOrdenadas()) vista.getModeloPrioridad().addRow(new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getUrgencia(), t.getTiempoEstimado(), t.getFechaEntrega()});

        vista.getModeloTodas().setRowCount(0);
        for (Tarea t : obtenerTodasLasTareas()) vista.getModeloTodas().addRow(new Object[]{t.getId(), t.getTitulo(), t.getDepartamento(), t.getUrgencia(), t.getTipoEstructura()});

        vista.actualizarMetricas(pilaUrgentes.getPila().size(), resueltasPila, colaProgramadas.getCola().size(), resueltasCola, listaGeneral.getLista().size(), resueltasLista);
    }
}