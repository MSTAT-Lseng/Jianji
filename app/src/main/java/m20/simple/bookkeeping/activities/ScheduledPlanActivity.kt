package m20.simple.bookkeeping.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.activities.schedplan.SchedPlanPagerAdapter
import m20.simple.bookkeeping.api.sched_plan.SchedPlanCreator
import m20.simple.bookkeeping.utils.UIUtils

class ScheduledPlanActivity : AppCompatActivity() {

    /*
    * ActivityMode values: "view", "create", "edit"
    * */
    private var activityMode = "view"
    private var editId = 0 // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityMode = intent.getStringExtra("mode") ?: activityMode

        setContentView(when (activityMode) {
            "create" -> R.layout.activity_scheduled_plan_create
            else -> R.layout.activity_scheduled_plan
        })
        setSupportActionBar(findViewById(R.id.toolbar))

        val uiUtils = UIUtils
        uiUtils.fillStatusBarHeight(this, findViewById(R.id.status_bar_view))
        uiUtils.setStatusBarTextColor(this, !uiUtils.isDarkMode(resources))

        when(activityMode) {
            "view" -> {
                uiUtils.commonNavBarHeight(findViewById(R.id.nav_bar_height), this)
                findViewById<Button>(R.id.add_item_button).setOnClickListener { toAddSchedPlanActivity() }
                loadItems()
            }
            "create" -> {
                val viewPager = findViewById<ViewPager>(R.id.view_pager)
                viewPager.adapter = SchedPlanPagerAdapter(supportFragmentManager, this)

                val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
                tabLayout.setupWithViewPager(viewPager)
            }
        }
    }

    private fun loadItems() {
        lifecycleScope.launch {
            val plansNumber = withContext(Dispatchers.IO) {
                SchedPlanCreator.getRecordsNumber(this@ScheduledPlanActivity)
            }
            if (plansNumber == 0) {
                findViewById<LinearLayout>(R.id.no_items_container).visibility = View.VISIBLE
                return@launch
            }
        }
    }

    private fun toAddSchedPlanActivity() {
        val intent = Intent(this, ScheduledPlanActivity::class.java)
            .putExtra("mode", "create")
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}