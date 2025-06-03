package m20.simple.bookkeeping.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SchedPlanConfig {

    private val PREF_NAME: String = "scheduled_plan_prefs"
    val KEY_LAST_RUN_TIME: String = "last_run_time"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setLongValue(context: Context, name: String, value: Long) {
        getPrefs(context).edit {
            putLong(name, value)
        }
    }

    fun getLongValue(context: Context, name: String, default: Long): Long {
        return getPrefs(context).getLong(name, default)
    }

}