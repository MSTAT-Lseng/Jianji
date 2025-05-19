package m20.simple.bookkeeping.api.billing

import android.content.Context
import android.content.res.Resources
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.favorite.FavoriteCreator
import m20.simple.bookkeeping.api.objects.BillingObject
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.utils.FileUtils
import m20.simple.bookkeeping.utils.TimeUtils

object BillingCreator {

    private const val CREATE_BILLING_CHECK_SUCCESS = 0
    const val CREATE_BILLING_SUCCESS = 1
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_SUCCESS = 2
    const val CREATE_BILLING_CHECK_FAILED = -1
    const val CREATE_BILLING_INSERT_FAILED = -2
    const val CREATE_BILLING_DEPOSIT_INSERT_FAILED = -3
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_CHECK_FAILED = -4
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_UPDATE_FAILED = -5
    const val CREATE_BILLING_TIME_CHECK_FAILED = -100
    const val CREATE_BILLING_DEPOSIT_TIME_CHECK_FAILED = -101
    const val CREATE_BILLING_AMOUNT_CHECK_FAILED = -102
    const val CREATE_BILLING_IOTYPE_CHECK_FAILED = -103
    const val CREATE_BILLING_CLASSIFY_CHECK_FAILED = -104
    const val CREATE_BILLING_DEPOSIT_TAG_CHECK_FAILED = -105
    const val CREATE_BILLING_WALLET_CHECK_FAILED = -106
    const val EDIT_BILLING_ID_CHECK_FAILED = -107
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_ID_FAILED = -108
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_CONSUMPTION_ID_FAILED = -109
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_TYPE_FAILED = -110
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_TIME_CHECK_FAILED = -111
    const val MODIFY_BILLING_DEPOSIT_PAY_DATE_UNKNOWN_FAILED = -112

    private fun createBillingCheck(billingObject: BillingObject,
                                   depositBillingDate: Long = 0L): Int {
        with(billingObject) {
            // 1. 账单时间检查：账单时间必须在当前日或之前
            val billingTime = TimeUtils.convertDateToDayLevelTimestamp(TimeUtils.getDateFromTimestamp(time))
            val nextDay = TimeUtils.getNextDayTimestamp(TimeUtils.getTimestamp())

            if (billingTime >= nextDay) {
                return CREATE_BILLING_TIME_CHECK_FAILED
            }

            // 2. 订金账单时间检查：如果是订金账单则实际支付日期必须在订金账单日期之后
            if (deposit == "true") {
                val depositTime = TimeUtils.convertDateToDayLevelTimestamp(TimeUtils.getDateFromTimestamp(depositBillingDate))
                if (depositTime <= billingTime) {
                    return CREATE_BILLING_DEPOSIT_TIME_CHECK_FAILED
                }
            }

            // 3. 金额检查：金额不能小于0
            if (amount < 0) return CREATE_BILLING_AMOUNT_CHECK_FAILED

            // 4. 收入/支出检查：该项的值只能是0或1
            if (iotype !in 0..1) return CREATE_BILLING_IOTYPE_CHECK_FAILED

            // 5. 类别检查：该项值不能为 ""
            if (classify.isEmpty()) return CREATE_BILLING_CLASSIFY_CHECK_FAILED

            // 6. 订金标志检查：只能是三个值：true，false，consumption
            if (deposit !in listOf("true", "false", "consumption")) {
                return CREATE_BILLING_DEPOSIT_TAG_CHECK_FAILED
            }

            // 7. 钱包检查：钱包不能小于1
            if (wallet < 1) return CREATE_BILLING_WALLET_CHECK_FAILED

            return CREATE_BILLING_CHECK_SUCCESS
        }
    }

    // 推荐异步使用此方法
    fun createBilling(billingObject: BillingObject,
                      depositBillingDate: Long,
                      context: Context): Pair<Int, Long> {

        val checkBilling = createBillingCheck(billingObject, depositBillingDate)
        if (checkBilling != CREATE_BILLING_CHECK_SUCCESS) {
            return Pair(CREATE_BILLING_CHECK_FAILED, checkBilling.toLong())
        }

        fun insertBilling(insertObject: BillingObject): Long {
            val billingDao = BillingDao(context)
            val record = billingDao.addRecord(
                time = insertObject.time,
                amount = insertObject.amount,
                iotype = insertObject.iotype,
                classify = insertObject.classify,
                notes = insertObject.notes,
                images = insertObject.images,
                deposit = insertObject.deposit,
                wallet = insertObject.wallet,
                tags = insertObject.tags
            )
            billingDao.close()
            return record
        }

        val billingID = insertBilling(billingObject)
        if (billingID == -1L) {
            return Pair(CREATE_BILLING_INSERT_FAILED, billingID)
        }

        if (billingObject.deposit == "true") {
            val depositBilling = billingObject.copy(
                time = depositBillingDate,
                deposit = "consumption"
            )
            val depositID = insertBilling(depositBilling)
            if (depositID == -1L) {
                return Pair(CREATE_BILLING_DEPOSIT_INSERT_FAILED, depositID)
            }
        }

        // modify wallet amount.
        WalletCreator.modifyWalletAmount(
            context,
            billingObject.wallet,
            if (billingObject.iotype == 0) -billingObject.amount else billingObject.amount
        )

        return Pair(CREATE_BILLING_SUCCESS, billingID)
    }

