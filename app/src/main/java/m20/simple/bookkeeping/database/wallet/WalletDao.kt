package m20.simple.bookkeeping.database.wallet

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log

class WalletDao(context: Context) {

    private val dbHelper: WalletDatabaseHelper = WalletDatabaseHelper(context, context.resources)
    private val tag = "WalletDao"
    private val closeDatabase = true

    // 添加一个钱包
    fun addWallet(name: String, balance: Int = 0): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WalletDatabaseHelper.COLUMN_NAME, name)
            put(WalletDatabaseHelper.COLUMN_BALANCE, balance)
        }
        val newRowId = db.insert(WalletDatabaseHelper.TABLE_WALLET, null, values)
        db.takeIf { closeDatabase }?.close()
        return newRowId
    }

    // 根据ID删除钱包
    fun deleteWallet(walletId: Long): Int {
        val db = dbHelper.writableDatabase
        val selection = "${WalletDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(walletId.toString())
        val deletedRows = db.delete(WalletDatabaseHelper.TABLE_WALLET, selection, selectionArgs)
        db.takeIf { closeDatabase }?.close()
        return deletedRows
    }

    // 根据ID修改钱包名称
    fun updateWalletName(walletId: Long, newName: String): Int {
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
    fun updateWalletBalance(walletId: Long, newBalance: Int): Int {
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
    fun getWalletNameAndBalance(walletId: Long): Pair<String?, Int?>? {
        val db = dbHelper.readableDatabase
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

        var name: String? = null
        var balance: Int? = null
        cursor.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_NAME))
                balance = it.getInt(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_BALANCE))
            }
        }
        db.takeIf { closeDatabase }?.close()
        return if (name != null && balance != null) Pair(name, balance) else null
    }

    // 根据钱包名称获取钱包ID
    fun getWalletIdByName(walletName: String): Long? {
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

        var walletId: Long? = null
        cursor.use {
            if (it.moveToFirst()) {
                walletId = it.getLong(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_ID))
            }
        }
        db.takeIf { closeDatabase }?.close()
        return walletId
    }

    // 根据名称查看是否已经存在钱包
    fun isWalletNameExists(walletName: String): Boolean {
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

        val exists = cursor.count > 0
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
                val id = it.getLong(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_ID))
                val name = it.getString(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_NAME))
                val balance = it.getInt(it.getColumnIndexOrThrow(WalletDatabaseHelper.COLUMN_BALANCE))
                walletList.add(WalletInfo(id, name, balance))
            }
        }
        db.takeIf { closeDatabase }?.close()
        return walletList
    }

    data class WalletInfo(val id: Long, val name: String, val balance: Int)
}