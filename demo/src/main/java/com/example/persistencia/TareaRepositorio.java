package com.example.persistencia;

import com.example.Modelo.Empleado;
import com.example.Modelo.Tarea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class TareaRepositorio {
    private final ConexionBD conexionBD;

    public TareaRepositorio(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    public void guardarTodas(Collection<Tarea> tareas) throws SQLException {
        String insertar = """
                INSERT INTO tareas (id, titulo, departamento, urgencia, tiempo_estimado, fecha_entrega, estructura, responsable_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET titulo=excluded.titulo,
                    departamento=excluded.departamento, urgencia=excluded.urgencia,
                    tiempo_estimado=excluded.tiempo_estimado, fecha_entrega=excluded.fecha_entrega,
                    estructura=excluded.estructura, responsable_id=excluded.responsable_id
                """;
        try (Connection conexion = conexionBD.abrirConexion()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement borrar = conexion.prepareStatement("DELETE FROM tareas");
                 PreparedStatement sentencia = conexion.prepareStatement(insertar)) {
                borrar.executeUpdate();
                for (Tarea tarea : tareas) {
                    sentencia.setInt(1, tarea.getId());
                    sentencia.setString(2, tarea.getTitulo());
                    sentencia.setString(3, tarea.getDepartamento());
                    sentencia.setInt(4, tarea.getUrgencia());
                    sentencia.setInt(5, tarea.getTiempoEstimado());
                    sentencia.setString(6, tarea.getFechaEntrega().toString());
                    sentencia.setString(7, tarea.getTipoEstructura());
                    // NULL cuando la tarea está "Sin Asignar"
                    sentencia.setString(8, tarea.tieneResponsable() ? tarea.getResponsableDirecto().getId() : null);
                    sentencia.addBatch();
                }
                sentencia.executeBatch();
                conexion.commit();
            } catch (SQLException ex) {
                conexion.rollback();
                throw ex;
            }
        }
    }

    /**
     * Carga las tareas y resuelve su Responsable Directo mediante {@code buscarEmpleado}
     * (id -> Empleado). Si el empleado ya no existe o no pertenece al departamento de la
     * tarea, la tarea queda "Sin Asignar".
     */
    public List<Tarea> cargarTodas(Function<String, Empleado> buscarEmpleado) throws SQLException {
        List<Tarea> tareas = new ArrayList<>();
        String sql = "SELECT id, titulo, departamento, urgencia, tiempo_estimado, fecha_entrega, estructura, responsable_id FROM tareas ORDER BY id";
        try (Connection conexion = conexionBD.abrirConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                Tarea tarea = new Tarea(resultado.getInt("id"), resultado.getString("titulo"),
                        resultado.getString("departamento"), resultado.getInt("urgencia"),
                        resultado.getString("estructura"), resultado.getInt("tiempo_estimado"),
                        LocalDate.parse(resultado.getString("fecha_entrega")));
                String responsableId = resultado.getString("responsable_id");
                if (responsableId != null && buscarEmpleado != null) {
                    Empleado responsable = buscarEmpleado.apply(responsableId);
                    if (responsable != null && responsable.getDepartamento().equalsIgnoreCase(tarea.getDepartamento())) {
                        tarea.setResponsableDirecto(responsable);
                    }
                }
                tareas.add(tarea);
            }
        }
        return tareas;
    }
}