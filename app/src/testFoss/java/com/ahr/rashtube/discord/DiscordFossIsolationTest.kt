package com.ahr.rashtube.discord

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiscordFossIsolationTest {
    @Test
    fun `foss classpath excludes functional Discord implementation`() {
        val forbiddenClasses = listOf(
            "com.ahr.rashtube.discord.DiscordTokenStore",
            "com.ahr.rashtube.discord.DiscordAuthTokens",
            "com.ahr.rashtube.discord.DiscordPlaybackSource",
            "com.ahr.rashtube.discord.DiscordPresenceCoordinator",
            "com.ahr.rashtube.discord.KizzyDiscordPresenceTransport",
            "com.ahr.rashtube.discord.KizzyGatewayProtocol",
        )

        forbiddenClasses.forEach { className ->
            assertThat(runCatching { Class.forName(className) }.isFailure).isTrue()
        }
    }

    @Test
    fun `foss runtime reports Discord unavailable`() {
        assertThat(DiscordPresenceRuntime.settingsState.value.isAvailable).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.isEnabled).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.summary)
            .isEqualTo(DiscordSettingsSummary.UNAVAILABLE)
    }
}
