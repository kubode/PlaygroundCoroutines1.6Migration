package com.github.kubode.coroutine16poc

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}