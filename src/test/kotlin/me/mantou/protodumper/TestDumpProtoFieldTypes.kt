package me.mantou.protodumper

import me.mantou.client.ProtoFieldTypes
import java.nio.file.Path
import kotlin.test.Test

class TestDumpProtoFieldTypes {
    @Test
    fun testDump() {
        val protoDumper = ProtoDumper()
        protoDumper.init()
        protoDumper.addNeedDumpClass(ProtoFieldTypes::class.java)
        protoDumper.dumpTo(Path.of("remapped"))
    }
}