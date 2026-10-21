# AI Limits ⚡ Android Quota & Reset Tracker

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](#)
[![Size](https://img.shields.io/badge/APK%20Size-~58%20KB-blue)](#)
[![Zero Bloat](https://img.shields.io/badge/Dependencies-Zero%20(Pure%20Native%20Java)-success)](#)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](#)

A fast, lightweight, and completely private native Android application for tracking AI quota limits, rolling resets, and token consumption across **Claude Pro (Anthropic)**, **Google Cloud (Gemini 1.5 Pro / Flash)**, and other AI services.

Tested and optimized for **Volla Phone (Volla OS / LineageOS / Android 14)** with true OLED battery-saving mode.

---

## 🌟 Highlights & Features

### 1. ⏳ Dual-View System: Time-Horizon vs Provider
* **Zeitfenster-Ansicht (Wochenlimits zuerst):** Aggregates all weekly limits across all AI models (Claude 3.5 Sonnet 7d, Gemini 1.5 Flash, Gemini Pro) to show your total remaining weekly capacity at a glance, followed by immediate 5-hour rolling working windows and daily midnight quotas.
* **Nach Anbieter (Provider View):** Classic grouped view sorting limits by provider cards (Claude Pro, Google Cloud, Antigravity Desktop Bridge).

### 2. 🌐 100% Standalone Cloud-Direct Integration (No PC Required)
* **Claude Pro Cloud Direct:** Connects directly via OAuth to Anthropic (`api.anthropic.com/api/oauth/usage`) with 5-hour limit, 7-day limit, and 7-day usage breakdown (Claude Code, Chats, Cowork).
* **Google Cloud Direct:** Connects directly to Google OAuth (`oauth2.googleapis.com`) with user profile verification (`googleapis.com/oauth2/v3/userinfo`) and Cloud Resource Manager project inspection (`cloudresourcemanager.googleapis.com/v1/projects`).
* **Desktop Bridge (Optional):** Supports connecting to local Antigravity Language Server bridges when on a home network.

### 3. 🔔 Message Center & Smart Notifications
* **Modus wählbar:**
  * **Einzelbenachrichtigungen:** Sofortige Alarme pro Modell bei Voll-Reset oder Frühwarnung.
  * **Sammelbenachrichtigung (AI Digest):** Bündelt alle überwachten Modelle in eine kompakte Statusmeldung. Wählbarer Rhythmus (1h, 2h, 4h, täglich).
* **Frühwarn-System:** Automatischer Alarm, sobald ein Limit unter 15% fällt, um Workflow-Unterbrechungen zu vermeiden.
* **Ruhezeiten (Do Not Disturb):** Benachrichtigungen zwischen 23:00 und 07:00 Uhr stumm schalten.
* **Live-Ereignis-Feed:** In-App Protokoll der letzten 25 Benachrichtigungen und Reset-Ereignisse.

### 4. 📱 Home Screen Live-Widget
* Native `AppWidgetProvider` for the Android launcher.
* Displays Claude Pro & Google Cloud quotas with live percentages and reset countdowns directly on the home screen without opening the app.

### 5. ⏱️ 1-Tap 5-Hour Quick Resetter
* One tap to arm a 5-hour rolling timer with exact `AlarmManager` wake-up alarm and vibration when you hit a usage limit.

### 6. 💎 AI Value & Cost Savings Calculator
* Real-time calculation estimating equivalent API costs for tokens processed vs monthly subscription cost (e.g. Claude Code 97% weekly volume).

### 7. ⚡ Real-Time Burn Rate & Pace Indicator
* Dynamic badge indicating current usage pace (`🔥 Hoher Verbrauch`, `⚡ Tempo: Optimal & Ausgewogen`, `🟢 Volle Power`).

### 8. 🌑 Pure OLED Black Mode
* True `#000000` black background for OLED screens (e.g. Volla Phone) to turn off pixels and maximize battery life on the go.

### 9. 📋 One-Click Share & Export
* Generates a clean Markdown summary of all active AI limits formatted for clipboard sharing.

---

## 🔒 Privacy & Security

* **Zero telemetry, zero tracking, no third-party analytics.**
* All tokens and credentials are stored strictly on-device in Android's private `EncryptedSharedPreferences`.
* Never sends credentials anywhere other than the official Anthropic and Google endpoints.

---

## 🛠️ Building & Installation

The project uses a pure, ultra-fast toolchain script (`build.sh`) without Gradle overhead, producing a tiny ~58 KB APK in under 5 seconds.

### Prerequisites
* JDK 11 or newer (`javac`)
* Android SDK build-tools (version 30+)
* Android platform JAR (`android-34` recommended)

### Build
```bash
# Clone the repository
git clone https://github.com/diggaknoxville/ai-limits.git
cd ai-limits

# Build the signed APK
./build.sh
```
The compiled APK will be created at `build/ai-limits.apk`.

### Install via ADB
```bash
adb install -r build/ai-limits.apk
```

---

## ⚙️ Configuration

Credentials can be configured directly inside the app:
1. Tap the **Settings** (⚙️) icon in the top right corner.
2. Enter your **Claude Refresh Token** (`sk-ant-ort01-...`).
3. Enter your **Google Refresh Token** or credentials from Google Cloud Console.
4. Tap **Claude Cloud prüfen** or **Google Cloud prüfen** to verify live connection.

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.

