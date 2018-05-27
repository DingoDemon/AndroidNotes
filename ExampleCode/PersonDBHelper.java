package com.example.dingo.demo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PersonDBHelper extends SQLiteOpenHelper {


    public static final String DB_NAME = "person.db";


    public static final String USER_TABLE_NAME = "person";


    private static final int DB_VERSION = 1;


    public static final String CREATE_PERSON_TABLE = "CREATE TABLE IF NOT EXISTS " + USER_TABLE_NAME
            .concat("(id integer primary key autoincrement,")
            .concat("name varchar(10),")
            .concat("sex integer,")
            .concat("age integer)");

    public PersonDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PERSON_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
