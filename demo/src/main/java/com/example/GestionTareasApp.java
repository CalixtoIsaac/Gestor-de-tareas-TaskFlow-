package com.example;

// ==========================================
// IMPORTS
// ==========================================
import javax.swing.SwingUtilities;

import com.example.Controlador.GestionTareasController;
import com.example.Modelo.ColaTareas;
import com.example.Modelo.ListaTareas;
import com.example.Modelo.PilaTareas;
import com.example.Vista.GestionTareasView;

// ==========================================================
// LANZADOR PRINCIPAL
// ==========================================================
public class GestionTareasApp {

    // ===============================================================================================================
    // --- MAIN PRINCIPAL
    // ===============================================================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Instancias de los Modelos
            PilaTareas pila = new PilaTareas();
            ColaTareas cola = new ColaTareas();
            ListaTareas lista = new ListaTareas();

            // Instancia de la Vista
            GestionTareasView vista = new GestionTareasView();

            // Instancia del Controlador vinculando Vista y Modelos
            new GestionTareasController(vista, pila, cola, lista);

            // Desplegar la interfaz
            vista.setVisible(true);
        });
    }
}
