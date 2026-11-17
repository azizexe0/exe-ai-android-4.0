
package com.exeai.assistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "exe_ai_memory.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_LEARNED = "learned";

    public DB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE " + TABLE_LEARNED + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "question TEXT NOT NULL, " +
            "answer TEXT NOT NULL, " +
            "source_url TEXT, " +
            "created_at INTEGER)"
        );
    }

    @Override
    public void onUpgrade(
        SQLiteDatabase db,
        int oldVersion,
        int newVersion
    ) {
        // Mevcut hafızayı koru.
    }

    public void saveLearned(
        String question,
        String answer,
        String sourceUrl
    ) {
        if (question == null || answer == null) return;
        if (question.trim().isEmpty() || answer.trim().isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("question", question.trim());
        values.put("answer", answer.trim());
        values.put("source_url", sourceUrl);
        values.put("created_at", System.currentTimeMillis());

        db.insert(TABLE_LEARNED, null, values);
    }

    public String findLearned(String query) {
        if (query == null || query.trim().isEmpty()) return null;

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
            "SELECT answer FROM " + TABLE_LEARNED +
            " WHERE LOWER(question) = LOWER(?)" +
            " ORDER BY id DESC LIMIT 1",
            new String[]{query.trim()}
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    public String knowledge(String query) {
        if (query == null || query.trim().isEmpty()) return null;

        String learned = findLearned(query);

        if (learned != null) {
            return learned;
        }

        return null;
    }
          }
          
