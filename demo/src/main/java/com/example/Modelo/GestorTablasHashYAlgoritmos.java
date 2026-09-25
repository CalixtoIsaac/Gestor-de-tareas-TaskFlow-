package com.example.Modelo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestorTablasHashYAlgoritmos {
    private Map<Integer, Tarea> mapaTareas = new HashMap<>();
    private Map<String, Empleado> mapaEmpleados = new HashMap<>();

    // --- TABLAS HASH ---
    public void guardarTarea(Tarea tarea) {
        mapaTareas.put(tarea.getId(), tarea);
    }

    public Tarea buscarTareaPorHash(int id) {
        return mapaTareas.get(id); // O(1)
    }

    public void guardarEmpleado(Empleado empleado) {
        mapaEmpleados.put(empleado.getId(), empleado);
    }

    public Empleado eliminarEmpleado(String id) {
        return mapaEmpleados.remove(id);
    }

    public Empleado buscarEmpleadoPorHash(String id) {
        return mapaEmpleados.get(id);// 0(1)
    }

    // --- ALGORITMO DE ORDENAMIENTO: QuickSort por Urgencia ---
    public static void ordenarTareasPorUrgenciaQuickSort(List<Tarea> tareas, int izq, int der) {
        if(izq < der){
            int pivoteIdx = particionar(tareas, izq, der);
            ordenarTareasPorUrgenciaQuickSort(tareas, izq, pivoteIdx -1);
            ordenarTareasPorUrgenciaQuickSort(tareas, pivoteIdx + 1, der);
        }
    }

    private static int particionar(List<Tarea> tareas, int izq, int der) {
        Tarea pivote = tareas.get(der);
        int i = izq - 1;
        for (int j = izq; j < der; j++){
            if(tareas.get(j).getUrgencia() >= pivote.getUrgencia()) {
                i++;
                Tarea temp = tareas.get(i);
                tareas.set(i, tareas.get(j));
                tareas.set(j, temp);
            }
        }
        Tarea temp = tareas.get(i + 1);
        tareas.set(i + 1, tareas.get(der));
        tareas.set(der, temp);
        return i + 1;
    }

    // --- BUSQUEDA BINARIA POR ID ---
    public static Tarea busquedaBinariaPorId(List<Tarea> tareasOrdenadasPorId, int idBuscado) {
        int inicio = 0;
        int fin = tareasOrdenadasPorId.size() - 1;

        while (inicio <= fin) {
            int medio = inicio + (fin - inicio) / 2;
            int comp = Integer.compare(tareasOrdenadasPorId.get(medio).getId(), idBuscado);

            if (comp == 0) return tareasOrdenadasPorId.get(medio);
            if (comp < 0) inicio = medio + 1;
            else fin = medio - 1;
        }
        return null;
    }

    // --- BÚSQUEDAS CON CONTEO DE COMPARACIONES (para la comparativa de rendimiento) ---
    /** Resultado de una búsqueda: la tarea encontrada (o null) y cuántas comparaciones se hicieron. */
    public record ResultadoBusqueda(Tarea tarea, int comparaciones) {}

    /** Tabla hash: se calcula el hash del ID y se accede directo a su casilla (≈1 comparación). */
    public ResultadoBusqueda buscarPorHashConConteo(int id) {
        return new ResultadoBusqueda(mapaTareas.get(id), 1);
    }

    /** Búsqueda binaria sobre una lista ordenada por ID: descarta la mitad en cada comparación. */
    public static ResultadoBusqueda busquedaBinariaConConteo(List<Tarea> tareasOrdenadasPorId, int idBuscado) {
        int inicio = 0, fin = tareasOrdenadasPorId.size() - 1, comparaciones = 0;
        while (inicio <= fin) {
            int medio = inicio + (fin - inicio) / 2;
            comparaciones++;
            int comp = Integer.compare(tareasOrdenadasPorId.get(medio).getId(), idBuscado);
            if (comp == 0) return new ResultadoBusqueda(tareasOrdenadasPorId.get(medio), comparaciones);
            if (comp < 0) inicio = medio + 1;
            else fin = medio - 1;
        }
        return new ResultadoBusqueda(null, comparaciones);
    }

    /** Búsqueda secuencial: revisa los elementos uno por uno hasta encontrarlo. */
    public static ResultadoBusqueda busquedaSecuencialConConteo(List<Tarea> tareas, int idBuscado) {
        int comparaciones = 0;
        for (Tarea t : tareas) {
            comparaciones++;
            if (t.getId() == idBuscado) return new ResultadoBusqueda(t, comparaciones);
        }
        return new ResultadoBusqueda(null, comparaciones);
    }
}
