package com.example.vitasegura; // Asegúrate de que esta línea esté presente

public class Medicamento {
    private String nombre;
    private String frecuencia;
    private String hora;
    private String dosis;
    private String notas;

    public Medicamento() {}

    public Medicamento(String nombre, String frecuencia, String hora, String dosis, String notas) {
        this.nombre = nombre;
        this.frecuencia = frecuencia;
        this.hora = hora;
        this.dosis = dosis;
        this.notas = notas;
    }

    // Getters y Setters...
    public String getNombre() { return nombre; }
    public String getFrecuencia() { return frecuencia; }
    public String getHora() { return hora; }
    public String getDosis() { return dosis; }
    public String getNotas() { return notas; }
}