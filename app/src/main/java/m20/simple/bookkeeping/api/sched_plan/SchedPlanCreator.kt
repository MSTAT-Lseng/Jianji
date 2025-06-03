package m20.simple.bookkeeping.api.sched_plan

import android.content.Context
import android.util.Log
import m20.simple.bookkeeping.api.objects.BillingObject
import m20.simple.bookkeeping.api.objects.BillingObjectCreator
import m20.simple.bookkeeping.database.sched_plan.SchedPlan
import m20.simple.bookkeeping.database.sched_plan.SchedPlanDao
import m20.simple.bookkeeping.database.sched_plan.SchedPlanDatabaseHelper
import m20.simple.bookkeeping.database.sched_plan.SchedPlanInstance

object SchedPlanCreator {

    private val TAG = "SchedPlanCreator"

    val CYCLE_WEEK = 1
    val CYCLE_MONTH = 2

    fun insertRecord(
        context: Context,
        cycle: Int,
        day: Int,
        billingObject: BillingObject,
        tags: String?
    ): Int {
        val helper = SchedPlanDatabaseHelper(context)
        val dao = SchedPlanDao(helper)

        return try {
            val record = SchedPlan(
                cycle = cycle,
                day = day,
                billing = billingObject.toString(),
                tags = tags
            )
            val rowId = dao.insertRecord(record)
            if (rowId > 0) {
                rowId.toInt()
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert record.", e)
            -1
        } finally {
            dao.close()
            helper.close()
        }
    }

    fun getRecordById(id: Int, context: Context): SchedPlanInstance? {
        val helper = SchedPlanDatabaseHelper(context)
        val dao = SchedPlanDao(helper)

        return try {
            val record = dao.getRecordById(id.toLong()) ?: return null
            val instance = SchedPlanInstance(
                id = record.id,
                cycle = record.cycle,
                day = record.day,
                billing = BillingObjectCreator.toObject(record.billing),
                tags = record.tags
            )
            instance
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get record by id.", e)
            null
        } finally {
            dao.close()
            helper.close()
        }
    }

    fun getRecordByCycle(cycle: Int, context: Context): List<SchedPlanInstance> {
        val helper = SchedPlanDatabaseHelper(context)
        val dao = SchedPlanDao(helper)

        return try {
            val records = dao.getRecordByCycle(cycle)
            records.map { record ->
                SchedPlanInstance(
                    id = record.id,
                    cycle = record.cycle,
                    day = record.day,
                    billing = BillingObjectCreator.toObject(record.billing),
                    tags = record.tags
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get record by cycle.", e)
            emptyList()
        } finally {
            dao.close()
            helper.close()
        }
    }

    fun updateRecord(
        context: Context,
        id: Int,
        cycle: Int,
        day: Int,
        billingObject: BillingObject,
        tags: String?
    ): Boolean {
        val helper = SchedPlanDatabaseHelper(context)
        val dao = SchedPlanDao(helper)
        return try {
            val record = SchedPlan(
                id = id.toLong(),
                cycle = cycle,
                day = day,
                billing = billingObject.toString(),
                tags = tags
            )
            dao.updateRecord(record) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update record.", e)
            false
        } finally {
            dao.close()
            helper.close()
        }
    }

    fun getRecordsNumber(context: Context): Int {
        val helper = SchedPlanDatabaseHelper(context)
        val dao = SchedPlanDao(helper)

        return try {
            dao.getRecordsNumber()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get record number.", e)
            0
        } finally {
            dao.close()
            helper.close()
        }
    }

    fun getAllRecords(context: Context): List<SchedPlanInstance> {
        val helper = SchedPlanDatabaseHelper(context)
        val dao = SchedPlanDao(helper)

        return try {
            val records = dao.getAllRecords()
            records.map { record ->
                SchedPlanInstance(
                    id = record.id,
                    cycle = record.cycle,
                    day = record.day,
                    billing = BillingObjectCreator.toObject(record.billing),
                    tags = record.tags
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all records.", e)
            emptyList()
        } finally {
            dao.close()
            helper.close()
        }
    }

    fun deleteRecordById(id: Int, context: Context): Boolean {
        val helper = SchedPlanDatabaseHelper(context)
        val dao = SchedPlanDao(helper)

        return try {
            dao.deleteRecordById(id.toLong()) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete record by id.", e)
            false
        } finally {
            dao.close()
            helper.close()
        }
    }

}