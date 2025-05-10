package m20.simple.bookkeeping.activities

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.HeroCarouselStrategy
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.billing.BillingCreator
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.utils.FileUtils
import m20.simple.bookkeeping.utils.TimeUtils
import m20.simple.bookkeeping.utils.UIUtils
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class BillingInfoActivity : AppCompatActivity() {

    private var defaultBillId = -1L
    private var billId : Long = defaultBillId
    private var depositStatus: String = "notSet"
    private var billCoroutineScope : Job? = null
    private var topBar : MaterialToolbar? = null
    private var modified : Boolean = false

    companion object {
        val modifiedBillKeys = "isModified"
    }

    private val launcher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
        startActivity(Intent(
            this, BillingInfoActivity::class.java
        ).putExtra("billingId", billId).putExtra("modified", true))
        modified = true; setModified()
        finish()
    }

    val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing_info)

        UIUtils().setStatusBarTextColor(this, !UIUtils().isDarkMode(resources))
        configToolbar()
        billId = intent.getLongExtra("billingId", defaultBillId)
        modified = intent.getBooleanExtra("modified", modified)
        if (checkBillingID()) {
            loadBillingInfo()
            setModified()
        }

    }

    private fun setModified() {
        if (!modified) return
        val resultIntent = Intent()
        resultIntent.putExtra(modifiedBillKeys, true)
        setResult(RESULT_OK, resultIntent)
    }

    private fun configToolbar() {
        topBar = findViewById(R.id.topAppBar)
        setSupportActionBar(topBar)
        topBar?.setNavigationOnClickListener { v ->
            onBackPressed()
        }
    }

    private fun checkBillingID(): Boolean {
        return (billId != -1L).also { if (!it) finish() }
    }

    private fun loadBillingInfo() {
        fun getBillingItemView(iconDrawable: Int,
                               title: String,
                               content: String,
                               verticalLayout: Boolean = false) : View {
            // load billing_info_item.xml
            val billingItemView = layoutInflater.inflate(R.layout.billing_info_item, null)
            val titleTextView = billingItemView.findViewById<TextView>(R.id.title)
            val contentTextView = billingItemView.findViewById<TextView>(R.id.content)
            val iconImageView = billingItemView.findViewById<ImageView>(R.id.icon)
            val container = billingItemView.findViewById<LinearLayout>(R.id.container)

            // verticalLayout
            if (verticalLayout) {
                container.orientation = LinearLayout.VERTICAL
                contentTextView.gravity = android.view.Gravity.START
                contentTextView.setPadding(4.dp, 8.dp, 4.dp, 0)
                contentTextView.setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant))
            }

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

        fun configPictureCarousel(record: BillingDao.Record) {
            if (record.images == null) {
                findViewById<LinearLayout>(R.id.carousel_container).visibility = View.GONE
                return
            }

            fun getData(): List<Uri> {
                val imageUriList = record.images.split(",")
                return imageUriList.mapNotNull { filename ->
                    FileUtils(this@BillingInfoActivity)
                        .getPhotosUriByName(filename.trim())
                }
            }

            val carouselRecyclerView = findViewById<RecyclerView>(R.id.carousel_recycler_view)
            // 设置CarouselLayoutManager为英雄策略
            carouselRecyclerView.layoutManager = CarouselLayoutManager(HeroCarouselStrategy())
            // 设置适配器
            carouselRecyclerView.adapter = CarouselAdapter(getData(), this, record)
            // 使用CarouselSnapHelper来使滚动停靠到最近的项目
            val snapHelper = CarouselSnapHelper()
            snapHelper.attachToRecyclerView(carouselRecyclerView)
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
            depositStatus = record?.deposit ?: depositStatus
            depositStatusText.text = when (record?.deposit) {
                "true" -> getString(R.string.deposit_pay)
                "false" -> getString(R.string.realtime_pay)
                "consumption" -> getString(R.string.deposit_consumption)
                else -> getString(R.string.realtime_pay)
            }
            invalidateOptionsMenu()

            // Wallet
            val walletName = withContext(Dispatchers.IO) {
                WalletCreator.getWalletNameAndBalance(this@BillingInfoActivity, record?.wallet!!.toLong())!!.first
            }
            listContainer.addView(getBillingItemView(R.drawable.account_balance_wallet_300,
                resources.getString(R.string.wallet),
                walletName!!))

            // Time
            val sdf = SimpleDateFormat("yyyy/MM/dd\nHH:mm", Locale.getDefault())
            val date = Date(record!!.time)

            listContainer.addView(getBillingItemView(R.drawable.schedule,
                resources.getString(R.string.bill_time),
                sdf.format(date)))

            // Notes
            if (record.notes != null && record.notes != "") {
                listContainer.addView(getBillingItemView(R.drawable.edit_note_300,
                    resources.getString(R.string.notes),
                    record.notes,
                    true))
            }

            // Images
            configPictureCarousel(record)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billCoroutineScope?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.billing_info_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val editItem = menu?.findItem(R.id.edit)
        fun setDepositItem() {
            editItem?.setIcon(R.drawable.edit_calendar)
            editItem?.setTitle(R.string.edit_deposit_date)
        }
        fun setConsumptionItem() {
            editItem?.setIcon(R.drawable.forward)
            editItem?.setTitle(R.string.jump_deposit_bill)
        }
        when (depositStatus) {
            "true" ->
                setDepositItem()
            "consumption" ->
                setConsumptionItem()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun editBilling() {
            val intent = Intent(this, CreateBillingActivity::class.java)
            intent.putExtra("isEditBilling", true)
            intent.putExtra("billingId", billId.toInt())
            launcher.launch(intent)
        }

        fun taskRemoveBilling() {
            CoroutineScope(Dispatchers.Main).launch {
                val removeCode = withContext(Dispatchers.IO) {
                    val billingCreator = BillingCreator
                    billingCreator.deleteBillingById(billId.toInt(), this@BillingInfoActivity)
                }
                if (removeCode) {
                    modified = true; setModified(); finish()
                    return@launch
                }
                Toast.makeText(
                    this@BillingInfoActivity,
                    getString(R.string.delete_billing_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fun removeBilling() {
            MaterialAlertDialogBuilder(this)
                .setView(R.layout.dialog_remove_billing)
                .setNegativeButton("取消") { _, _ -> }
                .setPositiveButton("确认") { _, _ ->
                    taskRemoveBilling()
                }
                .show()
        }

        fun taskEditBilling() {
            when (depositStatus) {
                "false" -> editBilling()
                "true" -> {
                    val timestamp = TimeUtils.getDayLevelTimestamp()
                    modifyDepositBillPayDate(timestamp)
                }
                "consumption" -> {
                    modified = true; setModified()
                    startActivity(Intent(this,
                        BillingInfoActivity::class.java)
                        .putExtra("billingId", billId - 1)
                        .putExtra("modified", true))
                    finish()
                }
            }
        }

        return when (item.itemId) {
            R.id.edit -> {
                taskEditBilling()
                true
            }
            R.id.delete -> {
                removeBilling()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun dialogUpdatePayDateFailed(
        billingCreator: BillingCreator,
        result: Pair<Int, Int>
    ) {
        MaterialAlertDialogBuilder(this@BillingInfoActivity)
            .setTitle(
                billingCreator.getCreateBillingFailedReason(result.first, resources)
            )
            .setMessage(
                billingCreator.getCreateBillingFailedReason(result.second, resources)
            )
            .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun depositPayDateEdited(timestamp: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            val billingCreator = BillingCreator
            val result = withContext(Dispatchers.IO) {
                billingCreator.modifyDepositBillingPayDate(billId, this@BillingInfoActivity, timestamp)
            }

            if (result.first == billingCreator.MODIFY_BILLING_DEPOSIT_PAY_DATE_SUCCESS) {
                modified = true; setModified()
                Toast.makeText(
                    this@BillingInfoActivity,
                    getString(R.string.modify_deposit_pay_date_success),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                dialogUpdatePayDateFailed(billingCreator, result)
            }
        }
    }

    private fun modifyDepositBillPayDate(timestamp: Long) {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_actual_consumption_date))
                .setSelection(TimeUtils.convertTimestampToLocalDate(timestamp)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli())
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = TimeUtils.getDateFromTimestamp(selection).apply {
                hours = 0
                minutes = 0
            }
            depositPayDateEdited(selectedDate.time)
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

}

// 假设这是一个简单的适配器实现
class CarouselAdapter(
    private val data: List<Uri>,
    private val activity: AppCompatActivity,
    private val record: BillingDao.Record
) : RecyclerView.Adapter<CarouselAdapter.MyViewHolder>() {

    // ViewHolder类，用于缓存视图
    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.carousel_image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.carousel_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val uri = data[position]
        holder.imageView.setImageURI(uri)

        // 设置点击事件
        holder.imageView.setOnClickListener {
            val intent = Intent(activity, PictureViewerActivity::class.java).apply {
                putExtra("provideType", "photos-storage")
                putExtra("images", record.images)
                putExtra("defaultItem", position)
            }
            activity.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = data.size
}