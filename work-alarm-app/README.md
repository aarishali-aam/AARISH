# ⏰ WorkAlarm - Android Work & Shift Alarm App

A modern, high-reliability Native Android application built with **Kotlin** and **Jetpack Compose (Material 3)** where you can **add custom works, jobs, shifts, meetings, and tasks**, and set tailored alarms with travel/wake-up lead times.

---

## 🌟 Key Features

1. **Add Custom "Works" with Automatic Alarms:**
   - Tap **`+ Add Work`** to add any work, job, shift, or task.
   - Enter **Work Title** (e.g. *Morning Warehouse Shift*, *Store Opening*, *Client Presentation*, *Field Inspection*).
   - Enter **Location / Workplace** (e.g. *Branch 4*, *Site B*, *Room 201*).
   - Choose **Work Hours** (Start Time & End Time).
   - Choose **Category**: Shift Job, Office, Meeting, Construction/Field, Delivery, Side Gig.
   - Set **Alarm Timing**:
     - *Exact Start Time* (0 mins)
     - *15, 30, 45, 60, 90, or 120 minutes before work starts* (prep & commute time).
   - Set **Repeat Days**: Mon-Fri, Weekends, Daily, or Custom Days.
   - Add **Notes & Checklist** (tools to bring, tasks to perform).
   - Check off works as **Completed** or toggle alarms on/off with one tap.

2. **Workplace Break & Focus Timers:**
   - Deep Focus Work (25m Pomodoro)
   - Micro-Breaks (5m)
   - Tea / Coffee Break (15m)
   - Meal / Lunch Break (45m with clock-in reminder)
   - Hourly Ergonomic Stretch (60m)
   - Ongoing background notification dial.

3. **Rock-Solid Android Alarm Reliability:**
   - Scheduled via `AlarmManager.setAlarmClock()`.
   - `WakeLock` & `AudioAttributes.USAGE_ALARM` ensure loud ringing and vibration.
   - Fullscreen `AlarmRingingActivity` wakes up locked screens (`setShowWhenLocked` & `setTurnScreenOn`).
   - Shows the Work's title, location, schedule, and notes right on the alarm ringing screen!
   - `BootReceiver` restores all alarms automatically when your phone reboots.

---

## 🛠️ How to Build the APK

### Method 1: Using Android Studio (Recommended)
1. Open **Android Studio**.
2. Click **Open** and select the folder:
   `C:\Users\USER\.gemini\antigravity\scratch\work-alarm-app`
3. Wait for Gradle sync to complete.
4. In the top menu, go to:
   **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. When complete, click **locate** in the popup to find:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer this `.apk` to your phone and install!

### Method 2: Command Line (Gradle)
```powershell
cd C:\Users\USER\.gemini\antigravity\scratch\work-alarm-app
.\gradlew assembleDebug
```
The APK will be generated at:
`app\build\outputs\apk\debug\app-debug.apk`
