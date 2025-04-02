package com.example.qrcodescanner

import android.content.Context
import android.content.Intent
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import okhttp3.MultipartBody.Companion.FORM
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.zxing.client.android.Intents.Scan
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var scanQrBtn: MaterialCardView
    private lateinit var viewAll: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanQrBtn = findViewById(R.id.scanQrBtn)
        viewAll = findViewById(R.id.viewAll)

        onCLicks()
    }

    private fun onCLicks() {
        scanQrBtn.setOnClickListener {
            scannerLauncher.launch(
                ScanOptions().setPrompt("Scan Qr Code")
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            )
        }
        viewAll.setOnClickListener {
            startActivity(Intent(this, AllScannedUsers::class.java))
        }
    }

    private val scannerLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        } else {
            // Toast.makeText(this, result.contents.slice(13..result.contents.length-12), Toast.LENGTH_LONG).show()
            HttpRequest(this, result.contents)
        }
    }

    fun HttpRequest(context: Context, data: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(FORM)
                    .addFormDataPart("data", data)
                    .build()

                val request = Request.Builder()
                    .url("https://m.oussamanh.com/api/validate-iftar")
                    .addHeader("Accept", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.code == 422) {
                        showCustomToast(context, "Ce QR n'est pas valable pour aujourd'hui", false)
                        Log.d("HTTP_ERROR", "Request failed: ${response.code}")
                    } else if (response.code == 404) {
                        showCustomToast(context, "L'utilisateur n'est pas enregistré", false)
                        Log.d("HTTP_ERROR", "Request failed: ${response.code}")
                    } else if (response.code == 400) {
                        showCustomToast(context, "Format de données QR non valide", false)
                        Log.d("HTTP_ERROR", "Request failed: ${response.code}")
                    } else if (response.code == 409) {
                        showCustomToast(context, "Ce code QR a déjà été validé", false)
                        Log.d("HTTP_ERROR", "Request failed: ${response.code}")
                    } else {
                        showCustomToast(context, "Le rapas a été validé avec succès", true)
                        Log.d("HTTP_SUCCESS", "Response: ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.d("ICHOUOOOOOOOOOOOO coroutine problem", e.printStackTrace().toString())
                e.printStackTrace().toString()
            }
        }
    }

    fun showCustomToast(context: Context, message: String, isSuccess: Boolean) {
        val layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.toast_layout, null)

        // Set message text
        val textView: TextView = layout.findViewById<TextView>(R.id.toastText)
        textView.text = message

        // Set icon
        // Set icon
        val icon: ImageView = layout.findViewById(R.id.toastIcon)
        val backgroundLayout: LinearLayout = layout.findViewById<LinearLayout>(R.id.toast_container)

        if (isSuccess) {
            icon.setImageResource(R.drawable.valide) // ✅ Success icon
            backgroundLayout.setBackgroundColor(resources.getColor(R.color.validatedToast)) // Green background
            textView.setTextColor(resources.getColor(R.color.white))
        } else {
            icon.setImageResource(R.drawable.error) // ❌ Error icon
            backgroundLayout.setBackgroundColor(resources.getColor(R.color.unvalidatedToast)) // Red background
            textView.setTextColor(resources.getColor(R.color.white))
        }

        // Create and show Toast
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_LONG
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100) // Moves it down a bit
        toast.view = layout
        toast.show()
    }

    fun showToast(context: Context, message : String){
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}