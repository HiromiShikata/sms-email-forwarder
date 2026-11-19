package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class GithubReleasePagesTest {

    private data class Case(
        val name: String,
        val pageSize: Int,
        val maxPages: Int,
        val fetchPage: (Int) -> List<GithubReleaseDto>,
        val expectedRequestedPages: List<Int>,
        val expectedTags: List<String>,
    )

    private fun release(tag: String): GithubReleaseDto = GithubReleaseDto(
        tagName = tag,
        body = "",
        universalApkUrl = null,
        draft = false,
        prerelease = false,
    )

    private fun pagesOf(vararg pages: List<GithubReleaseDto>): (Int) -> List<GithubReleaseDto> =
        { page -> pages.getOrElse(page - 1) { emptyList() } }

    private val cases = listOf(
        Case(
            name = "stops after the first page that is shorter than the page size",
            pageSize = 3,
            maxPages = 10,
            fetchPage = pagesOf(
                listOf(release("v1.0.0"), release("v1.0.1"), release("v1.0.2")),
                listOf(release("v1.0.3")),
            ),
            expectedRequestedPages = listOf(1, 2),
            expectedTags = listOf("v1.0.0", "v1.0.1", "v1.0.2", "v1.0.3"),
        ),
        Case(
            name = "a single short page results in exactly one fetch",
            pageSize = 2,
            maxPages = 10,
            fetchPage = pagesOf(listOf(release("v1.0.0"))),
            expectedRequestedPages = listOf(1),
            expectedTags = listOf("v1.0.0"),
        ),
        Case(
            name = "a full last page followed by an empty page stops at the empty page",
            pageSize = 2,
            maxPages = 10,
            fetchPage = pagesOf(
                listOf(release("v1.0.0"), release("v1.0.1")),
                listOf(release("v1.0.2"), release("v1.0.3")),
                emptyList(),
            ),
            expectedRequestedPages = listOf(1, 2, 3),
            expectedTags = listOf("v1.0.0", "v1.0.1", "v1.0.2", "v1.0.3"),
        ),
        Case(
            name = "stops at maxPages even when every page is full",
            pageSize = 2,
            maxPages = 2,
            fetchPage = { page ->
                listOf(release("v1.$page.0"), release("v1.$page.1"))
            },
            expectedRequestedPages = listOf(1, 2),
            expectedTags = listOf("v1.1.0", "v1.1.1", "v1.2.0", "v1.2.1"),
        ),
    )

    @Test
    fun `collect requests pages in order and returns the concatenated releases`() {
        val actual = cases.map { case ->
            val requestedPages = mutableListOf<Int>()
            val releases = GithubReleasePages.collect(
                pageSize = case.pageSize,
                maxPages = case.maxPages,
                fetchPage = { page ->
                    requestedPages += page
                    case.fetchPage(page)
                },
            )
            Triple(case.name, requestedPages.toList(), releases.map { it.tagName })
        }
        val expected = cases.map { Triple(it.name, it.expectedRequestedPages, it.expectedTags) }
        assertEquals(expected, actual)
    }
}
