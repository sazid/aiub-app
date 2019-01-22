package com.mohammedsazid.android.aiub

fun computeHash(s: String): Long {
    val p: Long = 307
    val m: Long = 1000000009
    var hashValue: Long = 0
    var pPow: Long = 1
    for (c: Char in s) {
        hashValue = (hashValue + (c.toInt() * pPow)) % m
        pPow = (pPow * p) % m
    }
    return hashValue
}
