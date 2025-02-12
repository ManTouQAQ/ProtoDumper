package me.mantou.protodumper

import me.mantou.client.ClientProto
import me.mantou.common.CommonProto
import me.mantou.server.ServerProto
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TestDumpMyProto {
    @Test
    fun testDump() {
        val protoDumper = ProtoDumper()
        protoDumper.init()
        protoDumper.addNeedDumpClass(ClientProto::class.java)
        protoDumper.addNeedDumpClass(CommonProto::class.java)
        protoDumper.addNeedDumpClass(ServerProto::class.java)
        protoDumper.dumpTo(Path.of("remapped"))
    }
}