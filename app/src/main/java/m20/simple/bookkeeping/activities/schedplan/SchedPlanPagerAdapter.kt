package m20.simple.bookkeeping.activities.schedplan

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import m20.simple.bookkeeping.R

class SchedPlanPagerAdapter(fm: FragmentManager, context: Context) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = listOf(SchedPlanWeekFragment(), SchedPlanMonthFragment()) // 两个Fragment
    private val titles = listOf(
        context.getString(R.string.week_scheduled),
        context.getString(R.string.month_scheduled)
    ) // 每个tab的标题

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position]

    override fun getPageTitle(position: Int): CharSequence = titles[position]
}