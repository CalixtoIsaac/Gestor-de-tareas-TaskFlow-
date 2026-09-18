package com.example.Modelo;

import java.util.Comparator;
import java.util.PriorityQueue;

public class ColaPrioridadTareas {
    
    private PriorityQueue<Tarea> colaPrioridad;

    public ColaPrioridadTareas(){
        // Ordena por urgencia (mayor prioridad primero) y desempata por fecha de entrega más cercana
        Comparator<Tarea> comparador = (t1, t2) -> {
            if (t1.getUrgencia() != t2.getUrgencia()) {
                return Integer.compare(t2.getUrgencia(), t1.getUrgencia());
            }
            return t1.getFechaEntrega().compareTo(t2.getFechaEntrega());
        };
        this.colaPrioridad = new PriorityQueue<>(comparador);
    }

    public void agregarTarea(Tarea tarea) {
        colaPrioridad.add(tarea);
    }

    public Tarea extraerSiguienteTarea() {
        return colaPrioridad.poll();
    }

    public Tarea verSiguienteTarea() {
        return colaPrioridad.peek();
    }

    public boolean estaVacia() {
        return colaPrioridad.isEmpty();
    }
}
