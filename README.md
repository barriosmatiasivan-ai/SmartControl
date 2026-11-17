# SmartControl — Universal Open-Source Remote (Google TV Wi-Fi + 1,957 IR Devices)

**SmartControl** is a lightweight, ad-free, open-source Android remote control app (Kotlin + Jetpack Compose) built to replace the official Google TV app and universal IR apps in a single fast package.

## Features
- **Google TV / Android TV over Wi-Fi (Protocol v2):**
  - Automatic mDNS discovery (`_androidtvremote2._tcp.`) + manual IP entry.
  - Secure TLS + 6-character PIN pairing with persistent local keystore.
  - **Auto-reconnect** when returning from background or when the socket drops.
  - **Touchpad mode** (swipe to navigate, tap = OK, double-tap = Back, long-press = Menu, adjustable sensitivity).
  - **Dedicated quick buttons:** Power, HDMI/Input, **Settings (`KEYCODE_SETTINGS`)**, Menu, Info, Media controls, Volume/Mute with live level feedback, Numpad + CH+/CH-.
  - **Keyboard & Push-to-Talk Voice** + **Quick App Launcher** (YouTube, Netflix, Prime Video, Disney+, Max, Spotify, Plex, Stremio, Twitch + custom deep links).
- **Universal IR Blaster (`ConsumerIrManager`):**
  - **1,957+ offline IR remotes bundled** across 46 categories (`TVs`, `ACs`, `SoundBars`, `Audio_and_Video_Receivers`, `Projectors`, `Fans`, `LED_Lighting`, `Cable_Boxes`, `Streaming_Devices`, `Cameras`, `Blu-Ray`, `DVD_Players`, `Heaters`, etc.) via the **Base IR** searchable browser.
  - **27 built-in instant presets** (Samsung, LG, Sony, TCL, Hisense, Philips RC5/RC6, RCA Smart/Classic 56kHz/38kHz, Xiaomi, Panasonic, Sharp, Toshiba, Roku TV, Vizio, JVC, LatAm OEMs, JBL/Samsung/LG/Sony soundbars, Epson/BenQ projectors, Apple TV, RGB LED strips).
  - **Auto-detect TV brand scanner** (cycles through Power IR codes until your TV responds).
  - **Auto IR Fallback:** Automatically sends Power, HDMI, Settings, Volume, and Navigation via IR when Wi-Fi is disconnected.
  - **Protocol encoders:** NEC/NECext, Samsung32, Sony SIRC, Philips RC5 & RC6, Panasonic/Kaseikyo, JVC, Sharp, RCA (56kHz & 38kHz), Pronto Hex, and Raw timings.

## Build APK (GitHub Actions or Local)
1. Push this repository to GitHub.
2. Go to **Actions → Build SmartControl APK** to download `SmartControl-debug-apk` or `SmartControl-release-unsigned-apk`.
3. Install on your phone or via ADB:
   ```bash
   adb install app-debug.apk
   ```

## License & Credits
- **SmartControl** is licensed under the [MIT License](LICENSE).
- Android TV Remote v2 protocol layer (`com.oscar.smartcontrol.protocol`) adapted from [outofcontextapps/androidtvremote](https://github.com/outofcontextapps/androidtvremote) (MIT License).
- Bundled offline IR database (`app/src/main/assets/irdb/`) sourced from [Lucaslhm/Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) (CC0 1.0 Universal).
