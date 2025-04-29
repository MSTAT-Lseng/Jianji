package m20.simple.bookkeeping.api.billing

import android.content.Context
import android.content.res.Resources
import m20.simple.bookkeeping.R
import m20.simple.bookkeeping.api.objects.BillingObject
import m20.simple.bookkeeping.api.wallet.WalletCreator
import m20.simple.bookkeeping.database.billing.BillingDao
import m20.simple.bookkeeping.database.wallet.WalletDao
import m20.simple.bookkeeping.utils.FileUtils
import m20.simple.bookkeeping.utils.TimeUtils

object BillingCreator {

    private const val CREATE_BILLING_CHECK_SUCCESS = 0
    const val CREATE_BILLING_SUCCESS = 1
    const val CREATE_BILLING_CHECK_FAILED = -1
    const val CREATE_BILLING_INSERT_FAILED = -2
    const val CREATE_BILLING_DEPOSIT_INSERT_FAILED = -3
    const val CREATE_BILLING_TIME_CHECK_FAILED = -100
    const val CREATE_BILLING_DEPOSIT_TIME_CHECK_FAILED = -101
    const val CREATE_BILLING_AMOUNT_CHECK_FAILED = -102
    const val CREATE_BILLING_IOTYPE_CHECK_FAILED = -103
    const val CREATE_BILLING_CLASSIFY_CHECK_FAILED = -104
    const val CREATE_BILLING_DEPOSIT_TAG_CHECK_FAILED = -105
    const val CREATE_BILLING_WALLET_CHECK_FAILED = -106
    const val EDIT_BILLING_ID_CHECK_FAILED = -107

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
                      context: Context): Pair<Int, Int> {

        val checkBilling = createBillingCheck(billingObject, depositBillingDate)
        if (checkBilling != CREATE_BILLING_CHECK_SUCCESS) {
            return Pair(CREATE_BILLING_CHECK_FAILED, checkBilling)
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
            return Pair(CREATE_BILLING_INSERT_FAILED, billingID.toInt())
        }

        if (billingObject.deposit == "true") {
            val depositBilling = billingObject.copy(
                time = depositBillingDate,
                deposit = "consumption"
            )
            val depositID = insertBilling(depositBilling)
            if (depositID == -1L) {
                return Pair(CREATE_BILLING_DEPOSIT_INSERT_FAILED, depositID.toInt())
            }
        }

        // modify wallet amount.
        WalletCreator.modifyWalletAmount(
            context,
            billingObject.wallet,
            if (billingObject.iotype == 0) -billingObject.amount else billingObject.amount
        )

        return Pair(CREATE_BILLING_SUCCESS, billingID.toInt())
    }

    // 推荐异步使用此方法
    fun modifyBilling(billingId: Int,
                      billingObject: BillingObject,
                      context: Context): Pair<Int, Int> {
        val billingDao = BillingDao(context)

        // 检查ID是否存在
        if (!billingDao.isRecordExists(billingId.toLong())) {
            billingDao.close()
            return Pair(CREATE_BILLING_CHECK_FAILED, EDIT_BILLING_ID_CHECK_FAILED)
        }

        val originalBillingDao = billingDao.getRecordById(billingId.toLong())

        // 检查修改的object是否合规
        val checkBilling = createBillingCheck(billingObject)
        if (checkBilling != CREATE_BILLING_CHECK_SUCCESS) {
            return Pair(CREATE_BILLING_CHECK_FAILED, checkBilling)
        }

        // 修改账单
        val modifiedItemNumber = billingDao.updateRecord(
            recordId = billingId.toLong(),
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
        fun getDelta(iotype: Int, amount: Int) = if (iotype == 0) amount else -amount
        walletCreator.modifyWalletAmount(
            context,
            originalBillingDao!!.wallet,
            getDelta(originalBillingDao.iotype, billingObject.amount)
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
    fun deleteBillingById(id: Int,
                          context: Context) : Boolean {
        val billingDao = BillingDao(context)
        return try {
            val recordToDelete = billingDao.getRecordById(id.toLong()) ?: return false.also { billingDao.close() }

            WalletCreator.modifyWalletAmount(
                context,
                recordToDelete.wallet,
                if (recordToDelete.iotype == 0) recordToDelete.amount else -recordToDelete.amount
            )

            val depositDelete = when (recordToDelete.deposit) {
                "true" -> billingDao.deleteRecord((id + 1).toLong()) > 0
                "consumption" -> billingDao.deleteRecord((id - 1).toLong()) > 0
                else -> true
            }

            val recordDelete = billingDao.deleteRecord(id.toLong()) > 0
            depositDelete && recordDelete
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
    fun getRecordById(recordId: Long, context: Context): BillingDao.Record? {
        val billingDao = BillingDao(context)
        val record = billingDao.getRecordById(recordId)
        billingDao.close()
        return record
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
        )

        return resources.getString(
            billingFailureReasons[reason] ?: R.string.create_billing_unknown_error,
            reason
        )
    }

}