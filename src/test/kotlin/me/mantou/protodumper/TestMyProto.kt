package me.mantou.protodumper

import me.mantou.client.ClientProto
import org.junit.jupiter.api.Test

class TestMyProto {
    @Test
    fun testClientProto() {
        val fileDescriptor = ClientProto.getDescriptor().file
        println(fileDescriptor.toString())
    }
}