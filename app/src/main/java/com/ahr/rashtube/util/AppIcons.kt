package com.ahr.rashtube.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ahr.rashtube.R

/** A launcher alias: its manifest suffix, the name the picker shows and the adaptive icon it installs. */
data class LauncherIcon(
    val suffix: String,
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int,
)

object AppIcons {
    /** Component name prefix shared by every alias (the application id namespace). */
    const val NAMESPACE = "com.ahr.rashtube"

    /** The alias enabled by default in the manifest. Used as a safe fallback. */
    const val DEFAULT_SUFFIX = ".IconFlowRed"

    /**
     * Every launcher alias, in manifest declaration order. A shipped alias is never removed or renamed:
     * a user who picked it would be left with no launcher entry after the update. Retire an icon by
     * pointing its alias at new art instead.
     */
    val ALL: List<LauncherIcon> =
        listOf(
            LauncherIcon(".IconFlowRed", R.string.icon_name_flow_red, R.mipmap.ic_launcher),
            LauncherIcon(".IconFlowLight", R.string.icon_name_flow_light, R.mipmap.ic_launcher_flow_light),
            LauncherIcon(".IconAmoled", R.string.icon_name_amoled, R.mipmap.ic_launcher_amoled),
            LauncherIcon(".IconMonochrome", R.string.icon_name_monochrome, R.mipmap.ic_launcher_monochrome),
        )

    val ALL_SUFFIXES: List<String> = ALL.map { it.suffix }

    fun icon(suffix: String): LauncherIcon = ALL.firstOrNull { it.suffix == suffix } ?: ALL.first { it.suffix == DEFAULT_SUFFIX }
}
