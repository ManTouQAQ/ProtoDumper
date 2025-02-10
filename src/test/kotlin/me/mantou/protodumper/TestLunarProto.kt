package me.mantou.protodumper

import com.lunarclient.websocket.cosmetic.v1.CustomizableCosmeticSettings
import org.junit.jupiter.api.Test

class TestLunarProto {
    @Test
    fun test() {
        CustomizableCosmeticSettings.newBuilder().addActiveCosmeticIds(1).build()
    }
}