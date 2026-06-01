package com.example

import android.app.AlertDialog
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = if (data != null) {
                val dataString = data.dataString
                val clipData = data.clipData
                if (clipData != null) {
                    Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                } else if (dataString != null) {
                    arrayOf(Uri.parse(dataString))
                } else {
                    null
                }
            } else {
                null
            }
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val wv = webView
                if (wv != null) {
                    wv.evaluateJavascript("if (window.handleAndroidBack) window.handleAndroidBack(); else false;") { result ->
                        val handled = result == "true"
                        if (!handled) {
                            finish()
                        }
                    }
                } else {
                    finish()
                }
            }
        })

        setContent {
            MyApplicationTheme {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            this@MainActivity.webView = this
                            // Settings
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                databaseEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            }
                            
                            // Clients
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onJsConfirm(
                                    view: WebView?,
                                    url: String?,
                                    message: String?,
                                    result: JsResult?
                                ): Boolean {
                                    AlertDialog.Builder(this@MainActivity)
                                        .setTitle("Confirmation")
                                        .setMessage(message)
                                        .setPositiveButton("Oui") { _, _ -> result?.confirm() }
                                        .setNegativeButton("Non") { _, _ -> result?.cancel() }
                                        .setOnCancelListener { result?.cancel() }
                                        .show()
                                    return true
                                }

                                override fun onJsAlert(
                                    view: WebView?,
                                    url: String?,
                                    message: String?,
                                    result: JsResult?
                                ): Boolean {
                                    AlertDialog.Builder(this@MainActivity)
                                        .setTitle("Message")
                                        .setMessage(message)
                                        .setPositiveButton("OK") { _, _ -> result?.confirm() }
                                        .setOnCancelListener { result?.cancel() }
                                        .show()
                                    return true
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                                    this@MainActivity.filePathCallback = filePathCallback
                                    
                                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "application/json"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                    try {
                                        fileChooserLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        filePathCallback?.onReceiveValue(null)
                                        this@MainActivity.filePathCallback = null
                                        return false
                                    }
                                    return true
                                }
                            }

                            // Add Javascript Bridge
                            addJavascriptInterface(QuizAppBridge(this@MainActivity), "QuizAppBridge")

                            // Load Asset Index
                            loadUrl("file:///android_asset/quizlet_clone.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                )
            }
        }
    }
}

class QuizAppBridge(private val activity: Activity) {
    @JavascriptInterface
    fun downloadJSON(json: String, filename: String) {
        activity.runOnUiThread {
            try {
                val tempFile = File(activity.cacheDir, filename)
                tempFile.writeText(json)
                val authority = "${activity.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(activity, authority, tempFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activity.startActivity(Intent.createChooser(intent, "Exporter le set de cartes"))
            } catch (e: Exception) {
                Toast.makeText(activity, "Erreur d'export: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}
