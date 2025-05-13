package m20.simple.bookkeeping.database.wallet

import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import m20.simple.bookkeeping.R

class WalletDatabaseHelper(context: Context, private val resources: Resources) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "WalletDatabaseHelper"
        private const val DATABASE_NAME = "wallet.db"
        private const val DATABASE_VERSION = 1 // 每次修改表结构需要增加版本号

        // 表名
        const val TABLE_WALLET: String = "records"

        // 列名
        const val COLUMN_ID: String = "id"
        const val COLUMN_NAME: String = "name"
        const val COLUMN_BALANCE: String = "balance"

        // 创建表的 SQL 语句
        private const val CREATE_TABLE_WALLET = "CREATE TABLE IF NOT EXISTS " + TABLE_WALLET + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_BALANCE + " INTEGER DEFAULT 0);"

    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建 wallet 表
        db.execSQL(CREATE_TABLE_WALLET)

        // 添加默认钱包条目
        insertDefaultWallet(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 当数据库版本升级时调用，通常用于处理表结构的变更
    }

    private fun insertDefaultWallet(db: SQLiteDatabase) {
        val values = ContentValues()
        values.put(COLUMN_NAME, resources.getString(R.string.db_default_wallet))
        values.put(COLUMN_BALANCE, 0)

        db.insert(TABLE_WALLET, null, values)
    }

}