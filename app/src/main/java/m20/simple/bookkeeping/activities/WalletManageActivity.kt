package m20.simple.bookkeeping.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

        val uiUtils = UIUtils
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
                    val balanceNumber = WalletCreator.convertNumberToAmount(balance)
                    if (balanceNumber == null) {
                        Toast.makeText(
                            this@WalletManageActivity,
                            R.string.invalid_balance,
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    // change balance
                    taskChangeWalletBalance(
                        walletListItem.walletId,
                        balanceNumber
                    )
                }
            )
        }

        fun setDefault() {
            setDefaultWallet(walletListItem.walletId)
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
                MENU_ITEM_SET_DEFAULT -> setDefault()
                MENU_ITEM_DELETE -> showDeleteWalletDialog(walletListItem.walletId)
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
                },
                this
            )

            val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = walletListAdapter
        }
        lifecycleScope.launch {
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

    private fun showDeleteWalletDialog(walletId: Int) {

        fun taskDeleteWallet(
            walletId: Int,
            transferBalance: Boolean
        ) {
            runWalletTask(
                task = { WalletCreator.deleteWallet(this, walletId, transferBalance) },
                successMessage = R.string.delete_wallet_success,
                errorMessage = R.string.delete_wallet_failed
            )
        }

        val inflater = LayoutInflater.from(this)
        val inputLayout = inflater.inflate(R.layout.delete_wallet_dialog, null)
        val transferBalanceCheckBox =
            inputLayout.findViewById<CheckBox>(R.id.transfer_balance_checkbox)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_wallet)
            .setView(inputLayout)
            .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                taskDeleteWallet(walletId, transferBalanceCheckBox.isChecked)
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun taskAddWallet(name: String) {
        lifecycleScope.launch {
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

    private fun runWalletTask(
        task: suspend () -> Boolean,
        successMessage: Int,
        errorMessage: Int,
        showSuccessToast: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                task()
            }
            if (result) {
                if (showSuccessToast) {
                    Toast.makeText(this@WalletManageActivity, successMessage, Toast.LENGTH_SHORT).show()
                }
                getWalletList()
                onSuccess()
            } else {
                Toast.makeText(this@WalletManageActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun taskRenameWallet(
        walletId: Int,
        newName: String
    ) {
        if (newName.isEmpty()) {
            Toast.makeText(this, R.string.wallet_name_empty, Toast.LENGTH_SHORT).show()
            return
        }

        runWalletTask(
            task = { WalletCreator.renameWallet(this, walletId, newName) },
            successMessage = R.string.rename_wallet_success,
            errorMessage = R.string.wallet_name_exists
        )
    }

    private fun taskChangeWalletBalance(
        walletId: Int,
        newBalance: Long
    ) {
        runWalletTask(
            task = { WalletCreator.setWalletAmount(this, walletId, newBalance) },
            successMessage = R.string.change_wallet_balance_success,
            errorMessage = R.string.change_wallet_balance_failed
        )
    }

    private fun setDefaultWallet(
        walletId: Int
    ) {
        runWalletTask(
            task = { WalletCreator.setDefaultWallet(this, walletId) },
            successMessage = R.string.set_default_wallet_success,
            errorMessage = R.string.set_default_wallet_failed
        )
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