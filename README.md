# Task Reminder — Android App

A clean Android app (Java, minSdk 26) that keeps your open tasks visible and reminds you every morning with a notification **and** a full-screen popup.

---

## Features

| Feature | Description |
|---|---|
| ✅ Add / Edit / Delete tasks | Title + optional notes |
| ✅ Mark tasks as done | Checkbox with strike-through |
| ✅ Daily notification | Tappable, shows open task count |
| ✅ Full-screen morning popup | Opens automatically at your chosen time |
| ✅ Survives reboots | Alarm is rescheduled after device restart |
| ✅ Persistent storage | Room (SQLite) — no internet needed |

---

## Project structure

```
TaskReminder/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/taskreminder/
│       │   ├── data/
│       │   │   ├── Task.java            ← Room entity
│       │   │   ├── TaskDao.java         ← DB queries
│       │   │   ├── AppDatabase.java     ← Room singleton
│       │   │   └── AlarmScheduler.java  ← Schedule/cancel daily alarm
│       │   ├── receiver/
│       │   │   ├── AlarmReceiver.java   ← Fires notification + popup
│       │   │   └── BootReceiver.java    ← Reschedules after reboot
│       │   └── ui/
│       │       ├── MainActivity.java    ← Task list + alarm time picker
│       │       ├── TaskAdapter.java     ← RecyclerView adapter
│       │       ├── TaskPopupActivity.java ← Morning full-screen overlay
│       │       └── EditTaskActivity.java  ← Create / edit a task
│       └── res/
│           ├── drawable/ic_notification.xml
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_task_popup.xml
│           │   ├── activity_edit_task.xml
│           │   └── item_task.xml
│           ├── menu/main_menu.xml
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml
├── build.gradle
└── settings.gradle
```

---

## How to build & run

### Prerequisites
- **Android Studio Hedgehog (2023.1)** or newer
- **JDK 8+** (bundled with Android Studio)
- Android SDK platform **API 34**

### Steps

1. **Open the project**
   - Launch Android Studio → *File → Open* → select the `TaskReminder` folder.

2. **Let Gradle sync**
   - Android Studio will download Room, Material, and other dependencies automatically.
   - If prompted about SDK path, accept the defaults.

3. **Run on a device or emulator**
   - Connect an Android device (API 26+) or create an emulator.
   - Press **▶ Run** (Shift+F10).

4. **Grant permissions when prompted**
   - **Notifications** — required to show the daily reminder notification.
   - **Schedule exact alarms** — on Android 12+ the app may open the system settings page; toggle "Alarms & reminders" on for Task Reminder.

---

## First-time setup inside the app

1. **Add some tasks** using the **＋** FAB button.
2. **Set the daily reminder time** by tapping the **clock icon** (⏰) in the toolbar.
3. Pick your preferred time (e.g. 08:00) and tap OK.
4. The alarm banner at the top of the main screen confirms the scheduled time.

From that point on, every day at that time:
- A **notification** appears in your status bar — tap it to open the task list.
- The **popup screen** launches automatically (even from the lock screen).

---

## Notes & customisation tips

### Changing the popup behaviour
- `TaskPopupActivity` uses `android:showOnLockScreen="true"` and `android:turnScreenOn="true"` in the manifest, so it wakes the device. Remove those attributes if you'd rather only show the popup when the phone is already unlocked.

### Exact vs. inexact alarms (Android 12+)
- The app requests `SCHEDULE_EXACT_ALARM`. If the user denies this, it falls back to `setRepeating()`, which Android may delay by a few minutes — still more than precise enough for a morning reminder.
- To request the permission proactively, add this to `MainActivity.onCreate()`:
  ```java
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
      if (!am.canScheduleExactAlarms()) {
          Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
          startActivity(i);
      }
  }
  ```

### Notification channel
- The notification channel is named **"Daily Task Reminder"** and uses `IMPORTANCE_HIGH` so it appears as a heads-up banner. You can lower the importance in `AlarmReceiver.java` if you prefer a silent notification.

---

## Dependencies (all fetched automatically by Gradle)

| Library | Version | Purpose |
|---|---|---|
| androidx.room | 2.6.1 | Local SQLite database |
| com.google.android.material | 1.11.0 | Material 3 UI components |
| androidx.appcompat | 1.6.1 | Backwards-compatible activities |
| androidx.recyclerview | 1.3.2 | Scrollable task list |
| androidx.constraintlayout | 2.1.4 | Flexible layouts |
