package com.ahr.rashtube.data.profile

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ahr.rashtube.widget.core.RashWidgets
import com.ahr.rashtube.widget.core.refresh.WidgetContentSync
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the first process after a switch redoes: the home screen kept drawing the previous profile's widgets. It runs
 * from the Application, since the system may start this process for a service before any screen.
 */
@Singleton
class PostSwitchWork
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val widgetContentSync: WidgetContentSync,
    ) {
        suspend fun runIfSwitched() {
            if (!ActiveProfile.registry(context).finishPostSwitch()) return
            widgetContentSync.redrawAfterSwitch()
            RashWidgets.updateAll(context, except = widgetContentSync.widgets)
        }
    }
