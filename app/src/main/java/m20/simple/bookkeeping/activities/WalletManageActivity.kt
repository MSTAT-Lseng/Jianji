package m20.simple.bookkeeping.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.activities.walletmanage.WalletListAdapter
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.utils.UIUtils

class WalletManageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_manage)
        setSupportActionBar(findViewById(R.id.toolbar))

        val uiUtils = UIUtils()
        uiUtils.fillStatusBarHeight(this, findViewById(R.id.status_bar_view))
        uiUtils.setStatusBarTextColor(this, !uiUtils.isDarkMode(resources))

        getWalletList()
    }

    private fun getWalletList() {
        fun updateUi(walletList: List<Pair<Int, String>>,
                     defaultWallet: Int?,
                     amountList: List<Int>) {
            val dataset = walletList.map { it.second }.toTypedArray()
            val walletListAdapter = WalletListAdapter(dataset)

            val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = walletListAdapter
        }
        CoroutineScope(Dispatchers.Main).launch {
            val walletCreator = WalletCreator
            val walletList = withContext(Dispatchers.IO) {
                walletCreator.getAllWallets(this@WalletManageActivity)
            }
            val defaultWallet = withContext(Dispatchers.IO) {
                walletCreator.getDefaultWallet(this@WalletManageActivity)?.first
            }
            val amountList = withContext(Dispatchers.IO) {
                walletList.map { (id, _) ->
                    walletCreator.getWalletNameAndBalance(this@WalletManageActivity, id.toLong())?.second ?: 0
                }
            }
            updateUi(walletList, defaultWallet, amountList)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}