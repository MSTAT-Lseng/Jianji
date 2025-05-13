package m20.simple.bookkeeping.activities.walletmanage

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.wallet.WalletCreator

data class WalletListItem(
    val walletId: Int,
    val walletName: String,
    val walletAmount: Long,
    val isDefaultWallet: Boolean
)

class WalletListAdapter(
    private val dataSet: Array<WalletListItem>,
    private val resources: Resources,
    private val onItemClick: (WalletListItem, view: View) -> Unit
) :
    RecyclerView.Adapter<WalletListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val walletNameTextView: TextView = view.findViewById(R.id.wallet_name)
        val walletAmountTextView: TextView = view.findViewById(R.id.wallet_amount)
        val walletDefaultTagView: TextView = view.findViewById(R.id.wallet_default_tag)
        val editWalletItemView: ImageView = view.findViewById(R.id.edit_wallet_item)
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
    }

    override fun getItemCount() = dataSet.size
}