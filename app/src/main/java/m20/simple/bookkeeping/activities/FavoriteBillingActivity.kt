package m20.simple.bookkeeping.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.billing.BillingCreator
import m20.simple.bookkeeping.api.favorite.FavoriteCreator
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.database.favorite.FavoriteDao
import m20.simple.bookkeeping.utils.UIUtils
import m20.simple.bookkeeping.widget.BillingItemWidget

class FavoriteBillingActivity : AppCompatActivity() {

    private var loadedPage = 0
    private var totalPage = 0
    private val perPageNumber = 15

    private var selectedItemId = mutableListOf<Long>()

    private val modifiedBillingListenLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
            loadedPage = 0
            totalPage = 0
            loadingFavoriteBillings(true)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_billing)
        setSupportActionBar(findViewById(R.id.toolbar))

        val uiUtils = UIUtils()
        uiUtils.fillStatusBarHeight(this, findViewById(R.id.status_bar_view))
        uiUtils.setStatusBarTextColor(this, !uiUtils.isDarkMode(resources))

        loadingFavoriteBillings()
        monitorSlideBottom()
    }

    private fun loadingFavoriteBillings(
        clearExistsBillings: Boolean = false
    ) {
        val itemLinearLayout = findViewById<LinearLayout>(R.id.item_linear)

        if (clearExistsBillings) {
            itemLinearLayout.removeAllViews()
        }

        fun noFavoriteBillings() {
            val emptyView = layoutInflater.inflate(R.layout.collect_empty_hint, null)
            itemLinearLayout.addView(emptyView)
        }

        fun loadFavoriteBillings(
            record: BillingDao.Record,
            allWallets: List<Pair<Int, String>>
        ) {
            val billingItemView = BillingItemWidget.getWidget(
                this@FavoriteBillingActivity,
                itemLinearLayout,
                record,
                resources,
                allWallets
            )

            billingItemView.setOnClickListener {
                if (selectedItemId.isEmpty()) {
                    val intent = Intent(this@FavoriteBillingActivity, BillingInfoActivity::class.java)
                    intent.putExtra("billingId", record.id)
                    modifiedBillingListenLauncher.launch(intent)
                } else {

                }
            }

            itemLinearLayout.addView(billingItemView)
        }

        lifecycleScope.launch {
            val favoriteCreator = FavoriteCreator

            // 获取收藏数量
            val favoritesCount = withContext(Dispatchers.IO) {
                favoriteCreator.getFavoriteCount(this@FavoriteBillingActivity)
            }

            // 没有收藏
            if (favoritesCount == 0L) {
                noFavoriteBillings()
                return@launch
            }

            if (totalPage == 0)
                totalPage = ((favoritesCount + perPageNumber - 1) / perPageNumber).toInt()

            // 到底了
            if (loadedPage >= totalPage) {
                return@launch
            }

            loadedPage += 1
            val offset = (loadedPage - 1) * perPageNumber
            val favoriteBillings = withContext(Dispatchers.IO) {
                favoriteCreator.getFavoritesPaginated(
                    this@FavoriteBillingActivity,
                    perPageNumber,
                    offset
                )
            }
            val allWallets = withContext(Dispatchers.IO) {
                WalletCreator.getAllWallets(this@FavoriteBillingActivity)
            }
            for (entry in favoriteBillings) {
                val billingId = entry.billing
                val billingRecord = withContext(Dispatchers.IO) {
                    BillingCreator.getRecordById(
                        billingId,
                        this@FavoriteBillingActivity,
                    )
                }
                loadFavoriteBillings(billingRecord, allWallets)
            }
        }
    }

    private fun monitorSlideBottom() {
        fun configChild(child: View, v: View, scrollY: Int) {
            val childHeight = child.height
            val scrollViewHeight = v.height
            if (scrollY >= (childHeight - scrollViewHeight)) {
                loadingFavoriteBillings()
            }
        }

        val itemScrollView = findViewById<ScrollView>(R.id.item_scroll)
        itemScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val child = itemScrollView.getChildAt(0)
            if (child != null) {
                configChild(child, v, scrollY)
            }
        }
    }

    private fun taskDeleteSelectedFavorite() {
        if (selectedItemId.isEmpty()) {
            Toast.makeText(
                this,
                resources.getString(R.string.favorite_billing_delete_empty),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_favorite, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.delete -> {
                taskDeleteSelectedFavorite()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}