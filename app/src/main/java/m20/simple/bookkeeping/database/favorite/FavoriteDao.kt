package m20.simple.bookkeeping.database.favorite

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class FavoriteDao(context: Context) {

    private val dbHelper: FavoriteDatabaseHelper = FavoriteDatabaseHelper(context)

    // 获取可写数据库实例
    private val writableDatabase: SQLiteDatabase
        get() = dbHelper.writableDatabase

    // 获取只读数据库实例
    private val readableDatabase: SQLiteDatabase
        get() = dbHelper.readableDatabase

    /**
     * 定义数据类 FavoriteEntry 来表示收藏条目
     */
    data class FavoriteEntry(
        val id: Long,
        val billing: Long,
        val tags: String?
    )

    /**
     * 增加一个收藏条目
     * @param billing 账单ID
     * @param tags 标签
     * @return 插入记录的 ID，如果插入失败返回 -1
     */
    fun addFavorite(billing: Long, tags: String?): Long {
        val values = ContentValues().apply {
            put(FavoriteDatabaseHelper.COLUMN_BILLING, billing)
            put(FavoriteDatabaseHelper.COLUMN_TAGS, tags)
        }
        return writableDatabase.insert(FavoriteDatabaseHelper.TABLE_NAME, null, values)
    }

    /**
     * 根据 ID 删除一个收藏条目
     * @param id 要删除的记录 ID
     * @return 受影响的行数，通常为 1，如果未找到则为 0
     */
    fun deleteFavoriteById(id: Long): Int {
        val selection = "${FavoriteDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return writableDatabase.delete(FavoriteDatabaseHelper.TABLE_NAME, selection, selectionArgs)
    }

    /**
     * 根据账单 ID 删除收藏条目
     * @param billingId 要删除的账单 ID
     * @return 受影响的行数
     */
    fun deleteFavoritesByBillingId(billingId: Long): Int {
        val selection = "${FavoriteDatabaseHelper.COLUMN_BILLING} = ?"
        val selectionArgs = arrayOf(billingId.toString())
        return writableDatabase.delete(FavoriteDatabaseHelper.TABLE_NAME, selection, selectionArgs)
    }

    /**
     * 根据 ID 修改一个收藏条目
     * @param id 要修改的记录 ID
     * @param billing 账单ID
     * @param tags 标签
     * @return 受影响的行数，通常为 1，如果未找到则为 0
     */
    fun updateFavoriteById(id: Long, billing: Long, tags: String?): Int {
        val values = ContentValues().apply {
            put(FavoriteDatabaseHelper.COLUMN_BILLING, billing)
            put(FavoriteDatabaseHelper.COLUMN_TAGS, tags)
        }
        val selection = "${FavoriteDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return writableDatabase.update(FavoriteDatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
    }

    /**
     * 根据账单 ID 修改收藏条目 (通常只修改标签等字段)
     * @param billingId 账单 ID
     * @param tags 新的标签值
     * @return 受影响的行数
     */
    fun updateFavoritesByBillingId(billingId: Long, tags: String?): Int {
        val values = ContentValues().apply {
            put(FavoriteDatabaseHelper.COLUMN_TAGS, tags) // 这里只更新 tags
            // 如果需要更新其他字段，可以在这里添加
            // put(FavoriteDatabaseHelper.COLUMN_BILLING, newBillingId) // 不建议通过此方法修改 billingId
        }
        val selection = "${FavoriteDatabaseHelper.COLUMN_BILLING} = ?"
        val selectionArgs = arrayOf(billingId.toString())
        return writableDatabase.update(FavoriteDatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
    }


    /**
     * 根据 ID 查询单个收藏条目
     * @param id 要查询的记录 ID
     * @return 如果找到记录则返回 FavoriteEntry 对象，否则返回 null
     */
    fun getFavoriteById(id: Long): FavoriteEntry? {
        val projection = arrayOf(
            FavoriteDatabaseHelper.COLUMN_ID,
            FavoriteDatabaseHelper.COLUMN_BILLING,
            FavoriteDatabaseHelper.COLUMN_TAGS
        )
        val selection = "${FavoriteDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        val cursor = readableDatabase.query(
            FavoriteDatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null,
            "1" // Limit 1
        )

        return cursor.use {
            if (it.moveToFirst()) {
                cursorToFavoriteEntry(it)
            } else {
                null // 未找到返回 null
            }
        }
    }

    /**
     * 根据账单 ID 查询收藏条目列表
     * @param billingId 要查询的账单 ID
     * @return 与该账单 ID 关联的 FavoriteEntry 对象列表
     */
    fun getFavoritesByBillingId(billingId: Long): List<FavoriteEntry> {
        val favoriteEntries = mutableListOf<FavoriteEntry>()
        val projection = arrayOf(
            FavoriteDatabaseHelper.COLUMN_ID,
            FavoriteDatabaseHelper.COLUMN_BILLING,
            FavoriteDatabaseHelper.COLUMN_TAGS
        )
        val selection = "${FavoriteDatabaseHelper.COLUMN_BILLING} = ?"
        val selectionArgs = arrayOf(billingId.toString())
        val cursor = readableDatabase.query(
            FavoriteDatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                favoriteEntries.add(cursorToFavoriteEntry(it))
            }
        }
        return favoriteEntries
    }

    /**
     * 获取收藏条目数量
     * @return 表中的条目总数
     */
    fun getFavoriteCount(): Long {
        val countQuery = "SELECT COUNT(*) FROM ${FavoriteDatabaseHelper.TABLE_NAME}"
        val cursor = readableDatabase.rawQuery(countQuery, null)
        var count: Long = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getLong(0)
            }
        }
        return count
    }

    /**
     * 实现分页查询功能
     * @param limit 每页条目数
     * @param offset 偏移量 (跳过的条目数)
     * @return 分页查询结果的 FavoriteEntry 对象列表
     */
    fun getFavoritesPaginated(limit: Int, offset: Int): List<FavoriteEntry> {
        val favoriteEntries = mutableListOf<FavoriteEntry>()
        val projection = arrayOf(
            FavoriteDatabaseHelper.COLUMN_ID,
            FavoriteDatabaseHelper.COLUMN_BILLING,
            FavoriteDatabaseHelper.COLUMN_TAGS
        )
        val limitString = "$limit OFFSET $offset"
        val cursor = readableDatabase.query(
            FavoriteDatabaseHelper.TABLE_NAME,
            projection,
            null, // selection
            null, // selectionArgs
            null, // groupBy
            null, // having
            "${FavoriteDatabaseHelper.COLUMN_ID} ASC", // orderBy, 通常按ID排序
            limitString // limit
        )

        cursor.use {
            while (it.moveToNext()) {
                favoriteEntries.add(cursorToFavoriteEntry(it))
            }
        }
        return favoriteEntries
    }

    /**
     * 判断某个账单ID是否已经被收藏
     * @param billingId 要检查的账单 ID
     * @return 如果该账单 ID 已被收藏，返回 true，否则返回 false
     */
    fun isBillingIdFavorited(billingId: Long): Boolean {
        val projection = arrayOf(FavoriteDatabaseHelper.COLUMN_ID) // 只查询ID即可
        val selection = "${FavoriteDatabaseHelper.COLUMN_BILLING} = ?"
        val selectionArgs = arrayOf(billingId.toString())
        val cursor = readableDatabase.query(
            FavoriteDatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null,
            "1" // 只查询一条匹配的记录即可
        )

        return cursor.use {
            it.moveToFirst() // 如果能移动到第一条记录，说明存在
        }
    }


    /**
     * 将 Cursor 对象转换为 FavoriteEntry 对象
     */
    private fun cursorToFavoriteEntry(cursor: Cursor): FavoriteEntry {
        return FavoriteEntry(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(FavoriteDatabaseHelper.COLUMN_ID)),
            billing = cursor.getLong(cursor.getColumnIndexOrThrow(FavoriteDatabaseHelper.COLUMN_BILLING)),
            tags = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteDatabaseHelper.COLUMN_TAGS))
        )
    }

    // 记得在不需要使用时关闭数据库连接 (通常在 Activity 或 Fragment 的 onDestroy 方法中调用)
    fun close() {
        writableDatabase.close()
        readableDatabase.close()
    }
}