package com.example.Modelo;

import java.time.LocalDate;

public class Tarea {
    private static int contadorId = 1;
    private int id;
    private String titulo;
    private String departamento;
    private int urgencia; // 1 (Baja) a 5 (Crítica)
    private String tipoEstructura; // "Pila", "Cola", "Lista", "Prioridad"
    private int tiempoEstimado; // En horas
    private LocalDate fechaEntrega;

    // Constructor completo
    public Tarea(String titulo, String departamento, int urgencia, String tipoEstructura, int tiempoEstimado, LocalDate fechaEntrega) {
        this.id = contadorId++;
        this.titulo = titulo;
        this.departamento = departamento;
        this.urgencia = urgencia;
        this.tipoEstructura = tipoEstructura;
        this.tiempoEstimado = tiempoEstimado;
        this.fechaEntrega = fechaEntrega != null ? fechaEntrega : LocalDate.now();
    }

    // Constructor de compatibilidad
    public Tarea(String titulo, String departamento, int urgencia, String tipoEstructura) {
        this(titulo, departamento, urgencia, tipoEstructura, 2, LocalDate.now().plusDays(1));
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDepartamento() { return departamento; }
    public int getUrgencia() { return urgencia; }
    public String getTipoEstructura() { return tipoEstructura; }
    public int getTiempoEstimado() { return tiempoEstimado; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }

    @Override
    public String toString() {
        return String.format("[ID: %d] %s | Depto: %s | Urgencia: %d | Tiempo Est: %d hrs | Fecha: %s | Tipo: %s",
                id, titulo, departamento, urgencia, tiempoEstimado, fechaEntrega, tipoEstructura);
    }
}