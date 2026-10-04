# GeoTrack — Offline-First Android Geofencing & Attendance Tracking Engine

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_14+_Ready-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Database-Room_SQLite-00599C?style=for-the-badge&logo=sqlite&logoColor=white" />
  <img src="https://img.shields.io/badge/Backend-Cloud_Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/Maps-MapTiler_%26_OSMDroid-22C55E?style=for-the-badge&logo=openstreetmap&logoColor=white" />
</p>

---

## Overview

**GeoTrack** is a high-performance, offline-first Android location tracking and automated workforce attendance engine built with **Jetpack Compose**, **Google Play Services Location API**, **Room SQLite**, and **Firebase Cloud Firestore**. 

It provides seamless geofencing boundary monitoring, zero-battery-drain background transitions, real-time interactive mapping with MapTiler, Wi-Fi network validation for anti-spoofing, and automated background synchronization via Jetpack WorkManager.

---

## 🚀 Key Features

### 1. Automated Geofence Engine & Boundary Monitoring (`Geofence`)
- **Hardware-Level Geofencing**: Powered by Google Play Services `GeofencingClient` for ultra-low battery footprint enter, exit, and dwell triggers.
- **Dynamic Office / Work Zone Radius**: Real-time configurable geofence zones with precision distance calculations.
- **Automated Check-In / Check-Out**: Automatically records check-in upon entering the zone and checks out upon exit with calculated shift duration.
- **Reboot Resilience (`BootReceiver`)**: Automatically re-registers active geofences and tracking policies on device restart.

### 2. High-Accuracy Live Location & Map Rendering (`Tracking`)
- **Foreground Tracking Service**: Compliant with **Android 14+** (`FOREGROUND_SERVICE_TYPE_LOCATION`) for reliable background GPS telemetry.
- **Zero-Google-Billing Map Rendering**: Integrated **OSMDroid** with custom **MapTiler** vector & satellite tile providers.
- **Live Location Telemetry**: Real-time coordinate inspection, dynamic accuracy indicators, and auto-centering map controls.
- **Persistent Status Notification**: Rich notification channel with live tracking indicators and one-tap manual status overrides.

### 3. Multi-Layer Anti-Spoofing & Network Verification (`Security`)
- **Office Wi-Fi BSSID / SSID Validation**: Verifies connected access points to eliminate GPS spoofing and mock-location bypasses.
- **OEM Battery Optimization Bypasser (`AutoStartHelper`)**: Direct guidance for Xiaomi, Samsung, Oppo, Vivo, and OnePlus devices to prevent background process killing.

### 4. Offline-First Architecture & Automated Cloud Sync (`Sync`)
- **Room SQLite Local Store**: Complete offline data availability for all attendance logs, session intervals, and timestamps.
- **Background Cloud Sync (`WorkManager` + `Firestore`)**: Periodic and event-driven synchronization ensures zero data loss during network dropouts.
- **Sync Conflict Resolution**: Local pending records are queued and reconciled seamlessly against Firebase Cloud Firestore once connectivity is restored.

### 5. Interactive Dashboard & Attendance Telemetry (`UI / UX`)
- **Live Status Matrix**: Real-time visual cards for tracking state, inside/outside zone indicators, and current session duration.
- **Shift History & Activity Logs**: Searchable, chronological log viewer with sync badges (`Synced` vs `Pending`), date filters, and total hours tally.
- **Material 3 Dynamic Theming**: Sleek, modern, high-contrast dark and light themes crafted purely in Jetpack Compose.

### 6. Authentication & User Profile Management (`Auth`)
- **Firebase Authentication**: Robust Email/Password authentication and Google Sign-In support.
- **User-Isolated Storage**: Strict per-user UUID scoping across Room database tables and Firestore document trees.

---

## 🛠 Tech Stack & Architecture

| Layer | Technology | Details |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern concise syntax, Coroutines & Flow |
| **UI Framework** | Jetpack Compose (Material 3) | 100% declarative UI with reactive state |
| **Local Persistence** | Room SQLite | DAO patterns, transactional writes, Flow streams |
| **Cloud Backend** | Firebase Cloud Firestore | Real-time NoSQL cloud database & security rules |
| **Authentication** | Firebase Auth | Secure user session management |
| **Location Services** | Google Play Services Location | Fused Location Provider & Geofencing Client |
| **Map Rendering** | OSMDroid + MapTiler | Custom tile overlays and interactive markers |
| **Background Sync** | Jetpack WorkManager | Battery-conscious background synchronization |
| **Architecture** | MVVM / Clean Architecture | Unidirectional data flow (UDF) & separation of concerns |
| **Build System** | Gradle 8.x+ (Kotlin DSL `.kts`) | Android Gradle Plugin (AGP) 8.x with Java 21 |

---

## 📂 Project Structure
