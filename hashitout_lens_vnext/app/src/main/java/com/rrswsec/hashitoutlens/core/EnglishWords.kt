package com.rrswsec.hashitoutlens.core

// this is not trying to be a whole dictionary.
// it just needs enough signal to tell "real words" from soup.
object EnglishWords {
    val common = setOf(
        "the", "and", "that", "have", "for", "not", "with", "you", "this", "but",
        "his", "from", "they", "say", "her", "she", "will", "one", "all", "would",
        "there", "their", "what", "about", "which", "when", "make", "like", "time",
        "just", "know", "take", "into", "year", "good", "some", "could", "them",
        "other", "than", "then", "look", "only", "come", "over", "think", "also",
        "back", "after", "work", "first", "well", "even", "want", "because", "these",
        "give", "most", "hello", "world", "cipher", "decode", "decoded", "message",
        "secret", "hidden", "code", "flag", "password", "open", "close", "data", "text",
        "link", "http", "https", "wifi", "user", "name", "email", "phone", "scan"
    )
}
