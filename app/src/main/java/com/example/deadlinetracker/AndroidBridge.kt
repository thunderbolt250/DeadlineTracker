package com.example.deadlinetracker

import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

class AndroidBridge(private val db: DatabaseHelper, private val webView: WebView) {

    @JavascriptInterface
    fun saveTask(taskJson: String) {
        try {
            db.saveTask(JSONObject(taskJson))
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
            db.updateTask(JSONObject(taskJson))
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

    @JavascriptInterface
    fun scheduleTaskReminder(taskId: Int, dueMillis: Long) {
        try {
            val tasks = db.getAllTasks()
            for (i in 0 until tasks.length()) {
                val t = tasks.getJSONObject(i)
                if (t.getInt("id") == taskId) {
                    NotificationScheduler.scheduleTaskReminder(
                        webView.context,
                        taskId,
                        t.getString("name"),
                        t.getString("course"),
                        dueMillis
                    )
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun cancelTaskReminder(taskId: Int) {
        NotificationScheduler.cancelTaskReminder(webView.context, taskId)
    }

    @JavascriptInterface
    fun shareText(content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, "Deadline Tracker Backup")
        }
        val chooser = Intent.createChooser(sendIntent, "Export backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        webView.context.startActivity(chooser)
    }
}
