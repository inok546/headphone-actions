# Headphone Actions

Control Bluetooth headphone features from **Samsung Modes and Routines**.

Headphone Actions exposes the controls of your headphones — noise cancelling, ambient sound,
Speak-to-Chat, DSEE Extreme — as Android app shortcuts. Samsung Modes and Routines runs them
through **Open an app or do an app action**, so your headphones can switch modes on their own:
when you leave home, get in the car, start a workout or at a set time.

The Sony WH-1000XM6 is the first supported model. Support is organized per model, so other
headphones can be added later.

> **Status:** early (0.x). Tested on a Samsung phone with a Sony WH-1000XM6.

## Supported actions

Sony WH-1000XM6:

| Action | Shortcut ID |
|---|---|
| Noise Cancelling | `sony.wh1000xm6.noise_control.anc` |
| Ambient Sound | `sony.wh1000xm6.noise_control.ambient` |
| Noise Control Off | `sony.wh1000xm6.noise_control.off` |
| Speak-to-Chat On | `sony.wh1000xm6.speak_to_chat.on` |
| Speak-to-Chat Off | `sony.wh1000xm6.speak_to_chat.off` |
| DSEE Extreme On | `sony.wh1000xm6.dsee.on` |
| DSEE Extreme Off | `sony.wh1000xm6.dsee.off` |

Shortcut IDs are stable, so routines that use them keep working across app updates.
Changing the noise control mode keeps your other settings (focus on voice, ambient sound level,
auto ambient sound) as they are.

## Requirements

- Android 8.0 or later. Automation needs a Samsung phone with Modes and Routines.
- Headphones paired with the phone in the Android Bluetooth settings.
- The **Nearby devices** permission (Android 12 and later).

## Installation

Download the APK from [Releases](https://github.com/inok546/headphone-actions/releases) and
install it. Your browser or file manager may ask you to allow installing apps.

Release APKs are signed with the project key. Its certificate SHA-256 fingerprint is:

```
D8:1A:48:52:85:61:2A:B2:B1:F8:54:8B:EC:ED:0F:89:C3:9A:80:89:93:1B:06:A2:67:4A:C4:96:85:34:EC:66
```

Builds made from source are signed with a different key, so uninstall one before installing
the other.

## Usage

1. Open Headphone Actions and allow **Nearby devices**.
2. Under **Paired devices**, tap **Register as Sony WH-1000XM6** next to your headphones.
   The app publishes the actions of that model as shortcuts. They stay the same until you
   remove the registration; connecting or disconnecting the headphones does not change them.
3. Optionally try things from the app: **Test connection**, or **Run** next to an action.
4. In **Modes and Routines**, create a routine and add
   **Then → Apps → Open an app or do an app action → Headphone Actions**, then pick an action.

When the routine runs, the action is sent to the headphones in the background; no window opens,
also with the screen off. If it fails, for example because the headphones are off, a short
message appears. **Last routine action** in the app shows the result of the latest run.

The actions also appear as app shortcuts in launchers (long-press the app icon).

## How it works

```
registered device → model support → supported actions → app shortcuts

routine → shortcut → RoutineActionActivity → ActionExecutionService → RFCOMM → headphones
```

- You register one device explicitly; the app never picks a device on its own.
- `RoutineActionActivity` is invisible and closes at once. It hands the action to
  `ActionExecutionService`, a short-lived foreground service of type `connectedDevice` that
  keeps the app alive for the few seconds the Bluetooth exchange takes, then stops. Android
  defers its notification by up to 10 seconds, so normally none is shown.
- Sony headphones are controlled with Sony's MDR protocol over Bluetooth RFCOMM. Each action
  opens a connection, reads the current state, changes it, confirms the change and disconnects.
- No background services, polling, cloud services, accounts, analytics or telemetry. The app
  talks to the headphones only when you run an action.

### Permissions

| Permission | Why |
|---|---|
| `BLUETOOTH_CONNECT` (Android 12+), `BLUETOOTH` (Android 11 and lower) | List paired devices and talk to the registered headphones |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Keep the app running while an action started by a routine is sent |

## Building from source

Requirements: JDK 17 or later, and the Android SDK with `platforms;android-37.0` and
`build-tools;36.0.0`. Gradle comes with the wrapper. Point `sdk.dir` in `local.properties`
(or `ANDROID_HOME`) at the SDK.

```bash
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
./gradlew test
./gradlew lint
```

`./gradlew assembleRelease` signs the release APK when these properties are set, for example in
`~/.gradle/gradle.properties`; otherwise it builds an unsigned APK:

```properties
headphoneActions.releaseStoreFile=/path/to/release.jks
headphoneActions.releaseStorePassword=…
headphoneActions.releaseKeyAlias=…
headphoneActions.releaseKeyPassword=…
```

### Project layout

```
app/src/main/java/io/github/inok546/headphoneactions/
├── MainActivity.kt, MainScreen.kt   UI (Jetpack Compose)
├── AppPreferences.kt                registered device, last routine action
├── bluetooth/                       paired device list
├── device/                          HeadphoneModel interface, registered device
│   └── sony/                        Sony WH-1000XM6 support
│       └── mdr/                     Sony MDR protocol: framing, session, commands
└── routines/                        shortcut publishing and execution
```

To add a model, implement `HeadphoneModel` — recognize the device by its Bluetooth name,
declare its actions with stable IDs, connect and execute them — and add it to
`supportedModels`. The UI and the Samsung integration need no changes and know nothing about
vendor protocols.

## Credits

The Sony MDR protocol code is derived from [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge).
WH-1000XM6 specifics come from [sonyctl](https://github.com/sevsev9/sonyctl) and
[BudsLink](https://github.com/maniacx/BudsLink). See [NOTICE.md](NOTICE.md) for details and
copyright notices.

## License

[GNU Affero General Public License v3.0 only](LICENSE) (`AGPL-3.0-only`).

This project is not affiliated with or endorsed by Sony or Samsung. Sony, WH-1000XM6,
Speak-to-Chat and DSEE Extreme are trademarks of Sony Group Corporation; Samsung and Modes and
Routines are trademarks of Samsung Electronics.