    // 推荐异步使用此方法
    fun modifyBilling(billingId: Long,
                      billingObject: BillingObject,
                      context: Context): Pair<Int, Long> {
        val billingDao = BillingDao(context)

        // 检查ID是否存在
        if (!billingDao.isRecordExists(billingId)) {
            billingDao.close()
            return Pair(CREATE_BILLING_CHECK_FAILED, EDIT_BILLING_ID_CHECK_FAILED.toLong())
        }

        val originalBillingDao = billingDao.getRecordById(billingId)

        // 检查修改的object是否合规
        val checkBilling = createBillingCheck(billingObject)
        if (checkBilling != CREATE_BILLING_CHECK_SUCCESS) {
            return Pair(CREATE_BILLING_CHECK_FAILED, checkBilling.toLong())
        }

        // 修改账单
        billingDao.updateRecord(
            recordId = billingId,
            time = billingObject.time,
            amount = billingObject.amount,
            iotype = billingObject.iotype,
            classify = billingObject.classify,
            notes = billingObject.notes,
            images = billingObject.images,
            deposit = billingObject.deposit,
            wallet = billingObject.wallet,
            tags = billingObject.tags
        )

        // 修改钱包内的余额
        val walletCreator = WalletCreator
        fun getDelta(iotype: Int, amount: Long) = if (iotype == 0) amount else -amount
        walletCreator.modifyWalletAmount(
            context,
            originalBillingDao.wallet,
            getDelta(originalBillingDao.iotype, originalBillingDao.amount)
        )
        walletCreator.modifyWalletAmount(
            context,
            billingObject.wallet,
            -getDelta(billingObject.iotype, billingObject.amount)
        )

        // 检查图片备注是否有变化
        fun compareImageNotesChanged() : Boolean {
            val originalImage = originalBillingDao.images
            val newImage = billingObject.images
            return originalImage != newImage
        }

        if (!compareImageNotesChanged()) return Pair(CREATE_BILLING_SUCCESS, billingId)

        originalBillingDao.images?.let { originalImages ->
            originalImages.split(",").forEach { image ->
                FileUtils(context).deletePhotos(image)
            }
        }

        billingDao.close()
        return Pair(CREATE_BILLING_SUCCESS, billingId)
    }

    // 推荐异步使用此方法
    fun modifyDepositBillingPayDate(
        billingId: Long,
        context: Context,
        timestamp: Long
    ): Pair<Int, Long> {
        val billingDao = BillingDao(context)
        val result: Pair<Int, Long> // 存储最终结果

        try {
            val billingRecord = billingDao.getRecordById(billingId)

            // 检查ID是否存在
            if (billingRecord.id == -1L) {
                result = Pair(
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_CHECK_FAILED,
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_ID_FAILED.toLong()
                )
                return result // 直接返回，并在finally中关闭DAO
            }

            val depositType = billingRecord.deposit
            val consumptionId: Long // 确定要修改的记录ID

            when (depositType) {
                "true" -> {
                    consumptionId = billingId + 1
                }
                "consumption" -> {
                    consumptionId = billingId
                }
                else -> {
                    result = Pair(
                        MODIFY_BILLING_DEPOSIT_PAY_DATE_CHECK_FAILED,
                        MODIFY_BILLING_DEPOSIT_PAY_DATE_TYPE_FAILED.toLong()
                    )
                    return result // 直接返回，并在finally中关闭DAO
                }
            }

            // 获取实际要修改的消费账单
            val consumptionRecord = billingDao.getRecordById(consumptionId)

            // 检查消费账单ID是否存在
            if (consumptionRecord.id == -1L) {
                result = Pair(
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_CHECK_FAILED,
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_CONSUMPTION_ID_FAILED.toLong()
                )
                return result // 直接返回，并在finally中关闭DAO
            }

            // 认证日期合法性
            if (timestamp <= billingRecord.time) {
                result = Pair(
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_CHECK_FAILED,
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_TIME_CHECK_FAILED.toLong()
                )
                return result // 直接返回，并在finally中关闭DAO
            }

            // 修改实际消费账单
            val modifiedItemNumber = billingDao.updateRecord(
                recordId = consumptionId,
                time = timestamp,
                amount = consumptionRecord.amount,
                iotype = consumptionRecord.iotype,
                classify = consumptionRecord.classify,
                notes = consumptionRecord.notes,
                images = consumptionRecord.images,
                deposit = consumptionRecord.deposit,
                wallet = consumptionRecord.wallet,
                tags = consumptionRecord.tags
            )

            result = if (modifiedItemNumber == 0) {
                Pair(
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_UPDATE_FAILED,
                    MODIFY_BILLING_DEPOSIT_PAY_DATE_UNKNOWN_FAILED.toLong()
                )
            } else {
                Pair(MODIFY_BILLING_DEPOSIT_PAY_DATE_SUCCESS, consumptionId)
            }

        } finally {
            // 确保在任何情况下都关闭 DAO
            billingDao.close()
        }

        return result
    }

