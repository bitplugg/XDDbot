# XDDBOT

Клиент-компаньон для **собственного** электросамоката (BLE). Подключается по вашему
ключу, показывает обмен, умеет базовые команды и демо-режим для проверки без железа.

> Только для устройств, которыми вы владеете или на которые у вас есть явное разрешение.
> В приложении **нет** подбора/генерации ключей и обхода чужих замков.

## Возможности

- Материалы: Kotlin, Jetpack Compose, Material 3 / Material You, minSdk 26.
- BLE: скан, подключение, ручной/файловый ввод ключа, сохранённые профили, автоподключение, журнал обмена.
- Команды: Unlock/Lock, Find car, RFID, Battery, Firmware + полная таблица опкодов.
- Демо-режим: полный UI без реального железа.
- Тест устойчивости разблокировки: настраиваемый интервал, лимит попыток, авто-стоп.

## Сборка

```bash
./gradlew assembleDebug     # debug APK
./gradlew test              # unit-тесты
./gradlew lint              # линт
./gradlew connectedDebugAndroidTest   # instrumented (нужен эмулятор/устройство)
```

Release-подпись задаётся через переменные окружения (или `keystore.properties`, не коммитится):

```
ANDROID_KEYSTORE_PATH
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

## CI (GitHub Actions)

`.github/workflows/android.yml`: lint + unit + instrumented (эмулятор API 30) →
debug/release APK. По тегу `v*` APK публикуются в GitHub Releases.
Сайт и статический API (`docs/`) деплоятся на GitHub Pages при пуше в `main`.

### Секреты репозитория

| Секрет | Назначение |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | release keystore, `base64 -w0` |
| `ANDROID_KEYSTORE_PASSWORD` | пароль хранилища |
| `ANDROID_KEY_ALIAS` | алиас ключа |
| `ANDROID_KEY_PASSWORD` | пароль ключа |

## Протокол

Кадр: `A3 A4 len (rand+50) (devType^rand) (cmd^rand) payload^rand crc8`.
Передача: START `0xFB` (тип, число пакетов, CRC16, deviceType, ключ) и DATA `0xFC`
(индекс, deviceType, 16 байт, CRC16). CRC8 — MAXIM/DALLAS, CRC16 — ARC (poly 0xA001).
Реализация: `xdbot.bitplugg.ru.protocol`.

## Документация

Подробности — в [Wiki](https://github.com/bitplugg/XDDbot/wiki).
