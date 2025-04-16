package m20.simple.bookkeeping.utils

object MathUtils {

    // 将字符串转换为整数
    fun stringToInt(str: String): Int = str.toIntOrNull() ?: 0

    // 数字是否是负数
    fun isNegative(num: Int) = num < 0

}