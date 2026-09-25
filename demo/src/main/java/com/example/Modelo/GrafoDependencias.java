package com.example.Modelo;

import java.util.*;

// ==========================================================
// GRAFO DIRIGIDO DE DEPENDENCIAS ENTRE TAREAS (por folio / ID)
// ==========================================================
// Una arista previa -> siguiente significa: "previa debe terminarse antes que siguiente".
// - Lista de adyacencia + grado de entrada.
// - Se rechazan autodependencias, duplicados y cualquier arista que forme un ciclo,
//   así el grafo siempre es acíclico (DAG) y siempre existe un orden de ejecución válido.
// - Orden de ejecución: ordenamiento topológico (algoritmo de Kahn).
public class GrafoDependencias {

    private final Map<Integer, Set<Integer>> listaAdyacencia = new TreeMap<>();
    private final Map<Integer, Integer> gradoEntrada = new TreeMap<>();

    /** Resultado del ordenamiento: la secuencia y la etapa (nivel) de cada tarea. */
    public record OrdenEjecucion(List<Integer> secuencia, Map<Integer, Integer> etapas) {}

    public void agregarTarea(int id) {
        listaAdyacencia.putIfAbsent(id, new TreeSet<>());
        gradoEntrada.putIfAbsent(id, 0);
    }

    /**
     * Agrega la dependencia previa -> siguiente.
     * @throws IllegalArgumentException si es la misma tarea, si ya existía o si formaría un ciclo.
     */
    public void agregarDependencia(int previa, int siguiente) {
        if (previa == siguiente) {
            throw new IllegalArgumentException("Una tarea no puede depender de sí misma.");
        }
        if (existeDependencia(previa, siguiente)) {
            throw new IllegalArgumentException("La dependencia #" + previa + " → #" + siguiente + " ya existe.");
        }
        if (existeCamino(siguiente, previa)) {
            throw new IllegalArgumentException("No se puede agregar #" + previa + " → #" + siguiente
                    + ": se formaría un ciclo (la tarea #" + siguiente + " ya es requisito, directo o indirecto, de la #"
                    + previa + "), y ninguna de las dos podría empezar.");
        }
        agregarTarea(previa);
        agregarTarea(siguiente);
        listaAdyacencia.get(previa).add(siguiente);
        gradoEntrada.merge(siguiente, 1, Integer::sum);
    }

    /**
     * REGLA DE NEGOCIO: solo se permiten dependencias entre tareas del MISMO departamento.
     * Valida los departamentos y, si coinciden, crea la arista previa -> siguiente
     * (aplicando también las validaciones de autodependencia, duplicado y ciclo).
     * @throws DependenciaEntreDepartamentosException si los departamentos son distintos.
     */
    public void agregarDependencia(Tarea previa, Tarea siguiente) {
        if (!mismoDepartamento(previa, siguiente)) {
            throw new DependenciaEntreDepartamentosException(previa, siguiente);
        }
        agregarDependencia(previa.getId(), siguiente.getId());
    }

    public static boolean mismoDepartamento(Tarea a, Tarea b) {
        return a.getDepartamento() != null && b.getDepartamento() != null
                && a.getDepartamento().trim().equalsIgnoreCase(b.getDepartamento().trim());
    }

    /** Se lanza cuando se intenta relacionar tareas de departamentos diferentes. */
    public static class DependenciaEntreDepartamentosException extends IllegalArgumentException {
        public DependenciaEntreDepartamentosException(Tarea previa, Tarea siguiente) {
            super("⚠ Transición no permitida:\nLas dependencias solo se pueden establecer entre tareas del mismo "
                    + "departamento\n(Ejemplo: Tarea #" + previa.getId() + " [" + previa.getDepartamento() + "] vs Tarea #"
                    + siguiente.getId() + " [" + siguiente.getDepartamento() + "]).");
        }
    }

    public boolean existeDependencia(int previa, int siguiente) {
        return listaAdyacencia.containsKey(previa) && listaAdyacencia.get(previa).contains(siguiente);
    }

