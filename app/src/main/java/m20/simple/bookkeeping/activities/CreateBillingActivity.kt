package m20.simple.bookkeeping.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.billing.BillingCreator
import m20.simple.bookkeeping.api.config.HintConfigCreator
import m20.simple.bookkeeping.api.objects.BillingObject
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.config.HintConfig
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.utils.FileUtils
import m20.simple.bookkeeping.utils.PremissionUtils
import m20.simple.bookkeeping.utils.TimeUtils
import m20.simple.bookkeeping.utils.UIUtils
import m20.simple.bookkeeping.widget.ClassifyPickerWidget
import m20.simple.bookkeeping.widget.WalletSelectionWidget
import java.io.File
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors


class CreateBillingActivity : AppCompatActivity() {

    private var isEditBilling: Boolean? = null
    private var isEditBillingId: Long? = null
    private var editBillingRecord: BillingDao.Record? = null

    private val billingObject = BillingObject(
        time = TimeUtils.getMinuteLevelTimestamp(),
        amount = 0,
        iotype = 0,
        classify = "others",
        notes = null,
        images = null,
        deposit = "false",
        wallet = 1,
        tags = null
    )
    private var depositBillingDate = TimeUtils.getDayLevelTimestamp()
    private val walletExecutorService = Executors.newSingleThreadExecutor()

    private var selectedPhotosNumber = 0
    private var selectedPhotosList = mutableListOf<Uri>()
    private var selectedPhotoNameList = mutableListOf<String>()
    private var photoUri: Uri? = null

    companion object {
        const val createBillingReturnedKey = "createdBilling"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_billing)
        setSupportActionBar(findViewById(R.id.toolbar))

        val uiUtils = UIUtils
        uiUtils.fillStatusBarHeight(this, findViewById(R.id.status_bar_view))
        uiUtils.setStatusBarTextColor(this, !uiUtils.isDarkMode(resources))
        configNavBarHeight()

        isEditBilling = intent.getBooleanExtra("isEditBilling", false)
        isEditBillingId = intent.getLongExtra("billingId", -1)

        fun initBillingObject() {
            if (!isEditBilling!!) return

            editBillingRecord?.let { record ->
                billingObject.apply {
                    time = record.time
                    amount = record.amount
                    iotype = record.iotype
                    classify = record.classify
                    notes = record.notes
                    images = record.images
                    deposit = record.deposit
                    wallet = record.wallet
                    tags = record.tags
                }
            }
        }

        fun initComponent() {
            configToolbar()
            configAmountEditText()

            configDatePicker()
            loadWalletSelector()
            configPhotoSelector()
            configNoteEditText()
            loadClassifyPicker()
            configDepositCheckBox()
            configIncomeCheckBox()

            configSubmitButton()
        }

