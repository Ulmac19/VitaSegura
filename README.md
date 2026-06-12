# VitaSegura 🩺📱

> Android health-monitoring app for elderly care, with a dual caregiver / elderly-user experience, real-time vitals from a BLE wearable, medication reminders, GPS tracking, and emergency alerts.

---

## Overview

**VitaSegura** is a native Android application that connects an elderly person (*Adulto Mayor*) with their caregiver (*Familiar / Cuidador*) around a single goal: keeping the elderly user safe and monitored in real time, even when the app is in the background.

The app pairs over Bluetooth Low Energy with a custom health band (the companion ESP32 firmware lives in a separate repository) to stream heart rate and blood-oxygen readings, fires medication reminders on schedule, shares live GPS location with the caregiver, and lets the elderly user trigger an emergency alert with a built-in cancel window. It is built to keep working under poor connectivity: emergency alerts are queued locally when offline and synced the moment the network returns.

This repository contains the **Android client (Java)** and a **Firebase Cloud Functions backend (Node.js)** for password recovery.

---

## Key features

- **Two role-based experiences from a single app** — the user's `esPrincipal` flag routes them to the caregiver dashboard or the elderly self-monitoring home.
- **Live vital signs over BLE** — a foreground service reads heart rate and SpO₂ from the wearable via the Nordic UART GATT profile and streams them to Firebase in real time.
- **Smart medication reminders** — caregivers schedule medications; the app computes the *next* upcoming dose dynamically from the start time and frequency, fires local notifications for every dose in the 24-hour cycle, and supports "only if in pain" medications with a one-tap "I took it" alert back to the caregiver.
- **Emergency alerts with offline resilience** — the elderly user triggers an alert with a 6-second cancel countdown; if offline, alerts are queued in SQLite (FIFO, capped at 50) and synced to Firebase on reconnect.
- **Real-time location sharing** — caregivers see the elderly user's current position and history on a Google Map.
- **Vital-signs charts** — historical heart rate / SpO₂ visualized with interactive line charts.
- **Caregiver alert center** — a background service watches for emergencies and abnormal vitals, persists a full notification history locally, and respects a user-configurable do-not-disturb toggle.
- **Connectivity-aware UI** — an offline banner that doesn't false-trigger when switching between Wi-Fi and mobile data.
- **Account management** — registration, login, and a full forgotten-password flow (email code → verify → reset) backed by Cloud Functions.

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java (Android), Node.js (backend) |
| Min / Target SDK | minSdk 28 · targetSdk 36 · compileSdk 36 · Java 11 |
| Build system | Gradle (Kotlin DSL, `build.gradle.kts`) |
| Architecture | Activity-based (no Fragments), foreground services for background data |
| Backend & data | Firebase Realtime Database, Auth, Storage, Cloud Messaging, Cloud Functions, Analytics (BOM 34.9.0) |
| Local storage | SQLite (`SQLiteOpenHelper` singletons) for notification history & offline alert queue |
| Connectivity | Bluetooth Low Energy (Nordic UART), `ConnectivityManager` network callbacks |
| Maps & location | Google Maps SDK 18.2.0, Fused Location |
| Charts | MPAndroidChart 3.1.0 |
| Media | Glide 4.16.0 (image loading), uCrop 2.2.8 (profile-photo cropping) |
| Email (backend) | Nodemailer (Gmail) for recovery codes |

---

## Architecture

VitaSegura is deliberately **Activity-based** and leans on **foreground services** so that monitoring continues when the app is backgrounded.

### Role split

User role is stored as a single boolean in Firebase (`Usuarios/[uid]/esPrincipal`):

- `true` → **Caregiver** → `MainFamiliarActivity` (monitor medications, location, vitals, alerts)
- `false` → **Elderly user** → `MainAdultoActivity` (self-monitoring, band pairing, emergency button)

### App flow

```
MainActivity (splash / router)
├── LoginActivity / RegistroActivity
│   └── RecuperarActivity → IngresarCodigoActivity → CambiarPassActivity
│
├── MainFamiliarActivity  (caregiver home)
│   ├── UsuariosActivity → AgregarAdultoMayor  (link elderly user via temp code)
│   ├── MedicamentosCuidadorActivity → FormularioMedicamentoActivity
│   ├── UbicacionAdultoActivity  (Google Maps + history)
│   ├── InformacionAbueloActivity
│   ├── NotificacionesActivity
│   └── ConfiguracionActivity → MiInformacionActivity
│
└── MainAdultoActivity  (elderly home)
    ├── MedicamentosAbueloActivity   (read-only, shows next dose)
    ├── MonitoreoSaludActivity       (vitals charts)
    ├── PulseraActivity              (BLE band pairing)
    ├── AlertaEmergenciaActivity     (6-sec countdown + cancel)
    ├── NotificacionesActivity
    └── ConfiguracionActivity → MiInformacionActivity
```

