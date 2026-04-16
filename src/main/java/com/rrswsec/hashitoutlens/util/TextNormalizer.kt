package com.rrswsec.hashitoutlens.util

object TextNormalizer {
    fun normalize(raw: String): String = raw.replace('
', ' ').replace(Regex("\s+"), " ").trim()

    fun materiallyChanged(old: String, new: String): Boolean {
        val o = normalize(old)
        val n = normalize(new)
        if (o == n) return false
        val minLen = minOf(o.length, n.length)
        if (minLen == 0) return true
        val samePrefix = o.zip(n).takeWhile { it.first == it.second }.count()
        return samePrefix < minLen * 0.88 || kotlin.math.abs(o.length - n.length) > 5
    }
}
