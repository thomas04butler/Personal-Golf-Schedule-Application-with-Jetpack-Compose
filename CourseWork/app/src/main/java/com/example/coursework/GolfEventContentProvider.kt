package com.example.coursework

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log

class GolfEventContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.coursework.provider.golfevents"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/golf_events")
        private const val EVENTS = 1
        private const val EVENT_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "golf_events", EVENTS)
            addURI(AUTHORITY, "golf_events/#", EVENT_ID)
        }
    }


    private lateinit var dbHelper: GolfEventDatabaseHelper

    override fun onCreate(): Boolean {
        dbHelper = GolfEventDatabaseHelper(context as Context)
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val db = dbHelper.readableDatabase
        return when (uriMatcher.match(uri)) {
            EVENTS -> db.query(GolfEventDatabaseHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
            EVENT_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(GolfEventDatabaseHelper.TABLE_NAME, projection, "${GolfEventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()), null, null, sortOrder)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.e("GolfResultContentProvider", "Hello")
        val db = dbHelper.writableDatabase
        val id = db.insert(GolfEventDatabaseHelper.TABLE_NAME, null, values)
        if (id > 0) {
            context?.contentResolver?.notifyChange(uri, null)
            return ContentUris.withAppendedId(CONTENT_URI, id)
        }
        throw IllegalArgumentException("Failed to insert row into $uri")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        return when (uriMatcher.match(uri)) {
            EVENTS -> db.update(GolfEventDatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
            EVENT_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(GolfEventDatabaseHelper.TABLE_NAME, values, "${GolfEventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        return when (uriMatcher.match(uri)) {
            EVENTS -> db.delete(GolfEventDatabaseHelper.TABLE_NAME, selection, selectionArgs)
            EVENT_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(GolfEventDatabaseHelper.TABLE_NAME, "${GolfEventDatabaseHelper.COLUMN_ID} = ?", arrayOf(id.toString()))
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            EVENTS -> "vnd.android.cursor.dir/$AUTHORITY.golf_events"
            EVENT_ID -> "vnd.android.cursor.item/$AUTHORITY.golf_events"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}