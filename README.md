[README_VitaSegura.md](https://github.com/user-attachments/files/28676874/README_VitaSegura.md)
# VitaSegura 🩺

> Continuous biometric monitoring wearable device for elderly users, with automatic fall detection.

---

## Overview

VitaSegura is an embedded system built on the **ESP32** microcontroller that enables real-time health monitoring for elderly individuals. The device measures heart rate and blood oxygen saturation (SpO2), and automatically detects falls — triggering alerts in under 2 seconds upon critical events.

The project was born from the need to provide an accessible, autonomous, and low-power solution for the care of vulnerable people.

---

## Key features

- **Real-time heart rate monitoring** via biometric sensor
- **SpO2 measurement** (blood oxygen saturation)
- **Automatic fall detection** with response time under 2 seconds
- **Low-power management** using ESP32 deep sleep mode
- **Modular architecture** with decoupled GPS libraries, enabling location-independent operation and reducing energy consumption by ~30%
- **Hardware interrupts** for immediate response to critical events

---

## Tech stack

| Component | Technology |
|---|---|
| Microcontroller | ESP32 |
| Language | C++ |
| Sensors | Biometric sensor (HR/SpO2), accelerometer |
| Location | GPS (decoupled module) |
| Firmware | Arduino IDE / FreeRTOS |
| Power management | Deep sleep, hardware interrupts |

---

## System architecture

```
┌─────────────────────────────────────────────┐
│                  ESP32                       │
│                                             │
│  ┌──────────┐   ┌──────────┐   ┌─────────┐ │
│  │  HR/SpO2 │   │  Accel-  │   │   GPS   │ │
│  │  Sensor  │   │erometer  │   │ Module* │ │
│  └────┬─────┘   └────┬─────┘   └────┬────┘ │
│       │              │              │       │
│  ┌────▼──────────────▼──────────────▼────┐  │
│  │          Processing layer             │  │
│  │     Reading · Filtering · Detection   │  │
│  └────────────────────┬──────────────────┘  │
│                       │                     │
│              ┌────────▼────────┐            │
│              │   Alert system  │            │
│              │   (< 2 sec.)    │            │
│              └─────────────────┘            │
└─────────────────────────────────────────────┘

* Decoupled GPS module — location-independent version available
```

---

## Test results

| Metric | Result |
|---|---|
| Fall detection response time | < 2 seconds |
| Energy consumption reduction (GPS-free version) | ~30% |
| Fall detection accuracy | Under validation |
| Estimated battery life | Field testing in progress |

---

## Getting started

### Prerequisites

- Arduino IDE 2.x or PlatformIO
- Board: ESP32 (any Wi-Fi/BT variant)
- Required libraries (see `lib/`)

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/Pelayo04/VitaSegura.git

# 2. Open the project in Arduino IDE or PlatformIO

# 3. Select your ESP32 board

# 4. Configure parameters in src/config.h (alert thresholds, pin mapping)

# 5. Compile and flash to the device
```

---

## Key learnings

- Resource management on embedded systems with limited memory and CPU
- Module decoupling to improve portability and reduce power consumption
- Hardware interrupt usage to guarantee real-time response
- UX design oriented toward non-technical users (elderly population)

---

## Project status

🟡 **In progress** — Functional prototype. Field testing underway.

---

## Author

Software Development Technologist — CETI Colomos

**Ulises Alberto Macías Ramírez**
[LinkedIn](https://www.linkedin.com/in/ulmac19) · [GitHub](https://github.com/Ulmac19)

**Daniel Eduardo Pelayo Gómez**
[LinkedIn](https://www.linkedin.com/in/daniel-pelayo-4097ab414) · [GitHub](https://github.com/Pelayo04)
