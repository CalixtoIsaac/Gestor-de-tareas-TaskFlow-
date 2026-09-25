package com.example.Modelo;

import java.util.ArrayList;
import java.util.List;

// ==========================================================
// ÁRBOL BINARIO DE BÚSQUEDA (ABB) DE TAREAS POR FOLIO / ID
// ==========================================================
// - Insertar, buscar y eliminar: recursivos, O(h) donde h = altura del árbol.
// - Recorridos recursivos: inorden (Izq-Raíz-Der), preorden (Raíz-Izq-Der), postorden (Izq-Der-Raíz).
// - Balanceo por Divide y Vencerás: se obtiene la secuencia ordenada (inorden) y se
//   reconstruye tomando el elemento central como raíz de cada subárbol -> altura mínima.
public class ArbolTareasABB {

    /** Nodo del árbol. Solo expone lectura para que la Vista pueda dibujarlo. */
    public static class Nodo {
        private final Tarea tarea;
        private Nodo izquierdo, derecho;

        private Nodo(Tarea tarea) { this.tarea = tarea; }

        public Tarea getTarea() { return tarea; }
        public int getId() { return tarea.getId(); }
        public Nodo getIzquierdo() { return izquierdo; }
        public Nodo getDerecho() { return derecho; }
    }

    /** Resultado de una búsqueda: la tarea (o null), comparaciones realizadas y la ruta de folios visitados. */
    public record ResultadoBusquedaABB(Tarea tarea, int comparaciones, List<Integer> ruta) {}

    private Nodo raiz;
    private int tamano;

    // ---------------------- INSERTAR ----------------------
    /** Inserta la tarea según su ID. Devuelve false si el ID ya existía (no se duplican folios). */
    public boolean insertar(Tarea tarea) {
        int antes = tamano;
        raiz = insertarRec(raiz, tarea);
        return tamano > antes;
    }

    private Nodo insertarRec(Nodo actual, Tarea tarea) {
        if (actual == null) {            // Caso base: posición libre encontrada
            tamano++;
            return new Nodo(tarea);
        }
        if (tarea.getId() < actual.getId()) {
            actual.izquierdo = insertarRec(actual.izquierdo, tarea);
        } else if (tarea.getId() > actual.getId()) {
            actual.derecho = insertarRec(actual.derecho, tarea);
        }
        return actual;                   // ID repetido: no se inserta
    }

    // ---------------------- BUSCAR ----------------------
    public Tarea buscar(int id) {
        return buscar(id, raiz);
    }

    private Tarea buscar(int id, Nodo actual) {
        if (actual == null) return null;
        if (id == actual.getId()) return actual.getTarea();
        return id < actual.getId() ? buscar(id, actual.izquierdo) : buscar(id, actual.derecho);
    }

    /** Búsqueda que además cuenta comparaciones y guarda la ruta recorrida (para la comparativa y el dibujo). */
    public ResultadoBusquedaABB buscarConConteo(int id) {
        List<Integer> ruta = new ArrayList<>();
        Tarea encontrada = buscarConteoRec(raiz, id, ruta);
        return new ResultadoBusquedaABB(encontrada, ruta.size(), ruta);
    }

    private Tarea buscarConteoRec(Nodo actual, int id, List<Integer> ruta) {
        if (actual == null) return null;
        ruta.add(actual.getId());        // una comparación por nodo visitado
        if (id == actual.getId()) return actual.getTarea();
        return id < actual.getId()
                ? buscarConteoRec(actual.izquierdo, id, ruta)
                : buscarConteoRec(actual.derecho, id, ruta);
    }

    // ---------------------- ELIMINAR ----------------------
    public boolean eliminar(int id) {
        int antes = tamano;
        raiz = eliminarRec(raiz, id);
        return tamano < antes;
    }

