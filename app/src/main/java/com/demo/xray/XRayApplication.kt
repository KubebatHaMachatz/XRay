package com.demo.xray

import android.app.Application

class XRayApplication : Application() {
    val container by lazy { AppContainer() }

    override fun onCreate() {
        super.onCreate()
        // See PRD FR-142: remove abandoned temporary import files from a previous process death.
        cacheDir.listFiles { file -> file.name.startsWith(TEMP_IMPORT_PREFIX) }?.forEach { it.delete() }
    }

    companion object {
        const val TEMP_IMPORT_PREFIX = "xray-import-"
    }
}
