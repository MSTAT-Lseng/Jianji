package m20.simple.bookkeeping.worker

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.api.billing.BillingCreator
import m20.simple.bookkeeping.api.config.SchedPlanConfigCreator
import m20.simple.bookkeeping.api.sched_plan.SchedPlanCreator
import m20.simple.bookkeeping.utils.TimeUtils
import kotlin.coroutines.CoroutineContext

class SchedPlanWorker(
    private val context: Context
) : CoroutineScope {

    private val TAG = "SchedPlanWorker"
    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    fun start(
        callback: () -> Unit = {},
    ) {
        Log.d(TAG, "Start function called.")
        if (job.isCancelled) {
            Log.d(TAG, "Job was cancelled, creating a new one.")
            job = Job()
        }
        run(callback)
    }

    fun stop() {
        Log.d(TAG, "Stop function called, cancelling job: $job")
        job.cancel()
    }

    fun isFinishedUpdated(): Boolean {
        val result = SchedPlanConfigCreator.getLastRunTime(context) == TimeUtils.getDayLevelTimestamp()
        Log.d(TAG, "IsFinishedUpdated function called. result: $result")
        return result
    }

    private fun taskWeekSchedPlan(
        lastRunTime: Long,
        dayLevelTimestamp: Long
    ) {
        Log.d(TAG, "TaskWeekSchedPlan function called.")
        SchedPlanCreator.getRecordByCycle(SchedPlanCreator.CYCLE_WEEK, context)
            .forEach { cycle ->
                TimeUtils.getTargetWeekdayTimestampsBetween(
                    lastRunTime,
                    dayLevelTimestamp,
                    cycle.day
                ).forEach { cycleTime ->
                    val billingInstance = cycle.billing // 获取 billing 实例
                    billingInstance.time = cycleTime     // 设置时间
                    BillingCreator.createBilling(billingInstance, cycleTime, context)
                }
            }
    }

    private fun taskMonthSchedPlan(
        lastRunTime: Long,
        dayLevelTimestamp: Long
    ) {
        Log.d(TAG, "TaskMonthSchedPlan function called.")
        SchedPlanCreator.getRecordByCycle(SchedPlanCreator.CYCLE_MONTH, context)
            .forEach { cycle ->
                TimeUtils.getTargetDayOfMonthTimestampsBetween(
                    lastRunTime,
                    dayLevelTimestamp,
                    cycle.day
                ).forEach { cycleTime ->
                    val billingInstance = cycle.billing // 获取 billing 实例
                    billingInstance.time = cycleTime
                    BillingCreator.createBilling(billingInstance, cycleTime, context)
                }
            }
    }

    private fun run(
        callback: () -> Unit = {}
    ) {
        launch {
            Log.d(TAG, "Run function called.")
            // 获取上次运行时间
            var lastRunTime = SchedPlanConfigCreator.getLastRunTime(context)
            val dayLevelTimestamp = TimeUtils.getDayLevelTimestamp()

            if (lastRunTime == 0L) {
                Log.d(TAG, "Run function: Jump lastRunTime == 0L")
                SchedPlanConfigCreator.setLastRunTime(context, dayLevelTimestamp)
                lastRunTime = dayLevelTimestamp
                callback()
                return@launch
            }

            if (isFinishedUpdated()) {
                Log.d(TAG, "Run function: Jump isFinishedUpdated(): true")
                callback()
                return@launch
            }

            Log.d(TAG, "Run function: startTaskSchedPlan")
            withContext(Dispatchers.IO) {
                taskWeekSchedPlan(lastRunTime, dayLevelTimestamp)
                taskMonthSchedPlan(lastRunTime, dayLevelTimestamp)
            }

            // 设置下次运行时间
            Log.d(TAG, "Run function: setLastRunTime: $dayLevelTimestamp")
            SchedPlanConfigCreator.setLastRunTime(context, dayLevelTimestamp)

            Log.d(TAG, "Run function: callback()")
            callback()
        }

    }

}

class SchedPlanReceiver(
    private val schedPlanWorker: SchedPlanWorker
) : BroadcastReceiver() {
    private val TAG = "SchedPlanReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED -> {
                Log.d(TAG, "ACTION_DATE_CHANGED")
                schedPlanWorker.start()
            }
        }
    }
}