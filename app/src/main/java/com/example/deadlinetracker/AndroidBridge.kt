package com.example.deadlinetracker

import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

class AndroidBridge(private val db: DatabaseHelper, private val webView: WebView) {

    @JavascriptInterface
    fun saveTask(taskJson: String) {
        try {
            val obj = JSONObject(taskJson)
            db.saveTask(obj)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun deleteTask(id: Int) {
        db.deleteTask(id)
    }

    @JavascriptInterface
    fun updateTask(taskJson: String) {
        try {
            val obj = JSONObject(taskJson)
            db.updateTask(obj)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun getAllTasks(): String {
        return db.getAllTasks().toString()
    }

    @JavascriptInterface
    fun getTodaySchedule(): String {
        return db.getTodaySchedule()
    }
}