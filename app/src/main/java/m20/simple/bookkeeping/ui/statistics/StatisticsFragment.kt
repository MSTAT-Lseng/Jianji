package m20.simple.bookkeeping.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val statisticsViewModel =
            ViewModelProvider(this).get(StatisticsViewModel::class.java)

        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textGallery
        statisticsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearToolbarSubtitle()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun clearToolbarSubtitle() {
        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
        toolbar?.subtitle = null
    }
}