package me.mantou.protodumper

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class TestDumpLunarProto {
    @Test
    fun testDump() {
        val protoDumper = ProtoDumper(File("lunar.jar"))
        protoDumper.init()
        protoDumper.dumpTo(Path.of("output"))
    }
}