package com.example.pineapple

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.pineapple.privacy.PrivacyService
import org.opencv.android.OpenCVLoader
import java.io.IOException
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private var mImageView : ImageView? = null
    private var mTextView : TextView? = null
    private var mServer : Server? = null
    private var wakeLock : PowerManager.WakeLock? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
        }
    }

    init {
        if (!OpenCVLoader.initDebug()) {
            Log.i("main","OpenCV not loaded")
        } else {
            Log.i("main","OpenCV loaded")
        }
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
                        intent = Intent(this@MainActivity, PrivacyService::class.java)
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        System.loadLibrary("opencv_java4")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mImageView = findViewById(R.id.imageView)
        mTextView = findViewById(R.id.textView)

        mTextView?.text = Utils.getIPAddress(true)

        /*mServer = Server( {
            Thread(SocketHandler(it)).start()
            true
        },7890)
        Thread(mServer).start()*/
        mServer = Server( {
            Thread(SocketHandler(it)).start()
            true
        },7890)
        Thread(mServer).start()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PineApple::MyWakelockTag")
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)

        val params = window.attributes
        params.screenBrightness = 0.0f
        window.attributes = params
    }

    override fun onStart() {
        super.onStart()
        Log.i("main","onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("main","onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i("main", "onPause")
    }


}