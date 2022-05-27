package com.github.kubode.coroutine16poc

import app.cash.turbine.test
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class RunWith(val value: KClass<out Runner>)
expect abstract class Runner
expect class AndroidJUnit4 : Runner
expect fun currentThreadName(): String

@RunWith(AndroidJUnit4::class)
class CommonGreetingTest {

//    @Test
    fun testKtor() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
//                    delay(100)
                    respondOk()
                }
            }
        }
        client.submitFormWithBinaryData("https://example.com", formData {  })
        assertEquals(200, client.get("https://example.com").status.value)
    }

    // Unconfinedだとjava.lang.IllegalStateException: Module with the Main dispatcher is missing.が起きる
//    private val dispatcher = StandardTestDispatcher()
//    private val dispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalStdlibApi::class)
//    @Test
    fun testReactor() = runTest {
        Dispatchers.setMain(coroutineContext[CoroutineDispatcher.Key]!!)
//        Dispatchers.setMain(Dispatchers.Default)
        val reactor = Reactor()
        reactor.state.test {
            assertEquals(0, awaitItem())
            println("send 1 before")
            reactor.send(1)
            println("send 1")
            assertEquals(1, awaitItem())
        }
        repeat(100) { reactor.send(it) }
        // destroy()しないとsend()のlaunchが通常のDispatchers.Mainになるためクラッシュ
        // destroy()しないと無限ストリームのcollectがあるので無限に待つかと思ったけどそんなことはなかった。idleにはなるから？
//        reactor.destroy()
        // もしくはadvanceUntilIdle()することで通常のDispatchers.MainにDispatchされるのを防げる
        advanceUntilIdle()
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testReactorWithDsl() = runReactorTest {
        val reactor = Reactor()
//        val reactor = Reactor(Dispatchers.Main)
//        val reactor = Reactor(Dispatchers.Default)
//        val reactor = Reactor(coroutineContext + Dispatchers.Main.immediate)
//        val reactor = Reactor(StandardTestDispatcher())
        reactor.state.test {
            println("state.test start")
            assertEquals(0, awaitItem())
            println("send 1 before")
            reactor.send(1)
            println("send 1")
            assertEquals(1, awaitItem())
            println("state.test end")
        }
        repeat(10) { reactor.send(it) }
//        reactor.destroy() // 必要に応じて
    }

    @OptIn(ExperimentalStdlibApi::class)
//    @Test
    fun testReactorRepeat() {
        repeat(10) { testReactor() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testReactorRepeatWithDsl() {
        repeat(10) { testReactorWithDsl() }
    }

    @Test
    fun testThreadName() = runTest {
        println(currentThreadName())
        withContext(Dispatchers.Default) {
            println(currentThreadName())
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun runReactorTest(block: suspend CoroutineScope.() -> Unit) {
    val dispatcher = StandardTestDispatcher()
    runTest(dispatcher, 1000) {
        Dispatchers.setMain(dispatcher)
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

class Reactor {
    private val _action = Channel<Int>(UNLIMITED)
    private val _state = MutableStateFlow(0)
    val state: Flow<Int> get() = _state
    private val scope = MainScope()

    init {
        scope.launch {
            state.collect { println("new: $it") }
        }
        scope.launch {
            println("launched")
            try {
                _action.receiveAsFlow()
                    .onEach {
                        withContext(Dispatchers.Default) {
                            println("delay")
                            delay(1)
                        }
                    }
                    .collect { value ->
                        _state.update { it + value }
                    }
            } catch (e: CancellationException) {
                println("cancelled")
                throw e
            }
        }
    }

    fun send(action: Int) {
        println("received $action")
        scope.launch {
            _action.send(action)
        }
    }

    fun destroy() {
        scope.cancel()
    }
}
