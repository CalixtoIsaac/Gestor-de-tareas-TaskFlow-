package com.example.persistencia;

import com.example.Modelo.Empleado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EmpleadoRepositorio {
    private final ConexionBD conexionBD;

    public EmpleadoRepositorio(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    public void guardarTodos(Collection<Empleado> empleados) throws SQLException {
        try (Connection conexion = conexionBD.abrirConexion()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement borrar = conexion.prepareStatement("DELETE FROM empleados");
                 PreparedStatement sentencia = conexion.prepareStatement("""
                         INSERT INTO empleados (id, nombre, departamento) VALUES (?, ?, ?)
                         ON CONFLICT(id) DO UPDATE SET nombre=excluded.nombre, departamento=excluded.departamento
                         """)) {
                borrar.executeUpdate();
                for (Empleado empleado : empleados) {
                    sentencia.setString(1, empleado.getId());
                    sentencia.setString(2, empleado.getNombre());
                    sentencia.setString(3, empleado.getDepartamento());
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

    public List<Empleado> cargarTodos() throws SQLException {
        List<Empleado> empleados = new ArrayList<>();
        try (Connection conexion = conexionBD.abrirConexion();
             PreparedStatement sentencia = conexion.prepareStatement("SELECT id, nombre, departamento FROM empleados ORDER BY id");
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                empleados.add(new Empleado(resultado.getString("id"), resultado.getString("nombre"),
                        resultado.getString("departamento")));
            }
        }
        return empleados;
    }
}