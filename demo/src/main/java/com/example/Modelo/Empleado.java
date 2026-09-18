package com.example.Modelo;

public class Empleado {
    
    private String id;
    private String nombre;
    private String departamento;

    public Empleado(String id, String nombre, String departamento) {
        this.id = id;
        this.nombre = nombre;
        this.departamento = departamento;
    }

    public String getId(){ return id; }
    public String getNombre() { return nombre; }
    public String getDepartamento() { return departamento; }
    
}