### Background services

Three foreground services keep data flowing:

- **`SaludService`** — reads vitals from the BLE band (Nordic UART), evaluates medication schedules every 10 s, and writes live vitals to `Usuarios/[uid]/MonitoreoActual`.
- **`ServicioNotificacionesAbuelo`** — delivers notifications queued for the elderly user.
- **`ServicioAlertasCuidador`** — watches emergencies and vitals for the caregiver, persists every alert to SQLite history, and fires push notifications only for recent alerts when not silenced.

BLE access is centralized in the **`BluetoothServiceManager`** singleton, which also exposes `enviarComandoVibrar()` to vibrate the band for haptic confirmation.

### Data model (Firebase Realtime Database)

```
Usuarios/[uid]/
  ├── profile (nombre, correo, teléfono, esPrincipal, fotoPerfil)
  ├── Medicamentos/             → medication schedules
  ├── MonitoreoActual/          → live vitals (bpm, oxi)
  ├── Ubicacion/                → current GPS
  ├── EmergenciasPendientes/    → queued emergency alerts
  └── NotificacionesPendientes/ → notification payloads

Vinculos/[cuidadorId]/          → caregiver → elderly UID mapping
Codigos_Temporales/[code]/      → linking codes (15-min TTL)
CodigosRecuperacion/            → password-recovery codes (5-min TTL)
```

---

## Project at a glance

- ~5,800 lines of Java across 37 source files
- 22 activities, 3 foreground services, 4 RecyclerView adapters, 4 model classes
- 28 XML layouts
- 2 Firebase Cloud Functions (Node.js)

---

## Getting started

### Prerequisites

- Android Studio (with an SDK for API 36)
- A physical device or emulator running Android 9 (API 28) or higher — a BLE-capable physical device is needed to test the wearable integration
- A Firebase project and a Google Maps API key

### Setup

```bash
# 1. Clone the repository
git clone <repo-url>
cd VitaSegura

# 2. Add your Google Maps key to local.properties
echo "MAPS_API_KEY=your_key_here" >> local.properties

# 3. Drop your Firebase config into app/google-services.json

# 4. Build & install on a connected device
gradlew.bat installDebug
```

### Common Gradle tasks

```bat
gradlew.bat assembleDebug          :: debug build
gradlew.bat installDebug           :: install on connected device
gradlew.bat test                   :: unit tests
gradlew.bat connectedAndroidTest   :: instrumented tests (needs device)
gradlew.bat clean                  :: clean
```

### Backend (Cloud Functions)

```bash
cd Backend-VitaSegura
firebase deploy --only functions
```

The backend exposes two callable functions: `enviarCodigoRecuperacion` (emails a 6-digit code, 5-min TTL) and `cambiarPasswordOlvidada` (verifies the code and updates the Auth password).

---

## Engineering highlights

- **Resilient offline UX** — emergency alerts are never lost: they're queued in a capped SQLite buffer and replayed on reconnect, each deleted only after a confirmed write.
- **Background-safe monitoring** — foreground services with the correct `connectedDevice` / `location` service types keep BLE and GPS data flowing under modern Android background restrictions.
- **Dynamic medication scheduling** — instead of showing a fixed start time, the app computes the next dose across the full 24-hour cycle, with anti-spam guarding against duplicate reminders.
- **Connection-aware notifications** — alert history is always persisted, while push delivery is gated by recency and a user preference, decoupling "what happened" from "what should buzz right now."
- **Clean role separation** — a single boolean drives two cohesive, role-specific experiences from one codebase.

---

## Conventions

- Source code and comments are written in **Spanish**.
- Views are wired with `findViewById` (no ViewBinding/DataBinding).
- Firebase reads use anonymous listener callbacks; role logic branches on `esPrincipal`.

---

## Project status

🟡 **In active development** — functional build; field testing alongside the companion wearable in progress.

---

## Authors

Software Development Technologist — CETI Colomos

**Ulises Alberto Macías Ramírez**
[LinkedIn](https://www.linkedin.com/in/ulmac19) · [GitHub](https://github.com/Ulmac19)

**Daniel Eduardo Pelayo Gómez**
[LinkedIn](https://www.linkedin.com/in/daniel-pelayo-4097ab414) · [GitHub](https://github.com/Pelayo04)
