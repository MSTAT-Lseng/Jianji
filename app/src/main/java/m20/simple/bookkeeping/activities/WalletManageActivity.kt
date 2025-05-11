package m20.simple.bookkeeping.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.activities.walletmanage.WalletListAdapter
import m20.simple.bookkeeping.activities.walletmanage.WalletListItem
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

    private fun onWalletItemClick(walletListItem: WalletListItem, view: View) {
        fun showRenameDialog() {
            val inflater = LayoutInflater.from(this)
            val inputLayout = inflater.inflate(R.layout.rename_wallet_dialog, null)
            // 获取布局中的 EditText
            val inputEditText =
                inputLayout.findViewById<TextInputEditText>(R.id.editTextUserInput)

            MaterialAlertDialogBuilder(this)
                .setTitle("输入信息")
                .setView(inputLayout)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取用户输入的内容
                    val userInput = inputEditText.text.toString()
                    // 在这里处理用户输入的内容，例如显示一个 Toast
                    Toast.makeText(this, "您输入了: $userInput", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(android.R.string.cancel)) { dialog, which ->
                    dialog.cancel()
                }
                .show()
        }

        fun rename() {
            showRenameDialog()
            /*CoroutineScope(Dispatchers.Main).launch {
                if (WalletCreator.renameWallet(
                    this@WalletManageActivity,
                    walletListItem.walletId.toInt(),
                    ""
                )) {

                }
            }*/
        }

        val MENU_ITEM_RENAME = 1
        val MENU_ITEM_DELETE = 2
        val MENU_ITEM_SET_DEFAULT = 3

        val popupMenu = PopupMenu(this, view)
        val menu = popupMenu.menu
        menu.add(0, MENU_ITEM_RENAME, 0, getString(R.string.rename_wallet_name))
        if (!walletListItem.isDefaultWallet) {
            menu.add(0, MENU_ITEM_DELETE, 1, getString(R.string.delete_wallet_name))
            menu.add(0, MENU_ITEM_SET_DEFAULT, 2, getString(R.string.set_default_wallet))
        }

        popupMenu.setOnMenuItemClickListener({ item ->
            val id = item.itemId
            when (id) {
                MENU_ITEM_RENAME -> rename()
            }
            false
        })

        popupMenu.show()
    }

    private fun getWalletList() {
        fun updateUi(walletList: List<Pair<Int, String>>,
                     defaultWallet: Int?,
                     amountList: List<Long>) {
            val dataset: List<WalletListItem> = walletList.mapIndexed { index, walletPair ->
                val walletId = walletPair.first // 钱包 ID
                val walletName = walletPair.second // 钱包名称
                val walletAmount = amountList.getOrNull(index) ?: 0L
                val isDefault = walletId == defaultWallet

                WalletListItem(walletId, walletName, walletAmount, isDefault)
            }

            val walletListAdapter = WalletListAdapter(dataset.toTypedArray(),
                resources,
                fun (walletListItem: WalletListItem, view: View) {
                    onWalletItemClick(walletListItem, view)
                }
            )

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
                    walletCreator.getWalletNameAndBalance(this@WalletManageActivity, id)?.second ?: 0
                }
            }
            updateUi(walletList, defaultWallet, amountList)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.wallet_manage_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}