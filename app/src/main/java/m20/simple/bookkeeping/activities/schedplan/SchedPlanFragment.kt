package m20.simple.bookkeeping.activities.schedplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.objects.BillingObject
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.utils.UIUtils
import m20.simple.bookkeeping.widget.ClassifyPickerWidget
import m20.simple.bookkeeping.widget.WalletSelectionWidget
import java.util.concurrent.Executors

open class SchedPlanFragment : Fragment() {

    private val walletExecutorService = Executors.newSingleThreadExecutor()
    val billingObject = BillingObject(
        time = 0,
        amount = 0,
        iotype = 0,
        classify = "others",
        notes = null,
        images = null,
        deposit = "false",
        wallet = 1,
        tags = null
    )

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
        UIUtils.commonNavBarHeight(view, requireActivity())

        fun loadClassifyPicker() {
            val rootView = view.findViewById<LinearLayout>(R.id.classify_layout)
            val classifyView = ClassifyPickerWidget.getWidget(
                rootView,
                layoutInflater,
                resources,
                { classify ->
                    billingObject.classify = classify
                },
                { _, _ -> }
            )
            rootView.addView(classifyView)
        }

        fun loadWalletSelector() {
            val rootView = view.findViewById<LinearLayout>(R.id.wallet_layout)
            val walletView = WalletSelectionWidget.getWidget(
                rootView,
                layoutInflater,
                resources,
                { _, walletInputText ->
                    walletInputText.hint = getString(R.string.choose_wallet)
                },
                { walletIdPosition ->
                    billingObject.wallet = walletIdPosition
                },
                requireActivity(),
                walletExecutorService
            )
            rootView.addView(walletView)
        }

        loadClassifyPicker()
        loadWalletSelector()

        configAmountEditText()
        configIncomeCheckBox()
        configNoteEditText()
    }

    fun getAmountEditText(): TextInputEditText? {
        return view?.findViewById(R.id.et_input_text)
    }

    fun getIncomeCheckBox(): CheckBox? {
        return view?.findViewById(R.id.cb_income)
    }

    fun getNoteEditText(): TextInputEditText? {
        return view?.findViewById(R.id.notes_input_text)
    }

    private fun configAmountEditText() {
        val amountEditText = getAmountEditText()

        fun taskAmount(amount: String) {
            billingObject.amount = WalletCreator.convertNumberToAmount(amount) ?: 0L
        }

        amountEditText?.doAfterTextChanged { editable ->
            val amount = editable?.toString().orEmpty()
            if (amount.isNotBlank()) {
                taskAmount(amount)
            } else {
                billingObject.amount = 0
            }
        }
    }

    private fun configIncomeCheckBox() {
        getIncomeCheckBox()?.setOnCheckedChangeListener { _, isChecked ->
            billingObject.iotype = if (isChecked) 1 else 0
        }
    }

    private fun configNoteEditText() {
        val editText = getNoteEditText()
        editText?.doAfterTextChanged { editable ->
            billingObject.notes = editable.toString().takeIf { it.isNotEmpty() }
        }
    }

    fun getScheduleDateTitleView(): LinearLayout? {
        return view?.findViewById(R.id.schedule_date_title_view)
    }

    fun getSaveButton(): Button? {
        return view?.findViewById(R.id.save_sched_plan_btn)
    }

    override fun onDestroy() {
        super.onDestroy()
        walletExecutorService.shutdown()
    }

}
