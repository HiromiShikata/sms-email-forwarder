package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import java.net.URL

class GithubAppUpdateRepository(
    private val githubRepo: String,
    private val versionCompare: AppVersionCompare = AppVersionCompare(),
) : AppUpdateRepository {
    override fun fetchLatest(currentVersion: String): AppUpdate {
        val dtos = GithubReleasePages.collect(pageSize = PAGE_SIZE, maxPages = MAX_PAGES) { page ->
            GithubReleasesJsonParser.parse(fetchReleasesPage(page))
        }
        return GithubAppUpdateMapper.toAppUpdate(dtos, currentVersion, versionCompare)
    }

    private fun fetchReleasesPage(page: Int): String {
        val url = "https://api.github.com/repos/$githubRepo/releases?per_page=$PAGE_SIZE&page=$page"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        return connection.getInputStream().bufferedReader().readText()
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 10
    }
}