    /** DFS recursiva: ¿se puede llegar de 'origen' a 'destino' siguiendo las flechas? */
    public boolean existeCamino(int origen, int destino) {
        return existeCaminoRec(origen, destino, new HashSet<>());
    }

    private boolean existeCaminoRec(int actual, int destino, Set<Integer> visitados) {
        if (actual == destino) return true;
        if (!visitados.add(actual)) return false;
        for (int vecino : listaAdyacencia.getOrDefault(actual, Set.of())) {
            if (existeCaminoRec(vecino, destino, visitados)) return true;
        }
        return false;
    }

    /** Quita una tarea (p. ej. ya finalizada) junto con todas sus dependencias. */
    public boolean eliminarTarea(int id) {
        if (!listaAdyacencia.containsKey(id)) return false;
        for (int vecino : listaAdyacencia.get(id)) {
            gradoEntrada.merge(vecino, -1, Integer::sum);
        }
        listaAdyacencia.remove(id);
        gradoEntrada.remove(id);
        for (Set<Integer> destinos : listaAdyacencia.values()) {
            destinos.remove(id);
        }
        return true;
    }

    public void limpiar() {
        listaAdyacencia.clear();
        gradoEntrada.clear();
    }

    /**
     * Ordenamiento topológico (Kahn): se atienden primero las tareas sin requisitos pendientes;
     * al "resolver" una, se descuenta el grado de entrada de las que dependen de ella.
     * Entre varias disponibles se elige el folio menor, para que el resultado sea estable.
     * La etapa de cada tarea es 1 + la mayor etapa de sus requisitos (tareas de la misma
     * etapa no dependen entre sí y podrían resolverse en paralelo).
     */
    public OrdenEjecucion calcularOrdenEjecucion() {
        Map<Integer, Integer> grados = new HashMap<>(gradoEntrada);
        Map<Integer, Integer> etapas = new HashMap<>();
        PriorityQueue<Integer> disponibles = new PriorityQueue<>();
        for (Map.Entry<Integer, Integer> e : grados.entrySet()) {
            if (e.getValue() == 0) {
                disponibles.add(e.getKey());
                etapas.put(e.getKey(), 1);
            }
        }
        List<Integer> orden = new ArrayList<>();
        while (!disponibles.isEmpty()) {
            int actual = disponibles.poll();
            orden.add(actual);
            for (int vecino : listaAdyacencia.getOrDefault(actual, Set.of())) {
                etapas.merge(vecino, etapas.get(actual) + 1, Math::max);
                grados.merge(vecino, -1, Integer::sum);
                if (grados.get(vecino) == 0) disponibles.add(vecino);
            }
        }
        if (orden.size() != listaAdyacencia.size()) {
            throw new IllegalStateException("Existe una dependencia circular entre las tareas.");
        }
        // Se presenta por etapas (y folio dentro de cada etapa). Sigue siendo un orden topológico
        // válido porque toda flecha va de una etapa a otra mayor.
        orden.sort(Comparator.comparing((Integer id) -> etapas.get(id)).thenComparing(id -> id));
        return new OrdenEjecucion(orden, etapas);
    }

    public Set<Integer> getTareas() { return Collections.unmodifiableSet(listaAdyacencia.keySet()); }

    /** Lista de aristas como pares {previa, siguiente}. */
    public List<int[]> getDependencias() {
        List<int[]> aristas = new ArrayList<>();
        for (Map.Entry<Integer, Set<Integer>> e : listaAdyacencia.entrySet()) {
            for (int destino : e.getValue()) aristas.add(new int[]{e.getKey(), destino});
        }
        return aristas;
    }

    public int getNumeroDependencias() { return getDependencias().size(); }
    public boolean estaVacio() { return listaAdyacencia.isEmpty(); }
    public boolean contiene(int id) { return listaAdyacencia.containsKey(id); }
}
