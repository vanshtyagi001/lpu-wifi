# LPU WiFI - Automated Captive Portal Login

LPU WiFI is an Android application designed to automate the login process for the Lovely Professional University (LPU) Wi-Fi captive portal. It ensures you stay connected by automatically handling authentication in the background, especially at scheduled times like early morning.

## 🚀 Features

- **Automated Login**: Automatically authenticates with the LPU Wi-Fi captive portal using stored credentials.
- **Scheduled Connections**: Schedule the app to trigger a connection at specific times (e.g., 6:00 AM) to ensure you have internet access when you wake up.
- **Background Service**: Uses a foreground service to execute connection logic reliably without being killed by the OS.
- **Network Scanning**: Scans for available Wi-Fi networks and identifies LPU-specific SSIDs.
- **Credential Management**: Securely stores your LPU credentials using Jetpack DataStore.
- **Clean UI**: Modern user interface built with Jetpack Compose, featuring separate screens for network management, scheduling, and settings.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) (for storing network and schedule data)
- **Persistence**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) (for secure credential storage)
- **Networking**: [OkHttp](https://square.github.io/okhttp/) & [Jsoup](https://jsoup.org/) (for captive portal automation and HTML parsing)
- **Dependency Injection**: [KSP](https://kotlinlang.org/docs/ksp-overview.html) (for Room compiler)
- **Architecture**: MVVM (Model-View-ViewModel)

## 📱 Permissions

The app requires several permissions to function correctly:

- `INTERNET`: To communicate with the captive portal.
- `ACCESS_WIFI_STATE` & `CHANGE_WIFI_STATE`: To scan and connect to Wi-Fi networks.
- `ACCESS_NETWORK_STATE` & `CHANGE_NETWORK_STATE`: To monitor connectivity.
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Required by Android for Wi-Fi scanning.
- `SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`: To trigger background connections at precise times.
- `POST_NOTIFICATIONS`: To keep you informed about connection status.
- `FOREGROUND_SERVICE`: To ensure the connection process is completed in the background.

## 🛠 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/LPUWiFI.git
   ```
2. Open the project in **Android Studio**.
3. Build and run the app on your device or emulator (Android 8.0+ recommended).

## 📂 Project Structure

- `com.example.lpuwifi.ui`: UI components and ViewModels.
- `com.example.lpuwifi.data`: Room database entities, DAOs, and DataStore management.
- `com.example.lpuwifi.network`: Wi-Fi control and Captive Portal authentication logic.
- `com.example.lpuwifi.service`: Background foreground service for connection execution.
- `com.example.lpuwifi.receiver`: Broadcast receivers for alarms and boot completion.
- `com.example.lpuwifi.scheduling`: Logic for scheduling connection attempts.

## 📝 License

This project is for personal use and informational purposes.
