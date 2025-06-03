package m20.simple.bookkeeping.utils

import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date


object TimeUtils {

    fun getTimestamp(): Long {
        return System.currentTimeMillis()
    }

    // 分钟级时间戳，秒与毫秒归零。
    fun getMinuteLevelTimestamp(): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // 日级时间戳，小时，分钟，秒与毫秒归零。
    fun getDayLevelTimestamp(): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun convertDateToMinuteLevelTimestamp(date: Date): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun convertDateToDayLevelTimestamp(date: Date): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getDateFromTimestamp(timestamp: Long): Date {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.time
    }

    // 获取传入 timestamp 的下一天，小时，分钟，秒与毫秒归零。
    fun getNextDayTimestamp(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // 获取传入 timestamp 的前一天，小时，分钟，秒与毫秒归零。
    fun getPreviousDayTimestamp(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 将 LocalDate 转换为 Date
     *
     * @param localDate 要转换的 LocalDate 对象
     * @return 对应的 Date 对象，如果 localDate 为 null，则返回 null
     */
    fun convertLocalDateToDate(localDate: LocalDate): Date {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    /**
     * 将 Timestamp 转换为 LocalDate
     *
     * @param timestamp 要转换的 Timestamp 时间戳
     * @return 对应的 LocalDate 对象，如果 Timestamp 为 null，则返回 null
     */
    fun convertTimestampToLocalDate(timestamp: Long): LocalDate {
        return Date(timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    /**
     * 计算两个 Timestamp 之间包含的目标星期几的时间戳列表。
     *
     * @param startTimestamp 起始时间戳（毫秒）
     * @param endTimestamp 结束时间戳（毫秒）
     * @param targetDayOfWeek 目标星期几 (0 = 周日, 1 = 周一, ..., 6 = 周六)
     * @return 匹配的天数的 Timestamp 列表
     */
    fun getTargetWeekdayTimestampsBetween(
        startTimestamp: Long,
        endTimestamp: Long,
        targetDayOfWeek: Int
    ): List<Long> {
        val result = mutableListOf<Long>()

        // 确保起始时间小于结束时间
        if (startTimestamp >= endTimestamp) return emptyList()

        var currentTimestamp = startTimestamp

        while (currentTimestamp <= endTimestamp) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentTimestamp
            }

            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // 注意：Calendar 的星期天是 1，星期一是 2，... 星期六是 7
            // 所以我们做转换：(dayOfWeek - 1) % 7 -> 0=Sunday, 1=Monday, ..., 6=Saturday
            val adjustedDayOfWeek = (dayOfWeek - 1) % 7

            if (adjustedDayOfWeek == targetDayOfWeek) {
                result.add(currentTimestamp)
            }

            // 移动到下一天的开始时间
            currentTimestamp = getNextDayTimestamp(currentTimestamp)
        }

        return result
    }

    /**
     * 计算两个 Timestamp 之间包含的目标日（1-31）的时间戳列表。
     *
     * @param startTimestamp 起始时间戳（毫秒）
     * @param endTimestamp 结束时间戳（毫秒）
     * @param targetDayOfMonth 目标日期 (1-31)
     * @return 匹配的天数的 Timestamp 列表
     */
    fun getTargetDayOfMonthTimestampsBetween(
        startTimestamp: Long,
        endTimestamp: Long,
        targetDayOfMonth: Int
    ): List<Long> {
        val result = mutableListOf<Long>()

        // 确保起始时间小于结束时间
        if (startTimestamp >= endTimestamp) return emptyList()

        var currentTimestamp = startTimestamp

        while (currentTimestamp <= endTimestamp) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentTimestamp
            }

            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            if (dayOfMonth == targetDayOfMonth) {
                result.add(currentTimestamp)
            }

            // 移动到下一天的开始时间
            currentTimestamp = getNextDayTimestamp(currentTimestamp)
        }

        return result
    }


}