# BRAHM CRM — Android app

Brahm InnerBrain Technologies Pvt. Ltd. · Developed by Sandeep Kushwah
Support: 7990918709 · versionName 1.1

A Material 3 mini-CRM for leads and customers:

- **Dashboard** — contacts / new / follow-up / open-task counts + pipeline
- **Leads (CRM)** — add leads, import phone contacts, edit, notes, delete
- **Board (Kanban)** — move leads through New → Follow-up → Closed
- **Inbox** — your own incoming notifications grouped per person (read-only)
- **AI reply** — Groq generates a reply *draft* you review, edit, copy or share
- **Settings** — one place for the Groq API key, feature on/off toggles,
  notification access, and customer-data controls

Side navigation drawer links all screens (mini-CRM lives in the side menu).

---

## Build the APK (Android Studio)

1. **Android Studio** → **Open** → choose this `BrahmCRM` folder.
2. Let Gradle sync finish (it downloads AGP 8.5.2, Kotlin 1.9.24, Compose, Room,
   Retrofit the first time). If prompted, install **SDK Platform 34**.
3. Plug in your phone (USB debugging on) **or** start an emulator.
4. Press **Run ▶**. The APK installs and opens on the device.

### Build an installable APK file to share
- Menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- The file lands in: `app/build/outputs/apk/debug/app-debug.apk`

### Command line (optional)
```bash
./gradlew assembleDebug        # Linux/Mac
gradlew.bat assembleDebug      # Windows
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

---

## First-run setup inside the app
1. **Settings → AI (Groq)** → paste your Groq API key (free from console.groq.com),
   leave the default model, **Save**.
2. **Settings → Permissions → Notification access → Grant** (only needed for the
   Inbox screen; it reads your own incoming notifications, read-only).
3. **Leads → Import phone contacts** to pull contacts in as leads.

## Notes
- `versionName` is kept at **1.1** as standard. `versionCode` is 1; raise it only
  when you ask for an update build.
- The API key is stored encrypted on the device (EncryptedSharedPreferences).
- The app never auto-sends anything — AI only drafts; you send manually.
- Min Android 7.0 (API 24), target Android 14 (API 34).
