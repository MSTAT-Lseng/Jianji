package m20.simple.bookkeeping.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        val uiUtils = UIUtils
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
            configLoadingMoreArea(showArea = false)
        }

        fun noFavoriteBillings() {
            val emptyView = layoutInflater.inflate(R.layout.collect_empty_hint, null)
            itemLinearLayout.addView(emptyView)
        }

        fun addSelectedItemId(id: Long, view: View): Boolean {
            val defaultBackground = BillingItemWidget.getDefaultBackground(this)
            val containerView = view.findViewById<LinearLayout>(R.id.item_container)
            if (selectedItemId.contains(id)) {
                selectedItemId.remove(id)
                containerView.background = defaultBackground
            } else {
                selectedItemId.add(id)
                containerView.setBackgroundColor(resources.getColor(R.color.billing_item_long_click))
            }
            return true
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
                    val intent =
                        Intent(this@FavoriteBillingActivity, BillingInfoActivity::class.java)
                    intent.putExtra("billingId", record.id)
                    modifiedBillingListenLauncher.launch(intent)
                } else {
                    addSelectedItemId(record.id, billingItemView)
                }
            }

            billingItemView.setOnLongClickListener {
                if (selectedItemId.isNotEmpty()) {
                    return@setOnLongClickListener true
                }
                addSelectedItemId(record.id, billingItemView)
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
                configLoadingMoreArea(loadingMore = false, showArea = true)
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
            if (favoriteBillings.size < perPageNumber) {
                configLoadingMoreArea(loadingMore = false, showArea = true)
            } else {
                configLoadingMoreArea(loadingMore = true, showArea = true)
            }
        }
    }

    private fun configLoadingMoreArea(
        loadingMore: Boolean = false,
        showArea: Boolean = true
    ) {
        val loadingMoreLinear = findViewById<LinearLayout>(R.id.loading_more_linear)
        val loadingMoreButton = findViewById<Button>(R.id.loading_more_btn)
        val noMoreText = findViewById<TextView>(R.id.no_more_text)

        if (loadingMoreButton.tag == null) {
            loadingMoreButton.tag = Any()
            loadingMoreButton.setOnClickListener {
                loadingFavoriteBillings()
            }
        }

        loadingMoreLinear.visibility = if (showArea) View.VISIBLE else View.GONE
        loadingMoreButton.visibility = if (loadingMore) View.VISIBLE else View.GONE
        noMoreText.visibility = if (loadingMore) View.GONE else View.VISIBLE
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

    private fun showDeleteTaskingDialog() {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.favorite_billing_delete_title))
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .show()

        fun taskDelete() {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    FavoriteCreator.cancelBillingFavorites(
                        this@FavoriteBillingActivity,
                        selectedItemId.toList()
                    )
                }
                selectedItemId.clear()
                loadedPage = 0
                totalPage = 0
                loadingFavoriteBillings(true)
                loadingDialog.dismiss()
                if (result) {
                    Toast.makeText(
                        this@FavoriteBillingActivity,
                        resources.getString(R.string.favorite_billing_delete_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                Toast.makeText(
                    this@FavoriteBillingActivity,
                    resources.getString(R.string.favorite_billing_delete_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        taskDelete()
    }

    private fun showCancelFavoriteDialog() {
        MaterialAlertDialogBuilder(this).apply {
            setTitle(getString(R.string.favorite_billing_delete_title))
            setMessage(getString(R.string.favorite_billing_delete_message))
            setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                showDeleteTaskingDialog()
            }
            setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
            show()
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
        showCancelFavoriteDialog()
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