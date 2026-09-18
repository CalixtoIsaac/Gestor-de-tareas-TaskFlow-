package com.example.Modelo;

import java.util.LinkedList;
import java.util.Queue;
// ==========================================================
// --- COLA (TAREAS PROGRAMADAS - FIFO) ---
// ==========================================================
public class ColaTareas {
    private Queue<Tarea> cola = new LinkedList<>();

    public void enqueue(Tarea tarea) {
        cola.add(tarea);
        System.out.println("[CONSOLA - COLA] ENQUEUE: Tarea encolada -> " + tarea.getTitulo());
    }

    public Tarea dequeue() {
        if (!cola.isEmpty()){
            Tarea t = cola.poll();
            System.out.println("[CONSOLA - COLA] DEQUEUE: Tarea atendida -> " + t.getTitulo());
            return t;
        }
        System.out.println("[CONSOLA - COLA] DEQUEUE Fallido: Cola vacia.");
        return null;
    }

    public Tarea front() {
        return cola.peek();
    }

    public Queue<Tarea> getCola() { return cola; }
}
