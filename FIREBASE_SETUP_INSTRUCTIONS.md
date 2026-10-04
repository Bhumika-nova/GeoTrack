# GeoTracker — Firebase Setup & Linking Instructions Guide

This guide is for the owner/administrator of the Firebase project (**`geotrack-c38f9`**) to properly link their Firebase project with the Android application and enable **Google Sign-In**, **Email/Password Authentication**, and **Firestore Attendance Synchronization**.

---

## 1. Firebase Project Overview

* **Firebase Project ID:** `geotrack-c38f9`
* **Package Name:** `com.example.geotrack`
* **Web Client ID:** `237321261644-as6smuo2l2bg6suj5ksvo85jtoh6g73d.apps.googleusercontent.com`
* **Firestore Collections Used:**
  * `attendance` — Stores employee attendance records (check-in, check-out, duration, Wi-Fi SSID, timestamps).
  * `employees` — Stores registered user profiles.

---

## 2. Linking SHA-1 Fingerprints for Google Sign-In

For Google Sign-In to succeed without throwing **`ApiException: 10`** (CommonStatusCodes.DEVELOPER_ERROR), the SHA-1 fingerprints of every computer building the debug APK must be added in the Firebase Console.

### Step-by-Step Instructions:

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Select the project **`geotrack-c38f9`**.
3. In the left sidebar, click the **Settings (gear)** icon next to **Project Overview** → select **Project settings**.
4. In the **General** tab, scroll down to the **Your apps** section.
5. Select the Android app with package name **`com.example.geotrack`**.
6. Under **SHA certificate fingerprints**, click **Add fingerprint**.
7. Enter the SHA-1 fingerprint:
   * **Testing Device / Collaborator Fingerprint:**
     ```
     16:CA:D4:FD:1E:C5:EE:76:11:B7:17:52:AF:D8:C1:D1:21:12:9D:81
     ```
   * **Your Own Local Fingerprint (if building on your PC):**
     Run this command in terminal/PowerShell:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
     Copy the `SHA1:` line and add it as an additional fingerprint.
8. Click **Save**.
9. Download the updated **`google-services.json`** from the same page and place it inside the `app/` directory of the project:
   ```
   GeoTracker/app/google-services.json
   ```

---

## 3. Enable Authentication Sign-In Methods

In the Firebase Console:
1. Navigate to **Build** → **Authentication** → **Sign-in method** tab.
2. Ensure the following providers are enabled:
   * **Email/Password**: Enabled (Email link optional).
   * **Google**: Enabled (select support email and save).

---

## 4. Firestore Security Rules

To ensure attendance records and employee profiles sync seamlessly while keeping the database secure, set the following security rules in **Firestore Database** → **Rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Allow authenticated employees to read and write attendance records
    match /attendance/{documentId} {
      allow read, write: if request.auth != null;
    }

    // Allow authenticated users to manage their employee profile
    match /employees/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Click **Publish**.

---

## 5. How the Attendance & Sync Flow Works

1. **Authentication:**
   * Users can register/log in via **Email & Password** or **Continue with Google**.
   * Both methods authenticate through `FirebaseAuth` and store credentials in local storage.

2. **Automated Geofencing Check-In:**
   * When an employee enters the geofence perimeter (150m) and is connected to the designated office Wi-Fi, the app initiates an in-office session.
   * Duplicate sessions are prevented via atomic database queries.

3. **Background Persistence:**
   * A persistent foreground service (`LocationForegroundService`) maintains presence tracking even if the app is removed from Recents.

4. **Automated Check-Out & Cloud Sync:**
   * When the employee exits the geofence, the session ends automatically.
   * `SyncWorker` (Jetpack WorkManager) pushes the completed session to the Firestore `attendance` collection upon internet connectivity.
   * The status badge updates from **"Pending Sync"** to **"Synced"**.

---

## 6. Switching from Testing to Production Office Credentials

Before deploying to production, update these values in `app/src/main/java/com/example/geotrack/utils/Constants.kt`:

* **`GEOFENCE_LAT`** and **`GEOFENCE_LON`**: Replace test coordinates with the physical office coordinates.
* **`OFFICE_WIFI_SSID`**: Replace test Wi-Fi name with the official office Wi-Fi network SSID.
* **`MAPTILER_API_KEY`**: Ensure your MapTiler cloud API key is set.
