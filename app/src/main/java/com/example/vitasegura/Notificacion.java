package com.example.vitasegura;

/**
 * Modelo de datos que representa una entrada del historial de notificaciones.
 *
 * Se utiliza para mostrar el historial persistido en SQLite mediante
 * NotificacionesDBHelper. El campo esEmergencia distingue las alertas críticas
 * (que se resaltan en rojo) de los avisos informativos.
 */
public class Notificacion {
    private String mensaje;
    private String hora;
    private String fecha;
    private boolean esEmergencia;

    public Notificacion(String mensaje, String hora, String fecha, boolean esEmergencia) {
        this.mensaje = mensaje;
        this.hora = hora;
        this.fecha = fecha;
        this.esEmergencia = esEmergencia;
    }

    public String getMensaje() { return mensaje; }
    public String getHora() { return hora; }
    public String getFecha() { return fecha; }
    public boolean isEsEmergencia() { return esEmergencia; }
}