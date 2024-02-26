package com.example.pineapple.privacy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.ImageReader
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.Size
import androidx.core.app.NotificationCompat
import com.example.pineapple.R
import com.example.pineapple.Server
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlin.math.sqrt

class PrivacyService : Service() {
    private var mServer : Server? = null
    private var mSocket : Socket? = null
    private var mCameraHolder : CameraHolder? = null
    private var mNewRGBFrame : Mat? = null
    private var mBackgroundMat : Mat? = null
    private var mTempMat : Mat? = null
    private var mStateLock = Object()
    private var mState = 0
    private var mValue = 0

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
                    synchronized(mStateLock) {
                        output.write(if (mState == 0) "fine $mValue".toByteArray() else "danger $mValue".toByteArray())
                    }
                    Thread.sleep(100)
                }catch(e: IOException){
                    e.printStackTrace()
                }
            }
        }
    }

    var ind = 0
    private inner class ImgHandler : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader){
            val image = reader.acquireLatestImage()

            if (image != null) {
                ImageProcess.cvtJavaCamera2Mat(
                    image,
                    mNewRGBFrame
                )!!
                // convert bitmap to byte array
                if (mBackgroundMat == null) {
                    mBackgroundMat = mNewRGBFrame?.clone()
                }
                Core.bitwise_xor(mNewRGBFrame, mBackgroundMat, mTempMat)
                val mean = Core.mean(mTempMat)
                val value = sqrt(mean.`val`[0] * mean.`val`[0] + mean.`val`[1] * mean.`val`[1] + mean.`val`[2] * mean.`val`[2])
                Log.i("mean", "mean: $value")
                if (value < 35) {
                    synchronized(mStateLock) {
                        mState = 0
                        mValue = value.toInt()
                    }
                } else {
                    synchronized(mStateLock) {
                        mState = 1
                        mValue = value.toInt()
                    }
                }
                if (ind % 30 == 0) {
                    mBackgroundMat = mNewRGBFrame?.clone()
                }
                image.close()
                ind += 1
            }
        }
    }

    override fun onCreate(){
        mNewRGBFrame = Mat()
        mTempMat = Mat()

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
        mServer = Server(onConnectCallBack,7891)
        Thread(mServer).start()
        mCameraHolder = CameraHolder(
            this,
            Size(640, 480),
            ImgHandler()
        )
        mCameraHolder?.startCapture()
    }

    override fun onDestroy() {
        mCameraHolder?.stop()
        Thread.sleep(100)
        Log.i("main", "PrivacyService destroyed")
    }
}