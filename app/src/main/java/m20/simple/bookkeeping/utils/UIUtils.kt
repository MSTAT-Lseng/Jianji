package m20.simple.bookkeeping.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import m20.simple.bookkeeping.R


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

    // 获得账单分类对应的图标，详见 arrays.xml。
    fun getCategoryPairs(resources: Resources,
                         activity: Activity,
                         thinIcon: Boolean = true): List<Pair<Int, String>> {
        val iconList = if (thinIcon) R.array.classify_icon_list_thin else R.array.classify_icon_list_regular
        return resources.obtainTypedArray(iconList).use { icons ->
            val categories = resources.getStringArray(R.array.classify_list_id)
            require(icons.length() == categories.size) {
                activity.getString(R.string.inconsistent_array_lengths)
            }

            (0 until icons.length()).map { index ->
                icons.getResourceId(index, -1) to categories[index]
            }
        }
    }

    // 获得分类ID对应的字符串，详见 arrays.xml。
    fun getCategories(resources: Resources): List<Pair<String, String>> {
        val classifyNames = resources.getStringArray(R.array.classify_list)
        val classifyIds = resources.getStringArray(R.array.classify_list_id)

        return classifyIds.zip(classifyNames) { id, name ->
            id to name
        }
    }

    // 复制文本到剪贴板
    fun copyTextToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

}