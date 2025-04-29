package m20.simple.bookkeeping.ui.setup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import m20.simple.bookkeeping.R

data class SetupListItem(
    val iconResId: Int,
    val title: String,
    val subtitle: String
)

class SetupListAdapter(private val dataSet: List<SetupListItem>) :
    RecyclerView.Adapter<SetupListAdapter.ViewHolder>() {

    var onItemClick: ((Int) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon = view.findViewById<ImageView>(R.id.icon)
        val title = view.findViewById<TextView>(R.id.title)
        val subtitle = view.findViewById<TextView>(R.id.subtitle)
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
    }

    override fun getItemCount() = dataSet.size
}
