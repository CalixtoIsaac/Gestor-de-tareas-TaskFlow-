package com.example.Modelo;

import java.util.ArrayList;
// ==========================================================
// --- LISTA (TAREAS POR DEPARTAMENTO - ACCESO ALEATORIO) ---
// ==========================================================
public class ListaTareas {
    private ArrayList<Tarea> lista = new ArrayList<>();

    public void insert(Tarea tarea) {
        lista.add(tarea);
        System.out.println("[CONSOLA - LISTA] INSERT: Tarea registrada por departamento -> " + tarea.getTitulo());
    }

    public boolean delete(int id) {
        for(int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == id) {
                Tarea eliminada = lista.remove(i);
                System.out.println("[CONSOLA - LISTA] DELETE: Tarea eliminada ID " + id + " -> " + eliminada.getTitulo());
                return true;
            }
        }
        System.out.println("[CONSOLA - LISTA] DELETE Fallido ID " + id + " no encontrado.");
        return false;
    }

    public ArrayList<Tarea> find(String departamento) {
        ArrayList<Tarea> resultado = new ArrayList<>();
        for(Tarea t: lista){
            if (t.getDepartamento().equalsIgnoreCase(departamento)){
                resultado.add(t);
            }
        }
        System.out.println("[CONSOLA - LISTA] FIND: Se encontraron " + resultado.size() + " tareas para el departamento " + departamento);
        return resultado;
    }

    public ArrayList<Tarea> getLista(){ return lista; }
}
