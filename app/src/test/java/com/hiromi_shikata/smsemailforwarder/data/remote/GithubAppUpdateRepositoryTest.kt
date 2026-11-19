package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubAppUpdateRepositoryTest {
    private fun entry(version: String) =
        "<entry><title>$version</title><content type=\"html\">&lt;p&gt;n$version&lt;/p&gt;</content></entry>"

    private fun feed(vararg versions: String) =
        "<feed xmlns=\"http://www.w3.org/2005/Atom\">${versions.joinToString("") { entry(it) }}</feed>"

    private val tenNewest = (345 downTo 336).map { "0.1.$it" }.toTypedArray()

    @Test
    fun `fetch reads only the releases atom feed and never the rest api`() {
        val requested = mutableListOf<String>()
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { url ->
            requested += url
            feed("0.1.345", "0.1.344")
        })
        repository.fetchReleasesNewerThan("0.1.343")
        assertEquals(listOf("https://github.com/owner/app/releases.atom"), requested)
    }

    @Test
    fun `fetch returns every feed entry when the feed reaches the installed version`() {
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { feed("0.1.346", "0.1.345", "0.1.344") })
        assertEquals(
            listOf("0.1.346", "0.1.345", "0.1.344"),
            repository.fetchReleasesNewerThan("0.1.344").map { it.version },
        )
    }

    @Test
    fun `fetch fails visibly when the feed is truncated before the installed version`() {
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { feed(*tenNewest) })
        val error = runCatching { repository.fetchReleasesNewerThan("0.1.300") }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertEquals(
            "The release feed lists only the newest 10 releases, and v0.1.336 is still newer than v0.1.300",
            error?.message,
        )
    }

    @Test
    fun `a full feed that already reaches the installed version is accepted`() {
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { feed(*tenNewest) })
        assertEquals(10, repository.fetchReleasesNewerThan("0.1.336").size)
    }

    @Test(expected = IllegalStateException::class)
    fun `fetch failure propagates to the caller`() {
        val repository = GithubAppUpdateRepository("owner/app", fetchText = { throw IllegalStateException("HTTP 503") })
        repository.fetchReleasesNewerThan("0.1.0")
    }
}
