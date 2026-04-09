package com.example.deadlinetracker

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var db: DatabaseHelper

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)

        // Schedule daily 8 AM notification
        NotificationHelper.createNotificationChannel(this)
        NotificationScheduler.scheduleDailyNotification(this)

        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
        }

        // Inject Android bridge
        webView.addJavascriptInterface(AndroidBridge(db, webView), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Push saved tasks into the page after it loads
                val tasks = db.getAllTasks()
                val json = tasks.toString()
                view?.evaluateJavascript("loadTasksFromAndroid($json);", null)
            }
        }

        webView.loadUrl("file:///android_asset/tracker.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}