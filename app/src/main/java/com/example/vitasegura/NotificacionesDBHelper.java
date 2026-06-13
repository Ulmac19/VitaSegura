package com.example.vitasegura;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper de SQLite que persiste el historial de notificaciones del cuidador.
 *
 * Implementa el patrón singleton (getInstance) y mantiene la conexión abierta
 * durante el ciclo de vida de la aplicación; por ello no se llama a db.close()
 * dentro de sus métodos. Todas las alertas se guardan sin importar su antigüedad;
 * limpiarHistorialAntiguo() purga los registros con más de 30 días.
 */
public class NotificacionesDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "VitaSeguraNotis.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NOTIFICACIONES = "notificaciones";

    private static NotificacionesDBHelper instance;

    public static synchronized NotificacionesDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotificacionesDBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private NotificacionesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NOTIFICACIONES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "mensaje TEXT, " +
                "hora TEXT, " +
                "fecha TEXT, " +
                "esEmergencia INTEGER, " +
                "timestamp_ms INTEGER)"; // timestamp en milisegundos para calcular el límite de 30 días
        db.execSQL(createTable);
    }

    /** Recrea la tabla al cambiar la versión del esquema. */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICACIONES);
        onCreate(db);
    }

    /** Inserta una notificación en el historial. */
    public void insertarNotificacion(String mensaje, String hora, String fecha, boolean esEmergencia, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("mensaje", mensaje);
        values.put("hora", hora);
        values.put("fecha", fecha);
        values.put("esEmergencia", esEmergencia ? 1 : 0);
        values.put("timestamp_ms", timestamp);
        db.insert(TABLE_NOTIFICACIONES, null, values);
    }

    /** Devuelve las notificaciones del historial filtradas por tipo (emergencias o avisos). */
    public List<Notificacion> obtenerNotificaciones(boolean emergencias) {
        List<Notificacion> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1 = emergencia, 0 = información
        int tipo = emergencias ? 1 : 0;

        // Ordenadas de la más reciente a la más antigua
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
        return lista;
    }

    /** Elimina las notificaciones con más de 30 días de antigüedad. */
    public void limpiarHistorialAntiguo() {
        SQLiteDatabase db = this.getWritableDatabase();
        // 30 días expresados en milisegundos
        long limiteTiempo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        db.delete(TABLE_NOTIFICACIONES, "timestamp_ms < ?", new String[]{String.valueOf(limiteTiempo)});
    }
}
