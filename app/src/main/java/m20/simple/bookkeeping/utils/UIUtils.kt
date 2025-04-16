package m20.simple.bookkeeping.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View


class UIUtils {

    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    fun fillStatusBarHeight(context: Context, statusBarView: View) {
        statusBarView.layoutParams?.apply {
            height = getStatusBarHeight(context)
            statusBarView.layoutParams = this
        }
    }

    fun setStatusBarTextColor(activity: Activity, isDark: Boolean) {
        val window = activity.window
        val decorView = window.decorView
        decorView.systemUiVisibility = if (isDark) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            0
        }
    }

    fun isDarkMode(resources: Resources): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

}