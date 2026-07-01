# DeadlineTracker — Claude Code Context

## What this project is
An Android app for CMU-Africa students to track assignment and exam deadlines. It is a **hybrid app**: the UI is a single HTML page (`tracker.html`) loaded inside a `WebView`, while Kotlin handles native features (SQLite storage, push notifications, Android share sheet). The JS and Kotlin sides communicate via a `JavascriptInterface` bridge.

## Project structure
```
app/src/main/
  assets/tracker.html          ← entire UI (HTML + CSS + JS, single file)
  java/com/example/deadlinetracker/
    MainActivity.kt            ← sets up WebView, loads tracker.html
    AndroidBridge.kt           ← JS interface exposed to the WebView as `Android`
    DatabaseHelper.kt          ← SQLite via SQLiteOpenHelper (table: tasks)
    NotificationScheduler.kt   ← schedules daily 8 AM alarm + per-task alarms
    NotificationReceiver.kt    ← handles BOOT_COMPLETED, task reminders, daily digest
    NotificationHelper.kt      ← creates the notification channel
  AndroidManifest.xml
```

## Architecture decisions
- **No external libraries** — pure Android SDK + WebView. No Retrofit, Room, etc.
- **Single HTML file** — all JS, CSS, and HTML live in `tracker.html`. Do not split into multiple asset files.
- **localStorage** for lightweight persistence (theme preference, courses list). SQLite for tasks.
- **`confirm()` dialogs are blocked by WebView** — never use `confirm()` or `alert()` for user decisions. Use custom HTML modals instead (alert() is acceptable only for simple validation errors).
- **Course list** is stored in `localStorage` under key `deadline_tracker_courses`, not in SQLite.

## Database schema (tasks table, version 2)
```sql
CREATE TABLE tasks (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  course TEXT NOT NULL,
  due TEXT,           -- ISO datetime string e.g. "2026-05-01T23:59", nullable
  type TEXT NOT NULL, -- "assignment" or "exam"
  done INTEGER NOT NULL DEFAULT 0,
  room TEXT,          -- exam room, nullable
  end_time TEXT       -- exam end time e.g. "11:30", nullable
)
```

## AndroidBridge JS interface (callable as `Android.*` in JS)
| Method | Description |
|---|---|
| `saveTask(taskJson)` | Insert or replace a task (uses CONFLICT_REPLACE) |
| `updateTask(taskJson)` | Update existing task by id |
| `deleteTask(id)` | Delete task by id |
| `getAllTasks()` | Returns JSON array string of all tasks |
| `getTodaySchedule()` | Returns urgent tasks string for daily notification |
| `scheduleTaskReminder(id, dueMillis)` | Schedule exact alarm 24h before due date |
| `cancelTaskReminder(id)` | Cancel a pending task alarm |
| `shareText(content)` | Open Android share sheet with text content |

## Notification system
- **Daily digest**: repeating alarm at 8 AM, request code `0`, lists tasks due within 4 days
- **Per-task reminder**: exact alarm 24h before due date, request code = task ID, action = `com.example.deadlinetracker.TASK_REMINDER`
- **Boot handling**: `BOOT_COMPLETED` reschedules both the daily alarm and all pending task alarms
- Notification IDs: daily digest = `1001`, per-task = `2000 + taskId`

## UI tabs
1. **Assignments** — tasks where `type === "assignment"`, grouped into Overdue / Upcoming / Completed
2. **Exams** — tasks where `type === "exam"`, same grouping, includes Room and End time fields
3. **Schedule** — 14-day suggested work schedule derived from upcoming tasks
4. **Courses** — manage course codes (stored in localStorage), includes Export backup button

## Styling conventions
- CSS custom properties for all colors — supports light and dark theme via `[data-theme="dark"]`
- Theme preference persisted to `localStorage` under key `theme`; restored via inline `<script>` in `<head>` before paint
- No emoji in UI unless user requests it
- Font: DM Sans (body), DM Mono (stat numbers)
- Primary accent color: `#3b65e8`
- Overdue tasks: red left border (`#ef4444`), light red background

## Things to never do
- Do not use `confirm()` in JS — WebView silently returns false
- Do not split `tracker.html` into multiple files
- Do not add external dependencies (no npm, no Gradle libs beyond existing)
- Do not seed default/sample tasks — the app starts empty on fresh install
- Do not add comments explaining what code does — only add comments for non-obvious WHY

## Key behaviours
- `nextId` starts at `1` and increments; `Math.max(...tasks.map(t => t.id)) + 1` on load from DB
- Course deletion is guarded — blocked if any tasks still reference that course
- Deleting a task also cancels its pending notification alarm
- Marking a task done cancels its alarm; un-marking it reschedules the alarm
- Edit modal closes when tapping the backdrop
- `isExam` is passed explicitly to `renderTaskList()` — do not infer it from `items.some()`
