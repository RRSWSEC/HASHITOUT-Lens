package com.rrswsec.hashitoutlens.core

// this is not trying to be a whole dictionary.
// it just needs enough signal to tell "real words" from soup.
object EnglishWords {
    val common = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "is", "was", "are", "been", "has", "had", "were", "did", "does", "done",
        "should", "must", "can", "could", "may", "might", "shall", "will", "would",
        "cipher", "decode", "decoded", "message", "secret", "hidden", "code", "flag",
        "password", "open", "close", "data", "text", "link", "http", "https", "wifi",
        "user", "name", "email", "phone", "scan", "system", "login", "admin", "root",
        "key", "access", "secure", "private", "public", "auth", "token", "session",
        "encrypt", "crypto", "hash", "md5", "sha1", "sha256", "aes", "rsa", "base64"
    )
}
