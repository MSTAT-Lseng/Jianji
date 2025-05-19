package m20.simple.bookkeeping.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.activities.FavoriteBillingActivity
import m20.simple.bookkeeping.activities.WalletManageActivity
import m20.simple.bookkeeping.databinding.FragmentSetupBinding
import m20.simple.bookkeeping.utils.UIUtils

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val setupViewModel =
            ViewModelProvider(this).get(SetupViewModel::class.java)

        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UIUtils().fillStatusBarHeight(requireContext(), binding.statusbarHeight)
        clearToolbarSubtitle()
        configList(view)
    }

    private fun configList(view: View) {

        fun taskClick(position: Int) {
            when (position) {
                0 -> {
                    // Wallet management
                    startActivity(Intent(requireActivity(), WalletManageActivity::class.java))
                }
                1 -> {
                    // Scheduled plan
                    Toast.makeText(requireContext(), "Scheduled plan clicked", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    // Collect View
                    startActivity(Intent(requireActivity(), FavoriteBillingActivity::class.java))
                }
            }
        }

        val dataset = listOf(
            // Wallet management
            SetupListItem(R.drawable.wallet_icon,
                getString(R.string.wallet_manage),
                getString(R.string.wallet_manage_summary)),
            // Scheduled plan
            SetupListItem(R.drawable.clock_icon,
                getString(R.string.scheduled_plan),
                getString(R.string.scheduled_plan_summary)),
            // Collect View
            SetupListItem(R.drawable.collect_icon,
                getString(R.string.collect_billing),
                getString(R.string.collect_billing_summary)),
        )

        val listAdapter = SetupListAdapter(dataset)
        listAdapter.onItemClick = { position ->
            taskClick(position)
        }
        val recyclerView: RecyclerView = view.findViewById(R.id.list_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = listAdapter

    }

    private fun clearToolbarSubtitle() {
        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
        toolbar?.subtitle = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}