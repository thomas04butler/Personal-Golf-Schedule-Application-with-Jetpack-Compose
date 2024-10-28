package com.example.coursework

import GolfResultDatabaseHelper
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log

class GolfResultContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.coursework.provider.golfresult"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/golf_result")
        private const val RESULTS = 1
        private const val RESULT_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "golf_result", RESULTS)
            addURI(AUTHORITY, "golf_result/#", RESULT_ID)
        }
    }

    private lateinit var dbHelper: GolfResultDatabaseHelper

    override fun onCreate(): Boolean {
        dbHelper = GolfResultDatabaseHelper(context as Context)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        return when (uriMatcher.match(uri)) {
            RESULTS -> db.query(
                GolfResultDatabaseHelper.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
            )
            RESULT_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(
                    GolfResultDatabaseHelper.TABLE_NAME,
                    projection,
                    "${GolfResultDatabaseHelper.COLUMN_ID} = ?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    sortOrder
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.e("GolfResultContentProvider", "Hello")
        Log.e("GolfResultContentProvider", GolfResultDatabaseHelper.TABLE_NAME)
        val db = dbHelper.writableDatabase
        val id = db.insert(GolfResultDatabaseHelper.TABLE_NAME, null, values) // Ensure TABLE_NAME is correct
        if (id > 0) {
            context?.contentResolver?.notifyChange(uri, null)
            return ContentUris.withAppendedId(CONTENT_URI, id)
        }
        throw IllegalArgumentException("Failed to insert row into $uri")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = dbHelper.writableDatabase
        return when (uriMatcher.match(uri)) {
            RESULTS -> db.update(
                GolfResultDatabaseHelper.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )
            RESULT_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(
                    GolfResultDatabaseHelper.TABLE_NAME,
                    values,
                    "${GolfResultDatabaseHelper.COLUMN_ID} = ?",
                    arrayOf(id.toString())
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        return when (uriMatcher.match(uri)) {
            RESULTS -> db.delete(GolfResultDatabaseHelper.TABLE_NAME, selection, selectionArgs)
            RESULT_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(
                    GolfResultDatabaseHelper.TABLE_NAME,
                    "${GolfResultDatabaseHelper.COLUMN_ID} = ?",
                    arrayOf(id.toString())
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            RESULTS -> "vnd.android.cursor.dir/$AUTHORITY.golf_result"
            RESULT_ID -> "vnd.android.cursor.item/$AUTHORITY.golf_result"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}
