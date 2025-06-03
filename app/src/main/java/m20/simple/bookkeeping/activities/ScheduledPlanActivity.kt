package m20.simple.bookkeeping.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
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

        when (activityMode) {
            "view" -> setContentView(R.layout.activity_scheduled_plan)
        }
        setSupportActionBar(findViewById(R.id.toolbar))

        val uiUtils = UIUtils
        uiUtils.fillStatusBarHeight(this, findViewById(R.id.status_bar_view))
        uiUtils.setStatusBarTextColor(this, !uiUtils.isDarkMode(resources))
        uiUtils.commonNavBarHeight(findViewById(R.id.nav_bar_height), this)

        findViewById<Button>(R.id.add_item_button).setOnClickListener { toAddSchedPlanActivity() }
        loadItems()
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