package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ReleaseManifestAppUpdateRepositoryTest {
    private val base = "https://example.test/releases"

    @Test
    fun `fetch reads the manifest asset from the latest release download url`() {
        val requested = mutableListOf<String>()
        val repository = ReleaseManifestAppUpdateRepository(base, fetchText = { url ->
            requested += url
            """[{"version":"0.1.2","tag":"v0.1.2","publishedAt":"x","notes":"n"}]"""
        })
        assertEquals(1, repository.fetchReleases().size)
        assertEquals(listOf("$base/latest/download/release-notes.json"), requested)
        assertTrue(requested.none { it.contains("api.github.com") })
    }

    @Test
    fun `a missing manifest propagates as a failure and is not treated as no releases`() {
        val repository = ReleaseManifestAppUpdateRepository(base, fetchText = { url ->
            throw IOException("HTTP 404 while reading $url")
        })
        val error = runCatching { repository.fetchReleases() }.exceptionOrNull()
        assertTrue(error is IOException)
    }

    @Test
    fun `a malformed manifest propagates as a failure`() {
        val repository = ReleaseManifestAppUpdateRepository(base, fetchText = { "<html>not a manifest" })
        val error = runCatching { repository.fetchReleases() }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}
