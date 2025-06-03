package m20.simple.bookkeeping.database.sched_plan

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import m20.simple.bookkeeping.api.objects.BillingObject

class SchedPlanDao(private val dbHelper: SchedPlanDatabaseHelper) {

    companion object {
        // 用于映射 Cursor 到数据对象的列索引
        private const val INDEX_ID = 0
        private const val INDEX_CYCLE = 1
        private const val INDEX_DAY = 2
        private const val INDEX_BILLING = 3
        private const val INDEX_TAGS = 4
    }

    // 插入一条记录
    fun insertRecord(record: SchedPlan): Long {
        val db: SQLiteDatabase = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SchedPlanDatabaseHelper.COLUMN_CYCLE, record.cycle)
            put(SchedPlanDatabaseHelper.COLUMN_DAY, record.day)
            put(SchedPlanDatabaseHelper.COLUMN_BILLING, record.billing)
            put(SchedPlanDatabaseHelper.COLUMN_TAGS, record.tags)
        }
        return db.insert(SchedPlanDatabaseHelper.TABLE_NAME, null, values)
    }

    // 根据ID删除记录
    fun deleteRecordById(id: Long): Int {
        val db: SQLiteDatabase = dbHelper.writableDatabase
        return db.delete(
            SchedPlanDatabaseHelper.TABLE_NAME,
            "${SchedPlanDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(id.toString())
        )
    }

    // 根据ID更新记录
    fun updateRecord(record: SchedPlan): Int {
        val db: SQLiteDatabase = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SchedPlanDatabaseHelper.COLUMN_CYCLE, record.cycle)
            put(SchedPlanDatabaseHelper.COLUMN_DAY, record.day)
            put(SchedPlanDatabaseHelper.COLUMN_BILLING, record.billing)
            put(SchedPlanDatabaseHelper.COLUMN_TAGS, record.tags)
        }
        return db.update(
            SchedPlanDatabaseHelper.TABLE_NAME,
            values,
            "${SchedPlanDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(record.id.toString())
        )
    }

    // 根据ID查询记录
    fun getRecordById(id: Long): SchedPlan? {
        val db: SQLiteDatabase = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            SchedPlanDatabaseHelper.TABLE_NAME,
            null,
            "${SchedPlanDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        var record: SchedPlan? = null
        if (cursor.moveToFirst()) {
            record = mapCursorToSchedPlan(cursor)
            cursor.close()
        }

        return record
    }

    // 根据cycle查询记录
    fun getRecordByCycle(cycle: Int): List<SchedPlan> {
        val records = mutableListOf<SchedPlan>()
        val db: SQLiteDatabase = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            SchedPlanDatabaseHelper.TABLE_NAME,
            null,
            "${SchedPlanDatabaseHelper.COLUMN_CYCLE} = ?",
            arrayOf(cycle.toString()),
            null,
            null,
            null
        )
        cursor.let {
            while (it.moveToNext()) {
                records.add(mapCursorToSchedPlan(it))
            }
            it.close()
        }
        return records
    }

    // 查询所有记录
    fun getAllRecords(): List<SchedPlan> {
        val records = mutableListOf<SchedPlan>()
        val db: SQLiteDatabase = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            SchedPlanDatabaseHelper.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor.let {
            while (it.moveToNext()) {
                records.add(mapCursorToSchedPlan(it))
            }
            it.close()
        }

        return records
    }

    // 获得记录数量
    fun getRecordsNumber(): Int {
        val db: SQLiteDatabase = dbHelper.readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM ${SchedPlanDatabaseHelper.TABLE_NAME}", null).use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    // 将 Cursor 映射为 SchedPlan 对象
    private fun mapCursorToSchedPlan(cursor: Cursor): SchedPlan {
        return SchedPlan(
            id = cursor.getLong(INDEX_ID),
            cycle = cursor.getInt(INDEX_CYCLE),
            day = cursor.getInt(INDEX_DAY),
            billing = cursor.getString(INDEX_BILLING),
            tags = cursor.getString(INDEX_TAGS)
        )
    }

    fun close() {
        dbHelper.close()
    }
}

data class SchedPlan(
    var id: Long = -1, // 自增主键，默认-1表示未插入数据库
    var cycle: Int,
    var day: Int,
    var billing: String,
    var tags: String?
)

data class SchedPlanInstance(
    var id: Long = -1,
    var cycle: Int,
    var day: Int,
    var billing: BillingObject,
    var tags: String?
)