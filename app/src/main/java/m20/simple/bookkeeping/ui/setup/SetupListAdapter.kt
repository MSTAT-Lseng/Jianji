package m20.simple.bookkeeping.ui.setup

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.utils.UIUtils

data class SetupListItem(
    val iconResId: Int,
    val title: String,
    val subtitle: String
)

class SetupListAdapter(
    private val dataSet: List<SetupListItem>,
    private val activity: Activity
) :
    RecyclerView.Adapter<SetupListAdapter.ViewHolder>() {

    var onItemClick: ((Int) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon = view.findViewById<ImageView>(R.id.icon)
        val title = view.findViewById<TextView>(R.id.title)
        val subtitle = view.findViewById<TextView>(R.id.subtitle)
        val rootLayout = view.findViewById<LinearLayout>(R.id.root_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.setup_list_row_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.icon.setImageResource(item.iconResId)
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(position)
        }

        // navBarHeight
        if (position == dataSet.size - 1) {
            UIUtils.getNavigationBarHeight(
                holder.rootLayout,
                activity,
                fun (navHeight) {
                    val params = holder.rootLayout.layoutParams as RecyclerView.LayoutParams
                    params.bottomMargin += navHeight
                    holder.rootLayout.layoutParams = params
                    holder.rootLayout.requestLayout()
                }
            )
        }
    }

    override fun getItemCount() = dataSet.size
}
