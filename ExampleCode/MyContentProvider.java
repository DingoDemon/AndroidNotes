package com.example.dingo.demo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

    public class MyContentProvider extends ContentProvider {

    private static final String AUTHORITY = "com.example.dingo.demo.MyContentProvider";

    public static final String PERSON_CONTENT_URI = "content://" + AUTHORITY + "/person";

    public static final int PERSON_URI_CODE = 0;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "person", 0);
    }

    private Context context;
    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        context = getContext();
        initProviderData();
        return false;
    }

    private void initProviderData() {
        db = new PersonDBHelper(context).getWritableDatabase();

    }


    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String tableName = getTableName(uri);
        if (tableName == null) {
            throw new IllegalArgumentException("UnSupported URI: " + uri);
        }
        return db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
    }


    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        String tableName = getTableName(uri);
        if (tableName == null) {
            throw new IllegalArgumentException("UnSupported URI: " + uri);
        }
        db.insert(tableName, null, values);
        context.getContentResolver().notifyChange(uri, null);
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        String tableName = getTableName(uri);
        if (tableName == null) {
            throw new IllegalArgumentException("UnSupported URI: " + uri);
        }
        int count = db.delete(tableName, selection, selectionArgs);
        if (count > 0) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return count;

    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        String tableName = getTableName(uri);
        if (tableName == null) {
            throw new IllegalArgumentException("UnSupported URI: " + uri);
        }
        int row = db.update(tableName, values,selection, selectionArgs);
        if (row > 0) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return row;

    }

    private String getTableName(Uri uri) {
        if (uriMatcher.match(uri) == PERSON_URI_CODE) {
            return PersonDBHelper.USER_TABLE_NAME;
        }
        return "";
    }


}
