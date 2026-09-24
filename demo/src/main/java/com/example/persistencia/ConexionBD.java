package com.example.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {
    private static final Path RUTA_BD = Path.of("data", "gestion_tareas.db");
    private final String url = "jdbc:sqlite:" + RUTA_BD;

    public ConexionBD() throws SQLException {
        try {
            Files.createDirectories(RUTA_BD.getParent());
        } catch (IOException ex) {
            throw new SQLException("No se pudo crear el directorio de la base de datos", ex);
        }
        crearTablas();
    }

    public Connection abrirConexion() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void crearTablas() throws SQLException {
        try (Connection conexion = abrirConexion(); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tareas (
                        id INTEGER PRIMARY KEY,
                        titulo TEXT NOT NULL,
                        departamento TEXT NOT NULL,
                        urgencia INTEGER NOT NULL,
                        tiempo_estimado INTEGER NOT NULL,
                        fecha_entrega TEXT NOT NULL,
                        estructura TEXT NOT NULL,
                        responsable_id TEXT
                    )
                    """);
            // Migración: bases de datos creadas antes de existir el "Responsable Directo"
            if (!existeColumna(conexion, "tareas", "responsable_id")) {
                sentencia.executeUpdate("ALTER TABLE tareas ADD COLUMN responsable_id TEXT");
            }
            sentencia.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS empleados (
                        id TEXT PRIMARY KEY,
                        nombre TEXT NOT NULL,
                        departamento TEXT NOT NULL
                    )
                    """);
        }
    }

    private boolean existeColumna(Connection conexion, String tabla, String columna) throws SQLException {
        try (Statement consulta = conexion.createStatement();
             ResultSet columnas = consulta.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (columnas.next()) {
                if (columna.equalsIgnoreCase(columnas.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
