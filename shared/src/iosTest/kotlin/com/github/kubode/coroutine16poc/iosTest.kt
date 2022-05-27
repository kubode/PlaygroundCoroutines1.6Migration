package com.github.kubode.coroutine16poc

import kotlin.test.Test
import kotlin.test.assertTrue
import platform.Foundation.NSThread

class IosGreetingTest {

    @Test
    fun testExample() {
        assertTrue(Greeting().greeting().contains("iOS"), "Check iOS is mentioned")
    }
}

actual abstract class Runner
actual class AndroidJUnit4 : Runner()
actual fun currentThreadName(): String = NSThread.currentThread.debugDescription.orEmpty()
