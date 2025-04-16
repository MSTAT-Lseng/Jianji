package m20.simple.bookkeeping.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import kotlin.math.abs

class CustomLinearLayout : LinearLayout {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    // 定义监听器属性
    private var onLeftSwipeListener: (() -> Unit)? = null
    private var onRightSwipeListener: (() -> Unit)? = null

    // 设置监听器的公共方法
    fun setOnLeftSwipeListener(listener: (() -> Unit)?) {
        onLeftSwipeListener = listener
    }

    fun setOnRightSwipeListener(listener: (() -> Unit)?) {
        onRightSwipeListener = listener
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true // 允许后续事件处理
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // 处理e1为null的情况
            if (e1 == null) return false

            // 去掉分号并优化常量定义
            val SWIPE_MIN_DISTANCE = 200
            val SWIPE_THRESHOLD_VELOCITY = 100

            // 使用扩展属性简化代码
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y

            // 移除不必要的类型转换，直接使用Float的绝对值
            if (abs(deltaX) > abs(deltaY)) {
                if (abs(deltaX) > SWIPE_MIN_DISTANCE &&
                    abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                ) {
                    // 显示Toast并返回true
                    if (deltaX > 0) {
                        onRightSwipeListener?.invoke()
                    } else {
                        onLeftSwipeListener?.invoke()
                    }
                    return true
                }
            }
            return false
        }
    })

    private fun init() {
        setOnTouchListener { _: View?, event: MotionEvent ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                performClick() // important
                return@setOnTouchListener true
            }
            true
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
