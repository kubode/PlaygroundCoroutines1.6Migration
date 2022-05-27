package com.github.kubode.coroutine16poc

import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidGreetingTest {

    @Test
    fun testExample() {
        assertTrue("Check Android is mentioned", Greeting().greeting().contains("Android"))
    }
}

actual typealias RunWith = org.junit.runner.RunWith
actual typealias Runner = org.junit.runner.Runner
actual typealias AndroidJUnit4 = androidx.test.ext.junit.runners.AndroidJUnit4
actual fun currentThreadName(): String = Thread.currentThread().name
