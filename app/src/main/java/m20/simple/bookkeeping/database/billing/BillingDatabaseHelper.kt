package m20.simple.bookkeeping.database.billing

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BillingDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "billing.db"
        private const val DATABASE_VERSION = 1

        // 表名
        const val TABLE_NAME = "records"

        // 列名
        const val COLUMN_ID = "id"
        const val COLUMN_TIME = "time"
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_IOTYPE = "iotype"
        const val COLUMN_CLASSIFY = "classify"
        const val COLUMN_NOTES = "notes"
        const val COLUMN_IMAGES = "images"
        const val COLUMN_DEPOSIT = "deposit"
        const val COLUMN_WALLET = "wallet"
        const val COLUMN_TAGS = "tags"
    }

    private val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${TABLE_NAME} (" +
                "${COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                "${COLUMN_TIME} INTEGER NOT NULL," + // 使用 INTEGER 存储 Unix 时间戳 (分钟精度)
    "${COLUMN_AMOUNT} INTEGER NOT NULL," +
    "${COLUMN_IOTYPE} INTEGER NOT NULL," +
    "${COLUMN_CLASSIFY} TEXT NOT NULL," +
    "${COLUMN_NOTES} TEXT," +
    "${COLUMN_IMAGES} TEXT," +
    "${COLUMN_DEPOSIT} TEXT NOT NULL," +
    "${COLUMN_WALLET} INTEGER NOT NULL," +
    "${COLUMN_TAGS} TEXT)"

    private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${TABLE_NAME}"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 当数据库版本更新时调用，这里可以进行数据迁移等操作
        // 示例：删除旧表并重新创建
        // db.execSQL(SQL_DELETE_ENTRIES)
        // onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}