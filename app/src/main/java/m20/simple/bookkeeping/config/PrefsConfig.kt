package m20.simple.bookkeeping.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PrefsConfig {

    private val PREF_NAME: String = "prefs_config_prefs"
    val KEY_DEFAULT_WALLET_ID: String = "default_wallet_id"
    val DEFAULT_WALLET_ID = 1

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getIntValue(context: Context, name: String, default: Int) : Int {
        return getPrefs(context).getInt(name, default)
    }

    fun setIntValue(context: Context, name: String, value: Int) {
        getPrefs(context).edit {
            putInt(name, value)
        }
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