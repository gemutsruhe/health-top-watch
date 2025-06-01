package com.suisei.healthtopwatch.model

data class PracticeInterval(
    var minutes: Int = 0,
    var seconds: Int = 0,
    var prepareTime: Int = 0
) {
    fun toSeconds(): Int {
        return minutes * 60 + seconds
    }

    fun reset() {
        minutes = 0
        seconds = 0
    }
}
