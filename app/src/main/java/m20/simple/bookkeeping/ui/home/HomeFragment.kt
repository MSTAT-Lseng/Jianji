package m20.simple.bookkeeping.ui.home

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekDayBinder
import kotlinx.coroutines.CoroutineScope
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
import m20.simple.bookkeeping.utils.TextUtils
import m20.simple.bookkeeping.utils.TimeUtils
import m20.simple.bookkeeping.utils.UIUtils
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
    private var loadBillingCoroutineScope : Job? = null

    private var selectedDateView : TextView? = null

    val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        UIUtils().fillStatusBarHeight(requireContext(), binding.statusbarHeight)

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
        }
        configCalendarViewCreated(view)
        loadBillingItems()
        configCustomLinearSwipe()
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

    private fun loadBillingItems() {

        fun addRecord(record: BillingDao.Record, allWallets: List<Pair<Int, String>>) {
            val billingItemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.billing_item, binding.billingItemContainer, false)

            billingItemView.apply {
                // classify
                val classifyImageView = findViewById<ImageView>(R.id.classify_image)
                val categoryPairs = UIUtils().getCategoryPairs(resources, requireActivity())
                val category = categoryPairs.find { it.second == record.classify }
                classifyImageView.setImageResource(category?.first ?: R.drawable.account_balance_wallet_thin)

                // Amount
                val amountTextView = findViewById<TextView>(R.id.amount_text)
                val amount = record.amount.takeIf { it != 0 }
                    ?.let { WalletCreator.convertAmountFormat(it.toString(),
                        true, record.iotype) }
                    ?: "0.00"
                val amountColor = resources.getColor(
                    if (record.iotype == 0) R.color.iotype_expenditure
                    else R.color.iotype_income
                )
                amountTextView.text = amount
                amountTextView.setTextColor(amountColor)

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
                walletTextView.text = walletName ?: requireActivity().getString(R.string.unknown_wallet)

                // Time
                val localTime = TimeUtils.getDateFromTimestamp(record.time).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                findViewById<TextView>(R.id.time_text).text =
                    localTime.format(DateTimeFormatter.ofPattern("HH:mm"))

                setOnClickListener {
                    startActivity(Intent(requireActivity(), BillingInfoActivity::class.java)
                        .putExtra("billingId", record.id))
                }

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
            if (UIUtils().isDarkMode(requireActivity().resources)) {
                emptyView.findViewById<ImageView>(R.id.empty_image).setImageDrawable(
                    resources.getDrawable(R.drawable.calendar_no_billing_dark, requireActivity().theme)
                )
            }
            binding.billingItemContainer.addView(emptyView)
        }
        addProgressIndicator()

        loadBillingCoroutineScope = CoroutineScope(Dispatchers.Main).launch {
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
                    configToolbarSubtitle("${week.days.first().date.year}/${String.format("%02d", week.days.first().date.monthValue)}")
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
                if (!UIUtils().isDarkMode(requireActivity().resources))
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
        titlesContainer.setOnClickListener {  }
        titlesContainer?.children?.forEachIndexed { index, child ->
            (child as TextView).text = daysOfWeek()[index]
                .getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }
    }

    private fun configToolbarSubtitle(subtitle: String) {
        if (!isToolbarLoaded) return
        viewModel.getToolbar()?.subtitle = subtitle
    }

    private fun configFloatingActionButton() {
        val launcher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
            configCalendar()
            loadBillingItems()
        }

        binding.fab.setOnClickListener {
            val intent = Intent(requireActivity(), CreateBillingActivity::class.java)
            launcher.launch(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        loadBillingCoroutineScope?.cancel()
        loadBillingCoroutineScope = null
    }
}

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.calendarDayText)
    lateinit var day: WeekDay
}