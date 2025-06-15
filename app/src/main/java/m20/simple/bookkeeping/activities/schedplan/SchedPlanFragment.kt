package m20.simple.bookkeeping.activities.schedplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.widget.ClassifyPickerWidget
import m20.simple.bookkeeping.widget.WalletSelectionWidget
import java.util.concurrent.Executors

open class SchedPlanFragment : Fragment() {

    private val walletExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_schedplan_create_layout, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fun loadClassifyPicker() {
            val rootView = view.findViewById<LinearLayout>(R.id.classify_layout)
            val classifyView = ClassifyPickerWidget.getWidget(
                rootView,
                layoutInflater,
                resources,
                { classify ->
                    //billingObject.classify = classify
                },
                { ids, container ->
                    /*if (isEditBilling == true) {
                        val classifyId = billingObject.classify
                        val index = ids.indexOf(classifyId)
                        container.check(container.getChildAt(index).id)
                    }*/
                }
            )
            rootView.addView(classifyView)
        }

        fun loadWalletSelector() {
            val rootView = view.findViewById<LinearLayout>(R.id.wallet_layout)
            val walletView = WalletSelectionWidget.getWidget(
                rootView,
                layoutInflater,
                resources,
                { defaultWalletID, walletInputText ->
                    //if (!isEditBilling!!) {
                        //billingObject.wallet = defaultWalletID
                        walletInputText.hint = getString(R.string.choose_wallet)
                    //} else {
                        //walletInputText.hint = getString(R.string.wallet_selector_default)
                    //}
                },
                { walletIdPosition ->
                    //val selectedItem: Int = walletIdPosition
                    //billingObject.wallet = selectedItem
                },
                requireActivity(),
                walletExecutorService
            )
            rootView.addView(walletView)
        }

        loadClassifyPicker()
        loadWalletSelector()
    }

    fun getScheduleDateTitleView(): LinearLayout? {
        return view?.findViewById(R.id.schedule_date_title_view)
    }

    override fun onDestroy() {
        super.onDestroy()
        walletExecutorService.shutdown()
    }

}
