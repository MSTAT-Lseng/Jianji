package m20.simple.bookkeeping.activities.schedplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import m20.simple.bookkeeping.R

class SchedPlanWeekFragment : SchedPlanFragment() {

    private var selectedWeek = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val scheduleDateTitleView = getScheduleDateTitleView()

        if (scheduleDateTitleView != null) {
            loadWeekDateTitleView(scheduleDateTitleView)
        }
    }

    private fun loadWeekDateTitleView(view: LinearLayout) {
        val items = arrayOf(
            getString(R.string.monday),
            getString(R.string.tuesday),
            getString(R.string.wednesday),
            getString(R.string.thursday),
            getString(R.string.friday),
            getString(R.string.saturday),
            getString(R.string.sunday))

        val titleView = layoutInflater.inflate(R.layout.week_date_title_view, view, false)
        val textField = titleView.findViewById<TextInputLayout>(R.id.select_week)

        val autoCompleteTextView = (textField.editText as? MaterialAutoCompleteTextView)
        autoCompleteTextView?.setSimpleItems(items)
        autoCompleteTextView?.setOnItemClickListener { parent, _, position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            Toast.makeText(requireContext(), "你选择了: $selectedItem，position：$position", Toast.LENGTH_SHORT).show()

            // 或者执行其他逻辑，比如更新 UI，保存数据等
            selectedWeek = position
        }

        view.addView(titleView)
    }

}