package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubAppUpdateRepositoryTest {
    private fun entry(version: String) =
        "<entry><title>$version</title><content type=\"html\">&lt;p&gt;n$version&lt;/p&gt;</content></entry>"

    private fun page(vararg versions: String) =
        "<feed xmlns=\"http://www.w3.org/2005/Atom\">${versions.joinToString("") { entry(it) }}</feed>"

    @Test
    fun `fetch reads the releases atom feed and never the rest api`() {
        val requested = mutableListOf<String>()
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { url ->
            requested += url
            if (url.endsWith("page=1")) page("0.1.346", "0.1.345") else "<feed xmlns=\"http://www.w3.org/2005/Atom\"/>"
        })
        repository.fetchReleasesNewerThan("0.1.344")
        assertTrue(requested.all { it.startsWith("https://github.com/owner/app/releases.atom") })
        assertTrue(requested.none { it.contains("api.github.com") })
    }

    @Test
    fun `fetch pages until a release at or below the installed version`() {
        val pages = mapOf(
            "page=1" to page("0.1.349", "0.1.348"),
            "page=2" to page("0.1.347", "0.1.346"),
            "page=3" to page("0.1.345", "0.1.344"),
        )
        val requested = mutableListOf<String>()
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { url ->
            requested += url
            pages.entries.first { url.endsWith(it.key) }.value
        })
        val releases = repository.fetchReleasesNewerThan("0.1.344")
        assertEquals(listOf("0.1.349", "0.1.348", "0.1.347", "0.1.346", "0.1.345", "0.1.344"), releases.map { it.version })
        assertEquals(3, requested.size)
    }

    @Test(expected = IllegalStateException::class)
    fun `fetch failure propagates to the caller`() {
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { throw IllegalStateException("HTTP 503") })
        repository.fetchReleasesNewerThan("0.1.0")
    }
}
