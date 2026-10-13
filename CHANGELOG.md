# Changelog

Все заметные изменения проекта XDDBOT.
Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версии следуют [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

### Added
- Проверка обновлений: приложение читает `version.json` со статического API GitHub Pages и предлагает обновиться.
- Онлайн-профили устройств (`devices.json`) с предзаполнением `deviceType` при создании ключа.
- Экспорт ключей в текстовый файл (в дополнение к импорту).
- Локализация интерфейса: русский (по умолчанию) и английский (`values-en`).
- Своя иконка запуска и splash screen на `androidx.core:core-splashscreen`.
- Модульные тесты декодера payload (`PayloadDecoderTest`).
- Страница «Приватность» на сайте проекта.

## [0.1.0] - 2026-10-14

Первый публичный каркас.

### Added
- Протокол: CRC-8/MAXIM, CRC-16/MODBUS, формат кадра, маскирование, опкоды.
- Работа с ключом устройства как с текстовым кодом (UTF-8), хранение в `EncryptedSharedPreferences`.
- Реальный BLE-транспорт (Nordic UART), сканирование, подключение, рукопожатие, команды unlock/lock/find.
- Декодирование ответов устройства (батарея, скорость, пробег, напряжение, прошивка, RFID).
- Демо-режим, журнал обмена, экран диагностики.
- Автоподключение и foreground-сервис с действиями unlock/lock в уведомлении.
- Опциональная анонимная телеметрия (выключена по умолчанию) и Cloudflare Worker для её приёма.
- CI: lint, unit-тесты, instrumented-тесты, сборка debug/release, сборка Pages.
- Сайт проекта, статический API (`version.json`, `features.json`, `devices.json`), Wiki.

[Unreleased]: https://github.com/bitplugg/XDDbot/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/bitplugg/XDDbot/releases/tag/v0.1.0
