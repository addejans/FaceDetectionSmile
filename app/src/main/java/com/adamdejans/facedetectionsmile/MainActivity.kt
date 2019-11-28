package com.adamdejans.facedetectionsmile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var cameraButton: Button
    private val REQUEST_IMAGE_CAPTURE = 123 //needs to be unique
    private lateinit var image: FirebaseVisionImage
    private lateinit var detector: FirebaseVisionFaceDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        cameraButton = findViewById(R.id.camera_button)

        cameraButton.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                var takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE) //TODO: Look at different types from `MediaStore.`
                if (takePictureIntent.resolveActivity(packageManager) != null){
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) //TODO: Can we remove this line somehow? You can in Java
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) { //TODO: Check for other activities the user may do
            // image passed to library in bitmap format (note: bitmap is a collection of data, so must be bundled)
            var extras = data?.extras
            var bitmap = extras?.get("data") as Bitmap
            detectFace(bitmap)
        }
    }

    private fun detectFace(bitmap: Bitmap) { //TODO: Look at adding more options
        var options =
            FirebaseVisionFaceDetectorOptions.Builder()
                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.15f)
                .setTrackingEnabled(true)
                .build()

        try {
            image = FirebaseVisionImage.fromBitmap(bitmap)
            detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        detector.detectInImage(image)
            .addOnSuccessListener(object: OnSuccessListener<List<FirebaseVisionFace>> {
                override fun onSuccess(firebaseVisionFaces: List<FirebaseVisionFace>?) {
                    var resultText = ""
                    var i = 1
                    if (firebaseVisionFaces != null) { //Kotlin was complaining for a null check
                        for(face in firebaseVisionFaces){
                            resultText = resultText.plus("\n"+i+".")
                                .plus("\nSmile: " + face.getSmilingProbability()*100+"%")
                                .plus("\nLeftEye: " + face.getLeftEyeOpenProbability()*100 + "%")
                                .plus("\nRightEye: " + face.getRightEyeOpenProbability()*100 + "%")
                            i++
                        }
                    }

                    if (firebaseVisionFaces != null) { //Kotlin was complaining for a null check
                        if (firebaseVisionFaces.isEmpty()){
                            Toast.makeText(this@MainActivity, "NO FACES DETECTED", Toast.LENGTH_SHORT).show()
                        } else{
                            val bundle = Bundle()
                            bundle.putString(LCOFaceDetection.RESULT_TEXT, resultText)
                            val resultDialog = ResultDialog()
                            resultDialog.arguments = bundle
                            resultDialog.isCancelable = false
                            resultDialog.show(supportFragmentManager, LCOFaceDetection.RESULT_DIALOG)
                        }
                    }
                }
            })
    }
}
