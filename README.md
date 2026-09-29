# Headphone Actions

**Automate your headphones, not just your phone.**

Headphone Actions is an open-source Android app that exposes advanced Bluetooth headphone controls as Android app shortcuts and home screen widget buttons, so they can be triggered from automation tools or with a single tap instead of only from a manufacturer's companion app.

Use headphone features such as noise cancelling, ambient sound, Speak-to-Chat, DSEE Extreme, and multipoint playback control directly from automation.

The first supported device is **Sony WH-1000XM6**. Automation is currently tested with **Samsung Modes and Routines**.

> **Status:** early 0.x project. Device support is currently limited, but the app is structured so additional headphone models and vendors can be added later.

## Why Headphone Actions?

Modern Bluetooth headphones often expose their most useful controls only inside proprietary companion apps.

Headphone Actions makes supported controls available to Android automation instead:

```text
Samsung Modes & Routines
          │
          ▼
   Headphone Actions
          │
          ▼
      device driver
          │
          ▼
     Bluetooth protocol
          │
          ▼
       headphones
```

Headphone Actions talks directly to supported headphones using their Bluetooth control protocols.

There are:

- no accounts;
- no cloud services;
- no analytics or telemetry;
- no continuous background polling;
- no permanently running background service.

The app communicates with the headphones only when an action runs — from the app, a routine or the widget — or when you explicitly test the connection or read the headphones' device list for the multipoint setup.

## Compatibility

### Android

- Android 8.0 or later
- Android 12+ requires the **Nearby devices** permission

### Automation

Currently tested with:

- **Samsung Modes and Routines**

The headphone-control layer itself is not Samsung-specific. Additional Android automation integrations may be added later.

## Supported headphones

| Headphones      | Status                        | Automation                 | Supported features                                                      |
| --------------- | ----------------------------- | -------------------------- | ----------------------------------------------------------------------- |
| Sony WH-1000XM6 | Supported and hardware-tested | Samsung Modes and Routines | Noise control, Speak-to-Chat, DSEE Extreme, multipoint playback control |

Support is implemented per device/model family. Future headphones can provide their own actions without putting vendor protocol logic into the Samsung integration.

## Installation

