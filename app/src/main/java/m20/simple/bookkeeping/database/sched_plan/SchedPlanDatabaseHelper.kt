package m20.simple.bookkeeping.database.sched_plan

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SchedPlanDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "sched_plan.db" // 数据库名称
        private const val DATABASE_VERSION = 1 // 数据库版本

        // 表名
        const val TABLE_NAME = "records"

        // 列名
        const val COLUMN_ID = "id" // 自增ID
        const val COLUMN_CYCLE = "cycle" // 重复周期
        const val COLUMN_DAY = "day" // 重复日期
        const val COLUMN_BILLING = "billing" // 账单内容
        const val COLUMN_TAGS = "tags" // 标签
    }

    private val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${TABLE_NAME} (" +
                "${COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${COLUMN_CYCLE} INTEGER NOT NULL, " +
                "${COLUMN_DAY} INTEGER NOT NULL, " +
                "${COLUMN_BILLING} TEXT NOT NULL, " +
                "${COLUMN_TAGS} TEXT)"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {}

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

}