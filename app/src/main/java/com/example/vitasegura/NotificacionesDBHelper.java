package com.example.vitasegura;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NotificacionesDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "VitaSeguraNotis.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NOTIFICACIONES = "notificaciones";

    public NotificacionesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NOTIFICACIONES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "mensaje TEXT, " +
                "hora TEXT, " +
                "fecha TEXT, " +
                "esEmergencia INTEGER, " +
                "timestamp_ms INTEGER)"; // Usamos milisegundos para calcular los 30 días
        db.execSQL(createTable);
    }

    // Actualiza la versión de la base de datos
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICACIONES);
        onCreate(db);
    }

    // Metodo para guardar una nueva notificación
    public void insertarNotificacion(String mensaje, String hora, String fecha, boolean esEmergencia, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("mensaje", mensaje);
        values.put("hora", hora);
        values.put("fecha", fecha);
        values.put("esEmergencia", esEmergencia ? 1 : 0);
        values.put("timestamp_ms", timestamp);
        db.insert(TABLE_NOTIFICACIONES, null, values);
        db.close();
    }

    // Metodo para obtener las notificaciones (filtrando por tipo)
    public List<Notificacion> obtenerNotificaciones(boolean emergencias) {
        List<Notificacion> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // significa true (emergencia), 0 significa false (información)
        int tipo = emergencias ? 1 : 0;

        // Ordenamos de la más reciente a la más antigua
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NOTIFICACIONES + " WHERE esEmergencia = ? ORDER BY timestamp_ms DESC", new String[]{String.valueOf(tipo)});

        if (cursor.moveToFirst()) {
            do {
                String mensaje = cursor.getString(cursor.getColumnIndexOrThrow("mensaje"));
                String hora = cursor.getString(cursor.getColumnIndexOrThrow("hora"));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"));

                lista.add(new Notificacion(mensaje, hora, fecha, emergencias));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    //Borra lo que tenga más de 30 días
    public void limpiarHistorialAntiguo() {
        SQLiteDatabase db = this.getWritableDatabase();
        // 30 días en milisegundos: 30L * 24 horas * 60 minutos * 60 segundos * 1000 milisegundos
        long limiteTiempo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        db.delete(TABLE_NOTIFICACIONES, "timestamp_ms < ?", new String[]{String.valueOf(limiteTiempo)});
        db.close();
    }
}
