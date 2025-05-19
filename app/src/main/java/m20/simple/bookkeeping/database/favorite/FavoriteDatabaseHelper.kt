package m20.simple.bookkeeping.database.favorite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FavoriteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "favorite.db" // 数据库名称
        private const val DATABASE_VERSION = 1 // 数据库版本

        // 表名
        const val TABLE_NAME = "records"

        // 列名
        const val COLUMN_ID = "id" // 自增ID
        const val COLUMN_BILLING = "billing" // 账单ID
        const val COLUMN_TAGS = "tags" // 标签
    }

    private val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${TABLE_NAME} (" +
                "${COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                "${COLUMN_BILLING} INTEGER," + // Int类型，存储账单ID
                "${COLUMN_TAGS} TEXT)" // 文本类型，存储标签

    private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${TABLE_NAME}"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 数据库版本更新时调用，通常在此处进行数据迁移
        // 简单示例：删除旧表并重新创建
        // db.execSQL(SQL_DELETE_ENTRIES)
        // onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}