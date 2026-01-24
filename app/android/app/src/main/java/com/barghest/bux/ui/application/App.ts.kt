package com.barghest.bux.ui.application

import android.app.Application
import com.barghest.bux.data.sync.SyncWorker
import com.barghest.bux.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        // Schedule periodic sync
        SyncWorker.schedule(this)
    }
}
