package m20.simple.bookkeeping.api.wallet

import android.content.Context
import m20.simple.bookkeeping.config.PrefsConfig
import m20.simple.bookkeeping.database.wallet.WalletDao

object WalletCreator {

    // 返回所有钱包信息 List<Pair<ID，名称>>
    fun getAllWallets(context: Context): List<Pair<Int, String>> {
        val walletDao = WalletDao(context)
        return walletDao.getAllWallets()
            .map { Pair(it.id.toInt(), it.name) }
            .sortedBy { it.first }
    }

    // 获得默认钱包信息，返回 (钱包ID，钱包名称)
    fun getDefaultWallet(context: Context): Pair<Int, String>? {
        val defaultWalletID = PrefsConfig.getIntValue(
            context,
            PrefsConfig.KEY_DEFAULT_WALLET_ID,
            PrefsConfig.DEFAULT_WALLET_ID
        )

        if (defaultWalletID < 1) return null

        val walletDao = WalletDao(context)
        return walletDao.getAllWallets()
            .find { it.id.toInt() == defaultWalletID }
            ?.let { Pair(defaultWalletID, it.name) }
    }

    // 修改钱包余额，传入：钱包ID、修改数值，返回余额，建议使用异步执行函数
    fun modifyWalletAmount(context: Context, walletID: Int, amount: Int): Int? {
        return WalletDao(context).run {
            getWalletNameAndBalance(walletID.toLong())?.let { (_, balance) ->
                val newAmount = (balance ?: 0) + amount
                updateWalletBalance(walletID.toLong(), newAmount)
            }
        }
    }

    // 根据ID查看钱包名称和余额
    fun getWalletNameAndBalance(context: Context, walletId: Long): Pair<String?, Int?>? {
        val walletDao = WalletDao(context)
        return walletDao.getWalletNameAndBalance(walletId)?.let {
            Pair(it.first, it.second)
        }
    }

    // 转换钱包余额至合理格式
    fun convertAmountFormat(originAmount: String,
                            needSymbol: Boolean = false,
                            ioType: Int = 0) : String? {
        if (originAmount.length < 2) return null // 处理长度不足的情况
        val beforeDecimal = originAmount.dropLast(2) // 获取小数点前的部分
        val afterDecimal = originAmount.takeLast(2)   // 获取小数点后的部分
        return "$beforeDecimal.$afterDecimal".let { str ->
            if (needSymbol) (if (ioType == 0) "-$str" else "+$str") else str
        }
    }

}