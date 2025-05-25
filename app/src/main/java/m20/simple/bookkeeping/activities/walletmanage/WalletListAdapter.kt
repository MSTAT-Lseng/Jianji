package m20.simple.bookkeeping.activities.walletmanage

import android.app.Activity
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.utils.UIUtils

data class WalletListItem(
    val walletId: Int,
    val walletName: String,
    val walletAmount: Long,
    val isDefaultWallet: Boolean
)

class WalletListAdapter(
    private val dataSet: Array<WalletListItem>,
    private val resources: Resources,
    private val onItemClick: (WalletListItem, view: View) -> Unit,
    private val activity: Activity
) :
    RecyclerView.Adapter<WalletListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val walletNameTextView: TextView = view.findViewById(R.id.wallet_name)
        val walletAmountTextView: TextView = view.findViewById(R.id.wallet_amount)
        val walletDefaultTagView: TextView = view.findViewById(R.id.wallet_default_tag)
        val editWalletItemView: ImageView = view.findViewById(R.id.edit_wallet_item)
        val rootLayout: LinearLayout = view.findViewById(R.id.root_layout)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.wallet_list_row_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]
        viewHolder.walletNameTextView.text = item.walletName
        viewHolder.walletAmountTextView.text = resources.getString(R.string.wallet_amount,
            WalletCreator.convertAmountFormat(item.walletAmount.toString())
        )
        if (item.isDefaultWallet) viewHolder.walletDefaultTagView.visibility = View.VISIBLE

        // onClickListener
        viewHolder.editWalletItemView.setOnClickListener {
            onItemClick(item, it)
        }

        // navBarHeight
        if (position == dataSet.size - 1) {
            UIUtils.getNavigationBarHeight(
                viewHolder.rootLayout,
                activity,
                fun (navHeight) {
                    val params = viewHolder.rootLayout.layoutParams as RecyclerView.LayoutParams
                    params.bottomMargin += navHeight
                    viewHolder.rootLayout.layoutParams = params
                    viewHolder.rootLayout.requestLayout()
                }
            )
        }
    }

    override fun getItemCount() = dataSet.size
}