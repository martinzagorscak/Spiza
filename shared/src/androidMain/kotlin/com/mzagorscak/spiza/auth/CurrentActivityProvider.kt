package com.mzagorscak.spiza.auth

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

interface CurrentActivityProvider {
    val current: Activity?
}

/**
 * Tracks the resumed Activity; Credential Manager needs one to show its UI.
 * Must be registered via Application.registerActivityLifecycleCallbacks in Application.onCreate.
 */
class CurrentActivityProviderImpl :
    CurrentActivityProvider,
    Application.ActivityLifecycleCallbacks {
    private var ref: WeakReference<Activity>? = null

    override val current: Activity? get() = ref?.get()

    override fun onActivityResumed(activity: Activity) {
        ref = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit
}
