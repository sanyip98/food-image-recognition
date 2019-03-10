package com.raywenderlich.deliciousfood

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_logged_in.*
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionCloudImageLabelerOptions
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mindorks.paracamera.Camera
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_logged_in.*
import java.util.stream.Collectors
import java.util.stream.Stream


class LoggedInActivity: AppCompatActivity() {

    private lateinit
    var camera: Camera
    private val PERMISSION_REQUEST_CODE = 1

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    var fbAuth = FirebaseAuth.getInstance()
    private var fs: FirebaseStorage? = null
    private var sr: StorageReference? = null
    private var filePath: Uri? = null
    private var s: String? = null
    private var md: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle ? ) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged_in)
        setSupportActionBar(toolbar)

        fs = FirebaseStorage.getInstance()
        sr = fs!!.reference
//        md!!.keepSynced(true)

//        viewManager = LinearLayoutManager(this)
//        viewAdapter = MyAdapter(arrayOf("a", "b", "c"))
//
//        recyclerView = findViewById<RecyclerView>(R.id.my_text_view).apply {
//            // use this setting to improve performance if you know that changes
//            // in content do not change the layout size of the RecyclerView
//            setHasFixedSize(true)
//
//            // use a linear layout manager
//            layoutManager = viewManager
//
//            // specify an viewAdapter (see also next example)
//            adapter = viewAdapter
//
//        }

        fab.setOnClickListener {
            view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        var btnLogOut = findViewById < Button > (R.id.btnLogout)

        btnLogOut.setOnClickListener {
            view ->
            showMessage(view, "Logging Out...")
            signOut()
        }

        fbAuth.addAuthStateListener {
            if (fbAuth.currentUser == null) {
                this.finish()
            }
        }

        camera = Camera.Builder()
                .resetToCorrectOrientation(true) //1
                .setTakePhotoRequestCode(Camera.REQUEST_TAKE_PHOTO) //2
                .setDirectory("pics") //3
                .setName("delicious_${System.currentTimeMillis()}") //4
                .setImageFormat(Camera.IMAGE_JPEG) //5
                .setCompression(75) //6
                .build(this)
    }

    fun signOut() {
        fbAuth.signOut()

    }

    fun showMessage(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction("Action", null).show()
    }

    fun takePicture(view: View) {
        if (!hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                !hasPermission(android.Manifest.permission.CAMERA)) {
            // If do not have permissions then request it
            requestPermissions()
        } else {
            // else all permissions granted, go ahead and take a picture using camera
            try {
                camera.takePicture()
            } catch (e: Exception) {
                // Show a toast for exception
                Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            mainLayout.snack(getString(R.string.permission_message), Snackbar.LENGTH_INDEFINITE) {
                action(getString(R.string.OK)) {
                    ActivityCompat.requestPermissions(this@LoggedInActivity,
                            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
                }
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
            return
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this,
                permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array < String > , grantResults: IntArray) {
        when(requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        camera.takePicture()
                    } catch (e: Exception) {
                        Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                                Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent ? ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
                val bitmap = camera.cameraBitmap
                if (bitmap != null) {
                    imageView2.setImageBitmap(bitmap)
                    detectDeliciousFoodOnCloud(bitmap)
                    loadDatabase(FirebaseDatabase.getInstance().reference)
                } else {
                    Toast.makeText(this.applicationContext, getString(R.string.picture_not_taken),
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

//    data class Salad(
//        val name: String = "",
//        val description: String = "",
//        var uuid: String = "")


    fun loadDatabase(firebaseData: DatabaseReference) {
        val availableSalads = hashMapOf(
                "Anna" to "Fresh and delicious",
                "Bell" to "Fresh and delicious",
                "Cat"  to "Fresh and delicious",
                "Diva" to "Fresh and delicious"
        )
        availableSalads.forEach {
            val key = firebaseData.child("users").push().key
            key?.let {uuid ->
//                it.uuid = uuid
                firebaseData.root.child("users").setValue(uuid)
//                firebaseData.root.child("users").child(uuid).setValue(it)
            }
        }
    }



    private fun displayResultMessage(hasDeliciousFood: Boolean) {
        responseCardView.visibility = View.VISIBLE

        if (hasDeliciousFood) {
            responseCardView.setCardBackgroundColor(Color.GREEN)
            responseTextView.text = getString(R.string.delicious_food)
        } else {
            responseCardView.setCardBackgroundColor(Color.RED)
            responseTextView.text = getString(R.string.not_delicious_food)
        }
    }

    private fun hasDeliciousFood(items: List < String > ): Boolean {
        for (result in items) {
            if (result.contains("Food", true))
                return true
        }
        return false
    }

    private fun detectDeliciousFoodOnDevice(bitmap: Bitmap) {
        //1
        progressBar.visibility = View.VISIBLE
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val options = FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.8f)
                .build()
        val detector = FirebaseVision.getInstance().getOnDeviceImageLabeler(options)

        //2
        detector.processImage(image)
                //3
                .addOnSuccessListener {

                    progressBar.visibility = View.INVISIBLE

                    if (hasDeliciousFood(it.map {
                                it.text
                            })) {
                        displayResultMessage(true)
                    } else {
                        displayResultMessage(false)
                    }

                } //4
                .addOnFailureListener {
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this.applicationContext, getString(R.string.error),
                            Toast.LENGTH_SHORT).show()

                }
    }


    private fun detectDeliciousFoodOnCloud(bitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val options = FirebaseVisionCloudImageLabelerOptions.Builder().build()
        val detector = FirebaseVision.getInstance()
                //1
                .getCloudImageLabeler(options)

        detector.processImage(image)
                .addOnSuccessListener {

                    progressBar.visibility = View.INVISIBLE

                    println(it.map { it.text })

                    //      md = FirebaseDatabase.getInstance().reference
                    //      md!!.child("images").child(uuid).child("tags").setValue(it.map{it.label.toString()})

//                    if (hasDeliciousFood(it.map { it.text })) {
//                        displayResultMessage(true)
//                    } else {
//                        displayResultMessage(false)
//                    }
                    responseCardView.visibility = View.VISIBLE

                    responseCardView.setCardBackgroundColor(Color.parseColor("#ffd1dc"))
                    responseTextView.text = "Tagged as: " + it.map { it.text }.toString().substring(1, it.map { it.text }.toString().length - 1)

                }
                .addOnFailureListener {
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this.applicationContext, getString(R.string.error),
                            Toast.LENGTH_SHORT).show()

                }
    }

}