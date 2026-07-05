package com.demo.xray.core.common

/** Testable time source. See PRD 13.3 key interfaces. */
interface Clock {
    fun nowEpochMs(): Long

    fun elapsedRealtimeMs(): Long
}

object SystemClock : Clock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMs(): Long = System.nanoTime() / 1_000_000
}
