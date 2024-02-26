package com.example.pineapple

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.pineapple.privacy.PrivacyService
import java.io.IOException
import java.net.Socket

class MainService : Service() {
    private var mServer: Server? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private inner class SocketHandler(val socket: Socket) : Runnable {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            try {
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val buffer = ByteArray(1024)
                val read = input.read(buffer)
                val message = String(buffer, 0, read)
                var intent : Intent? = null
                var port : Int? = null

                when (message) {
                    "privacy" -> {
                        intent = Intent(this@MainService, PrivacyService::class.java)
                        port = 7891
                        startForegroundService(intent)
                    }

                }
                if (intent != null) {
                    startService(intent)
                    output.write(port.toString().toByteArray())
                }
                Log.i("main", "message: $message")
                socket.close()
            } catch (e: IOException) {

                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel("pineapple", "Pineapple", NotificationManager.IMPORTANCE_DEFAULT)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        notificationManager.createNotificationChannel(notificationChannel)
        val notification = NotificationCompat.Builder(this, "pineapple")
            .setContentTitle("Pineapple")
            .setContentText("Pineapple is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(1, notification)
        mServer = Server( {
            Thread(SocketHandler(it)).start()
            true
        },7890)
        Thread(mServer).start()
    }

}