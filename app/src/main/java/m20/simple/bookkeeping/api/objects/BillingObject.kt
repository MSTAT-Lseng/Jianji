package m20.simple.bookkeeping.api.objects

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter

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
) {

    override fun toString(): String {
        return JSON.toJSONString(this, JSONWriter.Feature.WriteNulls)
    }

}

object BillingObjectCreator {

    fun toObject(json: String): BillingObject {
        return JSON.parseObject(json, BillingObject::class.java)
    }

}