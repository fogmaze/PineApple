package com.example.pineapple.pose

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.Size
import androidx.core.app.NotificationCompat
import com.example.pineapple.utils.CameraHolder
import com.example.pineapple.R
import com.example.pineapple.utils.Server
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class PoseService : Service() {
    private var mServer : Server? = null
    private var mSocket : Socket? = null
    private var mResultLock = Object()
    private var mCameraHolder : CameraHolder? = null

    init {
        if (!OpenCVLoader.initDebug()) {
            Log.i("main","OpenCV not loaded")
        } else {
            Log.i("main","OpenCV loaded")
        }
    }

    override fun onBind(intent: Intent) : IBinder? {
        return null
    }

    private inner class SocketInputHandler(val input : InputStream) : Runnable {
        override fun run(){
            val buffer = ByteArray(1024)

            while (true) {
                try{
                    if (mSocket?.isClosed == true) {
                        Log.i("main", "socket closed")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        break
                    }
                    val read = input.read(buffer)
                    val message = String(buffer, 0, read)
                    // sleep for 100 ms
                    if (message == "stop") {
                        Log.i("main", "socket closed")
                        mSocket?.close()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        break
                    }
                    Thread.sleep(100)
                }catch(e: IOException){
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class SocketOutputHandler(val output : OutputStream) : Runnable {
        override fun run(){
            while (true) {
                try{
                    if (mSocket?.isClosed == true) {
                        Log.i("main", "socket closed")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        break
                    }
                    synchronized(mResultLock) {

                    }
                    Thread.sleep(100)
                }catch(e: IOException){
                    e.printStackTrace()
                }
            }
        }
    }

    var ind = 0


    override fun onCreate(){

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel("pc", "Pvc", NotificationManager.IMPORTANCE_DEFAULT)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        notificationManager.createNotificationChannel(notificationChannel)
        val notification = NotificationCompat.Builder(this, "pc")
            .setContentTitle("Pvc")
            .setContentText("Pvc is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(1, notification)
        Log.i("main", "PrivacyService created")

        val onConnectCallBack = {
                socket : Socket ->
            Thread(SocketInputHandler(socket.getInputStream())).start()
            Thread(SocketOutputHandler(socket.getOutputStream())).start()
            mSocket = socket
            false
        }
        mServer = Server(onConnectCallBack, 7891)
        Thread(mServer).start()
        Detector(this, object : Detector.OnDetectedInfoListener {
            override fun onDetectedInfo(score: Float, classificationResult: List<Pair<String, Float>>?) {
                synchronized(mResultLock) {
                }
            }
        }).also {
            mCameraHolder = CameraHolder(
                this,
                Size(640, 480),
                it
            )
            mCameraHolder?.startCapture()
        }
    }

    override fun onDestroy() {
        mCameraHolder?.stop()
        Thread.sleep(100)
        Log.i("main", "PrivacyService destroyed")
    }
}
