package com.example.deadlinetracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "deadline_tracker.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                course TEXT NOT NULL,
                due TEXT,
                type TEXT NOT NULL,
                done INTEGER NOT NULL DEFAULT 0,
                room TEXT,
                end_time TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tasks")
        onCreate(db)
    }

    fun saveTask(task: JSONObject) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id",       task.getInt("id"))
            put("name",     task.getString("name"))
            put("course",   task.getString("course"))
            put("due",      if (task.isNull("due")) null else task.getString("due"))
            put("type",     task.getString("type"))
            put("done",     if (task.getBoolean("done")) 1 else 0)
            put("room",     if (task.has("room")) task.getString("room") else null)
            put("end_time", if (task.has("end")) task.getString("end") else null)
        }
        db.insertWithOnConflict("tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun updateTask(task: JSONObject) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name",     task.getString("name"))
            put("course",   task.getString("course"))
            put("due",      if (task.isNull("due")) null else task.getString("due"))
            put("done",     if (task.getBoolean("done")) 1 else 0)
            put("room",     if (task.has("room")) task.getString("room") else null)
            put("end_time", if (task.has("end")) task.getString("end") else null)
        }
        db.update("tasks", values, "id = ?", arrayOf(task.getInt("id").toString()))
        db.close()
    }

    fun deleteTask(id: Int) {
        val db = writableDatabase
        db.delete("tasks", "id = ?", arrayOf(id.toString()))
        db.close()
    }

    fun getAllTasks(): JSONArray {
        val result = JSONArray()
        val db = readableDatabase
        val cursor = db.query("tasks", null, null, null, null, null, "due ASC")
        cursor.use {
            while (it.moveToNext()) {
                val obj = JSONObject().apply {
                    put("id",     it.getInt(it.getColumnIndexOrThrow("id")))
                    put("name",   it.getString(it.getColumnIndexOrThrow("name")))
                    put("course", it.getString(it.getColumnIndexOrThrow("course")))
                    val due = it.getString(it.getColumnIndexOrThrow("due"))
                    if (due != null) put("due", due) else put("due", JSONObject.NULL)
                    put("type",   it.getString(it.getColumnIndexOrThrow("type")))
                    put("done",   it.getInt(it.getColumnIndexOrThrow("done")) == 1)
                    val room = it.getString(it.getColumnIndexOrThrow("room"))
                    if (room != null) put("room", room)
                    val end = it.getString(it.getColumnIndexOrThrow("end_time"))
                    if (end != null) put("end", end)
                }
                result.put(obj)
            }
        }
        db.close()
        return result
    }

    fun getTodaySchedule(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val tasks = getAllTasks()
        val urgent = mutableListOf<String>()

        for (i in 0 until tasks.length()) {
            val t = tasks.getJSONObject(i)
            if (t.getBoolean("done")) continue
            val due = if (t.isNull("due")) null else t.getString("due")
            due ?: continue
            // days left
            val dueDateStr = due.substring(0, 10)
            val dueDate = sdf.parse(dueDateStr) ?: continue
            val todayDate = sdf.parse(today) ?: continue
            val diffMs = dueDate.time - todayDate.time
            val daysLeft = (diffMs / 86400000).toInt()
            if (daysLeft in 0..4) {
                val label = when (daysLeft) {
                    0 -> "DUE TODAY"
                    1 -> "due tomorrow"
                    else -> "due in ${daysLeft}d"
                }
                urgent.add("• ${t.getString("name")} (${t.getString("course")}) — $label")
            }
        }
        return if (urgent.isEmpty()) "" else urgent.joinToString("\n")
    }
}