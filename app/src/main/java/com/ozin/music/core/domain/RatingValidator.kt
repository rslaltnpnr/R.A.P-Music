package com.ozin.music.core.domain

/** 0 (unrated) - 5 star rating range, used wherever a rating is persisted. */
object RatingValidator {
    const val MIN = 0
    const val MAX = 5

    fun clamp(rating: Int): Int = rating.coerceIn(MIN, MAX)
}
