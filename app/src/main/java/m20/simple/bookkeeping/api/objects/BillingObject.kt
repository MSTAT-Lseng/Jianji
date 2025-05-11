package m20.simple.bookkeeping.api.objects

data class BillingObject (
    var time: Long,
    var amount: Long,
    var iotype: Int,
    var classify: String,
    var notes: String?,
    var images: String?,
    var deposit: String,
    var wallet: Int,
    var tags: String?
)