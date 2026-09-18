package com.example.Modelo;

import java.util.*;

public class GrafoDependencias {
    private Map<String, List<String>> listaAdyacencia = new HashMap<>();
    private Map<String, Integer> gradoEntrada = new HashMap<>();

    public void agregarTarea(String idTarea) {
        listaAdyacencia.putIfAbsent(idTarea, new ArrayList<>());
        gradoEntrada.putIfAbsent(idTarea, 0);
    }

    public void agregarDependencia(String tareaPrevia, String tareaSiguiente) {
        agregarTarea(tareaPrevia);
        agregarTarea(tareaSiguiente);

        listaAdyacencia.get(tareaPrevia).add(tareaSiguiente);
        gradoEntrada.put(tareaSiguiente, gradoEntrada.get(tareaSiguiente) + 1);
    }

    public List<String> obtenerOrdenEjecucion() {
        List<String> orden = new ArrayList<>();
        Queue<String> colaSinDependencias = new LinkedList<>();
        Map<String, Integer> inDegreeCopy = new HashMap<>(gradoEntrada);

        for (Map.Entry<String, Integer> entry : inDegreeCopy.entrySet()) {
            if (entry.getValue() == 0) {
                colaSinDependencias.add(entry.getKey());
            }
        }

        while (!colaSinDependencias.isEmpty()) {
            String actual = colaSinDependencias.poll();
            orden.add(actual);

            if (listaAdyacencia.containsKey(actual)) {
                for (String vecino : listaAdyacencia.get(actual)) {
                    inDegreeCopy.put(vecino, inDegreeCopy.get(vecino) - 1);
                    if (inDegreeCopy.get(vecino) == 0) {
                        colaSinDependencias.add(vecino);
                    }
                }
            }
        }

        if (orden.size() != listaAdyacencia.size()) {
            throw new IllegalStateException("Error: Existe una dependencia circular en las tareas asignadas.");
        }

        return orden;
    }
}