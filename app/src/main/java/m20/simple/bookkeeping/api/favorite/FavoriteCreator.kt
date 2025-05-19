package m20.simple.bookkeeping.api.favorite

import android.content.Context
import m20.simple.bookkeeping.database.favorite.FavoriteDao

object FavoriteCreator {

    // 添加收藏
    fun addBillingFavorite(
        context: Context,
        billingId: Long,
        tags: String? = null
    ): Long {
        val favoriteDao = FavoriteDao(context)
        val result = favoriteDao.addFavorite(billingId, tags)
        favoriteDao.close()
        return result
    }

    // 检查账单是否已经被收藏
    fun isFavoriteBilling(
        context: Context,
        billingId: Long
    ): Boolean {
        val favoriteDao = FavoriteDao(context)
        val result = favoriteDao.isBillingIdFavorited(billingId)
        favoriteDao.close()
        return result
    }

    // 取消收藏账单
    fun cancelBillingFavorite(
        context: Context,
        billingId: Long
    ): Boolean {
        val favoriteDao = FavoriteDao(context)
        val result = favoriteDao.deleteFavoritesByBillingId(billingId)
        favoriteDao.close()
        return result > 0
    }

    // 分页查询
    fun getFavoritesPaginated(
        context: Context,
        limit: Int,
        offset: Int
    ): List<FavoriteDao.FavoriteEntry> {
        val favoriteDao = FavoriteDao(context)
        val result = favoriteDao.getFavoritesPaginated(limit, offset)
        favoriteDao.close()
        return result
    }

    // 获取收藏条目数量
    fun getFavoriteCount(
        context: Context
    ): Long {
        val favoriteDao = FavoriteDao(context)
        val result = favoriteDao.getFavoriteCount()
        favoriteDao.close()
        return result
    }

}