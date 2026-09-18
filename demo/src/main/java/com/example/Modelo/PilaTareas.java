package com.example.Modelo;

import java.util.Stack;

// ==========================================================
// IMPLEMENTACIÓN DE ESTRUCTURAS DE DATOS
// ==========================================================
// --- PILA (TAREAS URGENTES - LIFO) ---
public class PilaTareas {
    private Stack<Tarea> pila = new Stack<>();

    public void push(Tarea tarea){
        pila.push(tarea);
        System.out.println("[CONSOLA - PILA] PUSH: Tarea urgente agregada -> " + tarea.getTitulo());
    }

    public Tarea pop(){
        if(!pila.isEmpty()) {
            Tarea t = pila.pop();
            System.out.println("[CONSOLA - PILA] POP: Tarea urgente procesada -> " +  t.getTitulo());
        return t;
        }
        System.out.println("[CONSOLA - PILA] POP Fallido: Pila vacia.");
        return null;
    }

    public Tarea peek(){
        return pila.isEmpty() ? null : pila.peek();
    }

    public Stack<Tarea> getPila() { return pila; }
}
