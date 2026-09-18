package com.example.Modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcesadorRecursivo {
    
    // 1. Recursividad para estadísticas (Suma recursiva de tiempos)
    public static int calcularTiempoTotalEstimado(List<Tarea> tareas, int index) {
        if(index >= tareas.size()){
            return 0; // Caso base
        }
        return tareas.get(index).getTiempoEstimado() + calcularTiempoTotalEstimado(tareas, index + 1);
    }

    // 2. Algoritmo "Divide y Vencerás" para distribución equilibrada de tareas
    public static Map<String, List<Tarea>> distribuirTareasDivideYVenceras(List<Tarea> tareas, List<Empleado> empleados) {
        Map<String, List<Tarea>> asignaciones = new HashMap<>();
        for(Empleado e: empleados){
            asignaciones.put(e.getId(), new ArrayList<>());
        }
        if(!tareas.isEmpty() && !empleados.isEmpty()) {
            distribuirRec(tareas, 0, tareas.size() - 1, empleados, asignaciones);
        }
        return asignaciones;
    }

    private static void distribuirRec(List<Tarea> tareas, int inicio, int fin, List<Empleado> empleados, Map<String, List<Tarea>> asignaciones) {
        // Caso base: Subproblema de 1 tarea
        if (inicio == fin) {
            Empleado menosCargado = obtenerEmpleadoConMenosCarga(empleados, asignaciones);
            asignaciones.get(menosCargado.getId()).add(tareas.get(inicio));
            return;
        }

        // Divide
        int medio = inicio + (fin - inicio) / 2;

        // Vence
        distribuirRec(tareas, inicio, medio, empleados, asignaciones);
        distribuirRec(tareas, medio + 1, fin, empleados, asignaciones);
    }

    private static Empleado obtenerEmpleadoConMenosCarga(List<Empleado> empleados, Map<String, List<Tarea>> asignaciones) {
        Empleado seleccionado = empleados.get(0);
        int miniCarga = asignaciones.get(seleccionado.getId()).size();

        for (Empleado e: empleados) {
            int cargaActual = asignaciones.get(e.getId()).size();
            if (cargaActual < miniCarga) {
                miniCarga = cargaActual;
                seleccionado = e;
            }
        }
        return seleccionado;
    }
}