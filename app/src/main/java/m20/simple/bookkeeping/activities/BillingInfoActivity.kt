package m20.simple.bookkeeping.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.billing.BillingCreator
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.utils.TimeUtils
import m20.simple.bookkeeping.utils.UIUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class BillingInfoActivity : AppCompatActivity() {

    private var defaultBillId = -1L
    private var billId : Long = defaultBillId
    private var billCoroutineScope : Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing_info)

        UIUtils().setStatusBarTextColor(this, !UIUtils().isDarkMode(resources))
        billId = intent.getLongExtra("billingId", defaultBillId)
        if (checkBillingID()) {
            loadBillingInfo()
        }

    }

    private fun checkBillingID(): Boolean {
        return (billId != -1L).also { if (!it) finish() }
    }

    private fun loadBillingInfo() {
        fun getBillingItemView(iconDrawable: Int, title: String, content: String) : View {
            // load billing_info_item.xml
            val billingItemView = layoutInflater.inflate(R.layout.billing_info_item, null)
            val titleTextView = billingItemView.findViewById<TextView>(R.id.title)
            val contentTextView = billingItemView.findViewById<TextView>(R.id.content)
            val iconImageView = billingItemView.findViewById<ImageView>(R.id.icon)
            val container = billingItemView.findViewById<LinearLayout>(R.id.container)
            titleTextView.text = title
            contentTextView.text = content
            iconImageView.setImageResource(iconDrawable)
            container.setOnClickListener {
                UIUtils().copyTextToClipboard(
                    this@BillingInfoActivity,
                    "$title: $content"
                )
                Toast.makeText(
                    this@BillingInfoActivity,
                    getString(R.string.copy_to_clipboard),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return billingItemView
        }

        val ioTypeText = findViewById<TextView>(R.id.iotype_text)
        val amountText = findViewById<TextView>(R.id.amount_text)
        val classifyImageView = findViewById<ImageView>(R.id.classify_image)
        val depositStatusText = findViewById<TextView>(R.id.deposit_status)
        val listContainer = findViewById<LinearLayout>(R.id.list_container)

        val categoryPairs = UIUtils().getCategoryPairs(resources, this, false)
        val categories = UIUtils().getCategories(resources)

        billCoroutineScope = CoroutineScope(Dispatchers.Main).launch {
            val record = withContext(Dispatchers.IO) {
                BillingCreator.getRecordById(billId, this@BillingInfoActivity)
            }
            val ioType = record?.iotype

            val colorRes = if (ioType == 0) R.color.iotype_expenditure else R.color.iotype_income
            val color = ContextCompat.getColor(this@BillingInfoActivity, colorRes)

            // Amount
            ioTypeText.text = if (ioType == 0) "-" else "+"
            ioTypeText.contentDescription = if (ioType == 0) getString(R.string.expenditure) else getString(R.string.income)
            amountText.text = if (record?.amount == 0) "0.00" else WalletCreator.convertAmountFormat(record?.amount.toString())

            // Amount color
            amountText.setTextColor(color)
            ioTypeText.setTextColor(color)

            // classify
            classifyImageView.setImageResource(
                categoryPairs.find { it.second == record?.classify }?.first ?: R.drawable.account_balance_wallet_thin
            )
            classifyImageView.contentDescription = categories
                .find { it.first == record?.classify }?.second ?: getString(R.string.classify_icon)


            // Deposit Status
            depositStatusText.text = when (record?.deposit) {
                "true" -> getString(R.string.deposit_pay)
                "false" -> getString(R.string.realtime_pay)
                "consumption" -> getString(R.string.deposit_consumption)
                else -> getString(R.string.realtime_pay)
            }

            // Time
            val sdf = SimpleDateFormat("yyyy/MM/dd\nHH:mm", Locale.getDefault())
            val date = Date(record!!.time)

            listContainer.addView(getBillingItemView(R.drawable.schedule,
                resources.getString(R.string.bill_time),
                sdf.format(date)))

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billCoroutineScope?.cancel()
    }

}