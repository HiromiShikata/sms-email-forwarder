package com.hiromi_shikata.smsemailforwarder.domain

class AppVersionCompare {
    fun isNewer(latest: String, current: String): Boolean {
        val cleanVersion = { v: String -> v.split("+").first().split("-").first() }
        val toComponents = { v: String -> cleanVersion(v).split(".").mapNotNull { it.toIntOrNull() } }
        val latestParts = toComponents(latest)
        val currentParts = toComponents(current)
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
