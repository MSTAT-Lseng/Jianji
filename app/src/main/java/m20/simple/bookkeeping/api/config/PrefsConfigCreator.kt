package m20.simple.bookkeeping.api.config

import android.content.Context
import m20.simple.bookkeeping.config.PrefsConfig

object PrefsConfigCreator {

    fun getDefaultWalletId(context: Context): Int {
        return PrefsConfig.getIntValue(context, PrefsConfig.KEY_DEFAULT_WALLET_ID, PrefsConfig.DEFAULT_WALLET_ID)
    }

    fun setDefaultWalletId(context: Context, value: Int) {
        PrefsConfig.setIntValue(context, PrefsConfig.KEY_DEFAULT_WALLET_ID, value)
    }

    /*
    * 提示：请不要直接使用 getNavBarHeight 方法，未初始化的值为 0。
    * 应当使用 UIUtils 下的 getNavigationBarHeight 方法。
    * */
    fun getNavBarHeight(context: Context): Int {
        return PrefsConfig.getIntValue(context, PrefsConfig.KEY_NAV_BAR_HEIGHT, PrefsConfig.DEFAULT_NAV_BAR_HEIGHT)
    }

    fun setNavBarHeight(context: Context, value: Int) {
        PrefsConfig.setIntValue(context, PrefsConfig.KEY_NAV_BAR_HEIGHT, value)
    }

}