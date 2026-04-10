package com.example.vitasegura;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AlertasOfflineDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME= "VitaSeguraOffline.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "AlertasOffline";
    public static final String COL_ID = "id";
    public static final String COL_TIPO = "tipo";
    public static final String COL_MENSAJE = "mensaje";
    public static final String COL_TIMESTAMP = "timestamp";

    public AlertasOfflineDBHelper(Context context) {
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

    //Guardar con limite de 50 (Logica FIFO)
    public void insertarAlerta(String tipo, String mensaje, long timestamp){
        SQLiteDatabase db = this.getWritableDatabase();

        //Verificar cuantos registros existen
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        if(cursor.moveToFirst() && cursor.getInt(0) >= 50){
            //Si hay 50 o mas, eliminar el mas viejo
            db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_ID +
                    " = (SELECT MIN(" + COL_ID + ") FROM " + TABLE_NAME + ")");
        }
        cursor.close();

        //Insertar alerta con fecha/hora
        ContentValues values = new ContentValues();
        values.put(COL_TIPO, tipo);
        values.put(COL_MENSAJE, mensaje);
        values.put(COL_TIMESTAMP, timestamp);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    //Eliminar al sincronizar
    public void eliminarAlertas(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

}
