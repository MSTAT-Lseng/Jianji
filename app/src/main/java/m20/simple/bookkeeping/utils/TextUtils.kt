package m20.simple.bookkeeping.utils

object TextUtils {

    // 截取字符串到指定位数，如果有超出则增加 ...
    fun cutStringToLength(str: String, length: Int) =
        str.take(length) + if (str.length > length) "..." else ""

}