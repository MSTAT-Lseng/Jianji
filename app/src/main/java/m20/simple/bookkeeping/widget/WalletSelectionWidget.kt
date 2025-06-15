package m20.simple.bookkeeping.widget

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.wallet.WalletCreator
import java.util.concurrent.ExecutorService

object WalletSelectionWidget {

    fun getWidget(
        rootView: LinearLayout,
        layoutInflater: LayoutInflater,
        resources: Resources,
        checkEdit: (Int, MaterialAutoCompleteTextView) -> Unit,
        onClick: (Int) -> Unit,
        activity: Activity,
        executorService: ExecutorService
    ): LinearLayout {
        val view = layoutInflater.inflate(R.layout.wallet_selection_widget, rootView, false) as LinearLayout
        configWalletSelector(view, checkEdit, onClick, activity, resources, executorService)
        return view
    }

    private fun configWalletSelector(
        view: View,
        checkEdit: (Int, MaterialAutoCompleteTextView) -> Unit,
        onClick: (Int) -> Unit = {},
        activity: Activity,
        resources: Resources,
        executorService: ExecutorService
    ) {
        val walletInputText: MaterialAutoCompleteTextView = view.findViewById(R.id.wallet_input_text)

        fun updateUi(allWallets: List<Pair<Int, String>>, defaultWalletID: Int) {
            val wallets = allWallets.map { it.second }.toTypedArray()
            val walletIds = allWallets.map { it.first }.toTypedArray()
            walletInputText.setSimpleItems(wallets)

            walletInputText.setOnItemClickListener { parent, view, position, id ->
                onClick(walletIds[position])
            }

            checkEdit(defaultWalletID, walletInputText)
        }

        executorService.execute {
            WalletCreator.getDefaultWallet(activity, resources)
                .let { (defaultWalletID, _) ->
                    val allWallets = WalletCreator.getAllWallets(activity)
                    activity.runOnUiThread { updateUi(allWallets, defaultWalletID) }
                }
        }
    }

}