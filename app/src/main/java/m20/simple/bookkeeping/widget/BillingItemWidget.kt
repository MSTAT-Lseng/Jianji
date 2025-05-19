package m20.simple.bookkeeping.widget

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.utils.TextUtils
import m20.simple.bookkeeping.utils.TimeUtils
import m20.simple.bookkeeping.utils.UIUtils
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object BillingItemWidget {

    fun getWidget(
        activity: Activity,
        inflateViewGroup: ViewGroup,
        record: BillingDao.Record,
        resources: Resources,
        allWallets: List<Pair<Int, String>>,
    ): View {
        val billingItemView = LayoutInflater.from(activity)
            .inflate(R.layout.billing_item, inflateViewGroup, false)

        billingItemView.apply {
            // classify
            val classifyImageView = findViewById<ImageView>(R.id.classify_image)
            val categoryPairs = UIUtils().getCategoryPairs(resources, activity)
            val categories = UIUtils().getCategories(resources)
            val category = categoryPairs.find { it.second == record.classify }
            classifyImageView.setImageResource(
                category?.first ?: R.drawable.account_balance_wallet_thin
            )
            classifyImageView.contentDescription = categories
                .find { it.first == record.classify }?.second
                ?: resources.getString(R.string.classify_icon)

            // Amount
            val amountTextView = findViewById<TextView>(R.id.amount_text)
            val (amount, amountAccessibility) = record.amount.let { value ->
                val convertedTrue =
                    WalletCreator.convertAmountFormat(value.toString(), true, record.iotype)
                val convertedFalse =
                    WalletCreator.convertAmountFormat(value.toString(), false, record.iotype)
                convertedTrue to convertedFalse
            }
            val amountColor = resources.getColor(
                when {
                    record.deposit == "true" -> R.color.iotype_deposit
                    record.iotype == 0 -> R.color.iotype_expenditure
                    else -> R.color.iotype_income
                }
            )
            var amountContentDescription =
                if (record.iotype == 0)
                    "${resources.getString(R.string.expenditure)}$amountAccessibility"
                else
                    "${resources.getString(R.string.income)}$amountAccessibility"
            amountContentDescription += when {
                record.deposit == "true" ->
                    resources.getString(R.string.deposit_bill)
                record.deposit == "consumption" ->
                    resources.getString(R.string.consumption_bill)
                else -> ""
            }
            amountTextView.text = amount
            amountTextView.setTextColor(amountColor)
            amountTextView.contentDescription = amountContentDescription

            // Note
            val noteTextView = findViewById<TextView>(R.id.notes_text)
            val noteText = record.notes?.let { TextUtils.cutStringToLength(it, 15) }
            noteTextView.text = noteText
            when {
                record.notes == null -> noteTextView.visibility = View.GONE
            }

            // Wallet
            val walletTextView = findViewById<TextView>(R.id.wallet_text)
            val walletName = allWallets.find { it.first == record.wallet }?.second
            walletTextView.text =
                walletName ?: activity.getString(R.string.unknown_wallet)

            // Time
            val localTime = TimeUtils.getDateFromTimestamp(record.time).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            findViewById<TextView>(R.id.time_text).text =
                localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        return billingItemView
    }

    fun getDefaultBackground(
        context: Context
    ): Drawable {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            typedValue,
            true
        )
        return ContextCompat.getDrawable(context, typedValue.resourceId)!!
    }

}