package com.hiromi_shikata.smsemailforwarder.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionCompareTest {
    private val compare = AppVersionCompare()

    @Test
    fun `isNewer returns true when latest minor version is greater`() {
        assertTrue(compare.isNewer("1.1.0", "1.0.0"))
    }

    @Test
    fun `isNewer returns true when latest patch version is greater`() {
        assertTrue(compare.isNewer("1.0.1", "1.0.0"))
    }

    @Test
    fun `isNewer returns true when latest major version is greater`() {
        assertTrue(compare.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `isNewer returns false when versions are equal`() {
        assertFalse(compare.isNewer("1.0.0", "1.0.0"))
    }

    @Test
    fun `isNewer returns false when latest is older`() {
        assertFalse(compare.isNewer("0.9.0", "1.0.0"))
    }

    @Test
    fun `isNewer ignores build metadata in comparison`() {
        assertFalse(compare.isNewer("1.0.0+build123", "1.0.0"))
    }

    @Test
    fun `isNewer ignores pre-release suffix in comparison`() {
        assertFalse(compare.isNewer("1.0.0-beta", "1.0.0"))
    }

    @Test
    fun `isNewer handles two-component versions`() {
        assertTrue(compare.isNewer("1.1", "1.0"))
    }
}
