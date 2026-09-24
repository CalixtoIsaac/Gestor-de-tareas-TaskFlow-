package com.example.Modelo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Tarea {
    private static final DateTimeFormatter FORMATO_FECHA_VISTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static int contadorId = 1;
    private int id;
    private String titulo;
    private String departamento;
    private int urgencia; // 1 (Baja) a 5 (Crítica)
    private String tipoEstructura; // "Pila", "Cola", "Lista", "Prioridad"
    private int tiempoEstimado; // En horas
    private LocalDate fechaEntrega;
    // Responsable Directo (opcional). null = "Sin Asignar" -> se asigna después en Distribución.
    // Regla de negocio: si existe, debe pertenecer al MISMO departamento que la tarea.
    private Empleado responsableDirecto;

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

    public Tarea(int id, String titulo, String departamento, int urgencia, String tipoEstructura,
                 int tiempoEstimado, LocalDate fechaEntrega) {
        this.id = id;
        contadorId = Math.max(contadorId, id + 1);
        this.titulo = titulo;
        this.departamento = departamento;
        this.urgencia = urgencia;
        this.tipoEstructura = tipoEstructura;
        this.tiempoEstimado = tiempoEstimado;
        this.fechaEntrega = fechaEntrega != null ? fechaEntrega : LocalDate.now();
    }

    // Constructor completo con Responsable Directo (opcional)
    public Tarea(String titulo, String departamento, int urgencia, String tipoEstructura, int tiempoEstimado,
                 LocalDate fechaEntrega, Empleado responsableDirecto) {
        this(titulo, departamento, urgencia, tipoEstructura, tiempoEstimado, fechaEntrega);
        setResponsableDirecto(responsableDirecto);
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
    public Empleado getResponsableDirecto() { return responsableDirecto; }
    public boolean tieneResponsable() { return responsableDirecto != null; }

    public String getNombreResponsable() {
        return responsableDirecto != null ? responsableDirecto.getNombre() : "Sin Asignar";
    }

    // Días que faltan para la fecha de entrega respecto a hoy (negativo = ya venció)
    public long getDiasRestantes() { return calcularDiasRestantes(fechaEntrega); }
    public boolean estaVencida() { return getDiasRestantes() < 0; }
    public String getFechaEntregaFormateada() { return describirFechaEntrega(fechaEntrega); }

    public static long calcularDiasRestantes(LocalDate fecha) {
        return ChronoUnit.DAYS.between(LocalDate.now(), fecha);
    }

    // Ej.: "28/09/2026 (4 días restantes)", "24/09/2026 (vence hoy)", "20/09/2026 (vencida hace 4 días)"
    public static String describirFechaEntrega(LocalDate fecha) {
        if (fecha == null) return "Sin fecha";
        long dias = calcularDiasRestantes(fecha);
        String detalle;
        if (dias > 1) detalle = dias + " días restantes";
        else if (dias == 1) detalle = "1 día restante";
        else if (dias == 0) detalle = "vence hoy";
        else if (dias == -1) detalle = "vencida hace 1 día";
        else detalle = "vencida hace " + (-dias) + " días";
        return fecha.format(FORMATO_FECHA_VISTA) + " (" + detalle + ")";
    }

    // Asigna (o quita con null) el responsable validando la regla de negocio del departamento.
    public void setResponsableDirecto(Empleado responsable) {
        if (responsable != null && (departamento == null
                || !departamento.equalsIgnoreCase(responsable.getDepartamento()))) {
            throw new IllegalArgumentException("El responsable " + responsable.getNombre()
                    + " no pertenece al departamento " + departamento + ".");
        }
        this.responsableDirecto = responsable;
    }

    @Override
    public String toString() {
        return String.format("[ID: %d] %s | Depto: %s | Responsable: %s | Urgencia: %d | Tiempo Est: %d hrs | Entrega: %s | Tipo: %s",
                id, titulo, departamento, getNombreResponsable(), urgencia, tiempoEstimado, getFechaEntregaFormateada(), tipoEstructura);
    }
}