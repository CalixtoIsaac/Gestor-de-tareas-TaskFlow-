package com.example;

// ==========================================
// IMPORTS
// ==========================================
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
            GestionTareasController controlador = new GestionTareasController(vista, pila, cola, lista);
            vista.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    controlador.guardarEnBD();
                }
            });

            // Desplegar la interfaz
            vista.setVisible(true);
        });
    }
}
