package m20.simple.bookkeeping.api.config

import android.content.Context
import m20.simple.bookkeeping.config.HintConfig

object HintConfigCreator {

    fun getDepositBillHint(context: Context): Boolean {
        return HintConfig.getBooleanValue(context, HintConfig.KEY_DEPOSIT_BILL_HINT, true)
    }

    fun setDepositBillHint(context: Context, value: Boolean) {
        HintConfig.setBooleanValue(context, HintConfig.KEY_DEPOSIT_BILL_HINT, value)
    }

}