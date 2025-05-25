package m20.simple.bookkeeping.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.config.PrefsConfigCreator
import m20.simple.bookkeeping.config.PrefsConfig

object UIUtils {

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    fun commonNavBarHeight(navBarHeightView: View, activity: Activity) {
        getNavigationBarHeight(
            navBarHeightView,
            activity,
            fun(navHeight) {
                fillNavBarHeight(navBarHeightView, navHeight)
            }
        )
    }

    fun fillStatusBarHeight(context: Context, statusBarView: View) {
        fillViewHeight(statusBarView, getStatusBarHeight(context))
    }

    fun fillNavBarHeight(navBarView: View, height: Int) {
        fillViewHeight(navBarView, height)
    }

    private fun fillViewHeight(view: View, height: Int) {
        view.layoutParams.height += height
        view.requestLayout()
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
    fun getCategoryPairs(
        resources: Resources,
        activity: Activity,
        thinIcon: Boolean = true
    ): List<Pair<Int, String>> {
        val iconList =
            if (thinIcon) R.array.classify_icon_list_thin else R.array.classify_icon_list_regular

        val typedArray = resources.obtainTypedArray(iconList)
        val categories = resources.getStringArray(R.array.classify_list_id)

        require(typedArray.length() == categories.size) {
            activity.getString(R.string.inconsistent_array_lengths)
        }

        try {
            return (0 until typedArray.length()).map { index ->
                typedArray.getResourceId(index, -1) to categories[index]
            }
        } finally {
            typedArray.recycle()
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
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    fun getNavigationBarHeight(
        rootView: View,
        activity: Activity,
        callback: (Int) -> Unit = {}
    ) {
        val prefs = PrefsConfigCreator
        val prefsNavHeight = prefs.getNavBarHeight(activity)
        if (prefsNavHeight != 0) {
            callback(prefsNavHeight)
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val navHeight = getNavigationBarHeight(activity.window)
            if (navHeight != 0) {
                prefs.setNavBarHeight(activity, navHeight)
            }
            callback(navHeight)
            insets
        }
    }


    // 获取底部导航栏高度
    private fun getNavigationBarHeight(window: Window): Int {
        val windowInsets = window.decorView.rootWindowInsets
        val navigationBarHeight = windowInsets.systemWindowInsetBottom
        return navigationBarHeight
    }

}