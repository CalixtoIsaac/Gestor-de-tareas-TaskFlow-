package com.example.Modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class ProcesadorRecursivo {
    
    // 1. Recursividad para estadísticas (Suma recursiva de tiempos)
    public static int calcularTiempoTotalEstimado(List<Tarea> tareas, int index) {
        if(index >= tareas.size()){
            return 0; // Caso base
        }
        return tareas.get(index).getTiempoEstimado() + calcularTiempoTotalEstimado(tareas, index + 1);
    }

    // 2. Algoritmo "Divide y Vencerás" para distribución equilibrada de tareas
    public static Map<String, List<Tarea>> distribuirTareasDivideYVenceras(List<Tarea> tareas, ArbolEmpleados arbolEmpleados) {
        Map<String, List<Tarea>> asignaciones = new HashMap<>();

        if (tareas.isEmpty() || arbolEmpleados == null) {
            return asignaciones;
        }

        Map<String, List<Tarea>> tareasPorDepartamento = new LinkedHashMap<>();
        Map<String, List<Empleado>> empleadosPorDepartamento = new HashMap<>();

        for (Tarea tarea : tareas) {
            String departamento = tarea.getDepartamento();
            String claveDepartamento = departamento == null ? "" : departamento.toLowerCase();
            List<Tarea> tareasDelDepartamento = tareasPorDepartamento.computeIfAbsent(
                    claveDepartamento, clave -> new ArrayList<>());
            tareasDelDepartamento.add(tarea);

            if (!empleadosPorDepartamento.containsKey(claveDepartamento)) {
                empleadosPorDepartamento.put(claveDepartamento,
                        arbolEmpleados.obtenerPorDepartamento(departamento));
            }
        }

        for (List<Empleado> empleadosDelDepartamento : empleadosPorDepartamento.values()) {
            for (Empleado empleado : empleadosDelDepartamento) {
                asignaciones.put(empleado.getId(), new ArrayList<>());
            }
        }

        for (Map.Entry<String, List<Tarea>> grupo : tareasPorDepartamento.entrySet()) {
            List<Empleado> empleadosDelDepartamento = empleadosPorDepartamento.get(grupo.getKey());
            if (empleadosDelDepartamento != null && !empleadosDelDepartamento.isEmpty()) {
                List<Tarea> tareasDelDepartamento = grupo.getValue();
                distribuirRec(tareasDelDepartamento, 0, tareasDelDepartamento.size() - 1,
                        empleadosDelDepartamento, asignaciones);
            }
        }
        return asignaciones;
    }

    private static void distribuirRec(List<Tarea> tareas, int inicio, int fin, List<Empleado> empleados, Map<String, List<Tarea>> asignaciones) {
        if (inicio > fin || empleados.isEmpty()) {
            return;
        }

        // Caso base: Subproblema de 1 tarea
        if (inicio == fin) {
            Empleado menosCargado = obtenerEmpleadoConMenosCargaPorTiempo(empleados, asignaciones);
            asignaciones.get(menosCargado.getId()).add(tareas.get(inicio));
            return;
        }

        // Divide
        int medio = inicio + (fin - inicio) / 2;

        // Vence
        distribuirRec(tareas, inicio, medio, empleados, asignaciones);
        distribuirRec(tareas, medio + 1, fin, empleados, asignaciones);
    }

    private static Empleado obtenerEmpleadoConMenosCargaPorTiempo(List<Empleado> empleados, Map<String, List<Tarea>> asignaciones) {
        Empleado seleccionado = empleados.get(0);
        int miniCarga = calcularCarga(asignaciones.get(seleccionado.getId()));

        for (Empleado e: empleados) {
            int cargaActual = calcularCarga(asignaciones.get(e.getId()));
            if (cargaActual < miniCarga) {
                miniCarga = cargaActual;
                seleccionado = e;
            }
        }
        return seleccionado;
    }

    private static int calcularCarga(List<Tarea> tareas) {
        return calcularCargaRecursiva(tareas, 0);
    }

    private static int calcularCargaRecursiva(List<Tarea> tareas, int indice) {
        if (indice >= tareas.size()) {
            return 0;
        }
        return tareas.get(indice).getTiempoEstimado()
                + calcularCargaRecursiva(tareas, indice + 1);
    }
}