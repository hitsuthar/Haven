package com.hitsuthar.june

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class BinaryService : Service() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MyBinaryService", "Binary started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}