package m20.simple.bookkeeping.utils

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

object TalkBackUtils {

    fun announceText(context: Context, text: String?) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        // 检查无障碍服务是否启用
        if (am.isEnabled) {
            // 创建一个 TYPE_ANNOUNCEMENT 事件
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
            event.packageName = context.packageName // 设置包名
            event.text.add(text) // 添加要朗读的文本

            // 发送事件
            am.sendAccessibilityEvent(event)
        }
    }

}