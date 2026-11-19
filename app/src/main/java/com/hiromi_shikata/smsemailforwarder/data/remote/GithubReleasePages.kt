package com.hiromi_shikata.smsemailforwarder.data.remote

object GithubReleasePages {
    fun collect(
        pageSize: Int,
        maxPages: Int,
        fetchPage: (page: Int) -> List<GithubReleaseDto>,
    ): List<GithubReleaseDto> {
        val releases = mutableListOf<GithubReleaseDto>()
        for (page in 1..maxPages) {
            val pageReleases = fetchPage(page)
            releases += pageReleases
            if (pageReleases.size < pageSize) break
        }
        return releases
    }
}
