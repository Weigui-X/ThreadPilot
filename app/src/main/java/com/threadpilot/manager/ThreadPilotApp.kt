package com.threadpilot.manager

import android.app.Application
import com.threadpilot.manager.util.ErrorLogger

class ThreadPilotApp : Application() {

    lateinit var errorLogger: ErrorLogger
        private set

    override fun onCreate() {
        super.onCreate()
        errorLogger = ErrorLogger(this)
        errorLogger.install()
    }
}
