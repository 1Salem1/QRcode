package com.example.testocr

import android.Manifest.permission.*
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.print.PrintHelper
import com.bumptech.glide.Glide
import com.example.testocr.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.*
import java.io.File.separator
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var textRecognizer: TextRecognizer

    private var QRCODE = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&bgcolor=255-255-255&data="

    private var imagePath = ""
    private var imageName = ""

    private lateinit var scaledbmp: Bitmap

    private var storageRef = Firebase.storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getpermission = Intent()
                getpermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getpermission)
            }
        }

        binding.btnCamera.setOnClickListener {
            cameraPermissions()
        }

        binding.btnGallery.setOnClickListener {
            storagePermissions()
        }

        binding.btnUpload.setOnClickListener {
            saveQRCode()
        }

        binding.btnPrint.setOnClickListener {
            print()
        }

        binding.btnPdf.setOnClickListener {
            generatePDF()
        }

        binding.btnGenerateBmp.setOnClickListener {
            binding.imgQrcode.drawable?.let {
                scaledbmp = (it as BitmapDrawable).bitmap
            }
        }

    }

    private fun generatePDF(){
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val title = Paint()
        val mypageInfo = PageInfo.Builder(400, 700, 1).create()
        val myPage = pdfDocument.startPage(mypageInfo)
        val canvas = myPage.canvas
        canvas.drawBitmap(scaledbmp, 50f,50f, paint)
        title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        title.textSize = 15f
        title.color = ContextCompat.getColor(this, R.color.purple_200)
        canvas.drawText("A portal for IT professionals.", 209f, 100f, title)
        canvas.drawText("Geeks for Geeks", 209f, 80f, title)
        title.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
        title.color = ContextCompat.getColor(this, R.color.purple_200)
        title.textSize = 15f
        title.textAlign = Paint.Align.CENTER
        canvas.drawText("This is sample document which we have created.", 396f, 560f, title)
        pdfDocument.finishPage(myPage)

        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .toString() + separator
        val file = File(path)
        file.mkdir()
        val timeStamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH).format(Date())
        val fileName = "Oribiky_$timeStamp.pdf"
        val request = File(path + separator + fileName)
        try {
            val writer = FileWriter(request)
            pdfDocument.writeTo(FileOutputStream(request))
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e("error", "${e.message}")
        }


        /*/val file = File(Environment.DIRECTORY_DOWNLOADS, "GFG.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(
                this@MainActivity,
                "PDF file generated successfully.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            Log.e("error", "${e.message}")
        }*/
        pdfDocument.close()
    }

    private fun print(){
        binding.imgQrcode.drawable?.let {
            val mBitmap = (it as BitmapDrawable).bitmap
            val photoPrinter = PrintHelper(this)
            photoPrinter.scaleMode = PrintHelper.SCALE_MODE_FIT
            photoPrinter.printBitmap("print",mBitmap)
        }
    }


    private fun cameraPermissions() {
        if (ActivityCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA), 1)
        } else {
            cameraResult.launch(Intent(this, CameraViewActivity::class.java))
        }
    }

    private fun storagePermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
                2
            )
        } else {
            galleryResult.launch(
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraResult.launch(Intent(this, CameraViewActivity::class.java))
                } else {
                    Toast.makeText(this, "Camera permission requested", Toast.LENGTH_LONG).show()
                }
            }
            2 -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    galleryResult.launch(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                    )
                } else {
                    Toast.makeText(this, "Storage permission requested", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val cameraResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {

                val text = result.data!!.getStringExtra("text")

                binding.tvText.text = text

                Glide.with(this)
                    .load("$QRCODE$text")
                    .into(binding.imgQrcode)

            }
        }

    private val galleryResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data!!.data != null) {
                val imageUri = result.data!!.data
                try {
                    val imageStream: InputStream? = contentResolver.openInputStream(imageUri!!)
                    val bitmap = BitmapFactory.decodeStream(imageStream)
                    val inputImage: InputImage =
                        InputImage.fromBitmap(bitmap, fixRotation(imageUri))
                    imageRecognition(inputImage)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    private fun fixRotation(uri: Uri): Int {
        val ei: ExifInterface
        var fixOrientation = 0
        try {
            val input = contentResolver.openInputStream(uri)
            ei = ExifInterface(input!!)
            val orientation: Int = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            fixOrientation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 80
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                ExifInterface.ORIENTATION_NORMAL -> 0
                else -> 0
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fixOrientation
    }

    private fun imageRecognition(inputImage: InputImage) {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        textRecognizer.process(inputImage)
            .addOnSuccessListener {
                binding.tvText.text = it.text

                Glide.with(this)
                    .load("$QRCODE${it.text}")
                    .into(binding.imgQrcode)

            }
            .addOnFailureListener {
                Log.e("error", it.message.toString())
            }
    }

    private fun saveQRCode(){
        binding.imgQrcode.drawable?.let {
            val mBitmap = (it as BitmapDrawable).bitmap
            mBitmap.saveToGallery()
            uploadImage()
        }
    }

    //create file to save image from camera to gallery
    @Throws(IOException::class)
    private fun createImagineFile(): File {
        var path: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + separator.toString() + "OCRKotlin"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + separator.toString() + "OCRKotlin"
        }
        val outputDir = File(path)
        outputDir.mkdir()
        val timeStamp: String = SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(Date())
        val fileName = "OCRKotlin_$timeStamp.jpg"
        val image = File(path + separator.toString() + fileName)
        imagePath = image.absolutePath
        imageName = fileName
        Log.e("imagePath", imagePath)
        Log.e("imageAbsolute", image.absolutePath.toString())
        return image
    }

    //save bitmap to gallery
    private fun Bitmap.saveToGallery(): Uri? {
        val file = createImagineFile()
        if (Build.VERSION.SDK_INT >= 31) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, file.absolutePath)
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)

            val uri: Uri? =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(this, contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
                return uri
            }
        } else {
            saveImageToStream(this, FileOutputStream(file))
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            return Uri.fromFile(file)
        }

        return null
    }

    //save image to stream
    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //upload image
    private fun uploadImage() {
        val file = Uri.fromFile(File(imagePath))
        val fileName = imageName
        val imageURL = "/salem/$fileName"
        Log.e("name", imageURL)
        val riversRef = storageRef.child(imageURL)
        val uploadTask = riversRef.putFile(file)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            riversRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.e("success", "upload")
                Toast.makeText(this, "success upload", Toast.LENGTH_LONG).show()
            } else {
                Log.e("failed", "upload")
                Toast.makeText(this, "failed upload", Toast.LENGTH_LONG).show()
            }
        }
    }

}

