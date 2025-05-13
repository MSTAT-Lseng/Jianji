package m20.simple.bookkeeping.database.wallet

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class WalletDao(context: Context) {

    private val dbHelper: WalletDatabaseHelper = WalletDatabaseHelper(context, context.resources)
    private val tag = "WalletDao"
    private val closeDatabase = true

    // 添加一个钱包
    fun addWallet(name: String, balance: Long = 0): Int {
        // Error check
        if (name.isEmpty()) return -1
        if (isWalletNameExists(name)) return -2

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WalletDatabaseHelper.COLUMN_NAME, name)
            put(WalletDatabaseHelper.COLUMN_BALANCE, balance)
        }
        val newRowId = db.insert(WalletDatabaseHelper.TABLE_WALLET, null, values)
        db.takeIf { closeDatabase }?.close()
        return newRowId.toInt()
    }

    // 根据ID删除钱包
    fun deleteWallet(walletId: Int): Int {
        val db = dbHelper.writableDatabase
        val selection = "${WalletDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(walletId.toString())
        val deletedRows = db.delete(WalletDatabaseHelper.TABLE_WALLET, selection, selectionArgs)
        db.takeIf { closeDatabase }?.close()
        return deletedRows
    }

    // 根据ID修改钱包名称
    fun updateWalletName(walletId: Int, newName: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WalletDatabaseHelper.COLUMN_NAME, newName)
        }
        val selection = "${WalletDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(walletId.toString())
        val updatedRows = db.update(WalletDatabaseHelper.TABLE_WALLET, values, selection, selectionArgs)
        db.takeIf { closeDatabase }?.close()
        return updatedRows
    }

    // 根据ID修改钱包余额
    fun updateWalletBalance(walletId: Int, newBalance: Long): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WalletDatabaseHelper.COLUMN_BALANCE, newBalance)
        }
        val selection = "${WalletDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(walletId.toString())
        val updatedRows = db.update(WalletDatabaseHelper.TABLE_WALLET, values, selection, selectionArgs)
        db.takeIf { closeDatabase }?.close()
        return updatedRows
    }

    // 根据ID查看钱包名称和余额
    fun getWalletNameAndBalance(walletId: Int): Pair<String, Long> {
        val db = dbHelper.readableDatabase
        val closeDB = fun(db: SQLiteDatabase) { db.takeIf { closeDatabase }?.close() }
        val projection = arrayOf(
            WalletDatabaseHelper.COLUMN_NAME,
            WalletDatabaseHelper.COLUMN_BALANCE
        )
        val selection = "${WalletDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(walletId.toString())
        val cursor = db.query(
            WalletDatabaseHelper.TABLE_WALLET,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_NAME))
                val balance = it.getLong(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_BALANCE))
                closeDB(db)
                return Pair(name, balance)
            }
        }
        closeDB(db)
        return Pair("", 0)
    }

    // 根据钱包名称获取钱包ID
    fun getWalletIdByName(walletName: String): Int {
        val db = dbHelper.readableDatabase
        val projection = arrayOf(WalletDatabaseHelper.COLUMN_ID)
        val selection = "${WalletDatabaseHelper.COLUMN_NAME} = ?"
        val selectionArgs = arrayOf(walletName)
        val cursor = db.query(
            WalletDatabaseHelper.TABLE_WALLET,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        var walletId: Int? = null
        cursor.use {
            if (it.moveToFirst()) {
                walletId = it.getInt(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_ID))
            }
        }
        db.takeIf { closeDatabase }?.close()
        return walletId ?: -1
    }

    // 根据名称查看是否已经存在钱包
    fun isWalletNameExists(walletName: String): Boolean {
        val db = dbHelper.readableDatabase
        val sql = "SELECT 1 FROM ${WalletDatabaseHelper.TABLE_WALLET} WHERE ${WalletDatabaseHelper.COLUMN_NAME} = ?"
        val cursor = db.rawQuery(sql, arrayOf(walletName))

        val exists = cursor.moveToFirst()
        cursor.close()
        db.takeIf { closeDatabase }?.close()
        return exists
    }

    // 查看所有钱包信息
    fun getAllWallets(): List<WalletInfo> {
        val walletList = mutableListOf<WalletInfo>()
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            WalletDatabaseHelper.COLUMN_ID,
            WalletDatabaseHelper.COLUMN_NAME,
            WalletDatabaseHelper.COLUMN_BALANCE
        )
        val cursor = db.query(
            WalletDatabaseHelper.TABLE_WALLET,
            projection,
            null,
            null,
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_ID))
                val name = it.getString(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_NAME))
                val balance = it.getLong(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_BALANCE))
                walletList.add(WalletInfo(id, name, balance))
            }
        }
        db.takeIf { closeDatabase }?.close()
        return walletList
    }

    data class WalletInfo(val id: Int, val name: String, val balance: Long)
}