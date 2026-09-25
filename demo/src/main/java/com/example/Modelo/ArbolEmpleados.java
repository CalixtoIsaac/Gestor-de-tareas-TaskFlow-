package com.example.Modelo;

import java.util.ArrayList;
import java.util.List;

public class ArbolEmpleados {
    private class Nodo {
        Empleado empleado;
        Nodo izquierdo, derecho;

        Nodo(Empleado empleado){
            this.empleado = empleado;
        }
    }

    private Nodo raiz;

    public void insertar(Empleado empleado){
        raiz = insertarRec(raiz, empleado);
    }

    private Nodo insertarRec(Nodo actual, Empleado empleado) {
        if (actual == null) return new Nodo(empleado);

        int comp = empleado.getId().compareTo(actual.empleado.getId());
        if(comp < 0) {
            actual.izquierdo = insertarRec(actual.izquierdo, empleado);
        } else if(comp > 0) {
            actual.derecho = insertarRec(actual.derecho, empleado);
        }
        return actual;
    }

    public Empleado buscarPorId(String id) {
        return buscarRec(raiz, id);
    }

    private Empleado buscarRec(Nodo actual, String id) {
        if (actual == null) return null;
        if(id.equals(actual.empleado.getId())) return actual.empleado;

        if (id.compareTo((actual.empleado.getId()))< 0) {
            return buscarRec(actual.izquierdo, id);
        }
        return buscarRec(actual.derecho, id);
    }

    /** Elimina al empleado con ese ID del árbol. Devuelve el empleado eliminado o null si no existía. */
    public Empleado eliminar(String id) {
        Empleado encontrado = buscarPorId(id);
        if (encontrado != null) raiz = eliminarRec(raiz, id);
        return encontrado;
    }

    private Nodo eliminarRec(Nodo actual, String id) {
        if (actual == null) return null;
        int comp = id.compareTo(actual.empleado.getId());
        if (comp < 0) {
            actual.izquierdo = eliminarRec(actual.izquierdo, id);
        } else if (comp > 0) {
            actual.derecho = eliminarRec(actual.derecho, id);
        } else {
            // Caso 1 y 2: sin hijos o con un solo hijo -> se "sube" el hijo
            if (actual.izquierdo == null) return actual.derecho;
            if (actual.derecho == null) return actual.izquierdo;
            // Caso 3: dos hijos -> se reemplaza por el sucesor inorden (mínimo del subárbol derecho)
            Nodo sucesor = actual.derecho;
            while (sucesor.izquierdo != null) sucesor = sucesor.izquierdo;
            actual.empleado = sucesor.empleado;
            actual.derecho = eliminarRec(actual.derecho, sucesor.empleado.getId());
        }
        return actual;
    }

    public List<Empleado> obtenerPorDepartamento(String departamento) {
        List<Empleado> resultado = new ArrayList<>();
        buscarPorDeptRec(raiz, departamento, resultado);
        return resultado;
    }

    private void buscarPorDeptRec(Nodo actual, String dpto, List<Empleado> resultado) {
        if(actual != null) {
            buscarPorDeptRec(actual.izquierdo, dpto, resultado);
            if (actual.empleado.getDepartamento().equalsIgnoreCase(dpto)){
                resultado.add(actual.empleado);
            }
            buscarPorDeptRec(actual.derecho, dpto, resultado);
        }
    }
}
