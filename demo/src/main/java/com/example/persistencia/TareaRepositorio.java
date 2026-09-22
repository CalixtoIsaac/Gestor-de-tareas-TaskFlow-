package com.example.persistencia;

import com.example.Modelo.Tarea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TareaRepositorio {
    private final ConexionBD conexionBD;

    public TareaRepositorio(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    public void guardarTodas(Collection<Tarea> tareas) throws SQLException {
        String insertar = """
                INSERT INTO tareas (id, titulo, departamento, urgencia, tiempo_estimado, fecha_entrega, estructura)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET titulo=excluded.titulo,
                    departamento=excluded.departamento, urgencia=excluded.urgencia,
                    tiempo_estimado=excluded.tiempo_estimado, fecha_entrega=excluded.fecha_entrega,
                    estructura=excluded.estructura
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

    public List<Tarea> cargarTodas() throws SQLException {
        List<Tarea> tareas = new ArrayList<>();
        String sql = "SELECT id, titulo, departamento, urgencia, tiempo_estimado, fecha_entrega, estructura FROM tareas ORDER BY id";
        try (Connection conexion = conexionBD.abrirConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                tareas.add(new Tarea(resultado.getInt("id"), resultado.getString("titulo"),
                        resultado.getString("departamento"), resultado.getInt("urgencia"),
                        resultado.getString("estructura"), resultado.getInt("tiempo_estimado"),
                        LocalDate.parse(resultado.getString("fecha_entrega"))));
            }
        }
        return tareas;
    }
}