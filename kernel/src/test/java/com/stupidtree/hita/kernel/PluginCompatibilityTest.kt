package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.HITA_API_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginCompatibilityTest {

    private fun check(
        requiresApi: Int = HITA_API_VERSION,
        minHostApi: Int? = null,
        maxHostApi: Int? = null,
        apiAbi: String? = null,
        minSdk: Int? = null,
        hostApi: Int = HITA_API_VERSION,
        hostAbi: String? = null,
        sdkInt: Int? = null,
        exempt: Boolean = false,
    ): CompatibilityResult = PluginCompatibility.check(
        PluginArtifactMeta("p", requiresApi, minHostApi, maxHostApi, apiAbi, minSdk),
        hostApi = hostApi,
        hostAbi = hostAbi,
        sdkInt = sdkInt,
        exempt = exempt,
    )

    @Test
    fun compatibleWhenAllBoundsMatch() {
        assertEquals(CompatibilityResult.Compatible, check())
    }

    @Test
    fun exactExemptionBypassesNormalChecks() {
        assertEquals(
            CompatibilityResult.Compatible,
            check(requiresApi = HITA_API_VERSION + 1, exempt = true),
        )
    }

    @Test
    fun rejectsNewerMinimumHostApi() {
        val result = check(requiresApi = HITA_API_VERSION + 1) as CompatibilityResult.Rejected
        assertTrue(result.reasons.single().contains("requires host api"))
    }

    @Test
    fun rejectsHostNewerThanDeclaredMaximum() {
        val result = check(maxHostApi = HITA_API_VERSION - 1) as CompatibilityResult.Rejected
        assertTrue(result.reasons.single().contains("supports host api up to"))
    }

    @Test
    fun rejectsAbiMismatchOnlyWhenBothSidesDeclareIt() {
        assertTrue(check(apiAbi = "old", hostAbi = "new") is CompatibilityResult.Rejected)
        assertEquals(CompatibilityResult.Compatible, check(apiAbi = "old", hostAbi = null))
        assertEquals(CompatibilityResult.Compatible, check(apiAbi = null, hostAbi = "new"))
    }

    @Test
    fun rejectsTooNewAndroidSdkOnlyWhenDeviceSdkIsKnown() {
        assertTrue(check(minSdk = 35, sdkInt = 34) is CompatibilityResult.Rejected)
        assertEquals(CompatibilityResult.Compatible, check(minSdk = 35, sdkInt = null))
    }

    @Test
    fun collectsEveryReasonInsteadOfStoppingAtFirst() {
        val result = check(
            requiresApi = HITA_API_VERSION + 1,
            maxHostApi = HITA_API_VERSION - 1,
            apiAbi = "old",
            minSdk = 99,
            hostAbi = "new",
            sdkInt = 1,
        ) as CompatibilityResult.Rejected
        assertEquals(4, result.reasons.size)
    }
}
