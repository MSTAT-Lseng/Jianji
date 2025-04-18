package m20.simple.bookkeeping.activities

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
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
import m20.simple.bookkeeping.utils.UIUtils

class BillingInfoActivity : AppCompatActivity() {

    private var billId : Long? = null
    private var billCoroutineScope : Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing_info)

        UIUtils().setStatusBarTextColor(this, !UIUtils().isDarkMode(resources))
        billId = intent.getLongExtra("billingId", -1)
        if (checkBillingID()) {
            loadBillingInfo()
        }

    }

    private fun checkBillingID(): Boolean {
        return (billId != null && billId != -1L).also { if (!it) finish() }
    }

    private fun loadBillingInfo() {
        val ioTypeText = findViewById<TextView>(R.id.iotype_text)
        val amountText = findViewById<TextView>(R.id.amount_text)
        val classifyImageView = findViewById<ImageView>(R.id.classify_image)

        val categoryPairs = UIUtils().getCategoryPairs(resources, this, false)

        billCoroutineScope = CoroutineScope(Dispatchers.Main).launch {
            val record = withContext(Dispatchers.IO) {
                BillingCreator.getRecordById(billId!!, this@BillingInfoActivity)
            }
            val ioType = record?.iotype

            val colorRes = if (ioType == 0) R.color.iotype_expenditure else R.color.iotype_income
            val color = ContextCompat.getColor(this@BillingInfoActivity, colorRes)

            // Amount
            ioTypeText.text = if (ioType == 0) "-" else "+"
            amountText.text = if (record?.amount == 0) "0.00" else WalletCreator.convertAmountFormat(record?.amount.toString())

            // Amount color
            amountText.setTextColor(color)
            ioTypeText.setTextColor(color)

            classifyImageView.setImageResource(
                categoryPairs.find { it.second == record?.classify }?.first ?: R.drawable.account_balance_wallet_thin
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billCoroutineScope?.cancel()
    }

}