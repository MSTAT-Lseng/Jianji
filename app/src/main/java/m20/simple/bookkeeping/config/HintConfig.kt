package m20.simple.bookkeeping.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object HintConfig {

    private val PREF_NAME: String = "hint_config_prefs"
    val KEY_DEPOSIT_BILL_HINT: String = "deposit_bill_hint"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setBooleanValue(context: Context, name: String, value: Boolean) {
        getPrefs(context).edit {
            putBoolean(name, value)
        }
    }

    fun getBooleanValue(context: Context, name: String, default: Boolean): Boolean {
        return getPrefs(context).getBoolean(name, default)
    }

}