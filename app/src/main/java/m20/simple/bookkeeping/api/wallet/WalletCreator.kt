package m20.simple.bookkeeping.api.wallet

import android.content.Context
import android.content.res.Resources
import m20.simple.bookkeeping.api.billing.BillingCreator
import m20.simple.bookkeeping.config.PrefsConfig
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.database.wallet.WalletDao
import java.math.RoundingMode

object WalletCreator {

    // 返回所有钱包信息 List<Pair<ID，名称>>
    fun getAllWallets(context: Context): List<Pair<Int, String>> {
        val walletDao = WalletDao(context)
        return walletDao.getAllWallets()
            .map { Pair(it.id, it.name) }
            .sortedBy { it.first }
    }

    // 获得默认钱包信息，返回 (钱包ID，钱包名称)
    fun getDefaultWallet(context: Context, resources: Resources): Pair<Int, String> {
        val defaultWalletID = PrefsConfig.getIntValue(
            context,
            PrefsConfig.KEY_DEFAULT_WALLET_ID,
            PrefsConfig.DEFAULT_WALLET_ID
        )

        val walletDao = WalletDao(context)
        return walletDao.getAllWallets()
            .find { it.id == defaultWalletID }
            ?.let { Pair(defaultWalletID, it.name) }
            ?:run {
                val defaultWalletName = resources.getString(
                    m20.simple.bookkeeping.R.string.db_default_wallet
                )
                val newDefaultID = createWallet(context, defaultWalletName)
                setDefaultWallet(context, newDefaultID)
                Pair(newDefaultID, defaultWalletName)
            }
    }

    // 设置默认钱包，传入ID
    fun setDefaultWallet(context: Context, walletID: Int): Boolean {
        val walletDao = WalletDao(context)
        val walletInfo = walletDao.getWalletNameAndBalance(walletID)

        // 判断钱包名称是否非空
        val success = walletInfo.first.isNotEmpty()

        // 如果成功，则设置默认钱包ID
        if (success) {
            PrefsConfig.setIntValue(
                context,
                PrefsConfig.KEY_DEFAULT_WALLET_ID,
                walletID
            )
        }

        return success
    }

    // 更改钱包名称，传入ID
    fun renameWallet(context: Context, walletID: Int, newName: String): Boolean {
        val walletDao = WalletDao(context)

        // 检查
        if (newName.isEmpty()) return false
        if (walletDao.isWalletNameExists(newName)) return false

        return walletDao.updateWalletName(walletID, newName) > 0
    }

    // 修改钱包余额，传入：钱包ID、修改数值，返回修改结果，建议使用异步执行函数
    fun modifyWalletAmount(context: Context, walletID: Int, amount: Long): Boolean {
        val walletDao = WalletDao(context)
        val (_, balance) = walletDao.getWalletNameAndBalance(walletID)
        val newAmount = (balance) + amount
        return walletDao.updateWalletBalance(walletID, newAmount) > 0
    }

    // 更改钱包余额为全新数值，传入：钱包ID、修改数值，返回修改结果，建议使用异步执行函数
    fun setWalletAmount(context: Context, walletID: Int, amount: Long): Boolean {
        val walletDao = WalletDao(context)
        return walletDao.updateWalletBalance(walletID, amount) > 0
    }

    // 根据ID查看钱包名称和余额
    fun getWalletNameAndBalance(context: Context, walletId: Int): Pair<String, Long>? {
        val walletDao = WalletDao(context)
        return walletDao.getWalletNameAndBalance(walletId).let {
            if (it.first.isEmpty() && it.second == 0L) return null
            Pair(it.first, it.second)
        }
    }

    // 创建钱包，传入钱包名称，返回ID
    fun createWallet(context: Context, walletName: String): Int {
        val walletDao = WalletDao(context)
        return walletDao.addWallet(walletName, 0)
    }

    // 删除钱包，传入ID
    fun deleteWallet(context: Context, walletID: Int, transferBalance: Boolean = true): Boolean {
        val walletDao = WalletDao(context)
        val defaultWalletId = getDefaultWallet(context, context.resources).first

        // 转移账单记录
        BillingCreator.transferRecordWallet(walletID, defaultWalletId, context)

        if (transferBalance) {
            val (_, balance) = walletDao.getWalletNameAndBalance(walletID)
            val (_, defaultBalance) = walletDao.getWalletNameAndBalance(defaultWalletId)
            walletDao.updateWalletBalance(defaultWalletId, defaultBalance + balance)
        }

        return walletDao.deleteWallet(walletID) > 0
    }

    // 转换钱包余额至合理格式
    fun convertAmountFormat(
        originAmount: String,
        needSymbol: Boolean = false,
        ioType: Int = 0
    ): String {
        fun formatWithSymbol(amount: String): String {
            return when {
                !needSymbol -> amount
                ioType == 0 -> "-$amount"
                else -> "+$amount"
            }
        }

        if (originAmount.isEmpty()) return formatWithSymbol("0.00")
        if (originAmount.length == 1) return formatWithSymbol("0.0$originAmount")

        val length = originAmount.length
        val beforeDecimal = originAmount.substring(0, length - 2)
        val afterDecimal = originAmount.substring(length - 2)

        return formatWithSymbol("$beforeDecimal.$afterDecimal")
    }

    /**
     * 将原始的数值转换为符合余额格式的数值
     *
     * @param value 用户传入的字符串类型原始数值。
     * @return 处理后的 Long 值，如果输入无效则可能抛出 NumberFormatException。
     */
    fun convertNumberToAmount(value: String): Long? {
        val original = value.toBigDecimalOrNull() ?: return null
        val rounded = original.setScale(2, RoundingMode.HALF_UP)
        return rounded.toPlainString().replace(".", "").toLong()
    }

}