Download the latest APK from [Releases](https://github.com/inok546/headphone-actions/releases) and install it.

Android may ask you to allow your browser or file manager to install unknown apps.

Release APKs are signed with the project key. Its certificate SHA-256 fingerprint is:

```text
D8:1A:48:52:85:61:2A:B2:B1:F8:54:8B:EC:ED:0F:89:C3:9A:80:89:93:1B:06:A2:67:4A:C4:96:85:34:EC:66
```

## Quick start

1. Pair your headphones in Android Bluetooth settings.
2. Open **Headphone Actions**.
3. Allow the **Nearby devices** permission when requested.
4. Under **Paired devices**, register a supported headphone model.
5. Open **Samsung Modes and Routines**.
6. Create or edit a routine.
7. Add:
   **Then → Apps → Open an app or do an app action → Headphone Actions**
8. Choose the headphone action you want to run.

When the routine runs, Headphone Actions sends the command to the registered headphones in the background.

If execution fails — for example because the headphones are powered off — a short message appears and the app shows the result under **Last routine action**; the registered device and the shortcut set stay unchanged.

The first few actions also appear as app shortcuts when you long-press the Headphone Actions icon; launchers show only a limited number of shortcuts.

## Sony WH-1000XM6

### Available actions

| Action                  | Description                                                               |
| ----------------------- | ------------------------------------------------------------------------- |
| Noise Cancelling        | Enables active noise cancelling                                           |
| Ambient Sound           | Enables ambient sound mode                                                |
| Noise Control Off       | Disables both ANC and ambient sound                                       |
| Speak-to-Chat On        | Enables Speak-to-Chat                                                     |
| Speak-to-Chat Off       | Disables Speak-to-Chat                                                    |
| DSEE Extreme On         | Enables DSEE Extreme                                                      |
| DSEE Extreme Off        | Disables DSEE Extreme                                                     |
| Play on This Phone      | Switches playback to this phone and locks playback to it                  |
| Play on Other Device    | Switches playback to the other multipoint device and locks playback to it |
| Lock Playback Device    | Locks playback to the device currently playing                            |
| Auto Playback Switching | Removes the playback lock and restores automatic switching                |

Changing the noise-control mode preserves unrelated headphone settings such as ambient level, focus-on-voice, and automatic ambient-sound behavior where possible.

<details>
<summary>Stable action IDs</summary>

These IDs are intentionally stable because Samsung Routines may persist references to them.

| Action                  | Shortcut ID                                 |
| ----------------------- | ------------------------------------------- |
| Noise Cancelling        | `sony.wh1000xm6.noise_control.anc`          |
| Ambient Sound           | `sony.wh1000xm6.noise_control.ambient`      |
| Noise Control Off       | `sony.wh1000xm6.noise_control.off`          |
| Speak-to-Chat On        | `sony.wh1000xm6.speak_to_chat.on`           |
| Speak-to-Chat Off       | `sony.wh1000xm6.speak_to_chat.off`          |
| DSEE Extreme On         | `sony.wh1000xm6.dsee.on`                    |
| DSEE Extreme Off        | `sony.wh1000xm6.dsee.off`                   |
| Play on This Phone      | `sony.wh1000xm6.playback.lock_this_phone`   |
| Play on Other Device    | `sony.wh1000xm6.playback.lock_other_device` |
| Lock Playback Device    | `sony.wh1000xm6.playback.lock_current`      |
| Auto Playback Switching | `sony.wh1000xm6.playback.auto_switch`       |

</details>

### Multipoint setup

The playback-routing actions are intended for Sony multipoint mode, where the headphones are connected to two devices at the same time.

First enable **Connect to 2 devices simultaneously** in Sony Sound Connect.

Then, in Headphone Actions:

1. Open the **Multipoint** section.
2. Tap **Choose this phone**.
3. Select **This is my phone** next to the current phone in the device list reported by the headphones.

Android does not expose the phone's own Bluetooth address to ordinary apps, so this mapping is requested once from the user.

## Home screen widget

The Headphone Actions widget puts buttons for your registered headphones on the home screen. A tap runs the action in the background, just like a routine, and a short message confirms which action is being sent; errors are shown the same way. On Android 13 and later these messages need notifications to be allowed — the app's **Messages** section asks for it. The buttons send commands; the widget does not show the current state of the headphones.

- **Add it:** long-press the home screen → **Widgets** → **Headphone Actions**. A new widget starts with **Noise Cancelling**, **Ambient Sound** and **Speak-to-Chat Off**. (On Android 11 and earlier the settings screen opens first, with these preselected.)
- **Header:** when the widget is tall enough, a line at the top names the headphones the buttons are for (and the app, on a 4-cell-wide widget).
- **Sizes:** 4×1 by default; resize from 2×1 up to 2 rows.
  - 1 row: icon buttons, as many as fit, in your order (2 on a 2×1 widget).
  - 2 rows: tiles with an icon and a short label.
- **Change the buttons:** long-press the widget → **Settings**, check the actions to show, order them with the arrows and tap **Save**. Only actions of the registered headphones are offered.
- **Several widgets:** each widget keeps its own buttons.
- **Other headphones registered:** a widget set up for another model shows **Headphones changed** with a **Configure** button instead of guessing new buttons.
- **No headphones registered:** the widget shows **No device registered** with a button that opens the app.

Widgets update only after you register or remove headphones or change their buttons — not when the headphones connect or disconnect.

## Registered device behavior

Headphone Actions manages one explicitly registered headphone device.

The app does **not** automatically switch targets based on whichever supported headphones happen to be connected.

The shortcut set belongs to the registered device and does not change when that device connects or disconnects.

Changing or removing the managed headphones is an explicit user action.

Actions added by an app update appear only after you remove the registration and register the headphones again (choose this phone again afterwards if you use the multipoint actions). Existing action IDs stay the same, so your routines keep working.

## How it works

At a high level:

```text
registered device
      │
      ▼
  model support
      │
      ▼
supported actions
      │
      ▼
 Android shortcuts
```

When an automation invokes an action:

```text
Samsung Routine
      │
      ▼
Android app shortcut
      │
      ▼
RoutineActionActivity
      │
      ▼
ActionExecutionService
      │
      ▼
device-specific driver
      │
      ▼
Bluetooth RFCOMM / vendor protocol
      │
      ▼
headphones
```

`RoutineActionActivity` is intentionally minimal and immediately hands execution to a short-lived foreground service. Home screen widget buttons start the same `ActionExecutionService` directly.

For Sony WH-1000XM6, Headphone Actions communicates using Sony's MDR protocol over Bluetooth RFCOMM. Each action opens the required connection, exchanges the necessary protocol messages, confirms the result where possible, and disconnects.

## Permissions

| Permission                            | Why                                                                   |
| ------------------------------------- | --------------------------------------------------------------------- |
| `BLUETOOTH_CONNECT` on Android 12+    | List paired devices and communicate with the registered headphones    |
| `BLUETOOTH` on Android 11 and earlier | Bluetooth communication on older Android versions                     |
| `FOREGROUND_SERVICE`                  | Let an action started by a routine or the widget finish reliably in the background |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Android foreground-service type used for the short Bluetooth exchange |
| `POST_NOTIFICATIONS` on Android 13+   | Optional, requested from the app's **Messages** section: without it Android hides the short messages shown for widget taps and failed actions |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `ACCESS_NETWORK_STATE` | Added by WorkManager, which Jetpack Glance uses to update the home screen widget; not used by Headphone Actions' own code |

No location permission is requested merely to control already paired headphones, and the app has no Internet permission.

## Building from source

### Requirements

- JDK 17 or later
- Android SDK
- Android SDK Platform `platforms;android-37.0`
- Android Build Tools `build-tools;36.0.0`
- Android Platform Tools for `adb`

With the Android SDK command-line tools:

```bash
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
```

Gradle is provided through the repository's Gradle Wrapper. A system Gradle installation is not required.

Configure the Android SDK through `ANDROID_HOME` or `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then run:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

GitHub Actions runs the same three commands for every push and pull request to `main`.

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Release signing

`./gradlew assembleRelease` signs the release APK when these properties are configured, for example in `~/.gradle/gradle.properties`:

```properties
headphoneActions.releaseStoreFile=/path/to/release.jks
headphoneActions.releaseStorePassword=…
headphoneActions.releaseKeyAlias=…
headphoneActions.releaseKeyPassword=…
```

Without release-signing configuration, the build produces an unsigned release artifact.

## Project structure

```text
app/src/main/java/io/github/inok546/headphoneactions/
├── MainActivity.kt
├── MainScreen.kt
├── AppPreferences.kt
├── bluetooth/
├── device/
│   ├── HeadphoneModel.kt
│   └── sony/
│       └── mdr/
├── routines/
└── widget/
```

The important architectural rule is that vendor protocol logic stays inside device support code.

Samsung integration should only need to know:

- which device is registered;
- which actions that model exposes;
- which stable action ID was requested.

It should not contain Sony MDR packet logic.

## Adding support for new headphones

Headphone Actions is intended to support additional headphone models through device-specific implementations.

A new model generally needs to provide:

1. device recognition;
2. a list of supported actions with stable IDs;
3. the Bluetooth transport/protocol implementation required by that model;
4. execution logic for those actions;
5. automated tests for protocol-independent behavior where practical;
6. real-hardware validation before claiming model support.

The current model interface is intentionally small and may evolve as a second vendor or substantially different protocol family is added.

Do not assume that a model is supported only because it appears protocol-compatible with another one.

## Roadmap

- [x] Samsung Modes and Routines integration
- [x] Background action execution
- [x] Sony WH-1000XM6 support
- [x] Noise control
- [x] Speak-to-Chat
- [x] DSEE Extreme
- [x] Multipoint playback control
- [x] Home screen widget
- [ ] Additional Sony headphone models
- [ ] Additional headphone vendors
- [ ] Additional Android automation integrations

## Privacy

Headphone Actions has no account system, cloud backend, analytics, advertising, or telemetry.

The app stores only local data: the registered headphones, the phone chosen for multipoint, the buttons of each widget and the result of the last routine action. It has no Internet permission.

Bluetooth communication stays between the Android device and the headphones.

## Open-source acknowledgements

Headphone Actions builds on Bluetooth protocol research from other open-source projects.

Sony MDR protocol code is derived in part from [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge).

Additional WH-1000XM6 protocol research comes from:

- [sonyctl](https://github.com/sevsev9/sonyctl)
- [BudsLink](https://github.com/maniacx/BudsLink)

Action icons are [Material Symbols](https://github.com/google/material-design-icons) by Google (Apache License 2.0).

See [NOTICE.md](NOTICE.md) for exact provenance, licenses, and copyright notices.

## License

Headphone Actions is licensed under the [GNU Affero General Public License v3.0 only](LICENSE) (`AGPL-3.0-only`).

This project is not affiliated with or endorsed by Sony or Samsung.

Sony, WH-1000XM6, Speak-to-Chat, and DSEE Extreme are trademarks of Sony Group Corporation. Samsung and Modes and Routines are trademarks of Samsung Electronics.

