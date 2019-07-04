package com.example.camerastreamapplication.imageProcessing

import java.io.Closeable
import java.nio.ByteBuffer

enum class STATE
{
    EMPTY, FULL, OPENED_FOR_READING, OPENED_FOR_WRITING
}

class DoubleBuffer(imageSize: Int)
{

    class Buffer(val buffer: ByteBuffer, var state: STATE) : Closeable
    {
        override fun close()
        {
            buffer.rewind()
            this.state = when (this.state)
            {
                STATE.OPENED_FOR_READING -> STATE.EMPTY
                STATE.OPENED_FOR_WRITING -> STATE.FULL
                else                     -> throw IllegalStateException("Buffer to be closed cannot be empty or full")
            }
        }
    }

    private val frontBuffer: Buffer
    private val backBuffer: Buffer

    init
    {
        frontBuffer = Buffer(ByteBuffer.allocateDirect(imageSize), STATE.EMPTY)
        backBuffer = Buffer(ByteBuffer.allocateDirect(imageSize), STATE.EMPTY)
    }

    fun getBufferForReading(): Buffer?
    {
        return when
        {
            frontBuffer.state == STATE.FULL -> {
                frontBuffer.state = STATE.OPENED_FOR_READING
                frontBuffer
            }
            backBuffer.state == STATE.FULL  -> {
                backBuffer.state = STATE.OPENED_FOR_READING
                backBuffer
            }
            else                            -> null
        }
    }

    fun getBufferForWriting(): Buffer?
    {

        if (frontBuffer.state == STATE.EMPTY)
        {
            frontBuffer.state = STATE.OPENED_FOR_WRITING
            return frontBuffer
        }
        return when (backBuffer.state)
        {
            STATE.EMPTY ->
            {
                backBuffer.state = STATE.OPENED_FOR_WRITING
                backBuffer
            }
            STATE.FULL  ->
            {
                if (frontBuffer.state == STATE.EMPTY)
                {
                    frontBuffer.state = STATE.OPENED_FOR_WRITING
                    frontBuffer
                }
                else
                {
                    backBuffer.state = STATE.OPENED_FOR_WRITING
                    backBuffer
                }
            }
            else        -> null
        }
    }
}