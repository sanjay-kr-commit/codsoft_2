package codsoft.internship.sanjay.quotesapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DB(
    context : Context
) : SQLiteOpenHelper(
    context , DATABASE_NAME ,
    null ,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "DB"
        const val DATABASE_VERSION = 1

    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE IF NOT EXISTS DB(" +
                    "ID TEXT PRIMARY KEY NOT NULL , " +
                    "VALUE TEXT" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL( "DROP TABLE DB" )
        onCreate( db )
    }

    infix fun String.insert( value : String ) : Boolean {
        try {
            writableDatabase
                .insert(
                    "DB" , null , ContentValues().apply {
                        put( "ID" , this@insert )
                        put( "VALUE" , value )
                    }
                )
        }catch ( e : Exception ){
            Log.e( "insertionError" , e.stackTraceToString() )
            return false
        }
        return true
    }

    val String.delete : Boolean
    get() {
        val effectedRow = writableDatabase
            .delete(
                "DB" , "ID=?" , arrayOf( this )
            )
        return effectedRow > 0
    }

    infix fun String.update( value: String ) : Boolean {
        delete
        return insert( value )
    }

    val String.value : String
        @SuppressLint("Range")
        get() = StringBuilder().apply {
            try {
                val cursor = readableDatabase
                    .rawQuery("SELECT * FROM DB WHERE ID=?", arrayOf(this@value))
                if (cursor.moveToNext()) {
                    append(cursor.getString(cursor.getColumnIndex("VALUE")))
                }
            } catch ( e : Exception ) {
                Log.e( "fetchError" , e.stackTraceToString() )
            }
        }.toString()


}