package com.stupidtree.hitax.data

import com.stupidtree.hita.api.SyncRecord

/** sync/pull-record 的信任边界：hook 只能改 data，不能改 key/ids。 */
object SyncHookPolicy {
    fun accept(
        original: SyncRecord,
        expectedIds: List<String>,
        expectedData: List<String>,
        hooked: SyncRecord,
    ): SyncRecord = if (hooked.key == original.key && hooked.ids == expectedIds) {
        hooked
    } else {
        // 不能返回 original：hook 可能已经原地改写了它的 ids/data。
        SyncRecord(original.key, expectedIds.toList(), expectedData.toList())
    }
}