    // 推荐异步使用此方法
    fun deleteBillingById(id: Long,
                          context: Context) : Boolean {
        val billingDao = BillingDao(context)
        return try {
            val recordToDelete = billingDao.getRecordById(id)
            if (recordToDelete.id == -1L) {
                return false
            }

            WalletCreator.modifyWalletAmount(
                context,
                recordToDelete.wallet,
                if (recordToDelete.iotype == 0) recordToDelete.amount else -recordToDelete.amount
            )

            when (recordToDelete.deposit) {
                "true" -> billingDao.deleteRecord((id + 1))
                "consumption" -> billingDao.deleteRecord((id - 1))
            }

            FavoriteCreator.cancelBillingFavorite(context, id)

            val recordDelete = billingDao.deleteRecord(id) > 0
            recordDelete
        } finally {
            billingDao.close()
        }
    }

    // 推荐异步使用此方法
    fun getRecordsByDay(dayInMillis: Long, context: Context): List<BillingDao.Record> {
        val billingDao = BillingDao(context)
        val records = billingDao.getRecordsByDay(dayInMillis)
        billingDao.close()
        return records
    }

    // 推荐异步使用此方法
    fun getRecordById(recordId: Long, context: Context): BillingDao.Record {
        val billingDao = BillingDao(context)
        val record = billingDao.getRecordById(recordId)
        billingDao.close()
        return record
    }

    // 推荐异步使用此方法
    fun transferRecordWallet(oldWallet: Int, newWallet: Int, context: Context) : Int {
        val billingDao = BillingDao(context)
        val result = billingDao.transferRecordWallet(oldWallet, newWallet)
        billingDao.close()
        return result
    }

    fun getCreateBillingFailedReason(reason: Int, resources: Resources): String {
        val billingFailureReasons = mapOf(
            CREATE_BILLING_TIME_CHECK_FAILED to R.string.create_billing_time_check_failed,
            CREATE_BILLING_DEPOSIT_TIME_CHECK_FAILED to R.string.create_billing_deposit_time_check_failed,
            CREATE_BILLING_AMOUNT_CHECK_FAILED to R.string.create_billing_amount_check_failed,
            CREATE_BILLING_IOTYPE_CHECK_FAILED to R.string.create_billing_iotype_check_failed,
            CREATE_BILLING_CLASSIFY_CHECK_FAILED to R.string.create_billing_classify_check_failed,
            CREATE_BILLING_DEPOSIT_TAG_CHECK_FAILED to R.string.create_billing_deposit_tag_check_failed,
            CREATE_BILLING_WALLET_CHECK_FAILED to R.string.create_billing_wallet_check_failed,
            CREATE_BILLING_CHECK_FAILED to R.string.create_billing_check_failed,
            CREATE_BILLING_INSERT_FAILED to R.string.create_billing_insert_failed,
            CREATE_BILLING_DEPOSIT_INSERT_FAILED to R.string.create_billing_deposit_insert_failed,
            MODIFY_BILLING_DEPOSIT_PAY_DATE_CHECK_FAILED to R.string.modify_billing_deposit_pay_date_check_failed,
            MODIFY_BILLING_DEPOSIT_PAY_DATE_ID_FAILED to R.string.modify_billing_deposit_pay_date_id_failed,
            MODIFY_BILLING_DEPOSIT_PAY_DATE_TYPE_FAILED to R.string.modify_billing_deposit_pay_date_type_failed,
            MODIFY_BILLING_DEPOSIT_PAY_DATE_CONSUMPTION_ID_FAILED to R.string.modify_billing_deposit_pay_date_consumption_id_failed,
            MODIFY_BILLING_DEPOSIT_PAY_DATE_TIME_CHECK_FAILED to R.string.modify_billing_deposit_pay_date_time_check_failed,
            MODIFY_BILLING_DEPOSIT_PAY_DATE_UPDATE_FAILED to R.string.modify_billing_deposit_pay_date_update_failed,
            MODIFY_BILLING_DEPOSIT_PAY_DATE_UNKNOWN_FAILED to R.string.modify_billing_deposit_pay_date_unknown_failed,
        )

        return resources.getString(
            billingFailureReasons[reason] ?: R.string.create_billing_unknown_error,
            reason
        )
    }

}