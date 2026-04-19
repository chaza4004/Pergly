package com.example.pergly

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class ScanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val scannedValue = barcode.rawValue

                if (scannedValue.isNullOrBlank()) {
                    Toast.makeText(this, "QR code is empty", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val resultIntent = Intent().apply {
                    putExtra("PERGOLA_ID", scannedValue)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnCanceledListener {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Scan failed: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }
}