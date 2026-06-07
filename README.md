[README_VitaSegura.md](https://github.com/user-attachments/files/28676784/README_VitaSegura.md)
# VitaSegura 🩺

> Dispositivo wearable de monitoreo biométrico continuo orientado a adultos mayores, con detección automática de caídas.

---

## Descripción general

VitaSegura es un sistema embebido desarrollado sobre el microcontrolador **ESP32** que permite monitorear en tiempo real el estado de salud de adultos mayores. El dispositivo mide frecuencia cardíaca, saturación de oxígeno en sangre (SpO2) y detecta caídas de manera automática, emitiendo alertas en menos de 2 segundos ante eventos críticos.

El proyecto nació de la necesidad de ofrecer una solución accesible, autónoma y de bajo consumo energético para el cuidado de personas en situación de vulnerabilidad.

---

## Características principales

- **Monitoreo de frecuencia cardíaca** en tiempo real mediante sensor biométrico
- **Medición de SpO2** (saturación de oxígeno en sangre)
- **Detección automática de caídas** con tiempo de respuesta menor a 2 segundos
- **Gestión de bajo consumo** mediante modo deep sleep del ESP32
- **Arquitectura modular** con librerías de GPS desacopladas, permitiendo operar sin dependencia de ubicación y reduciendo el consumo energético ~30%
- **Interrupciones de hardware** para respuesta inmediata ante eventos

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Microcontrolador | ESP32 |
| Lenguaje | C++ |
| Sensores | Sensor biométrico (FC/SpO2), acelerómetro |
| Localización | GPS (módulo desacoplado) |
| Firmware | Arduino IDE / FreeRTOS |
| Gestión energética | Deep sleep, interrupciones HW |

---

## Arquitectura del sistema

```
┌─────────────────────────────────────────────┐
│                  ESP32                       │
│                                             │
│  ┌──────────┐   ┌──────────┐   ┌─────────┐ │
│  │ Sensor   │   │Aceleró-  │   │ Módulo  │ │
│  │ FC/SpO2  │   │  metro   │   │  GPS*   │ │
│  └────┬─────┘   └────┬─────┘   └────┬────┘ │
│       │              │              │       │
│  ┌────▼──────────────▼──────────────▼────┐  │
│  │         Capa de procesamiento         │  │
│  │   Lectura · Filtrado · Detección      │  │
│  └────────────────────┬──────────────────┘  │
│                       │                     │
│              ┌────────▼────────┐            │
│              │  Sistema alerta │            │
│              │  (< 2 seg.)     │            │
│              └─────────────────┘            │
└─────────────────────────────────────────────┘

* Módulo GPS desacoplado — versión independiente de ubicación disponible
```

---

## Resultados de pruebas

| Métrica | Resultado |
|---|---|
| Tiempo de respuesta ante caída | < 2 segundos |
| Reducción de consumo energético (versión sin GPS) | ~30% |
| Precisión de detección de caídas | En validación |
| Autonomía estimada de batería | En pruebas de campo |

## Cómo ejecutar

### Requisitos previos

- Arduino IDE 2.x o PlatformIO
- Placa: ESP32 (cualquier variante con WiFi/BT)
- Librerías necesarias (ver `lib/`)

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/Pelayo04/VitaSegura.git

# 2. Abrir el proyecto en Arduino IDE o PlatformIO

# 3. Seleccionar la placa ESP32 correspondiente

# 4. Configurar parámetros en src/config.h (umbrales de alerta, pines)

# 5. Compilar y cargar al dispositivo
```

---

## Aprendizajes clave del proyecto

- Gestión de recursos en sistemas embebidos con memoria y CPU limitada
- Desacoplamiento de módulos para mejorar portabilidad y consumo
- Uso de interrupciones de hardware para garantizar respuesta en tiempo real
- Diseño orientado a usuarios no técnicos (adultos mayores)

---

## Estado del proyecto

🟡 **En desarrollo** — Prototipo funcional. Pruebas de campo en curso.

---

## Autor

**Daniel Eduardo Pelayo Gómez**
**Ulises Alberto Macías Ramírez**
Tecnólogos en Desarrollo de Software — CETI Colomos
[LinkedIn](https://www.linkedin.com/in/daniel-pelayo-4097ab414) · [GitHub](https://github.com/Pelayo04)
