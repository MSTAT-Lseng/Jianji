package m20.simple.bookkeeping.activities.schedplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import m20.simple.bookkeeping.R

open class SchedPlanFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_schedplan_create_layout, container, false)
        return view
    }

    private fun getScheduleDateTitleView() {

    }

}
