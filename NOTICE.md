# Third-party sources

Headphone Actions is licensed under AGPL-3.0-only (see `LICENSE`). It contains code
derived from the following projects. Each derived file names its source and keeps the
original copyright notice in its header.

## Gadgetbridge

- Project: https://codeberg.org/Freeyourgadget/Gadgetbridge
- License: GNU AGPL version 3 or later (used here under version 3)

| File in this repository | Derived from |
|---|---|
| `app/src/main/java/io/github/inok546/headphoneactions/device/sony/mdr/SonyMdrMessage.kt` | `service/devices/sony/headphones/protocol/Message.java`, `MessageType.java` — Copyright (C) 2021-2024 José Rebelo |
| `app/src/main/java/io/github/inok546/headphoneactions/device/sony/mdr/SonyMdrHandshake.kt` | `service/devices/sony/headphones/SonyHeadphonesSupport.java` — Copyright (C) 2021-2024 Arjan Schrijver, José Rebelo; `service/devices/sony/headphones/SonyHeadphonesProtocol.java` — Copyright (C) 2021-2026 José Rebelo |

The WH-1000XM6 Bluetooth name pattern in `device/sony/SonyWh1000xm6.kt` follows
Gadgetbridge's `SonyWH1000XM6Coordinator.java`.
