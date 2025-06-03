package m20.simple.bookkeeping.ui.home

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekDayBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.activities.BillingInfoActivity
import m20.simple.bookkeeping.activities.CreateBillingActivity
import m20.simple.bookkeeping.api.billing.BillingCreator
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.databinding.FragmentHomeBinding
import m20.simple.bookkeeping.utils.TalkBackUtils
import m20.simple.bookkeeping.utils.TextUtils
import m20.simple.bookkeeping.utils.TimeUtils
import m20.simple.bookkeeping.utils.UIUtils
import m20.simple.bookkeeping.widget.BillingItemWidget
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var isToolbarLoaded = false
    private var isFirstSubtitleLock = true
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var loadBillingDay = 0L
    private var selectedBillingList = mutableListOf<Long>()

    private var selectedDateView: TextView? = null

    private val modifiedBillingListenLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
            configCalendar()
            loadBillingItems()
        }

    val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        UIUtils.fillStatusBarHeight(requireContext(), binding.statusbarHeight)

        loadBillingDay = TimeUtils.getDayLevelTimestamp()
        configCalendar()
        configFloatingActionButton()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.toolbarMessage.observe(viewLifecycleOwner) { message ->
            isToolbarLoaded = message
            configToolbarSubtitle(YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy/MM")))

            val schedPlanWorker = viewModel.getSchedPlanWorker()
            if (schedPlanWorker != null) {
                if (schedPlanWorker.isFinishedUpdated()) baseConfig(view) else
                    schedPlanWorker.start(fun() { baseConfig(view) })
            } else baseConfig(view)
        }
    }

    private fun baseConfig(view: View) {
        configCalendarViewCreated(view)
        loadBillingItems()
        configCustomLinearSwipe()
        initNavBottomHeight(view)
    }

    private fun initNavBottomHeight(view: View) {
        val customLinear = view.findViewById<CustomLinearLayout>(R.id.billing_item_container)
        UIUtils.getNavigationBarHeight(
            customLinear, requireActivity(),
            fun(height) {
                plusCustomLinearPaddingBottom(customLinear, height)
                plusFabMarginBottom(height)
            }
        )
    }

    private fun plusCustomLinearPaddingBottom(customLinear: CustomLinearLayout, padding: Int) {
        customLinear.setPadding(
            customLinear.paddingLeft,
            customLinear.paddingTop,
            customLinear.paddingRight,
            customLinear.paddingBottom + padding
        )
    }

    private fun plusFabMarginBottom(margin: Int) {
        val fab = binding.fab
        val params = fab.layoutParams as ConstraintLayout.LayoutParams
        params.bottomMargin += margin
        fab.layoutParams = params
    }

    private fun configCustomLinearSwipe() {
        fun createSwipeListener(timeModifier: (Long) -> Long): () -> Unit = {
            val modifiedTime = timeModifier(loadBillingDay)
            val localDate = TimeUtils.convertTimestampToLocalDate(modifiedTime)

            when {
                localDate.isAfter(LocalDate.now()) -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.cannot_swipe_to_future,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                modifiedTime < 0L -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.cannot_swipe_to_ancient,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    loadBillingDay = modifiedTime
                    loadBillingItems()
                    configCalendar()
                }
            }
        }

        val leftListener = createSwipeListener { TimeUtils.getNextDayTimestamp(it) }
        val rightListener = createSwipeListener { TimeUtils.getPreviousDayTimestamp(it) }

        listOf(binding.contentLayout, binding.billingItemContainer).forEach { view ->
            view.setOnLeftSwipeListener(leftListener)
            view.setOnRightSwipeListener(rightListener)
        }
    }

    private fun deleteSelectedBillings() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_billings))
            .setView(R.layout.dialog_loading)
            .setCancelable(false) // 不可取消
            .show()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                selectedBillingList.forEach { id ->
                    BillingCreator.deleteBillingById(id, requireActivity())
                }
            }
            dialog.dismiss()
            loadBillingItems()
            Toast.makeText(
                requireContext(),
                getString(R.string.delete_success),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun confirmDeleteBillingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_billings))
            .setMessage(getString(R.string.delete_billings_message))
            .setNegativeButton(getString(android.R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .setPositiveButton(getString(android.R.string.ok)) { d, _ ->
                deleteSelectedBillings()
            }
            .show()
    }

    private fun loadBillingItems() {
        selectedBillingList.clear() // 清空选中的账单列表

        configToolbarTitle(getString(R.string.menu_home))
        configFloatingActionIcon()

        fun addRecord(
            record: BillingDao.Record,
            allWallets: List<Pair<Int, String>>,
            placeholder: Boolean = false
        ) {
            val billingItemWidget = BillingItemWidget
            val billingItemView = billingItemWidget.getWidget(
                requireActivity(),
                binding.billingItemContainer,
                record,
                resources,
                allWallets
            )

            fun configLongClick(view: View, id: Long): Boolean {
                val container = view.findViewById<LinearLayout>(R.id.item_container)
                if (selectedBillingList.contains(id)) {
                    selectedBillingList.remove(id)
                    container.background = billingItemWidget.getDefaultBackground(
                        requireContext()
                    )
                } else {
                    selectedBillingList.add(id)
                    container.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.billing_item_long_click)
                    )
                }
                val toolbarTitle =
                    if (selectedBillingList.isEmpty()) getString(R.string.menu_home)
                    else getString(R.string.selected_items, selectedBillingList.size)
                configToolbarTitle(toolbarTitle)
                configFloatingActionIcon()
                // Accessibility
                TalkBackUtils.announceText(requireContext(), toolbarTitle)
                return true
            }

            billingItemView.setOnClickListener {
                if (selectedBillingList.isEmpty()) {
                    val intent = Intent(requireActivity(), BillingInfoActivity::class.java)
                    intent.putExtra("billingId", record.id)
                    modifiedBillingListenLauncher.launch(intent)
                    return@setOnClickListener
                }
                configLongClick(it, record.id)
            }

            billingItemView.setOnLongClickListener {
                if (selectedBillingList.isNotEmpty()) return@setOnLongClickListener true
                configLongClick(it, record.id)
            }

            if (placeholder) {
                billingItemView.visibility = View.INVISIBLE
            }
            binding.billingItemContainer.addView(billingItemView)
        }

        fun createCircularProgressIndicator(): CircularProgressIndicator {
            return CircularProgressIndicator(requireActivity()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                isIndeterminate = true
                setPadding(16.dp, 32.dp, 16.dp, 32.dp)
            }
        }

        val billingItemContainer = binding.billingItemContainer
        fun addProgressIndicator() {
            billingItemContainer.removeAllViews()
            val progressIndicator = createCircularProgressIndicator()
            billingItemContainer.gravity = Gravity.CENTER
            billingItemContainer.addView(progressIndicator)
        }

        fun resetBillingItemContainer() {
            billingItemContainer.removeAllViews()
            billingItemContainer.gravity = Gravity.NO_GRAVITY
        }

        // 创建过渡动画（淡入淡出）
        fun createFadeTransition() {
            val fadeTransition = Fade()  // 默认模式是 Fade.IN_OUT（先淡出旧视图，再淡入新视图）
            // 开始延迟过渡
            TransitionManager.beginDelayedTransition(billingItemContainer, fadeTransition)
        }

        fun addBillingEmptyView() {
            val emptyView = LayoutInflater.from(requireContext())
                .inflate(R.layout.billing_empty_view, binding.billingItemContainer, false)
            if (UIUtils.isDarkMode(requireActivity().resources)) {
                emptyView.findViewById<ImageView>(R.id.empty_image).setImageDrawable(
                    resources.getDrawable(
                        R.drawable.calendar_no_billing_dark,
                        requireActivity().theme
                    )
                )
            }
            binding.billingItemContainer.addView(emptyView)
        }
        addProgressIndicator()

        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                BillingCreator.getRecordsByDay(loadBillingDay, requireActivity())
            }
            val allWallets = withContext(Dispatchers.IO) {
                WalletCreator.getAllWallets(requireActivity())
            }
            resetBillingItemContainer()
            createFadeTransition()
            records.forEach { record ->
                addRecord(record, allWallets)
            }
            if (records.isEmpty()) {
                addBillingEmptyView()
            } else {
                addRecord(BillingCreator.getNullRecord(), allWallets, true)
            }
        }
    }

    private fun configCalendar() {
        binding.weekCalendarView.apply {
            dayBinder = object : WeekDayBinder<DayViewContainer> {
                override fun create(view: View): DayViewContainer =
                    DayViewContainer(view).apply {
                        textView.setOnClickListener {
                            val dayDate = day.date
                            if (dayDate.isAfter(LocalDate.now())) return@setOnClickListener

                            configSelectedDateView(textView)
                            loadBillingDay = TimeUtils.convertDateToDayLevelTimestamp(
                                TimeUtils.convertLocalDateToDate(dayDate)
                            )
                            loadBillingItems()
                        }
                    }

                override fun bind(container: DayViewContainer, data: WeekDay) {
                    bindWeekDayBinder(container, data)
                }
            }
            weekScrollListener = { week ->
                if (isFirstSubtitleLock) {
                    isFirstSubtitleLock = false
                } else {
                    configToolbarSubtitle(
                        "${week.days.first().date.year}/${
                            String.format(
                                "%02d",
                                week.days.first().date.monthValue
                            )
                        }"
                    )
                }
            }
            setup(
                startDate = getStartDate(),
                endDate = LocalDate.now(),
                firstDayOfWeek = daysOfWeek().first()
            )

            scrollToWeek(TimeUtils.convertTimestampToLocalDate(loadBillingDay))
        }
    }

    private fun configSelectedDateView(textView: TextView) {
        selectedDateView?.apply {
            setTextColor(resources.getColor(R.color.md_theme_onSecondaryContainer))
            setBackgroundResource(R.drawable.calendar_day_ripple)
        }
        textView.apply {
            setTextColor(
                if (!UIUtils.isDarkMode(requireActivity().resources))
                    resources.getColor(android.R.color.white)
                else
                    resources.getColor(R.color.md_theme_onSecondaryContainer)
            )
            setBackgroundResource(
                R.drawable.calendar_current_day
            )
        }
        selectedDateView = textView
    }

    private fun bindWeekDayBinder(container: DayViewContainer, data: WeekDay) {
        container.day = data
        container.textView.apply {
            text = data.date.dayOfMonth.toString()
            val localDate = LocalDate.now()
            val isAfterDate = data.date.isAfter(localDate)
            setTextColor(
                if (isAfterDate)
                    resources.getColor(android.R.color.darker_gray)
                else
                    resources.getColor(R.color.md_theme_onSecondaryContainer)
            )
            setBackgroundResource(R.drawable.calendar_day_ripple)
            isClickable = !isAfterDate
        }
        if (data.date == TimeUtils.convertTimestampToLocalDate(loadBillingDay)) {
            configSelectedDateView(container.textView)
        }
    }

    private fun getStartDate(): LocalDate {
        return TimeUtils.convertTimestampToLocalDate(0L)
    }

    private fun configCalendarViewCreated(view: View) {
        val titlesContainer = view.findViewById<ViewGroup>(R.id.titlesContainer)
        titlesContainer.setOnClickListener {
            showDatePicker()
        }
        titlesContainer.contentDescription = getString(R.string.manual_date_selection)
        titlesContainer?.children?.forEachIndexed { index, child ->
            (child as TextView).text = daysOfWeek()[index]
                .getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }
    }

    private fun configToolbarSubtitle(subtitle: String) {
        if (!isToolbarLoaded) return
        viewModel.getToolbar()?.subtitle = subtitle
    }

    private fun configToolbarTitle(title: String) {
        if (!isToolbarLoaded) return
        viewModel.getToolbar()?.title = title
    }

    private fun configFloatingActionIcon() {
        if (selectedBillingList.isNotEmpty()) {
            binding.fab.setImageResource(R.drawable.delete)
            binding.fab.contentDescription = getString(R.string.delete)
        } else {
            binding.fab.setImageResource(R.drawable.add)
            binding.fab.contentDescription = getString(R.string.add_a_bill)
        }
    }

    private fun configFloatingActionButton() {
        val launcher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
            configCalendar()
            loadBillingItems()
        }

        binding.fab.setOnClickListener {
            if (selectedBillingList.isNotEmpty()) {
                confirmDeleteBillingsDialog()
                return@setOnClickListener
            }
            val intent = Intent(requireActivity(), CreateBillingActivity::class.java)
            launcher.launch(intent)
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date_dialog))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // 默认选中今天
            .build()
        datePicker.show(requireActivity().supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = datePicker.selection
            selectedDate?.let {
                // 调整为用户选择的日期
                loadBillingDay = TimeUtils.convertDateToDayLevelTimestamp(
                    TimeUtils.getDateFromTimestamp(it)
                )
                configCalendar()
                loadBillingItems()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.getSchedPlanWorker()?.stop()
    }
}

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarDayText)
    lateinit var day: WeekDay
}