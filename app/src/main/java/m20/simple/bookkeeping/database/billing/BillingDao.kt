package m20.simple.bookkeeping.database.billing

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BillingDao(context: Context) {

    private val dbHelper: BillingDatabaseHelper = BillingDatabaseHelper(context)

    // 获取可写数据库实例
    private val writableDatabase: SQLiteDatabase
        get() = dbHelper.writableDatabase

    // 获取只读数据库实例
    private val readableDatabase: SQLiteDatabase
        get() = dbHelper.readableDatabase

    /**
     * 增加一条账单记录
     * @param time 发生时间（分钟级时间戳）
     * @param amount 金额
     * @param iotype 收支类型 (0: 支出, 1: 收入)
     * @param classify 账单分类
     * @param notes 文字备注
     * @param images 图片备注
     * @param deposit 订金参数
     * @param wallet 钱包 ID
     * @param tags 标签（程序内部用）
     * @return 插入记录的 ID，如果插入失败返回 -1
     */
    fun addRecord(
        time: Long,
        amount: Long,
        iotype: Int,
        classify: String,
        notes: String?,
        images: String?,
        deposit: String,
        wallet: Int,
        tags: String?
    ): Long {
        val values = ContentValues().apply {
            put(BillingDatabaseHelper.COLUMN_TIME, time)
            put(BillingDatabaseHelper.COLUMN_AMOUNT, amount)
            put(BillingDatabaseHelper.COLUMN_IOTYPE, iotype)
            put(BillingDatabaseHelper.COLUMN_CLASSIFY, classify)
            put(BillingDatabaseHelper.COLUMN_NOTES, notes)
            put(BillingDatabaseHelper.COLUMN_IMAGES, images)
            put(BillingDatabaseHelper.COLUMN_DEPOSIT, deposit)
            put(BillingDatabaseHelper.COLUMN_WALLET, wallet)
            put(BillingDatabaseHelper.COLUMN_TAGS, tags)
        }
        return writableDatabase.insert(BillingDatabaseHelper.TABLE_NAME, null, values)
    }

    /**
     * 根据 ID 删除一条账单记录
     * @param recordId 要删除的记录 ID
     * @return 受影响的行数，通常为 1，如果未找到则为 0
     */
    fun deleteRecord(recordId: Long): Int {
        val selection = "${BillingDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(recordId.toString())
        return writableDatabase.delete(BillingDatabaseHelper.TABLE_NAME, selection, selectionArgs)
    }

    /**
     * 修改一条账单记录
     * @param recordId 要修改的记录 ID
     * @param time 发生时间（分钟级时间戳）
     * @param amount 金额
     * @param iotype 收支类型 (0: 支出, 1: 收入)
     * @param classify 账单分类
     * @param notes 文字备注
     * @param images 图片备注
     * @param tags 标签（程序内部用）
     * @param deposit 订金参数
     * @param wallet 钱包 ID
     * @return 受影响的行数，通常为 1，如果未找到则为 0
     */
    fun updateRecord(
        recordId: Long,
        time: Long,
        amount: Long,
        iotype: Int,
        classify: String,
        notes: String?,
        images: String?,
        deposit: String,
        wallet: Int,
        tags: String?
    ): Int {
        val values = ContentValues().apply {
            put(BillingDatabaseHelper.COLUMN_TIME, time)
            put(BillingDatabaseHelper.COLUMN_AMOUNT, amount)
            put(BillingDatabaseHelper.COLUMN_IOTYPE, iotype)
            put(BillingDatabaseHelper.COLUMN_CLASSIFY, classify)
            put(BillingDatabaseHelper.COLUMN_NOTES, notes)
            put(BillingDatabaseHelper.COLUMN_IMAGES, images)
            put(BillingDatabaseHelper.COLUMN_DEPOSIT, deposit)
            put(BillingDatabaseHelper.COLUMN_WALLET, wallet)
            put(BillingDatabaseHelper.COLUMN_TAGS, tags)
        }
        val selection = "${BillingDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(recordId.toString())
        return writableDatabase.update(BillingDatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
    }

    /**
     * 根据 ID 查询单个账单条目的内容
     * @param recordId 要查询的记录 ID
     * @return 如果找到记录则返回 Record 对象，否则返回一个空的 Record 对象
     */
    fun getRecordById(recordId: Long): Record {
        val projection = arrayOf(
            BillingDatabaseHelper.COLUMN_ID,
            BillingDatabaseHelper.COLUMN_TIME,
            BillingDatabaseHelper.COLUMN_AMOUNT,
            BillingDatabaseHelper.COLUMN_IOTYPE,
            BillingDatabaseHelper.COLUMN_CLASSIFY,
            BillingDatabaseHelper.COLUMN_NOTES,
            BillingDatabaseHelper.COLUMN_IMAGES,
            BillingDatabaseHelper.COLUMN_DEPOSIT,
            BillingDatabaseHelper.COLUMN_WALLET,
            BillingDatabaseHelper.COLUMN_TAGS
        )
        val selection = "${BillingDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(recordId.toString())
        val cursor = readableDatabase.query(
            BillingDatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        return cursor.use {
            if (it.moveToFirst()) {
                cursorToRecord(it)
            } else {
                Record(
                    id = -1,
                    time = 0,
                    amount = 0,
                    iotype = 0,
                    classify = "",
                    notes = null,
                    images = null,
                    deposit = "",
                    wallet = 0,
                    tags = null
                ) // 返回一个空的 Record 对象
            }
        }
    }

    /**
     * 查询指定一天内的账单条目列表
     * @param dayInMillis 当天的毫秒级时间戳 (例如：Calendar.getInstance().set(year, month, day, 0, 0, 0).timeInMillis)
     * @return 当天账单条目的 Record 对象列表
     */
    fun getRecordsByDay(dayInMillis: Long): List<Record> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dayInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayInMillis = calendar.timeInMillis
        val endOfDayInMillis = startOfDayInMillis + TimeUnit.DAYS.toMillis(1) - TimeUnit.SECONDS.toMillis(1) // 减去一秒钟，保证精确到当天

        // val startTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(startOfDayInMillis)
        // val endTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(endOfDayInMillis)

        val projection = arrayOf(
            BillingDatabaseHelper.COLUMN_ID,
            BillingDatabaseHelper.COLUMN_TIME,
            BillingDatabaseHelper.COLUMN_AMOUNT,
            BillingDatabaseHelper.COLUMN_IOTYPE,
            BillingDatabaseHelper.COLUMN_CLASSIFY,
            BillingDatabaseHelper.COLUMN_NOTES,
            BillingDatabaseHelper.COLUMN_IMAGES,
            BillingDatabaseHelper.COLUMN_DEPOSIT,
            BillingDatabaseHelper.COLUMN_WALLET,
            BillingDatabaseHelper.COLUMN_TAGS
        )
        val selection = "${BillingDatabaseHelper.COLUMN_TIME} >= ? AND ${BillingDatabaseHelper.COLUMN_TIME} <= ?"
        // val selectionArgs = arrayOf(startTimeInMinutes.toString(), endTimeInMinutes.toString())
        val selectionArgs = arrayOf(startOfDayInMillis.toString(), endOfDayInMillis.toString())
        val cursor = readableDatabase.query(
            BillingDatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            "${BillingDatabaseHelper.COLUMN_TIME} ASC" // 可以按时间排序
        )

        val records = mutableListOf<Record>()
        cursor.use {
            while (it.moveToNext()) {
                records.add(cursorToRecord(it))
            }
        }
        return records
    }

    /**
     * 查询账单ID是否存在
     * @param recordId 账单ID
     * @return 查询结果
     */
    fun isRecordExists(recordId: Long): Boolean {
        val projection = arrayOf(BillingDatabaseHelper.COLUMN_ID)
        val selection = "${BillingDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(recordId.toString())
        val cursor = readableDatabase.query(
            BillingDatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        return cursor.use {
            it.moveToFirst()
        }
    }

    /**
     * 查询指定多天内的账单条目列表
     * @param startDateInMillis 开始日期的毫秒级时间戳
     * @param endDateInMillis 结束日期的毫秒级时间戳
     * @return 指定日期范围内账单条目的 Record 对象列表
     */
    fun getRecordsByDateRange(startDateInMillis: Long, endDateInMillis: Long): List<Record> {
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startDateInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = endDateInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(startCalendar.timeInMillis)
        val endTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(endCalendar.timeInMillis)

        val projection = arrayOf(
            BillingDatabaseHelper.COLUMN_ID,
            BillingDatabaseHelper.COLUMN_TIME,
            BillingDatabaseHelper.COLUMN_AMOUNT,
            BillingDatabaseHelper.COLUMN_IOTYPE,
            BillingDatabaseHelper.COLUMN_CLASSIFY,
            BillingDatabaseHelper.COLUMN_NOTES,
            BillingDatabaseHelper.COLUMN_IMAGES,
            BillingDatabaseHelper.COLUMN_DEPOSIT,
            BillingDatabaseHelper.COLUMN_WALLET,
            BillingDatabaseHelper.COLUMN_TAGS
        )
        val selection = "${BillingDatabaseHelper.COLUMN_TIME} >= ? AND ${BillingDatabaseHelper.COLUMN_TIME} <= ?"
        val selectionArgs = arrayOf(startTimeInMinutes.toString(), endTimeInMinutes.toString())
        val cursor = readableDatabase.query(
            BillingDatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            "${BillingDatabaseHelper.COLUMN_TIME} ASC" // 可以按时间排序
        )

        val records = mutableListOf<Record>()
        cursor.use {
            while (it.moveToNext()) {
                records.add(cursorToRecord(it))
            }
        }
        return records
    }

    /**
     * 将 Cursor 对象转换为 Record 对象
     */
    private fun cursorToRecord(cursor: Cursor): Record {
        return Record(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_ID)),
            time = cursor.getLong(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_TIME)),
            amount = cursor.getLong(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_AMOUNT)),
            iotype = cursor.getInt(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_IOTYPE)),
            classify = cursor.getString(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_CLASSIFY)),
            notes = cursor.getString(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_NOTES)),
            images = cursor.getString(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_IMAGES)),
            deposit = cursor.getString(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_DEPOSIT)),
            wallet = cursor.getInt(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_WALLET)),
            tags = cursor.getString(cursor.getColumnIndexOrThrow(BillingDatabaseHelper.COLUMN_TAGS))
        )
    }

    /**
     * 定义数据类 Record 来表示账单条目
     */
    data class Record(
        val id: Long,
        val time: Long,
        val amount: Long,
        val iotype: Int,
        val classify: String,
        val notes: String?,
        val images: String?,
        val deposit: String,
        val wallet: Int,
        val tags: String?
    )

    // 记得在不需要使用时关闭数据库连接 (通常在 Activity 或 Fragment 的 onDestroy 方法中调用)
    fun close() {
        writableDatabase.close()
        readableDatabase.close()
    }
}