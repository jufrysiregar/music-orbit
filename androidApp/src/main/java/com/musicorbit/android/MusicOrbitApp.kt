package com.musicorbit.android

import android.app.Application
import com.musicorbit.android.di.androidModule
import com.musicorbit.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MusicOrbitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MusicOrbitApp)
            modules(sharedModule, androidModule)
        }
    }
}