    private Nodo eliminarRec(Nodo actual, int id) {
        if (actual == null) return null;
        if (id < actual.getId()) {
            actual.izquierdo = eliminarRec(actual.izquierdo, id);
            return actual;
        }
        if (id > actual.getId()) {
            actual.derecho = eliminarRec(actual.derecho, id);
            return actual;
        }
        // Nodo encontrado
        if (actual.izquierdo == null) { tamano--; return actual.derecho; }
        if (actual.derecho == null) { tamano--; return actual.izquierdo; }
        // Dos hijos: se reemplaza por el sucesor inorden (mínimo del subárbol derecho)
        Nodo sucesor = actual.derecho;
        while (sucesor.izquierdo != null) sucesor = sucesor.izquierdo;
        Nodo reemplazo = new Nodo(sucesor.getTarea());
        reemplazo.izquierdo = actual.izquierdo;
        reemplazo.derecho = eliminarRec(actual.derecho, sucesor.getId());
        return reemplazo;
    }

    // ---------------------- RECORRIDOS RECURSIVOS ----------------------
    public List<Integer> inorden() {
        List<Integer> r = new ArrayList<>();
        inordenRec(raiz, r);
        return r;
    }

    private void inordenRec(Nodo n, List<Integer> r) {
        if (n == null) return;
        inordenRec(n.izquierdo, r);      // Izquierda
        r.add(n.getId());                // Raíz
        inordenRec(n.derecho, r);        // Derecha
    }

    public List<Integer> preorden() {
        List<Integer> r = new ArrayList<>();
        preordenRec(raiz, r);
        return r;
    }

    private void preordenRec(Nodo n, List<Integer> r) {
        if (n == null) return;
        r.add(n.getId());                // Raíz
        preordenRec(n.izquierdo, r);     // Izquierda
        preordenRec(n.derecho, r);       // Derecha
    }

    public List<Integer> postorden() {
        List<Integer> r = new ArrayList<>();
        postordenRec(raiz, r);
        return r;
    }

    private void postordenRec(Nodo n, List<Integer> r) {
        if (n == null) return;
        postordenRec(n.izquierdo, r);    // Izquierda
        postordenRec(n.derecho, r);      // Derecha
        r.add(n.getId());                // Raíz
    }

    // ---------------------- BALANCEO (DIVIDE Y VENCERÁS) ----------------------
    /** Reconstruye el árbol con altura mínima. Devuelve {alturaAntes, alturaDespues}. */
    public int[] balancear() {
        int alturaAntes = altura();
        List<Tarea> ordenadas = new ArrayList<>();
        recolectarInorden(raiz, ordenadas);
        raiz = construirBalanceado(ordenadas, 0, ordenadas.size() - 1);
        return new int[]{alturaAntes, altura()};
    }

    private void recolectarInorden(Nodo n, List<Tarea> destino) {
        if (n == null) return;
        recolectarInorden(n.izquierdo, destino);
        destino.add(n.getTarea());
        recolectarInorden(n.derecho, destino);
    }

    private Nodo construirBalanceado(List<Tarea> ordenadas, int inicio, int fin) {
        if (inicio > fin) return null;                       // Caso base: sublista vacía
        int medio = inicio + (fin - inicio) / 2;             // Divide: el central será la raíz
        Nodo nodo = new Nodo(ordenadas.get(medio));
        nodo.izquierdo = construirBalanceado(ordenadas, inicio, medio - 1); // Vence mitad izquierda
        nodo.derecho = construirBalanceado(ordenadas, medio + 1, fin);      // Vence mitad derecha
        return nodo;                                          // Combina
    }

    // ---------------------- MÉTRICAS ----------------------
    /** Altura en niveles (árbol vacío = 0, solo raíz = 1). */
    public int altura() { return alturaRec(raiz); }

    private int alturaRec(Nodo n) {
        return n == null ? 0 : 1 + Math.max(alturaRec(n.izquierdo), alturaRec(n.derecho));
    }

    /** Altura mínima posible para la cantidad actual de nodos: ceil(log2(n + 1)). */
    public int alturaMinima() {
        int niveles = 0;
        while ((1 << niveles) - 1 < tamano) niveles++;
        return niveles;
    }

    public boolean contiene(int id) { return buscar(id) != null; }
    public int getTamano() { return tamano; }
    public boolean estaVacio() { return raiz == null; }
    public Nodo getRaiz() { return raiz; }
}