        lifecycleScope.launch {
            editBillingRecord = if (isEditBilling == true) {
                withContext(Dispatchers.IO) {
                    BillingCreator.getRecordById(
                        isEditBillingId!!,
                        this@CreateBillingActivity
                    )
                }
            } else {
                null
            }
            // 空值检查
            if (isEditBilling == true && editBillingRecord?.id == -1L) {
                Toast.makeText(this@CreateBillingActivity,
                    R.string.billing_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            initBillingObject()
            initComponent()
        }
    }

    private fun configNavBarHeight() {
        val finishBillingButton = findViewById<Button>(R.id.finish_billing_btn)
        val navBarHeightView = findViewById<View>(R.id.nav_bar_height)
        UIUtils.getNavigationBarHeight(
            finishBillingButton,
            this,
            fun (navHeight) {
                configButtonMarginBottom(finishBillingButton, navHeight)
                UIUtils.fillNavBarHeight(navBarHeightView, navHeight)
            }
        )
    }

    private fun configButtonMarginBottom(button: Button, margin: Int) {
        val params = button.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin += margin
        button.layoutParams = params
    }

    private fun configToolbar() {
        if (isEditBilling == true) {
            supportActionBar?.title = getString(R.string.edit_bill)
            return
        }
        supportActionBar?.title = getString(R.string.add_a_bill)
    }

    private fun configSubmitButton() {

        fun taskSubmit() {
            // 处理选取的图片
            for (uri in selectedPhotosList) {
                val fileName = FileUtils(this).storePhotosFromUri(uri)
                if (fileName != null) {
                    billingObject.images = billingObject.images?.plus(",$fileName") ?: fileName
                    selectedPhotoNameList.add(fileName)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.store_photos_failed),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
            // 创建账单
            lifecycleScope.launch {
                // 添加记录（切换到 IO 线程）
                val billingCreator = withContext(Dispatchers.IO) {
                    val activityContext = this@CreateBillingActivity
                    if (isEditBilling == true && isEditBillingId != null) {
                        BillingCreator.modifyBilling(isEditBillingId!!, billingObject, activityContext)
                    } else {
                        BillingCreator.createBilling(billingObject, depositBillingDate, activityContext)
                    }
                }

                val (status, code) = billingCreator
                if (status == BillingCreator.CREATE_BILLING_SUCCESS) {
                    val resultIntent = Intent()
                    resultIntent.putExtra(createBillingReturnedKey, code)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    for (fileName in selectedPhotoNameList) {
                        FileUtils(this@CreateBillingActivity).deletePhotos(fileName)
                    }
                    val failedMessage = when (status) {
                        BillingCreator.CREATE_BILLING_CHECK_FAILED -> {
                            BillingCreator.getCreateBillingFailedReason(status, resources) +
                                    BillingCreator.getCreateBillingFailedReason(code.toInt(), resources)
                        }

                        BillingCreator.CREATE_BILLING_INSERT_FAILED,
                        BillingCreator.CREATE_BILLING_DEPOSIT_INSERT_FAILED -> {
                            BillingCreator.getCreateBillingFailedReason(status, resources) + code
                        }

                        else -> ""
                    }

                    MaterialAlertDialogBuilder(this@CreateBillingActivity)
                        .setTitle(resources.getString(R.string.create_billing_failed))
                        .setMessage(failedMessage)
                        .setPositiveButton(resources.getString(R.string.billing_alert_okay)) { _, _ ->
                            // Respond to positive button press
                        }
                        .show()
                }
            }

        }

        val submitButton = findViewById<Button>(R.id.finish_billing_btn)
        submitButton.setOnClickListener {
            taskSubmit()
        }
    }

    private fun configAmountEditText() {
        val editText = findViewById<TextInputEditText>(R.id.et_input_text)
        editText.setText(if (isEditBilling == true) WalletCreator.convertAmountFormat(billingObject.amount.toString(),
            false) else "")

        editText.requestFocus()

        fun taskAmount(amount: String) {
            billingObject.amount = WalletCreator.convertNumberToAmount(amount) ?: 0L
        }

        editText.doAfterTextChanged { editable ->
            val amount = editable?.toString().orEmpty()
            if (amount.isNotBlank()) {
                taskAmount(amount)
            } else {
                billingObject.amount = 0
            }
        }
    }

    private fun configIncomeCheckBox() {
        val income = findViewById<CheckBox>(R.id.cb_income)
        if (isEditBilling == true) {
            income.isChecked = (billingObject.iotype == 1)
        }

        income.setOnCheckedChangeListener { _, isChecked ->
            billingObject.iotype = if (isChecked) 1 else 0
        }
    }

    private fun configDepositCheckBox() {
        val depositCheckBox = findViewById<CheckBox>(R.id.cb_deposit)

        if (isEditBilling == true) {
            depositCheckBox.isEnabled = false
        }

        fun setDepositDate() {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .setSelection(billingObject.time)
                .build()
            datePicker.isCancelable = false
            datePicker.show(supportFragmentManager, "DATE_PICKER")
            datePicker.addOnPositiveButtonClickListener { timestamp ->
                depositBillingDate = TimeUtils.convertDateToDayLevelTimestamp(
                    TimeUtils.getDateFromTimestamp(timestamp)
                )

                val dateTime: LocalDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(depositBillingDate),
                    ZoneId.systemDefault()
                )

                Toast.makeText(
                    this,
                    resources.getString(
                        R.string.deposit_date,
                        dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            datePicker.addOnNegativeButtonClickListener {
                depositCheckBox.isChecked = false
            }
        }

        fun showHintDialog() {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.deposit_selected_hint_title))
                .setMessage(resources.getString(R.string.deposit_selected_hint_message))
                .setCancelable(false)
                .setNeutralButton(resources.getString(R.string.deposit_never_show)) { _, _ ->
                    HintConfigCreator.setDepositBillHint(this, false)
                    setDepositDate()
                }
                .setPositiveButton(resources.getString(R.string.deposit_ok)) { _, _ ->
                    setDepositDate()
                }
                .show()
        }

        depositCheckBox.setOnCheckedChangeListener { _, isChecked ->
            billingObject.deposit = if (isChecked) "true" else "false"
            if (isChecked && HintConfigCreator.getDepositBillHint(this)) {
                showHintDialog()
            } else if (isChecked) {
                setDepositDate()
            } else {
                depositBillingDate = TimeUtils.getDayLevelTimestamp()
            }
        }
    }

    private fun configNoteEditText() {
        val editText = findViewById<TextInputEditText>(R.id.notes_input_text)
        editText.setText(if (isEditBilling == true) billingObject.notes else "")
        editText.doAfterTextChanged { editable ->
            billingObject.notes = editable.toString().takeIf { it.isNotEmpty() }
        }
    }

    private val chooseImagesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val imageUris = mutableListOf<Uri>()

                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        imageUris.add(uri)
                    }
                } ?: data?.data?.let { uri ->
                    imageUris.add(uri)
                }

