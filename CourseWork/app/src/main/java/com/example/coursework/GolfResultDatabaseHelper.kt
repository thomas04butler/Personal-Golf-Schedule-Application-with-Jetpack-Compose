import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class GolfResultDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "golf_result.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "golf_result"
        const val COLUMN_ID = "id"
        const val COLUMN_DATE_PLAYED = "date_played"
        const val COLUMN_NUMBER_OF_HOLES = "number_of_holes"
        const val COLUMN_GOLF_COURSE = "golf_course"
        const val COLUMN_PAR = "par"
        const val COLUMN_SCORE = "score"
        const val COLUMN_HOLE_PAR = "hole_par"
        const val COLUMN_HOLE_SCORE = "hole_score"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_DATE_PLAYED TEXT, "
                + "$COLUMN_GOLF_COURSE TEXT, "
                + "$COLUMN_NUMBER_OF_HOLES INTEGER, "
                + "$COLUMN_PAR INTEGER, "
                + "$COLUMN_SCORE INTEGER, "
                + "$COLUMN_HOLE_PAR TEXT, "
                + "$COLUMN_HOLE_SCORE TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}