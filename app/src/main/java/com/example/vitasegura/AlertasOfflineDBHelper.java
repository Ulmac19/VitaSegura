package com.example.vitasegura;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper de SQLite que encola las alertas de emergencia generadas sin conexión.
 *
 * Implementa el patrón singleton (getInstance) y aplica una política FIFO con
 * tope de 50 registros: al llegar al límite descarta el más antiguo. Las alertas
 * almacenadas se suben a Firebase y se eliminan al recuperar la conexión desde
 * MainAdultoActivity.sincronizarAlertasPendientes().
 */
public class AlertasOfflineDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME= "VitaSeguraOffline.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "AlertasOffline";
    public static final String COL_ID = "id";
    public static final String COL_TIPO = "tipo";
    public static final String COL_MENSAJE = "mensaje";
    public static final String COL_TIMESTAMP = "timestamp";

    private static AlertasOfflineDBHelper instance;

    public static synchronized AlertasOfflineDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AlertasOfflineDBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private AlertasOfflineDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable= "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TIPO + " TEXT, " +
                COL_MENSAJE + " TEXT, " +
                COL_TIMESTAMP + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /** Inserta una alerta en la cola, descartando la más antigua si ya hay 50 (FIFO). */
    public void insertarAlerta(String tipo, String mensaje, long timestamp){
        SQLiteDatabase db = this.getWritableDatabase();

        // Si la cola está llena (50 registros), elimina el más antiguo antes de insertar
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        if(cursor.moveToFirst() && cursor.getInt(0) >= 50){
            db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_ID +
                    " = (SELECT MIN(" + COL_ID + ") FROM " + TABLE_NAME + ")");
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(COL_TIPO, tipo);
        values.put(COL_MENSAJE, mensaje);
        values.put(COL_TIMESTAMP, timestamp);

        db.insert(TABLE_NAME, null, values);
    }

    /** Elimina una alerta de la cola tras subirla correctamente a Firebase. */
    public void eliminarAlertas(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

}
