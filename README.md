<p align="center">
  <img src="docs/vola/brand/vola-mark.svg" width="112" alt="Vola logo">
</p>

<h1 align="center">Vola</h1>

<p align="center">
  <strong>Красивый, быстрый и приватный браузер для Android.</strong><br>
  <sub>A calm, private Android browser with a design inspired by Zen Browser.</sub>
</p>

<p align="center">
  <img alt="Android 13+" src="https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white">
  <img alt="GeckoView" src="https://img.shields.io/badge/engine-GeckoView-FF7139?logo=firefoxbrowser&logoColor=white">
  <img alt="Material 3" src="https://img.shields.io/badge/UI-Material%203-6D4CFF?logo=materialdesign&logoColor=white">
  <a href="LICENSE"><img alt="License: MPL 2.0" src="https://img.shields.io/badge/License-MPL%202.0-22D3EE.svg"></a>
</p>

<p align="center">
  <img src="docs/vola/screenshots/new-tab.png" width="200" alt="Новая вкладка">
  &nbsp;
  <img src="docs/vola/screenshots/tabs-light.png" width="200" alt="Обзор вкладок, светлая тема">
  &nbsp;
  <img src="docs/vola/screenshots/tabs-dark.png" width="200" alt="Обзор вкладок, тёмная тема">
  &nbsp;
  <img src="docs/vola/screenshots/settings-dark.png" width="200" alt="Настройки внешнего вида">
</p>

## Что такое Vola

Vola — браузер, в котором вокруг страницы нет ничего лишнего: плавающая адресная строка,
мягкие цвета, крупные скругления и пружинные анимации. Всё в духе Zen Browser и на базе
Material 3.

- **Красота без шума.** Собственная палитра Vola, тёмная и чистая OLED-тема, Material You по желанию.
- **Приватность по умолчанию.** Никакой телеметрии, аналитики и Google Play Services. Режим
  «Только HTTPS», локальная блокировка рекламы и трекеров, приватные вкладки ничего не пишут на диск.
- **Расширения Firefox.** Движок GeckoView поддерживает расширения Firefox, uBlock Origin установлен сразу.
- **Жесты.** Переключение вкладок свайпом по адресной строке, визуальный обзор вкладок, тактильная отдача.
- **Телефон и планшет**, интерфейс на русском и английском.

### Скоро

| | |
| --- | --- |
| **Пространства** | Отдельные наборы вкладок со своим названием, эмодзи и цветом |
| **Essentials** | Закреплённые сайты над обзором вкладок |
| **Компактный режим** | Страница на весь экран, панели появляются по жесту |
| **Split View** | Две вкладки рядом |
| **Темы** | Готовые темы и редактор акцента, скруглений и плотности |
| **Glance** | Предпросмотр ссылки по долгому тапу |

## Установка

Нужен Android 13 или новее на 64-битном ARM (arm64-v8a), то есть почти любой современный
телефон или планшет.

- **Релизы** появятся на странице [Releases](https://github.com/MikeSPL187/Browser/releases).
  Для автообновления добавьте адрес этого репозитория в [Obtainium](https://github.com/ImranR98/Obtainium).
- **Свежие сборки** каждого изменения — в разделе **Artifacts** соответствующего запуска на вкладке
  [Actions](https://github.com/MikeSPL187/Browser/actions). Сборка **Vola Preview** ставится
  отдельным приложением и не мешает основной.

| Сборка | Движок |
| --- | --- |
| Vola | GeckoView, расширения Firefox |
| Vola WebView | Android System WebView, меньше размер |

## Разработка

Kotlin, Jetpack Compose, Material 3 Expressive, JDK 17. Каждое изменение собирается и проверяется
в GitHub Actions. Правила проекта — в [`CLAUDE.md`](CLAUDE.md), документация по устройству кода —
в [`docs/`](docs/).

```bash
./gradlew testFullDebugUnitTest lintFullDebug   # тесты и lint
./gradlew assembleFullDebug                     # debug APK (GeckoView)
```

## Лицензия

[Mozilla Public License 2.0](LICENSE). Сторонние компоненты и списки фильтров распространяются под
своими лицензиями; перечень есть в приложении: **Настройки → О приложении**.
