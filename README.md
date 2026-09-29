<p align="center">
  <img src="docs/vola/brand/vola-mark.svg" width="120" alt="Vola logo">
</p>

<h1 align="center">Vola</h1>

<p align="center">
  <strong>A calm, private Android browser.</strong><br>
  GeckoView with Firefox extensions, Android System WebView as a lightweight alternative,
  and a design inspired by Zen Browser.
</p>

<p align="center">
  <a href="https://github.com/MikeSPL187/Browser/releases"><img alt="Release" src="https://img.shields.io/github/v/release/MikeSPL187/Browser?display_name=tag&sort=semver"></a>
  <a href="https://github.com/MikeSPL187/Browser/actions/workflows/build.yml"><img alt="Build" src="https://github.com/MikeSPL187/Browser/actions/workflows/build.yml/badge.svg"></a>
  <img alt="Android 13+" src="https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white">
  <a href="LICENSE"><img alt="License: MPL 2.0" src="https://img.shields.io/badge/License-MPL%202.0-orange.svg"></a>
</p>

> **English:** Vola is an Android browser forked from [Candy Browser](https://github.com/sk2andy/candy-browser)
> (MPL-2.0). It keeps Candy's gesture-first browsing, dual engines and local privacy tools, and adds a
> Zen-inspired design: Workspaces, Essentials, Compact Mode, Split View, themes and Glance.

## Что это

Vola — браузер для Android с упором на красоту, удобство, скорость и приватность.

- **Два движка.** GeckoView (основной, поддерживает расширения Firefox, в том числе uBlock Origin) и
  Android System WebView (лёгкая сборка). Интерфейс общий, движок — сменный адаптер.
- **Приватность по умолчанию.** Никакой телеметрии и аналитики, никаких Google Play Services.
  Блокировка рекламы и трекеров работает локально, приватные вкладки ничего не пишут на диск,
  профили изолированы друг от друга.
- **Жесты и плавность.** Плавающая адресная строка, переключение вкладок свайпом, визуальный обзор
  вкладок, пружинные анимации и тактильная отдача.
- **Телефон и планшет.** На широких экранах вкладки показываются полосой.

### В разработке (в духе Zen Browser)

| Фича | Суть |
| --- | --- |
| Workspaces | Отдельные пространства вкладок со своим названием, эмодзи и акцентным цветом |
| Essentials | Сетка закреплённых сайтов над обзором вкладок |
| Compact Mode | Страница на весь экран, панели появляются по жесту |
| Split View | Две вкладки рядом с перетаскиваемым разделителем |
| Темы | Готовые темы и редактор акцента, скруглений и плотности |
| Glance | Быстрый предпросмотр ссылки по долгому тапу |

## Установка

Нужен Android 13 или новее, 64-битный ARM (arm64-v8a) — это почти все современные телефоны и
планшеты.

- **Релизы:** скачать APK на странице [Releases](https://github.com/MikeSPL187/Browser/releases).
- **Автообновление:** добавить `https://github.com/MikeSPL187/Browser` в
  [Obtainium](https://github.com/ImranR98/Obtainium).
- **Свежие сборки:** каждый PR и коммит в `main` собирает оптимизированную сборку **Vola Preview**
  (ставится отдельным приложением рядом с релизной). Скачать можно из раздела **Artifacts**
  соответствующего запуска в [Actions](https://github.com/MikeSPL187/Browser/actions).

| APK | Движок |
| --- | --- |
| `Vola-vX.Y.Z-arm64-v8a-release.apk` | GeckoView, расширения Firefox |
| `Vola-vX.Y.Z-systemwebview-release.apk` | Android System WebView, меньше размер |

## Разработка

Проект ведётся без локального компьютера: код пишет Claude Code в облаке, собирает и проверяет
GitHub Actions ([`.github/workflows/build.yml`](.github/workflows/build.yml)). Правила проекта —
в [`CLAUDE.md`](CLAUDE.md).

Стек: Kotlin, Jetpack Compose, Material 3 Expressive, JDK 17, Gradle Kotlin DSL. Версии зависимостей —
в [`gradle/libs.versions.toml`](gradle/libs.versions.toml); Dependabot раз в неделю предлагает
обновления (GeckoView — отдельным PR, чтобы исправления безопасности движка не ждали).

```bash
./gradlew testFullDebugUnitTest lintFullDebug   # тесты и lint
./gradlew assembleFullDebug                     # debug APK (GeckoView)
./gradlew assembleSystemwebviewDebug            # debug APK (System WebView)
```

Для локальной сборки нужны Android SDK 37.1 и JDK 17. Подпись релизов и выпуск версий описаны в
[`docs/vola/signing.md`](docs/vola/signing.md).

Документация по устройству кода (наследие Candy) — в [`docs/`](docs/README.md) и
[`AGENTS.md`](AGENTS.md).

## Лицензия и благодарности

Vola распространяется по [Mozilla Public License 2.0](LICENSE).

Vola основан на [Candy Browser](https://github.com/sk2andy/candy-browser) André Naumann. Спасибо
за отличную основу! Идеи дизайна вдохновлены [Zen Browser](https://zen-browser.app); код Zen не
используется.

Сторонние компоненты и списки фильтров распространяются под своими лицензиями. Полный перечень
есть в приложении (**Настройки → О приложении**) и в
`app/src/main/assets/third_party_notices.txt`.
