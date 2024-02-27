package com.example.pineapple.pose

import android.content.Context
import android.graphics.Bitmap
import android.media.ImageReader
import com.example.pineapple.pose.data.Device
import com.example.pineapple.utils.YuvToRgbConverter
import com.example.pineapple.pose.data.Person
import com.example.pineapple.pose.ml.ModelType
import com.example.pineapple.pose.ml.MoveNet
import com.example.pineapple.pose.ml.PoseClassifier
import com.example.pineapple.pose.ml.PoseDetector

class Detector(
    private val context: Context,
    private val listener: OnDetectedInfoListener?
) : ImageReader.OnImageAvailableListener {
    private var rgbConverter: YuvToRgbConverter = YuvToRgbConverter(context)
    private final lateinit var imageBitmap: Bitmap
    private var detector: PoseDetector = MoveNet.create(context, Device.CPU, ModelType.Thunder)
    private var classifier: PoseClassifier = PoseClassifier.create(context)
    override fun onImageAvailable(reader: ImageReader?) {
        val image = reader?.acquireLatestImage()

        if (image != null) {
            if (!::imageBitmap.isInitialized) {
                imageBitmap = Bitmap.createBitmap(
                    image.width, image.height, Bitmap.Config.ARGB_8888
                )
            }
            rgbConverter.yuvToRgb(image, imageBitmap)

            val persons = mutableListOf<Person>()
            var classificationResult: List<Pair<String, Float>>? = null

            detector.estimatePoses(imageBitmap).let {
                persons.addAll(it)

                // if the model only returns one item, allow running the Pose classifier.
                if (persons.isNotEmpty()) {
                    classifier.run {
                        classificationResult = classify(persons[0])
                    }
                }
            }

            // if the model returns only one item, show that item's score.
            if (persons.isNotEmpty()) {
                listener?.onDetectedInfo(persons[0].score, classificationResult)
            }
            // convert bitmap to byte array

            image.close()
        }
        }

    interface OnDetectedInfoListener {
        fun onDetectedInfo(score: Float, classificationResult: List<Pair<String, Float>>?)
    }

}