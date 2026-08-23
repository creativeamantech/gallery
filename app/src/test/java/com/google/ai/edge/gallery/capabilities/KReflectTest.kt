package com.google.ai.edge.gallery.capabilities
import org.junit.Test
import kotlin.reflect.full.memberFunctions
class KReflectTest {
    @Test fun test() {
        println(DummyMathTool::class.memberFunctions.map { it.name })
    }
}
