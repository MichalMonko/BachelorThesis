package com.example.camerastreamapplication.threading

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ThreadExecutor
{
    private val NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()
    private const val KEEP_ALIVE_TIME = 1L
    private val KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS

    private val workQueue = LinkedBlockingQueue<Runnable>()

    private val threadPool = ThreadPoolExecutor(
            2,
            NUMBER_OF_CORES,
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            workQueue
    )

    fun execute(runnable: Runnable)
    {
        threadPool.execute(runnable)
    }
}