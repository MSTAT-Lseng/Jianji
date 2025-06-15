package m20.simple.bookkeeping.widget

import android.content.res.Resources
import android.view.LayoutInflater
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

import m20.simple.bookkeeping.R

object ClassifyPickerWidget {

    fun getWidget(
        rootView: LinearLayout,
        layoutInflater: LayoutInflater,
        resources: Resources,
        onClickListener: (String) -> Unit,
        otherCallback: (Array<String>, MaterialButtonToggleGroup) -> Unit
    ): HorizontalScrollView {
        val view = layoutInflater.inflate(R.layout.classify_layout, rootView, false) as HorizontalScrollView

        fun configClassifyPicker() {
            val container = view.findViewById<MaterialButtonToggleGroup>(R.id.classify_linear)
            val classifyList = resources.getStringArray(R.array.classify_list)
            val ids = resources.getStringArray(R.array.classify_list_id)
            val icons = resources.obtainTypedArray(R.array.classify_icon_list)

            val iconList = List(icons.length()) { i -> icons.getResourceId(i, -1) }
                .filter { it != -1 }
            icons.recycle()

            classifyList.withIndex().forEach { (i, classifyName) ->
                val classifyIcon = iconList.getOrNull(i) ?: return@forEach
                val classifyId = ids.getOrNull(i) ?: return@forEach

                val classifyButton = layoutInflater.inflate(
                    R.layout.create_billing_classify_button,
                    container,
                    false
                ) as MaterialButton

                classifyButton.apply {
                    text = classifyName
                    setIconResource(classifyIcon)
                    setOnClickListener {
                        onClickListener(classifyId)
                    }
                }
                container.addView(classifyButton)
            }
            otherCallback(ids, container)
        }
        configClassifyPicker()

        return view
    }

}