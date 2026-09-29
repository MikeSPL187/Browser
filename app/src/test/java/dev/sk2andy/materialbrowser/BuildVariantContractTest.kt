package dev.sk2andy.materialbrowser

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildVariantContractTest {
    @Test
    fun `user certificate trust matches build type`() {
        val expected = when (BuildConfig.BUILD_TYPE) {
            "debug", "release", "localRelease" -> false
            "userCaDebug", "userCaRelease" -> true
            else -> error("Unknown build type: ${BuildConfig.BUILD_TYPE}")
        }

        assertEquals(expected, BuildConfig.TRUST_USER_CERTIFICATES)
    }

    @Test
    fun `github updater is enabled only for github release channels`() {
        val expected = when (BuildConfig.BUILD_TYPE) {
            "release", "userCaRelease" -> true
            "debug", "localRelease", "userCaDebug" -> false
            else -> error("Unknown build type: ${BuildConfig.BUILD_TYPE}")
        }

        assertEquals(expected, BuildConfig.ENABLE_GITHUB_UPDATES)
    }

    @Test
    fun `distribution capabilities match flavor`() {
        val expectedSystemWebViewOnly = when (BuildConfig.FLAVOR) {
            "full" -> false
            "systemwebview" -> true
            else -> error("Unknown flavor: ${BuildConfig.FLAVOR}")
        }

        assertEquals(expectedSystemWebViewOnly, BuildConfig.SYSTEM_WEBVIEW_ONLY)
    }

    @Test
    fun `production application identity isolates release channels`() {
        val flavorSuffix = when (BuildConfig.FLAVOR) {
            "full" -> ""
            "systemwebview" -> ".systemwebview"
            else -> error("Unknown flavor: ${BuildConfig.FLAVOR}")
        }
        val buildTypeSuffix = when (BuildConfig.BUILD_TYPE) {
            "release" -> ""
            "userCaRelease" -> ".ca"
            "debug", "localRelease", "userCaDebug" -> return
            else -> error("Unknown build type: ${BuildConfig.BUILD_TYPE}")
        }

        assertEquals(
            "io.github.mikespl187.vola$flavorSuffix$buildTypeSuffix",
            BuildConfig.APPLICATION_ID,
        )
    }
}
