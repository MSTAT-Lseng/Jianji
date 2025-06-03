package m20.simple.bookkeeping.api.config

import android.content.Context
import m20.simple.bookkeeping.config.SchedPlanConfig

object SchedPlanConfigCreator {

    fun getLastRunTime(context: Context): Long {
        return SchedPlanConfig.getLongValue(context, SchedPlanConfig.KEY_LAST_RUN_TIME, 0L)
    }

    fun setLastRunTime(context: Context, value: Long) {
        SchedPlanConfig.setLongValue(context, SchedPlanConfig.KEY_LAST_RUN_TIME, value)
    }

}