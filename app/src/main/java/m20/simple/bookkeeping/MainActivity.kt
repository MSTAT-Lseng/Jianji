package m20.simple.bookkeeping

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import m20.simple.bookkeeping.api.backup.BackupCreator
import m20.simple.bookkeeping.api.config.PrefsConfigCreator
import m20.simple.bookkeeping.config.PrefsConfig
import m20.simple.bookkeeping.databinding.ActivityMainBinding
import m20.simple.bookkeeping.ui.home.HomeViewModel
import m20.simple.bookkeeping.utils.PackageUtils
import m20.simple.bookkeeping.utils.UIUtils
import m20.simple.bookkeeping.worker.SchedPlanReceiver
import m20.simple.bookkeeping.worker.SchedPlanWorker

class MainActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var schedPlanReceiver: SchedPlanReceiver
    private lateinit var schedPlanWorker: SchedPlanWorker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        viewModel.setToolbar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        configTopBar()
        sendToolbarMessage()
        configNavigationHeader()

        schedPlanWorker = SchedPlanWorker(this)
        schedPlanReceiver = SchedPlanReceiver(schedPlanWorker)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        registerReceiver(schedPlanReceiver, filter)

        viewModel.setSchedPlanWorker(schedPlanWorker)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun configTopBar() {
        val uiUtils = UIUtils
        // set status bar height
        uiUtils.fillStatusBarHeight(this@MainActivity, findViewById(R.id.status_bar_view))
        // set status bar text color
        uiUtils.setStatusBarTextColor(this, !uiUtils.isDarkMode(resources))

        // optimizing the status bar color of DrawerLayout in light mode
        if (uiUtils.isDarkMode(resources)) {
            return
        }
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawerLayout.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                uiUtils.setStatusBarTextColor(this@MainActivity, false)
            }

            override fun onDrawerClosed(drawerView: View) {
                uiUtils.setStatusBarTextColor(this@MainActivity, true)
            }
        })
    }

    private fun configNavigationHeader() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val textView = headerView.findViewById<TextView>(R.id.titleView)
        textView.text = getString(
            R.string.nav_title,
            getString(R.string.app_name),
            PackageUtils(this).getVersionName()
        )
    }

    private fun sendToolbarMessage() {
        viewModel.toolbarMessage.value = true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(schedPlanReceiver)
    }

}