                if (imageUris.isNotEmpty()) {
                    processImageUris(imageUris)
                }
            }
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoUri?.let { uri ->
                    selectedPhotosList.add(uri)
                    selectedPhotosNumber++
                    changeSelectedPhotosBtnText(selectedPhotosNumber)
                }
            }
        }

    private fun loadClassifyPicker() {
        val rootView = findViewById<LinearLayout>(R.id.classify_layout)
        val classifyView = ClassifyPickerWidget.getWidget(
            rootView,
            layoutInflater,
            resources,
            { classify ->
                billingObject.classify = classify
            },
            { ids, container ->
                if (isEditBilling == true) {
                    val classifyId = billingObject.classify
                    val index = ids.indexOf(classifyId)
                    container.check(container.getChildAt(index).id)
                }
            }
        )
        rootView.addView(classifyView)
    }

    private fun configPhotoSelector() {

        fun openImageChooser() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            chooseImagesLauncher.launch(intent)
        }

        fun openTakePhoto() {
            if (!PremissionUtils.requestCameraPermission(this)) return

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                resolveActivity(packageManager) ?: return
            }

            createImageFile().let { photoFile ->
                photoUri = FileProvider.getUriForFile(
                    this,
                    "m20.simple.bookkeeping.fileprovider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                takePhotoLauncher.launch(photoUri)
            }
        }

        fun clearSelectedImages() {
            selectedPhotosNumber = 0
            selectedPhotosList.clear()
            billingObject.images = null
            changeSelectedPhotosBtnText(0)
        }

        fun handlePhotoSelection() {
            if (selectedPhotosNumber == 0) openImageChooser() else clearSelectedImages()
        }

        fun handleTakePhoto() {
            if (selectedPhotosNumber == 0) openTakePhoto() else Toast.makeText(
                this,
                getString(R.string.clear_photo_tips),
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<LinearLayout>(R.id.add_photos_btn_linear).apply {
            setOnClickListener {
                handleTakePhoto()
            }
            setOnLongClickListener {
                handlePhotoSelection()
                true
            }
        }

        if (isEditBilling == true && billingObject.images != null) {
            val storageImages = billingObject.images!!.split(",")
            selectedPhotosNumber = storageImages.size
            changeSelectedPhotosBtnText(selectedPhotosNumber)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun processImageUris(uris: List<Uri>) {
        selectedPhotosList = uris.toMutableList()
        selectedPhotosNumber = uris.size
        changeSelectedPhotosBtnText(uris.size)
    }

    private fun changeSelectedPhotosBtnText(size: Int) {
        val icon = findViewById<ImageView>(R.id.select_photos_icon)
        val text = findViewById<TextView>(R.id.select_photos_text)

        val (iconRes, textRes) = if (size == 0) {
            R.drawable.photo_camera to R.string.add_billing_photos
        } else {
            R.drawable.done_all to R.string.selected_photos_number
        }

        icon.setImageResource(iconRes)
        text.text = getString(textRes, size.takeIf { it > 0 })
    }

    private fun loadWalletSelector() {
        val rootView = findViewById<LinearLayout>(R.id.wallet_layout)
        val walletView = WalletSelectionWidget.getWidget(
            rootView,
            layoutInflater,
            resources,
            { defaultWalletID, walletInputText ->
                if (!isEditBilling!!) {
                    billingObject.wallet = defaultWalletID
                    walletInputText.hint = getString(R.string.choose_wallet)
                } else {
                    walletInputText.hint = getString(R.string.wallet_selector_default)
                }
            },
            { walletIdPosition ->
                val selectedItem: Int = walletIdPosition
                billingObject.wallet = selectedItem
            },
            this,
            walletExecutorService
        )
        rootView.addView(walletView)
    }

    private fun configDatePicker() {
        val dateLabel = findViewById<TextView>(R.id.date_label)
        val timeLabel = findViewById<TextView>(R.id.time_label)

        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val stf = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun formatDate(date: Date) = sdf.format(date)
        fun formatTime(date: Date) = stf.format(date)

        fun updateBillingTime(date: Date) {
            billingObject.time = TimeUtils.convertDateToMinuteLevelTimestamp(date)
        }

        fun updateDateLabel(date: Date) {
            dateLabel.text = formatDate(date)
        }

        fun updateTimeLabel(date: Date) {
            timeLabel.text = formatTime(date)
        }

        fun getDateFromTimestamp(timestamp: Long) = TimeUtils.getDateFromTimestamp(timestamp)
        fun getDate() = getDateFromTimestamp(billingObject.time)

        val dateObject = getDateFromTimestamp(billingObject.time)
        updateDateLabel(dateObject)
        updateTimeLabel(dateObject)

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date))
            .setSelection(
                TimeUtils.convertTimestampToLocalDate(billingObject.time)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            )
            .build()

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(dateObject.hours)
            .setMinute(dateObject.minutes)
            .setTitleText(getString(R.string.select_time))
            .build()

        dateLabel.setOnClickListener {
            datePicker.show(supportFragmentManager, "DATE_PICKER")
            datePicker.addOnPositiveButtonClickListener { timestamp ->
                val selectedDate = getDateFromTimestamp(timestamp).apply {
                    hours = getDate().hours
                    minutes = getDate().minutes
                }
                updateBillingTime(selectedDate)
                updateDateLabel(selectedDate)
            }
        }

        timeLabel.setOnClickListener {
            timePicker.show(supportFragmentManager, "TIME_PICKER")
            timePicker.addOnPositiveButtonClickListener {
                val selectedDate = getDate().apply {
                    hours = timePicker.hour
                    minutes = timePicker.minute
                }
                updateBillingTime(selectedDate)
                updateTimeLabel(selectedDate)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        walletExecutorService.shutdown()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { // 判断是否是返回键
            // 在这里添加你的返回逻辑
            onBackPressed() // 默认行为是关闭当前 Activity
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}