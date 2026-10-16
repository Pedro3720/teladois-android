package com.pedro3720.teladois

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun textGreetsInPortuguese() {
        assertEquals("Olá, TelaDois!", Greeting.text())
    }
}
