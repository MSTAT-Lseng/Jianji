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
import com.google.android.material.textfield.TextInputLayout
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

        fun rename() {
            userInputDialog(
                getString(R.string.rename_wallet_name),
                getString(R.string.input_new_name),
                fun(name: String) {
                    taskRenameWallet(walletListItem.walletId, name)
                }
            )
        }

        fun changeBalance() {
            userInputDialog(
                getString(R.string.change_wallet_balance),
                getString(R.string.input_new_balance),
                fun(balance: String) {
                    val balanceNumber = balance.toLongOrNull()
                    if (balanceNumber == null) {
                        Toast.makeText(
                            this@WalletManageActivity,
                            R.string.invalid_balance,
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                }
            )
        }

        val MENU_ITEM_RENAME = 1
        val MENU_ITEM_DELETE = 2
        val MENU_ITEM_SET_DEFAULT = 3
        val MENU_ITEM_CHANGE_BALANCE = 4

        val popupMenu = PopupMenu(this, view)
        val menu = popupMenu.menu
        menu.add(0, MENU_ITEM_RENAME, 0, getString(R.string.rename_wallet_name))
        menu.add(0, MENU_ITEM_CHANGE_BALANCE, 1, getString(R.string.change_wallet_balance))
        if (!walletListItem.isDefaultWallet) {
            menu.add(0, MENU_ITEM_DELETE, 2, getString(R.string.delete_wallet_name))
            menu.add(0, MENU_ITEM_SET_DEFAULT, 3, getString(R.string.set_default_wallet))
        }

        popupMenu.setOnMenuItemClickListener({ item ->
            val id = item.itemId
            when (id) {
                MENU_ITEM_RENAME -> rename()
                MENU_ITEM_CHANGE_BALANCE -> changeBalance()
            }
            false
        })

        popupMenu.show()
    }

    private fun getWalletList() {
        fun updateUi(
            walletList: List<Pair<Int, String>>,
            defaultWallet: Int?,
            amountList: List<Long>
        ) {
            val dataset: List<WalletListItem> = walletList.mapIndexed { index, walletPair ->
                val walletId = walletPair.first // 钱包 ID
                val walletName = walletPair.second // 钱包名称
                val walletAmount = amountList.getOrNull(index) ?: 0L
                val isDefault = walletId == defaultWallet

                WalletListItem(walletId, walletName, walletAmount, isDefault)
            }

            val walletListAdapter = WalletListAdapter(
                dataset.toTypedArray(),
                resources,
                fun(walletListItem: WalletListItem, view: View) {
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
                walletCreator.getDefaultWallet(this@WalletManageActivity, resources).first
            }
            val amountList = withContext(Dispatchers.IO) {
                walletList.map { (id, _) ->
                    walletCreator.getWalletNameAndBalance(this@WalletManageActivity, id)?.second
                        ?: 0
                }
            }
            updateUi(walletList, defaultWallet, amountList)
        }
    }

    private fun userInputDialog(
        title: String,
        hintText: String,
        positiveCallback: (String) -> Unit
    ) {
        val inflater = LayoutInflater.from(this)
        val inputLayout = inflater.inflate(R.layout.rename_wallet_dialog, null)
        val inputEditText =
            inputLayout.findViewById<TextInputEditText>(R.id.editTextUserInput)
        val textInputLayout =
            inputLayout.findViewById<TextInputLayout>(R.id.textInputLayout)
        textInputLayout.hint = hintText

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(inputLayout)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                val userInput = inputEditText.text.toString()
                positiveCallback(userInput)
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun taskAddWallet(name: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val walletCreator = WalletCreator
            val result = withContext(Dispatchers.IO) {
                walletCreator.createWallet(this@WalletManageActivity, name)
            }
            when (result) {
                -1 -> {
                    Toast.makeText(
                        this@WalletManageActivity,
                        R.string.wallet_name_empty,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                -2 -> {
                    Toast.makeText(
                        this@WalletManageActivity,
                        R.string.wallet_name_exists,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    Toast.makeText(
                        this@WalletManageActivity,
                        R.string.add_wallet_success,
                        Toast.LENGTH_SHORT
                    ).show()
                    getWalletList()
                }
            }
        }
    }

    private fun taskRenameWallet(
        walletId: Int,
        newName: String
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            if (newName.isEmpty()) {
                Toast.makeText(
                    this@WalletManageActivity,
                    R.string.wallet_name_empty,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val walletCreator = WalletCreator
            val result = withContext(Dispatchers.IO) {
                walletCreator.renameWallet(this@WalletManageActivity, walletId, newName)
            }
            if (result) {
                Toast.makeText(
                    this@WalletManageActivity,
                    R.string.rename_wallet_success,
                    Toast.LENGTH_SHORT
                ).show()
                getWalletList()
                return@launch
            }
            Toast.makeText(
                this@WalletManageActivity,
                R.string.wallet_name_exists,
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.wallet_manage_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.add -> {
                userInputDialog(
                    getString(R.string.add_wallet),
                    getString(R.string.add_wallet_hint),
                    fun(name: String) {
                        taskAddWallet(name)
                    })
            }
        }
        return super.onOptionsItemSelected(item)
    }

}